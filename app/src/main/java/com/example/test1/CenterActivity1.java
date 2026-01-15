package com.example.test1;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
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

        String username = LoginManager.getUsername(requireContext());
        List<String> subjects = LoginManager.getSubjectsList(requireContext());
        dbHelper = new ScoreDatabaseHelper(requireContext(), username, subjects);

        setupCharts(subjects);

        return view;
    }

    private void setupCharts(List<String> subjects) {
        chartContainer.removeAllViews();

        Cursor cursor = dbHelper.getWritableDatabase().query(
                "score_table", null, null, null, null, null, "id ASC");

        if (cursor == null || cursor.getCount() == 0) {
            TextView tv = new TextView(getContext());
            tv.setText("暂无成绩数据，请先到“储存”页面导入 Excel");
            chartContainer.addView(tv);
            return;
        }

        List<String> examNames = new ArrayList<>();
        while (cursor.moveToNext()) {
            examNames.add(cursor.getString(cursor.getColumnIndexOrThrow("exam_name")));
        }

        for (String subject : subjects) {
            List<Entry> entries = new ArrayList<>();
            cursor.moveToFirst();
            int index = 0;
            do {
                float score = cursor.getFloat(cursor.getColumnIndexOrThrow(subject));
                entries.add(new Entry(index++, score));
            } while (cursor.moveToNext());

            addChartToLayout(subject, entries, examNames);
        }
        cursor.close();
    }

    private void addChartToLayout(String subjectName, List<Entry> entries, List<String> examNames) {
        int textColor = getDynamicColor();
        int themeColor = ContextCompat.getColor(requireContext(), R.color.my_light_primary);
        // 标题
        TextView title = new TextView(getContext());
        title.setText(subjectName + " 成绩走势");
        title.setTextColor(textColor);
        title.setTextSize(18f);
        title.setPadding(0, 40, 0, 10);
        chartContainer.addView(title);

        LineChart chart = new LineChart(getContext());
        chart.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 600)); // 高度 600px

        LineDataSet dataSet = new LineDataSet(entries, subjectName);
        dataSet.setColor(themeColor);
        dataSet.setCircleColor(Color.RED);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextColor(textColor);
        dataSet.setDrawValues(true);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(textColor);
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
        });
        xAxis.setLabelRotationAngle(-45);

        chart.getAxisLeft().setTextColor(textColor);
        chart.getLegend().setTextColor(textColor);

        chart.getLegend().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.animateX(700);
        chart.invalidate(); // 刷新

        chartContainer.addView(chart);
    }
    private int getDynamicColor() {
        int nightModeFlags = getContext().getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            return Color.WHITE;
        }
        return Color.BLACK;
    }
}