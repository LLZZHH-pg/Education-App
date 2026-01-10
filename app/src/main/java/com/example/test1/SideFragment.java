package com.example.test1;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
public class SideFragment extends Fragment{
    private OnSidebarItemClickListener listener;

    public interface OnSidebarItemClickListener {
//        void onLoginClicked();
        void onThemeToggleClicked();
        void onAboutClicked();
        void onLogoutClicked();

        void onTelClicked();
        void onEmailClicked();
    }

    public void setOnSidebarItemClickListener(OnSidebarItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_side, container, false);

        // 设置用户名显示
        TextView usernameText = view.findViewById(R.id.username);
        String usernameInside = "未登录";
        if (getArguments() != null) {
            usernameInside = getArguments().getString("username", "未登录");
        }
        usernameText.setText(usernameInside);

//        Button btnLogin = view.findViewById(R.id.btn_login);
        Button btnTheme = view.findViewById(R.id.btn_theme);
        Button btnAbout = view.findViewById(R.id.btn_about);
        Button btnLogout = view.findViewById(R.id.btn_logout);

        Button btnTel=view.findViewById(R.id.btn_tel);
        Button btnEmail=view.findViewById(R.id.btn_email);

//        btnLogin.setOnClickListener(v -> {
//            if (listener != null) {
//                listener.onLoginClicked();
//            }
//        });

        btnLogout.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLogoutClicked();
            }
        });

        btnTheme.setOnClickListener(v -> {
            if (listener != null) {
                listener.onThemeToggleClicked();
            }
        });

        btnAbout.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAboutClicked();
            }
        });

        btnTel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTelClicked();
            }
        });
        btnEmail.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEmailClicked();
            }
        });

        return view;
    }

}
