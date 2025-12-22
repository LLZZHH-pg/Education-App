// LoginManager.java
package com.example.test1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class LoginManager {
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_LAST_ACTIVITY = "last_activity";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USERNAME = "username";


    // 简单用户数据模型
    private static class User {
        String username;
        String password;
        User(String u, String p) {
            this.username = u;
            this.password = p;
        }
    }
    // 预置用户列表，仅用于本地测试
    private static final List<User> PRESET_USERS = new ArrayList<>();
    static {
        PRESET_USERS.add(new User("student1", "123456"));
        PRESET_USERS.add(new User("teacher1", "abc123"));
        PRESET_USERS.add(new User("admin", "admin123"));
    }

    /**
     * 保存当前Activity信息，然后跳转到登录页
     */
    public static void navigateToLogin(Activity currentActivity) {
        // 保存当前Activity的类名
        String currentActivityName = currentActivity.getClass().getName();
        saveLastActivity(currentActivity, currentActivityName);

        // 跳转到登录页
        Intent intent = new Intent(currentActivity, LoginActivity.class);
        currentActivity.startActivity(intent);

        // 添加过渡动画（可选）
        currentActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * 登录成功后返回到之前的Activity
     */
    public static void returnToPreviousActivity(Activity loginActivity) {
        String lastActivityName = getLastActivity(loginActivity);

        try {
            if (lastActivityName != null && !lastActivityName.isEmpty()) {
                // 通过反射创建之前的Activity实例
                Class<?> previousActivityClass = Class.forName(lastActivityName);
                Intent intent = new Intent(loginActivity, previousActivityClass);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                loginActivity.startActivity(intent);
            } else {
                // 如果没有保存的Activity，默认回到MainActivity
                Intent intent = new Intent(loginActivity, MainActivity.class);
                loginActivity.startActivity(intent);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            // 如果类找不到，回到MainActivity
            Intent intent = new Intent(loginActivity, MainActivity.class);
            loginActivity.startActivity(intent);
        }
        clearLastActivity(loginActivity);
        // 结束登录Activity
        loginActivity.finish();
    }

    /**
     * 保存最后的Activity信息
     */
    public static void saveLastActivity(Context context, String activityName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LAST_ACTIVITY, activityName).apply();
    }

    /**
     * 获取最后保存的Activity信息
     */
    private static String getLastActivity(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LAST_ACTIVITY, "");
    }


    // \*\*业务：仅检查账号是否存在\*\*
    public static boolean userExists(String username) {
        for (User user : PRESET_USERS) {
            if (user.username.equals(username)) {
                return true;
            }
        }
        return false;
    }

    // \*\*业务：检查账号和密码是否匹配\*\*
    public static boolean validateUser(String username, String password) {
        for (User user : PRESET_USERS) {
            if (user.username.equals(username) && user.password.equals(password)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 设置登录状态
     */
    public static void setLoggedIn(Context context, boolean isLoggedIn) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply();
    }

    /**
     * 获取登录状态
     */
    public static boolean isLoggedIn(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * 设置用户名
     */
    public static void setUsername(Context context, String username) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    /**
     * 获取用户名
     */
    public static String getUsername(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USERNAME, "");
    }

    /**
     * 清除保存的Activity信息
     */
    public static void clearLastActivity(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_LAST_ACTIVITY).apply();
    }

    /**
     * 退出登录，清除登录状态和用户名
     */
    public static void logout(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .putString(KEY_USERNAME, "")
                .apply();
    }
}