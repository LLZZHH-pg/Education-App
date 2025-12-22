// java
package com.example.test1;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends BaseActivity implements SideFragment.OnSidebarItemClickListener{

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_DARK_MODE = "dark_mode";
    private boolean isSidebarOpen = false;
    private Button menuButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        menuButton = findViewById(R.id.menu_button);
        updateMenuButton(); // 初始化菜单按钮状态
//        if (menuButton != null) {
//            menuButton.setOnClickListener(this::showSidebar);
//        }
        // 设置主内容区域点击监听，点击时关闭侧边栏
        findViewById(R.id.fragment_container).setOnClickListener(v -> {
            if (isSidebarOpen) {
                closeSidebar();
            }
        });
        // 使用 Handler 延迟执行，确保布局完全初始化
        if (savedInstanceState == null) {
            // 默认页面
            findViewById(android.R.id.content).post(this::showPage1);
        }
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav!= null) {
            bottomNav.setOnItemSelectedListener(this::onBottomNavigationItemSelected);
        }
    }

    private boolean onBottomNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_page1) {
            showPage1();
            return true;
        } else if (id == R.id.nav_page2) {
            showPage2();
            return true;
        } else if (id == R.id.nav_page3) {
            showPage3();
            return true;
        }
        return false;
    }
    private void showPage1() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new CenterActivity1())
                .commit();
        updateToolbarTitle("页面1");
        closeSidebar();
    }
    private void showPage2() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new CenterActivity2())
                .commit();
        updateToolbarTitle("页面2");
        closeSidebar();
    }

    private void showPage3() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new CenterActivity3())
                .commit();
        updateToolbarTitle("页面3");
        closeSidebar();
    }

    private void updateToolbarTitle(String title) {
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(title);
        }
    }
//    private void showMenu(View anchor) {
//        PopupMenu popup = new PopupMenu(this, anchor);
//        popup.getMenu().add(0, 1, 0, "登录");
//        popup.getMenu().add(0, 2, 1, "切换主题色");
//        popup.getMenu().add(0,3,2,"详细信息");
//        popup.setOnMenuItemClickListener(this::onMenuItemSelected);
//        popup.show();
//    }

//    private boolean onMenuItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        if (id == 1) { // 登录
//            LoginManager.navigateToLogin(this);
//            return true;
//        } else if (id == 2) { // 切换主题色（夜间/日间切换示例）
//            toggleTheme();
//            return true;
//        }else if(id ==3){ // 详细信息
//            showAboutDialog();
//            return true;
//        }
//        return false;
//    }
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
//    @Override
//    public void onLoginClicked() {
//        closeSidebar();
//        LoginManager.navigateToLogin(this);
//    }
    @Override
    public void onLogoutClicked() {
        closeSidebar();
        LoginManager.logout(this);
        updateMenuButton();
    }

    @Override
    public void onThemeToggleClicked() {
        closeSidebar();
        toggleTheme();
    }

    @Override
    public void onAboutClicked() {
        closeSidebar();
        showAboutDialog();
    }
    private void toggleTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDark = prefs.getBoolean(KEY_DARK_MODE, false);
        boolean newDark = !isDark;
        prefs.edit().putBoolean(KEY_DARK_MODE, newDark).apply();

        AppCompatDelegate.setDefaultNightMode(
                newDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        // 立即重建 Activity 以应用主题改变
        recreate();
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
