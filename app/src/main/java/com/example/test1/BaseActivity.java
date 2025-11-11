package com.example.test1;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 保存当前Activity信息
        String clsName = this.getClass().getName();
        if (!"com.example.test1.LoginActivity".equals(clsName)
                && !"com.example.test1.RegisterActivity".equals(clsName)) {
            LoginManager.saveLastActivity(this, clsName);
        }
    }

    /**
     * 统一的登录跳转方法
     */
    protected void navigateToLogin() {
        LoginManager.navigateToLogin(this);
    }

    /**
     * 检查登录状态
     */
    protected boolean isUserLoggedIn() {
        return LoginManager.isLoggedIn(this);
    }
}
