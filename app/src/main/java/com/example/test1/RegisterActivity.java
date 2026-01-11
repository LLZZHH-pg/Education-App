// java
            package com.example.test1;

            import android.app.DatePickerDialog;
            import android.content.Intent;
            import android.os.Bundle;
            import android.text.Html;
            import android.text.InputType;
            import android.text.method.LinkMovementMethod;
            import android.widget.Button;
            import android.widget.CheckBox;
            import android.widget.EditText;
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
                    EditText subjectsEditText = findViewById(R.id.subjectsEditText);

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


                    registerButton.setOnClickListener(v -> {
                        String username = usernameEditText.getText().toString().trim();
                        String password = passwordEditText.getText().toString().trim();
                        String date = dateEditText.getText().toString().trim();
                        String subjectsRaw = subjectsEditText.getText().toString().trim();

                        if (!agreeCheckBox.isChecked()) {
                            new AlertDialog.Builder(RegisterActivity.this)
                                    .setTitle("提示")
                                    .setMessage("请先同意用户协议")
                                    .setPositiveButton("同意", (dialog, which) -> agreeCheckBox.setChecked(true))
                                    .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                                    .show();
                        } else if (username.isEmpty() || password.isEmpty() || date.isEmpty() || subjectsRaw.isEmpty()) {
                            Toast.makeText(this, "请完整填写用户名、密码、生日、学科", Toast.LENGTH_SHORT).show();
                        } else if(!subjectsRaw.matches("^[\\u4e00-\\u9fa5a-zA-Z0-9\\s]+$")){
                            Toast.makeText(this, "学科格式错误", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            UserDatabaseHelper dbHelper = new UserDatabaseHelper(this);
                            if (dbHelper.checkUserExists(username)) {
                                Toast.makeText(this, "该用户名已存在", Toast.LENGTH_SHORT).show();
                            } else {
                                String formattedSubjects = subjectsRaw.replaceAll("\\s+", " ").trim();
                                boolean success = dbHelper.registerUser(username, password, date,formattedSubjects);
                                if (success) {
                                    Toast.makeText(this, "注册成功！请登录", Toast.LENGTH_SHORT).show();
                                    finish(); // 返回登录页
                                } else {
                                    Toast.makeText(this, "注册失败，请重试", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });

                    clearButton.setOnClickListener(v -> {
                        usernameEditText.setText("");
                        passwordEditText.setText("");
                        dateEditText.setText("");
                    });
                }
            }