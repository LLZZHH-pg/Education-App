package com.example.test1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LoginManager {
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String AI_PREF_NAME = "ai_cache";
    private static final String KEY_LAST_ACTIVITY = "last_activity";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_SUBJECTS = "user_subjects";

    public static void navigateToLogin(Activity currentActivity) {
        String currentActivityName = currentActivity.getClass().getName();
        saveLastActivity(currentActivity, currentActivityName);

        Intent intent = new Intent(currentActivity, LoginActivity.class);
        currentActivity.startActivity(intent);

        currentActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

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
                Intent intent = new Intent(loginActivity, MainActivity.class);
                loginActivity.startActivity(intent);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Intent intent = new Intent(loginActivity, MainActivity.class);
            loginActivity.startActivity(intent);
        }
        clearLastActivity(loginActivity);
        loginActivity.finish();
    }

    public static void saveLastActivity(Context context, String activityName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LAST_ACTIVITY, activityName).apply();
    }

    private static String getLastActivity(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LAST_ACTIVITY, "");
    }


    public static boolean userExists(Context context, String username) {
        UserDatabaseHelper dbHelper = new UserDatabaseHelper(context);
        return dbHelper.checkUserExists(username);
    }

    public static boolean validateUser(Context context, String username, String password) {
        UserDatabaseHelper dbHelper = new UserDatabaseHelper(context);
        return dbHelper.validateLogin(username, password);
    }


    public static void setLoggedIn(Context context, boolean isLoggedIn) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply();
    }

    public static boolean isLoggedIn(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static void setUsername(Context context, String username) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_USERNAME, username).apply();
    }

    public static String getUsername(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_USERNAME, "");
    }

    public static void setSubjects(Context context, String username) {
        UserDatabaseHelper dbHelper = new UserDatabaseHelper(context);
        String subjects = dbHelper.getUserSubjects(username);
        if(!(subjects == null || subjects.isEmpty())){
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_SUBJECTS, subjects).apply();
        }

    }

    public static List<String> getSubjectsList(Context context) {

        String subjects = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
                .getString(KEY_SUBJECTS, "");
        if(subjects == null || subjects.isEmpty()){
            return new ArrayList<>();
        }else {
            return new ArrayList<>(Arrays.asList(subjects.split(" ")));
        }
    }

    public static void clearLastActivity(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_LAST_ACTIVITY).apply();
    }

    public static void logout(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        context.getSharedPreferences(AI_PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
}