// java
package com.example.test1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class CenterActivity2 extends Fragment {
    private Spinner spinner;
    private TextView tvResponse;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String delta = intent.getStringExtra("response");


            if (delta != null && !delta.isEmpty()) {
                // 追加流式内容
                tvResponse.append(delta);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_page2, container, false);
        spinner = v.findViewById(R.id.subject_spinner);
        tvResponse = v.findViewById(R.id.tv_ai_response);
        Button btn = v.findViewById(R.id.btn_ai_assist);

        // 加载下拉菜单数据
        List<String> subjects = new ArrayList<>();
        subjects.add("全部");
        subjects.addAll(LoginManager.getSubjectsList(requireContext()));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, subjects);
        spinner.setAdapter(adapter);

        btn.setOnClickListener(view -> {
            tvResponse.setText("AI 正在思考中...");
            Intent intent = new Intent(getContext(), AIService.class);
            intent.putExtra("subject", spinner.getSelectedItem().toString());
            requireContext().startService(intent); // 启动 Service
        });

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        // 统一使用 LocalBroadcastManager 注册
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(receiver, new IntentFilter(AIService.ACTION_AI_REPLY));
    }
    @Override
    public void onStop() {
        super.onStop();
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(receiver);
    }
}