package com.example.test1;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.List;

public class ScoreDatabaseHelper extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;
    private String tableName;
    private List<String> subjects;

    // 数据库名建议以用户名为区分，或者用统一数据库不同表名
    // 这里采用：一个用户一个数据库，库名为 scores_{username}.db
    public ScoreDatabaseHelper(Context context, String username, List<String> subjects) {
        super(context, "scores_" + username + ".db", null, DB_VERSION);
        this.tableName = "score_table";
        this.subjects = subjects;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(tableName).append(" (");
        sql.append("id INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sql.append("upload_time TEXT, ");
        sql.append("exam_name TEXT, "); // 考试名（文件名）

        // 动态添加学科列
        for (int i = 0; i < subjects.size(); i++) {
            sql.append("\"").append(subjects.get(i)).append("\" REAL");
            if (i < subjects.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(")");

        db.execSQL(sql.toString());
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 后续实现修改学科时的 ALTER TABLE 逻辑
    }

    // 查询所有数据，按新到旧排序
    public Cursor getAllScores() {
        SQLiteDatabase db = this.getReadableDatabase();
        // 根据 id 降序排列，即为最后插入的在最前面
        return db.query(tableName, null, null, null, null, null, "id DESC");
    }
}
