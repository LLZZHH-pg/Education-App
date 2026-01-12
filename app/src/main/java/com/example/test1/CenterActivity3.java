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
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);
            String fileName = getFileName(uri); // 获取考试名

            // 1. 获取数据库连接
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // 2. 检查文件名是否已经存在
            boolean isUpdate = false;
            long existingId = -1;
            Cursor checkCursor = db.query(dbHelper.getTableName(), new String[]{"id"},
                    "exam_name = ?", new String[]{fileName}, null, null, null);

            if (checkCursor != null && checkCursor.getCount() > 0) {
                checkCursor.moveToFirst();
                existingId = checkCursor.getLong(0);
                isUpdate = true;
                checkCursor.close();
                Toast.makeText(getContext(), "上传同名文件，将进行数据更新", Toast.LENGTH_SHORT).show();
            }

            Row headerRow = sheet.getRow(0);
            Map<String, Integer> headerMap = new HashMap<>();
            int nameIndex = -1;

            // 解析表头
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String header = getSafeString(headerRow.getCell(i));
                if ("姓名".equals(header)) nameIndex = i;
                headerMap.put(header, i);
            }

            if (nameIndex == -1) {
                Toast.makeText(getContext(), "未找到'姓名'列", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> subjects = LoginManager.getSubjectsList(requireContext());
            boolean foundUser = false;

            // 遍历行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String rowName = getSafeString(row.getCell(nameIndex));
                if (currentUsername.equals(rowName)) {
                    foundUser = true;
                    ContentValues values = new ContentValues();

                    // 3. 填充学科成绩 (REAL 类型)
                    for (String subject : subjects) {
                        Integer colIndex = headerMap.get(subject);
                        if (colIndex != null) {
                            values.put("\"" + subject + "\"", getCellValue(row.getCell(colIndex)));
                        }
                    }

                    if (isUpdate) {
                        // 仅更新分数，不改变 exam_name 和 upload_time
                        db.update(dbHelper.getTableName(), values, "id = ?", new String[]{String.valueOf(existingId)});
                    } else {
                        // 新增数据，添加时间、文件名和分数
                        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                        values.put("upload_time", currentTime);
                        values.put("exam_name", fileName);
                        db.insert(dbHelper.getTableName(), null, values);
                    }
                    break; // 找到当前用户后跳出循环
                }
            }

            if (!foundUser) {
                Toast.makeText(getContext(), "Excel中未找到当前用户的数据", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), isUpdate ? "数据已更新" : "导入完成", Toast.LENGTH_SHORT).show();
                refreshData();
            }

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

            // 设置长按删除逻辑
            adapter.setOnItemLongClickListener((id, examName) -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("删除确认")
                        .setMessage("确定要删除 '" + examName + "' 的成绩吗？")
                        .setPositiveButton("删除", (dialog, which) -> {
                            // 1. 从数据库删除
                            dbHelper.deleteScoreById(id);
                            // 2. 刷新列表
                            refreshData();
                            Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });

            recyclerView.setAdapter(adapter);
        } else {
            // 传入新查询的 Cursor 并通知 UI 更新
            adapter.swapCursor(cursor);
        }
    }
}