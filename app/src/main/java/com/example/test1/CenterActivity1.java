package com.example.test1;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;

public class CenterActivity1 extends Fragment {
    private LinearLayout chartContainer;
    private ScoreDatabaseHelper dbHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page1, container, false);
        chartContainer = view.findViewById(R.id.chart_container);

        // 1. 初始化数据库
        String username = LoginManager.getUsername(requireContext());
        List<String> subjects = LoginManager.getSubjectsList(requireContext());
        dbHelper = new ScoreDatabaseHelper(requireContext(), username, subjects);

        // 2. 绘制图表
        setupCharts(subjects);

        return view;
    }

    private void setupCharts(List<String> subjects) {
        chartContainer.removeAllViews();

        // 关键：从数据库获取数据。由于默认从上到下是旧到新，
        // 我们需要按 ID 升序读取，以保证 X 轴从左到右是时间正序。
        Cursor cursor = dbHelper.getWritableDatabase().query(
                "score_table", null, null, null, null, null, "id ASC");

        if (cursor == null || cursor.getCount() == 0) {
            TextView tv = new TextView(getContext());
            tv.setText("暂无成绩数据，请先到“储存”页面导入 Excel");
            chartContainer.addView(tv);
            return;
        }

        // 提取考试名称（X轴标签）
        List<String> examNames = new ArrayList<>();
        while (cursor.moveToNext()) {
            examNames.add(cursor.getString(cursor.getColumnIndexOrThrow("exam_name")));
        }

        // 为每个学科画一张图
        for (String subject : subjects) {
            List<Entry> entries = new ArrayList<>();
            cursor.moveToFirst();
            int index = 0;
            do {
                float score = cursor.getFloat(cursor.getColumnIndexOrThrow(subject));
                entries.add(new Entry(index++, score));
            } while (cursor.moveToNext());

            // 创建并配置图表
            addChartToLayout(subject, entries, examNames);
        }
        cursor.close();
    }

    private void addChartToLayout(String subjectName, List<Entry> entries, List<String> examNames) {
        // 标题
        TextView title = new TextView(getContext());
        title.setText(subjectName + " 成绩走势");
        title.setTextSize(18f);
        title.setPadding(0, 40, 0, 10);
        chartContainer.addView(title);

        // 图表对象
        LineChart chart = new LineChart(getContext());
        chart.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 600)); // 高度 600px

        // 数据集设置
        LineDataSet dataSet = new LineDataSet(entries, subjectName);
        dataSet.setColor(Color.BLUE);
        dataSet.setCircleColor(Color.RED);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(true); // 显示具体分数文本
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // 使线条圆滑

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        // X 轴配置
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < examNames.size()) {
                    return examNames.get(index);
                }
                return "";
            }
        }); // 设置考试名为标签
        xAxis.setLabelRotationAngle(-45); // 考试名太长时旋转角度

        // 通用美化
        chart.getDescription().setEnabled(false); // 隐藏右下角描述
        chart.getAxisRight().setEnabled(false); // 隐藏右侧坐标轴
        chart.animateX(1000); // 载入动画
        chart.invalidate(); // 刷新

        chartContainer.addView(chart);
    }
}