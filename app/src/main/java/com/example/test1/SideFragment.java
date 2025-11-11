package com.example.test1;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
public class SideFragment extends Fragment{
    private OnSidebarItemClickListener listener;

    public interface OnSidebarItemClickListener {
        void onLoginClicked();
        void onThemeToggleClicked();
        void onAboutClicked();
    }

    public void setOnSidebarItemClickListener(OnSidebarItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_side, container, false);

        Button btnLogin = view.findViewById(R.id.btn_login);
        Button btnTheme = view.findViewById(R.id.btn_theme);
        Button btnAbout = view.findViewById(R.id.btn_about);

        btnLogin.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLoginClicked();
            }
            closeSidebar();
        });

        btnTheme.setOnClickListener(v -> {
            if (listener != null) {
                listener.onThemeToggleClicked();
            }
            closeSidebar();
        });

        btnAbout.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAboutClicked();
            }
            closeSidebar();
        });

        return view;
    }

    private void closeSidebar() {
        // 移除侧边栏 Fragment
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .remove(this)
                    .commit();
        }
    }
}
