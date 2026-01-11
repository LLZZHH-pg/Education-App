package com.example.test1;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ScoreAdapter extends RecyclerView.Adapter<ScoreAdapter.ViewHolder> {

    private Cursor cursor;
    private List<String> subjects;

    public ScoreAdapter(Cursor cursor, List<String> subjects) {
        this.cursor = cursor;
        this.subjects = subjects;
    }

    public void swapCursor(Cursor newCursor) {
        if (cursor != null) cursor.close();
        cursor = newCursor;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 使用内置的简单布局或自定义 item_score.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (cursor != null && cursor.moveToPosition(position)) {
            String examName = cursor.getString(cursor.getColumnIndexOrThrow("exam_name"));
            String time = cursor.getString(cursor.getColumnIndexOrThrow("upload_time"));

            holder.text1.setText(examName + " (" + time + ")");

            // 动态拼接当前用户关注的学科成绩
            StringBuilder scoreDetail = new StringBuilder();
            for (String subject : subjects) {
                int colIndex = cursor.getColumnIndex(subject);
                if (colIndex != -1) {
                    String val = cursor.getString(colIndex);
                    scoreDetail.append(subject).append(": ").append(val != null ? val : "0").append("  ");
                }
            }
            holder.text2.setText(scoreDetail.toString().trim());
        }
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;
        ViewHolder(View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }
}