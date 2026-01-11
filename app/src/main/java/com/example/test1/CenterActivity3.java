package com.example.test1;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Date;

public class CenterActivity3 extends Fragment {
    private ScoreDatabaseHelper dbHelper;
    private String currentUsername;
    private RecyclerView recyclerView;
    private ScoreAdapter adapter;

    // 注册文件选择器
    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    processExcelFile(uri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_page3, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化基础信息
        currentUsername = LoginManager.getUsername(requireContext());
        List<String> subjects = LoginManager.getSubjectsList(requireContext());
        dbHelper = new ScoreDatabaseHelper(requireContext(), currentUsername, subjects);

        // 设置 RecyclerView
        recyclerView = view.findViewById(R.id.rv_score_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 绑定选择文件按钮逻辑
        view.findViewById(R.id.btn_select_excel).setOnClickListener(v -> {
            filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        });

        // 初始加载数据
        refreshData();
    }

    private void processExcelFile(Uri uri) {
        String fileName = getFileName(uri);
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0); // 标题行

            // 1. 找到“姓名”列索引和各学科在Excel中的索引
            int nameIndex = -1;
            Map<String, Integer> excelSubjectMap = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String header = getSafeString(headerRow.getCell(i));
                if ("姓名".equals(header)) nameIndex = i;
                excelSubjectMap.put(header, i);
            }

            // 2. 遍历数据行
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String rowName = getSafeString(row.getCell(nameIndex));

                // 3. 匹配当前登录用户名
                if (currentUsername.equals(rowName)) {
                    ContentValues values = new ContentValues();
                    values.put("upload_time", currentTime);
                    values.put("exam_name", fileName);

                    // 4. 比较数据库列和文件列
                    List<String> dbSubjects = LoginManager.getSubjectsList(requireContext()); // 应从配置获取
                    for (String subject : dbSubjects) {
                        if (excelSubjectMap.containsKey(subject)) {
                            // 文件中有，取值
                            Cell cell = row.getCell(excelSubjectMap.get(subject));
                            values.put("\"" + subject + "\"", getCellValue(cell));
                        } else {
                            // 文件中没有，存 0.0
                            values.put("\"" + subject + "\"", 0.0);
                        }
                    }
                    db.insert(dbHelper.getTableName(), null, values);
                }
            }
            Toast.makeText(getContext(), "导入完成", Toast.LENGTH_SHORT).show();
            refreshData();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 获取单元格字符串工具方法
    private Double getCellValue(Cell cell) {
        if (cell == null) return 0.0;
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    String val = cell.getStringCellValue().trim();
                    if (val.isEmpty()) return 0.0;
                    return Double.parseDouble(val);
                default:
                    return 0.0;
            }
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    // 获取单元格安全字符串的工具方法
    private String getSafeString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // 如果是数字，通过 DataFormatter 格式化或者直接转 String
                // 防止 123 变成 123.0，我们转为长整型再转字符串
                double numericValue = cell.getNumericCellValue();
                if (numericValue == (long) numericValue) {
                    return String.valueOf((long) numericValue);
                } else {
                    return String.valueOf(numericValue);
                }
            default:
                return "";
        }
    }
    // 获取文件名的工具方法
    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        return result != null ? result : "unknown_file";
    }



    private void refreshData() {
        Cursor cursor = dbHelper.getAllScores();
        List<String> subjects = LoginManager.getSubjectsList(requireContext());

        if (adapter == null) {
            adapter = new ScoreAdapter(cursor, subjects);
            recyclerView.setAdapter(adapter);
        } else {
            // 传入新查询的 Cursor 并通知 UI 更新
            adapter.swapCursor(cursor);
        }
    }
}