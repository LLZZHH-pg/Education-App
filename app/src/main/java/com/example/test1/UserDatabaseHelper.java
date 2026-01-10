package com.example.test1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.security.MessageDigest;

public class UserDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "UserSystem.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_USERS = "users";

    public UserDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT, birthday TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // 密码加密逻辑 (SHA-256)
    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return password; // 回退方案
        }
    }

    // 插入新用户
    public boolean registerUser(String username, String password, String birthday) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", hashPassword(password));
        values.put("birthday", birthday);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    // 检查用户是否存在
    public boolean checkUserExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, "username=?", new String[]{username}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // 验证登录
    public boolean validateLogin(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String hashedPassword = hashPassword(password);
        Cursor cursor = db.query(TABLE_USERS, null, "username=? AND password=?", new String[]{username, hashedPassword}, null, null, null);
        boolean valid = cursor.getCount() > 0;
        cursor.close();
        return valid;
    }
}