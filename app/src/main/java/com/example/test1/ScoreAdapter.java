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
    private OnItemLongClickListener longClickListener;

    public ScoreAdapter(Cursor cursor, List<String> subjects) {
        this.cursor = cursor;
        this.subjects = subjects;
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(long id, String examName);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void swapCursor(Cursor newCursor) {
        if (cursor != null) cursor.close();
        cursor = newCursor;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 使用自定义的 item_score 布局
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_score, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (cursor != null && cursor.moveToPosition(position)) {
            // 1. 考试名
            long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
            String examName = cursor.getString(cursor.getColumnIndexOrThrow("exam_name"));
            holder.tvExamName.setText(examName);

            // 2. 时间 (只截取日期部分 yyyy-MM-dd，如果太长的话)
            String fullTime = cursor.getString(cursor.getColumnIndexOrThrow("upload_time"));
            holder.tvUploadTime.setText(fullTime.split(" ")[0]);

            holder.tvExamName.setText(examName);

            // 3. 动态拼接学科成绩
            StringBuilder sb = new StringBuilder();
            for (String subject : subjects) {
                int colIndex = cursor.getColumnIndex(subject);
                if (colIndex != -1) {
                    double score = cursor.getDouble(colIndex);
                    // 格式化：如果是整数则不显示小数点
                    String scoreStr = (score == (long) score)
                            ? String.valueOf((long) score)
                            : String.valueOf(score);
                    sb.append(subject).append(":").append(scoreStr).append(" ");
                }
            }
            holder.tvSubjectsScores.setText(sb.toString().trim());

            holder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(id, examName);
                }
                return true; // 表示消费了长按事件
            });
        }
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvExamName, tvUploadTime, tvSubjectsScores;
        ViewHolder(View itemView) {
            super(itemView);
            tvExamName = itemView.findViewById(R.id.tv_exam_name);
            tvUploadTime = itemView.findViewById(R.id.tv_upload_time);
            tvSubjectsScores = itemView.findViewById(R.id.tv_subjects_scores);
        }
    }
}