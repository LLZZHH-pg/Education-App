package com.example.test1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class LoginActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "LOGIN_CHANNEL";
    private static final int NOTIFICATION_ID = 1001;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    // 大图片尺寸
    private static final int ORIGINAL_IMAGE_WIDTH = 228;
    private static final int ORIGINAL_IMAGE_HEIGHT = 180;
    private float startX=0f;
    private float startY=0f;

    // 可活动区域参数
    private static final int ACTIVITY_AREA_CENTER_X = 122; // 中心点X相对图片左上角坐标
    private static final int ACTIVITY_AREA_CENTER_Y = 70;  // 中心点Y相对图片左上角坐标
    private static final int ACTIVITY_AREA_WIDTH = 30;    // 可活动区域宽度
    private static final int ACTIVITY_AREA_HEIGHT = 31;  // 可活动区域高度
    private float activityAreaCenterX;
    private float activityAreaCenterY;


    // 小图片尺寸
    private static final int SMALL_IMAGE_WIDTH = 56;
    private static final int SMALL_IMAGE_HEIGHT = 63;
    private float touchimage2Width=SMALL_IMAGE_WIDTH;
    private float touchimage2Height=SMALL_IMAGE_HEIGHT;


    // 屏幕尺寸
    private int screenWidth;
    private int screenHeight;
    private float screenCenterX;
    private float screenCenterY;

    // 缩放系数和映射关系
    private float touchimageScale = 1.0f;
    private float screenScaleX=1.0f;
    private float screenScaleY=1.0f;

    // View引用
    private ImageView touchImage;
    private ImageView touchImage2;
    private FrameLayout imageContainer;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) {
                        Toast.makeText(this, "需要通知权限以显示登录状态", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        checkNotificationPermission();

        touchImage = findViewById(R.id.touchImage);
        touchImage2 = findViewById(R.id.touchImage2);
        imageContainer = findViewById(R.id.imageContainer);

        EditText usernameEditText = findViewById(R.id.usernameEditText);
        EditText passwordEditText = findViewById(R.id.passwordEditText);

        View rootView = findViewById(android.R.id.content);
        rootView.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                case android.view.MotionEvent.ACTION_MOVE: {
                    float rawX = event.getRawX();
                    float rawY = event.getRawY();
                    updateSmallImagePosition(rawX, rawY);
                    break;
                }
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL: {
                    resetSmallImagePosition();
                    break;
                }
                default:
                    break;
            }
            return true;
        });

        imageContainer.post(this::initializeSmallImagePosition);

        Button registerButton = findViewById(R.id.registerButton);
        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        TextView agreeText = findViewById(R.id.agreeText);
        CheckBox agreeCheckBox = findViewById(R.id.agreeCheckBox);
        String linkText = "同意<font color='#2196F3'><u><a href='https://cn.bing.com/'>用户协议</a></u></font>";
        agreeText.setText(Html.fromHtml(linkText, Html.FROM_HTML_MODE_LEGACY));
        agreeText.setMovementMethod(LinkMovementMethod.getInstance());
        agreeText.setOnClickListener(v -> {
            boolean isChecked = !agreeCheckBox.isChecked();
            agreeCheckBox.setChecked(isChecked);
        });
        agreeCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
        });

        Button loginButton = findViewById(R.id.loginButton);

        createNotificationChannel();

        loginButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (!agreeCheckBox.isChecked()) {
                new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this)
                        .setTitle("提示")
                        .setMessage("请先同意用户协议")
                        .setPositiveButton("同意", (dialog, which) -> {
                            agreeCheckBox.setChecked(true);
                        })
                        .setNegativeButton("取消", (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .show();
            } else if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
            } else {
                if (!LoginManager.userExists(this, username)) {
                    new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this)
                            .setTitle("账号不存在")
                            .setMessage("未找到该账号，是否前往注册？")
                            .setPositiveButton("注册", (dialog, which) -> {
                                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                                startActivity(intent);
                            })
                            .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                            .show();
                    return;
                }

                boolean valid = LoginManager.validateUser(this,username, password);
                if (!valid) {
                    new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this)
                            .setTitle("登录失败")
                            .setMessage("用户名或密码错误")
                            .setPositiveButton("好的", (dialog, which) -> dialog.dismiss())
                            .show();
                    return;
                }

                sendLoginNotification(username, true, "登录成功");

                LoginManager.setSubjects(this, username);
                LoginManager.setLoggedIn(this, true);
                LoginManager.setUsername(this, username);
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
                LoginManager.returnToPreviousActivity(LoginActivity.this);
            }
        });

    }


    /**
     * 初始化小图片位置
     */
    private void initializeSmallImagePosition() {
        // 计算图片缩放比例和映射系数
        calculateImageScale();
        // 重置小图片到可活动区域中心
        resetSmallImagePosition();
    }
    /**
     * 计算图片缩放比例和映射系数
     */
    private void calculateImageScale() {
        // 获取屏幕尺寸
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
        screenCenterX = screenWidth / 2f;
        screenCenterY = screenHeight / 2f;

        // 获取大图片的显示尺寸计算图片的缩放比例
        float touchimageScaleX = (float) touchImage.getWidth() / ORIGINAL_IMAGE_WIDTH;
        float touchimageScaleY = (float) touchImage.getHeight() / ORIGINAL_IMAGE_HEIGHT;
        touchimageScale = Math.min(touchimageScaleX, touchimageScaleY);

        //计算活动区域中心点位置
        startX= (touchImage.getWidth() - ORIGINAL_IMAGE_WIDTH*touchimageScale) / 2f;
        startY= (touchImage.getHeight() - ORIGINAL_IMAGE_HEIGHT*touchimageScale) / 2f;
        activityAreaCenterX= ACTIVITY_AREA_CENTER_X * touchimageScale + startX;
        activityAreaCenterY= ACTIVITY_AREA_CENTER_Y * touchimageScale + startY;

        // 设置小图片的大小
        touchimage2Width=touchimage2Width*touchimageScale;
        touchimage2Height=touchimage2Height*touchimageScale;
        ViewGroup.LayoutParams lp = touchImage2.getLayoutParams();
        lp.width = Math.round(touchimage2Width);
        lp.height = Math.round(touchimage2Height);
        touchImage2.setLayoutParams(lp);

        //计算屏幕X,Y映射比例
        screenScaleX=ACTIVITY_AREA_WIDTH*touchimageScale/screenWidth;
        screenScaleY=ACTIVITY_AREA_HEIGHT*touchimageScale/screenHeight;

    }
    /**
     * 重置小图片到可活动区域中心
     */
    private void resetSmallImagePosition() {
        float newX= activityAreaCenterX - touchimage2Width / 2;
        float newY= activityAreaCenterY - touchimage2Height / 2;
        // 更新小图片位置
        touchImage2.setX(newX);
        touchImage2.setY(newY);
    }
    /**
     * 更新小图片位置
     */
    private void updateSmallImagePosition(float touchX, float touchY) {
        // 计算触摸点相对于屏幕中心的偏移
        float newX = (touchX - screenCenterX) * screenScaleX + activityAreaCenterX - touchimage2Width / 2;
        float newY = (touchY - screenCenterY) * screenScaleY + activityAreaCenterY - touchimage2Height / 2;

        // 更新小图片位置
        touchImage2.setX(newX);
        touchImage2.setY(newY);
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
    private void sendLoginNotification(String username, boolean isSuccess, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            checkNotificationPermission();
            Toast.makeText(this, "请先授予通知权限", Toast.LENGTH_SHORT).show();
            return;
        }

        // 构建通知内容
        String notificationTitle = isSuccess ? "🎉 登录成功" : "❌ 登录失败";
        String notificationText;

        if (isSuccess) {
            notificationText = String.format(
                    "用户名: %s\n状态: %s\n时间: %s",
                    username,
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
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
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