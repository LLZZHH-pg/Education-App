// java
package com.example.test1;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class MainActivity extends BaseActivity implements SideFragment.OnSidebarItemClickListener{

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_DARK_MODE = "dark_mode";
    private boolean isSidebarOpen = false;
    private Button menuButton;
    private int currentNavId = R.id.nav_page1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        menuButton = findViewById(R.id.menu_button);
        updateMenuButton(); // 初始化菜单按钮状态

        // 初始化底部导航监听
        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        navView.setOnItemSelectedListener(item -> {
            currentNavId = item.getItemId();
            updateToolbarTitle(item.getTitle().toString());
            refreshFragment(); // 切换 Tab 时触发逻辑判断
            return true;
        });

        // 初始加载第一个页面
        refreshFragment();

        findViewById(R.id.fragment_container).setOnClickListener(v -> {
            if (isSidebarOpen) {
                closeSidebar();
            }
        });
    }

    /**
     * 核心逻辑：刷新 Fragment。
     * 1. 检查登录状态。
     * 2. 未登录则显示 EmptyActivity。
     * 3. 已登录则显示对应的 CenterFragment，并尝试初始化该用户的成绩数据库。
     */
    private void refreshFragment() {
        boolean loggedIn = LoginManager.isLoggedIn(this);
        Fragment targetFragment;

        if (!loggedIn) {
            // 未登录：强制显示占位 Fragment
            targetFragment = new EmptyActivity();
        } else {
            // 已登录：触发成绩数据库初始化
            checkAndInitUserDatabase();

            // 根据底部导航 ID 分发对应的真实 Fragment
            if (currentNavId == R.id.nav_page1) {
                targetFragment = new CenterActivity1();
            } else if (currentNavId == R.id.nav_page2) {
                targetFragment = new CenterActivity2();
            } else if (currentNavId == R.id.nav_page3){
                targetFragment = new CenterActivity3();
            }else {
                targetFragment = new CenterActivity1(); // 默认回退
            }
        }

        // 执行 Fragment 替换，解决 Activity 渲染冲突
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, targetFragment)
                .commitAllowingStateLoss();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshFragment();
        updateMenuButton();
    }

    private void updateMenuButton() {
        if (menuButton != null) {
            if (LoginManager.isLoggedIn(this)) {
                String username = LoginManager.getUsername(this);
                if (username.length() >= 2) {
                    menuButton.setText(username.substring(0, 2));
                } else {
                    menuButton.setText(username);
                }
                menuButton.setOnClickListener(this::showSidebar);
            } else {
                menuButton.setText("登录");
                menuButton.setOnClickListener(v -> LoginManager.navigateToLogin(this));
            }
        }
    }

    /**
     * 成绩数据库触发器
     */
    private void checkAndInitUserDatabase() {
        String username = LoginManager.getUsername(this);
        // 获取学科列表 (假定 LoginManager 已实现返回 List<String> 的方法)
        List<String> subjects = LoginManager.getSubjectsList(this);

        if (!username.isEmpty() && subjects != null && !subjects.isEmpty()) {
            // 实例化 Helper 并获取数据库实例，这会自动触发 ScoreDatabaseHelper 的 onCreate
            ScoreDatabaseHelper scoreDbHelper = new ScoreDatabaseHelper(this, username, subjects);
            try {
                // 仅通过触发 getWritableDatabase 确保表被创建
                scoreDbHelper.getWritableDatabase();
            } finally {
                scoreDbHelper.close();
            }
        }
    }

    private void updateToolbarTitle(String title) {
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(title);
        }
    }

    private void showSidebar(View anchor) {
        if (isSidebarOpen) {
            closeSidebar();
        } else {
            openSidebar();
        }
    }

    /**
     * 打开侧边栏
     */
    private void openSidebar() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        SideFragment sidebarFragment = new SideFragment();
        sidebarFragment.setOnSidebarItemClickListener(this);

        // 传递用户名给SideFragment
        Bundle args = new Bundle();
        String username = LoginManager.getUsername(this);
        args.putString("username", username);
        sidebarFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,  // 使用系统自带的动画
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .replace(R.id.side_container, sidebarFragment)
                .commit();
        if (drawer != null) {
            drawer.openDrawer(GravityCompat.END); // 打开抽屉
        }
        isSidebarOpen = true;
        // 禁用主内容区域的交互
        setMainContentInteraction(false);
    }

    /**
     * 关闭侧边栏
     */
    private void closeSidebar() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        SideFragment fragment = (SideFragment) getSupportFragmentManager()
                .findFragmentById(R.id.side_container);

        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                    )
                    .remove(fragment)
                    .commit();
        }
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawer(GravityCompat.END); // 关闭抽屉
        }
        isSidebarOpen = false;
        // 启用主内容区域的交互
        setMainContentInteraction(true);
    }
    /**
     * 设置主内容区域的交互状态
     */
    private void setMainContentInteraction(boolean enabled) {
        View mainContent = findViewById(R.id.fragment_container);
        if (mainContent != null) {
            mainContent.setClickable(!enabled);
            mainContent.setFocusable(!enabled);
        }
    }

    // 侧边栏菜单项点击回调
    @Override
    public void onEditSubjectsClicked() {
        closeSidebar();

        // 1. 获取当前用户名
        String username = LoginManager.getUsername(this);
        if (username.equals("未登录")) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. 从数据库读取当前学科字符串
        UserDatabaseHelper dbHelper = new UserDatabaseHelper(this);
        String currentSubjects = dbHelper.getUserSubjects(username);

        // 3. 弹出对话框
        android.widget.EditText editText = new android.widget.EditText(this);
        editText.setText(currentSubjects);
        editText.setHint("新学科(请用空格分隔)");

        new AlertDialog.Builder(this)
                .setTitle("修改学科设置")
                .setView(editText)
                .setPositiveButton("保存", (dialog, which) -> {
                    String newSubjects = editText.getText().toString().trim();
                    if (newSubjects.isEmpty()) return;
                    List<String> newSubjectsList = java.util.Arrays.asList(newSubjects.split(" "));
                    ScoreDatabaseHelper scoreDbHelper = new ScoreDatabaseHelper(this, username, newSubjectsList);
                    scoreDbHelper.addMissingColumns(newSubjectsList);
                    scoreDbHelper.close();

                    if (dbHelper.updateUserSubjects(username, newSubjects)) {
                        LoginManager.setSubjects(this, username);
                        Toast.makeText(this, "学科更新成功", Toast.LENGTH_SHORT).show();

                        // 如果需要立即刷新 Page3 的界面，可以发送一个广播或通过 FragmentManager 通知
                    } else {
                        Toast.makeText(this, "更新失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onLogoutClicked() {
        closeSidebar();
        LoginManager.logout(this);
        updateMenuButton();
        refreshFragment();
        Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onAboutClicked() {
        closeSidebar();
        showAboutDialog();
    }

    private void showAboutDialog() {
        String appName = getApplicationInfo().loadLabel(getPackageManager()).toString();
        String version = "V00";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String message = "软件名: " + appName + "\n作者: llzzhh\n版本: " + version;

        new AlertDialog.Builder(this)
                .setTitle("关于")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    @Override
    public void onTelClicked() {
        closeSidebar();
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_DIAL,
                android.net.Uri.parse("tel:+8613587958303"));
        startActivity(intent);
    }
    @Override
    public void onEmailClicked() {
        closeSidebar();
        String phone = "+8613587958303";
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SENDTO,
                android.net.Uri.parse("smsto:" + phone));
        intent.putExtra("sms_body", "hi");
        startActivity(intent);

    }

}
