package com.example.test1;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class LoginActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "LOGIN_CHANNEL";
    private static final int NOTIFICATION_ID = 1001;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        EditText usernameEditText = findViewById(R.id.usernameEditText);
        EditText passwordEditText = findViewById(R.id.passwordEditText);

        ImageView touchImage = findViewById(R.id.touchImage);
        TextView coordinatesText = findViewById(R.id.coordinatesText);
        View rootView = findViewById(android.R.id.content);
        rootView.setOnTouchListener((v, event) -> {
            float rawX = event.getRawX();
            float rawY = event.getRawY();

            // 获取图片在屏幕上的位置
            int[] imgLoc = new int[2];
            touchImage.getLocationOnScreen(imgLoc);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    String text = String.format("屏幕坐标: (%.1f, %.1f)", rawX, rawY);
                    coordinatesText.setText(text);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    touchImage.setAlpha(1.0f);
                    break;
            }
            return true;
        });

        Button registerButton = findViewById(R.id.registerButton);
        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        TextView agreeText = findViewById(R.id.agreeText);
        CheckBox agreeCheckBox = findViewById(R.id.agreeCheckBox);
        // 设置带链接的文本
        String linkText = "同意<font color='#2196F3'><u><a href='https://cn.bing.com/'>用户协议</a></u></font>";
        agreeText.setText(Html.fromHtml(linkText, Html.FROM_HTML_MODE_LEGACY));
        agreeText.setMovementMethod(LinkMovementMethod.getInstance());
        agreeText.setOnClickListener(v -> {
            boolean isChecked = !agreeCheckBox.isChecked();
            agreeCheckBox.setChecked(isChecked);
        });
        agreeCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 这里可以处理选中状态变化的逻辑
        });

        Button loginButton = findViewById(R.id.loginButton);

        // 创建通知渠道
        createNotificationChannel();

        loginButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (!agreeCheckBox.isChecked()) {
                // 弹出对话框提醒用户
                new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this)
                        .setTitle("提示")
                        .setMessage("请先同意用户协议")
                        .setPositiveButton("同意", (dialog, which) -> {
                            // 用户选择同意，勾选 CheckBox
                            agreeCheckBox.setChecked(true);
                        })
                        .setNegativeButton("取消", (dialog, which) -> {
                            // 用户选择取消，关闭对话框
                            dialog.dismiss();
                        })
                        .show();
            } else if (username.isEmpty() || password.isEmpty()) {
                // 发送登录失败通知
                sendLoginNotification(username, password, false, "用户名或密码为空");
                Toast.makeText(LoginActivity.this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
            } else {
                String combinedText = "用户名: " + username + "+密码: " + password;
                Toast.makeText(LoginActivity.this, combinedText, Toast.LENGTH_SHORT).show();
                // 发送登录成功通知
                sendLoginNotification(username, password, true, "登录成功");
                // 设置登录状态
                LoginManager.setLoggedIn(this, true);
                LoginManager.setUsername(this, username);
                // 返回到之前的Activity
                LoginManager.returnToPreviousActivity(this);
            }
        });

        Button clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(v-> {
            usernameEditText.setText("");
            passwordEditText.setText("");
        });

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 按返回键或手势时的自定义行为
                LoginManager.returnToPreviousActivity(LoginActivity.this);
            }
        });

    }

    /**
     * 创建通知渠道（Android 8.0+需要）
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "登录通知";
            String description = "用于显示用户登录状态和信息的通知";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /*
     * 发送登录通知
     * @param username 用户名
     * @param password 密码
     * @param isSuccess 是否登录成功
     * @param message 附加消息
     */
    private void sendLoginNotification(String username, String password, boolean isSuccess, String message) {

        // 构建通知内容
        String notificationTitle = isSuccess ? "🎉 登录成功" : "❌ 登录失败";
        String notificationText;

        if (isSuccess) {
            notificationText = String.format(
                    "用户名: %s\n密码: %s\n状态: %s\n时间: %s",
                    username,
                    password,
                    message,
                    getCurrentTime()
            );
        } else {
            notificationText = String.format(
                    "用户名: %s\n状态: %s\n时间: %s",
                    username,
                    message,
                    getCurrentTime()
            );
        }

        // 构建Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(notificationTitle)
                .setContentText(isSuccess ? "点击查看详情" : "请重新尝试登录")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis());

        // 显示通知
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // 检查通知权限
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } else {
            Toast.makeText(this, "请开启通知权限以接收登录状态", Toast.LENGTH_LONG).show();
        }
    }

    /*
     * 获取当前时间字符串
     */
    private String getCurrentTime() {
        return new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
    }
}