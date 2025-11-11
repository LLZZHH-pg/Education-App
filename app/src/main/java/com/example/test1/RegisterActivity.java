// java
            package com.example.test1;

            import android.app.DatePickerDialog;
            import android.content.Intent;
            import android.os.Bundle;
            import android.text.Html;
            import android.text.InputType;
            import android.text.method.LinkMovementMethod;
            import android.widget.ArrayAdapter;
            import android.widget.Button;
            import android.widget.CheckBox;
            import android.widget.EditText;
            import android.widget.Spinner;
            import android.widget.TextView;
            import android.widget.Toast;

            import androidx.appcompat.app.AlertDialog;
            import androidx.appcompat.app.AppCompatActivity;

            import java.util.Calendar;
            import java.util.Locale;

            public class RegisterActivity extends AppCompatActivity {
                @Override
                protected void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);
                    setContentView(R.layout.activity_register);

                    EditText usernameEditText = findViewById(R.id.usernameEditText);
                    EditText passwordEditText = findViewById(R.id.passwordEditText);
                    EditText dateEditText = findViewById(R.id.dateEditText);
                    Spinner preSpinner = findViewById(R.id.preSpinner);

                    TextView agreeText = findViewById(R.id.agreeText);
                    CheckBox agreeCheckBox = findViewById(R.id.agreeCheckBox);

                    Button loginButton = findViewById(R.id.loginButton);
                    Button registerButton = findViewById(R.id.registerButton);
                    Button clearButton = findViewById(R.id.clearButton);

                    // 返回登录
                    loginButton.setOnClickListener(v -> {
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                    });

                    // 同意协议
                    String linkText = "同意<font color='#2196F3'><u><a href='https://cn.bing.com/'>用户协议</a></u></font>";
                    agreeText.setText(Html.fromHtml(linkText, Html.FROM_HTML_MODE_LEGACY));
                    agreeText.setMovementMethod(LinkMovementMethod.getInstance());
                    agreeText.setOnClickListener(v -> agreeCheckBox.setChecked(!agreeCheckBox.isChecked()));

                    // 生日选择：禁止键盘，点击弹出日期选择
                    dateEditText.setInputType(InputType.TYPE_NULL);
                    dateEditText.setFocusable(false);
                    dateEditText.setOnClickListener(v -> {
                        Calendar c = Calendar.getInstance();
                        int y = c.get(Calendar.YEAR);
                        int m = c.get(Calendar.MONTH);
                        int d = c.get(Calendar.DAY_OF_MONTH);
                        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                            String picked = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                            dateEditText.setText(picked);
                        }, y, m, d).show();
                    });

                    // 偏好选择：禁止键盘，点击弹出列表
                    final String[] prefs = new String[] {"运动", "音乐", "旅行", "电影", "阅读", "游戏"};
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, prefs);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    preSpinner.setAdapter(adapter);

                    registerButton.setOnClickListener(v -> {
                        String username = usernameEditText.getText().toString().trim();
                        String password = passwordEditText.getText().toString().trim();
                        String date = dateEditText.getText().toString().trim();
                        String pref = preSpinner.getSelectedItem() == null ? "" : preSpinner.getSelectedItem().toString();

                        if (!agreeCheckBox.isChecked()) {
                            new AlertDialog.Builder(RegisterActivity.this)
                                    .setTitle("提示")
                                    .setMessage("请先同意用户协议")
                                    .setPositiveButton("同意", (dialog, which) -> agreeCheckBox.setChecked(true))
                                    .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                                    .show();
                        } else if (username.isEmpty() || password.isEmpty() || date.isEmpty() || pref.isEmpty()) {
                            Toast.makeText(this, "请完整填写用户名、密码、生日与偏好", Toast.LENGTH_SHORT).show();
                        } else {
                            String message = username + " | 注册成功";
                            Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        }
                    });

                    clearButton.setOnClickListener(v -> {
                        usernameEditText.setText("");
                        passwordEditText.setText("");
                        dateEditText.setText("");
                        preSpinner.setSelection(0);
                    });
                }
            }