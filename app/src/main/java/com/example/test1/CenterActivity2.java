// java
package com.example.test1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class CenterActivity2 extends Fragment {
    private Spinner spinner;
    private TextView tvResponse;


    private boolean waitingFirstChunk = false;
    private static final String PREF_NAME = "ai_cache";
    private static final String KEY_AI_TEXT = "ai_text_accumulated";
    private static final String KEY_SELECTED_SUBJECT = "selected_subject";
    private static final String KEY_IS_REQUESTING = "is_requesting";
    private static final String DEFAULT_HINT = "AI 正在思考中...";
    private SharedPreferences prefs;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String delta = intent.getStringExtra("response");
            if (delta == null || delta.isEmpty()) return;

            prefs.edit().putBoolean(KEY_IS_REQUESTING, false).apply();

            // 把分片追加到缓存（保证后台也能持续累计）
            String old = prefs.getString(KEY_AI_TEXT, "");
            String merged = (old == null ? "" : old) + delta;
            prefs.edit().putString(KEY_AI_TEXT, merged).apply();

            if (tvResponse == null) return;

            // 仅当本次是用户新请求的首包，才把占位提示替换掉
            if (waitingFirstChunk) {
                CharSequence cur = tvResponse.getText();
                if (cur != null && DEFAULT_HINT.contentEquals(cur)) {
                    tvResponse.setText("");
                    Toast.makeText(context, "已清空", Toast.LENGTH_SHORT).show();
                }
                waitingFirstChunk = false;
            } else {
                // 非新请求首包：如果当前只是占位提示，但缓存已有内容，则直接切换到缓存内容
                CharSequence cur = tvResponse.getText();
                if (cur != null && DEFAULT_HINT.contentEquals(cur) && merged.length() > 0) {
                    tvResponse.setText(merged);
                    return;
                }
            }
            tvResponse.append(delta);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_page2, container, false);
        prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        spinner = v.findViewById(R.id.subject_spinner);
        tvResponse = v.findViewById(R.id.tv_ai_response);
        Button btn = v.findViewById(R.id.btn_ai_assist);

        List<String> subjects = new ArrayList<>();
        subjects.add("全部");
        subjects.addAll(LoginManager.getSubjectsList(requireContext()));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, subjects);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        String savedSubject = prefs.getString(KEY_SELECTED_SUBJECT, null);
        if (savedSubject != null) {
            int idx = subjects.indexOf(savedSubject);
            if (idx >= 0) {
                spinner.setSelection(idx, false);
            }
        }
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                Object item = spinner.getSelectedItem();
                if (item != null) {
                    prefs.edit().putString(KEY_SELECTED_SUBJECT, item.toString()).apply();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // no-op
            }
        });

        String cached = prefs.getString(KEY_AI_TEXT, "");
        boolean isRequesting = prefs.getBoolean(KEY_IS_REQUESTING, false);

        if (cached != null && !cached.isEmpty()) {
            tvResponse.setText(cached); // 有内容就直接显示，不显示占位提示
            waitingFirstChunk = false;
        }else if (isRequesting) {
            tvResponse.setText(DEFAULT_HINT);
            waitingFirstChunk = true;
        }
        btn.setOnClickListener(view -> {
            prefs.edit()
                    .remove(KEY_AI_TEXT)
                    .putBoolean(KEY_IS_REQUESTING, true)
                    .apply();
            waitingFirstChunk = true;
            tvResponse.setText(DEFAULT_HINT);

            Intent intent = new Intent(getContext(), AIService.class);
            intent.putExtra("subject", spinner.getSelectedItem().toString());
            requireContext().startService(intent);
        });

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(receiver, new IntentFilter(AIService.ACTION_AI_REPLY));
    }
    @Override
    public void onStop() {
        super.onStop();
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(receiver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tvResponse = null;
        spinner = null;
    }

    @Override
    public void onSaveInstanceState(@androidx.annotation.NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (tvResponse != null) {
            outState.putString("ai_content", tvResponse.getText().toString());
        }
    }

}