// java
        package com.example.test1;

        import android.app.DatePickerDialog;
        import android.app.TimePickerDialog;
        import android.content.ContentValues;
        import android.content.Context;
        import android.database.Cursor;
        import android.database.sqlite.SQLiteDatabase;
        import android.database.sqlite.SQLiteOpenHelper;
        import android.graphics.Color;
        import android.graphics.Typeface;
        import android.graphics.drawable.GradientDrawable;
        import android.os.Bundle;
        import android.text.TextUtils;
        import android.util.TypedValue;
        import android.view.Gravity;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.ArrayAdapter;
        import android.widget.Button;
        import android.widget.EditText;
        import android.widget.Spinner;
        import android.widget.TableLayout;
        import android.widget.TableRow;
        import android.widget.TextView;
        import android.widget.Toast;

        import androidx.annotation.Nullable;
        import androidx.fragment.app.Fragment;

        import java.text.ParseException;
        import java.text.SimpleDateFormat;
        import java.util.ArrayList;
        import java.util.Calendar;
        import java.util.Date;
        import java.util.List;
        import java.util.Locale;

        public class CenterActivity2 extends Fragment {

            private EditText etTitle;
            private EditText etContent;
            private EditText etTime;
            private Spinner spStatus;
            private TableLayout tableLayout;
            private ScheduleDbHelper dbHelper;

            // 时间相关
            private SimpleDateFormat isoFormatter;      // 存库用：yyyy\-MM\-dd\'T\'HH:mm
            private SimpleDateFormat displayFormatter;  // 显示用：yyyy\-MM\-dd HH:mm
            private String selectedIsoTime = null;

            private final String[] statusOptions = {"待完成", "进行中", "已完成", "已取消"};

            // 当前登录用户名（作为 userId 使用）
            private String currentUserName = null;

            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                     Bundle savedInstanceState) {

                // 先判断是否登录
                Context ctx = requireContext();
                if (!LoginManager.isLoggedIn(ctx)) {
                    // 未登录，只显示“请先登录”
                    TextView tv = new TextView(ctx);
                    tv.setText("请先登录");
                    tv.setGravity(Gravity.CENTER);
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
                    tv.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                    return tv;
                }

                // 已登录，获取用户名作为 userId
                currentUserName = LoginManager.getUsername(ctx);
                if (TextUtils.isEmpty(currentUserName)) {
                    TextView tv = new TextView(ctx);
                    tv.setText("请先登录");
                    tv.setGravity(Gravity.CENTER);
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
                    tv.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                    return tv;
                }

                View view = inflater.inflate(R.layout.fragment_page2, container, false);
                initViews(view);

                isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault());
                displayFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                dbHelper = new ScheduleDbHelper(requireContext());
                loadSchedules();
                return view;
            }

            @Override
            public void onDestroyView() {
                super.onDestroyView();
                if (dbHelper != null) {
                    dbHelper.close();
                }
            }

            private void initViews(View view) {
                etTitle = view.findViewById(R.id.et_schedule_title);
                etContent = view.findViewById(R.id.et_schedule_content);
                etTime = view.findViewById(R.id.et_schedule_time);
                spStatus = view.findViewById(R.id.sp_schedule_status);
                tableLayout = view.findViewById(R.id.table_schedules);

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        statusOptions
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spStatus.setAdapter(adapter);

                // 时间输入框：点击弹出日期+时间选择
                etTime.setFocusable(false);
                etTime.setClickable(true);
                etTime.setOnClickListener(v -> showDateTimePicker());

                Button btnSave = view.findViewById(R.id.btn_save_schedule);
                btnSave.setOnClickListener(v -> saveSchedule());

                TextView header = view.findViewById(R.id.tv_schedule_header);
                header.setTypeface(header.getTypeface(), Typeface.BOLD);
            }

            private void showDateTimePicker() {
                final Calendar cal = Calendar.getInstance();

                DatePickerDialog dp = new DatePickerDialog(
                        requireContext(),
                        (view, year, month, dayOfMonth) -> {
                            cal.set(Calendar.YEAR, year);
                            cal.set(Calendar.MONTH, month);
                            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                            TimePickerDialog tp = new TimePickerDialog(
                                    requireContext(),
                                    (timeView, hourOfDay, minute) -> {
                                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                        cal.set(Calendar.MINUTE, minute);
                                        Date chosen = cal.getTime();

                                        // 内部存 ISO，界面显示普通格式
                                        selectedIsoTime = isoFormatter.format(chosen);
                                        etTime.setText(displayFormatter.format(chosen));
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    true
                            );
                            tp.show();
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                );
                dp.show();
            }

            private void saveSchedule() {
                if (TextUtils.isEmpty(currentUserName)) {
                    Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show();
                    return;
                }

                String title = etTitle.getText().toString().trim();
                String content = etContent.getText().toString().trim();
                String status = (String) spStatus.getSelectedItem();

                if (TextUtils.isEmpty(title) ||
                        TextUtils.isEmpty(content) ||
                        TextUtils.isEmpty(selectedIsoTime)) {
                    Toast.makeText(requireContext(), "请完整填写日程信息（含时间）", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 带 userName 的日程
                Schedule schedule = new Schedule(
                        0,
                        title,
                        content,
                        status,
                        selectedIsoTime,
                        currentUserName
                );
                long result = dbHelper.insertSchedule(schedule);
                if (result > 0) {
                    clearInputs();
                    loadSchedules();
                    Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "保存失败", Toast.LENGTH_SHORT).show();
                }
            }

            private void clearInputs() {
                etTitle.setText("");
                etContent.setText("");
                etTime.setText("");
                spStatus.setSelection(0);
                selectedIsoTime = null;
            }

            private void loadSchedules() {
                if (tableLayout == null || TextUtils.isEmpty(currentUserName)) return;

                List<Schedule> schedules = dbHelper.loadSchedulesDescForUser(currentUserName);
                int childCount = tableLayout.getChildCount();
                if (childCount > 1) {
                    tableLayout.removeViews(1, childCount - 1);
                }

                for (Schedule schedule : schedules) {
                    TableRow row = new TableRow(requireContext());

                    // 先创建本行所有单元格
                    List<TextView> cells = new ArrayList<>();
                    cells.add(createCell(schedule.getTitle()));
                    cells.add(createCell(schedule.getContent()));

                    String timeDisplay;
                    try {
                        Date d = isoFormatter.parse(schedule.getEventTime());
                        timeDisplay = (d != null) ? displayFormatter.format(d) : schedule.getEventTime();
                    } catch (ParseException e) {
                        timeDisplay = schedule.getEventTime();
                    }
                    cells.add(createCell(timeDisplay));
                    cells.add(createCell(schedule.getStatus()));

                    // 先测量，得到本行中最大的高度
                    int maxH = 0;
                    int availWidth = tableLayout.getWidth();
                    int widthSpec = View.MeasureSpec.makeMeasureSpec(
                            (availWidth > 0 ? availWidth : getResources().getDisplayMetrics().widthPixels),
                            View.MeasureSpec.AT_MOST
                    );
                    int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

                    for (TextView cell : cells) {
                        cell.measure(widthSpec, heightSpec);
                        int h = cell.getMeasuredHeight();
                        if (h > maxH) maxH = h;
                    }
                    if (maxH <= 0) {
                        maxH = dp(48); // 兜底高度
                    }

                    // 把最大高度设置给本行每个单元格
                    for (TextView cell : cells) {
                        TableRow.LayoutParams lp = new TableRow.LayoutParams(
                                0,
                                maxH,
                                1f
                        );
                        cell.setLayoutParams(lp);
                        row.addView(cell);
                    }

                    tableLayout.addView(row);
                }
            }

            private TextView createCell(String text) {
                TextView cell = new TextView(requireContext());
                cell.setText(text);
                cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
                cell.setPadding(dp(12), dp(12), dp(12), dp(12));
                cell.setMaxLines(Integer.MAX_VALUE);
                cell.setLineSpacing(0f, 1.1f);
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.TRANSPARENT);
                bg.setStroke(dp(1), Color.parseColor("#666666"));
                cell.setBackground(bg);
                // 初始高度用 WRAP\_CONTENT，真实高度在 loadSchedules 里统一改为 maxH
                cell.setLayoutParams(new TableRow.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                return cell;
            }

            private int dp(int value) {
                float density = getResources().getDisplayMetrics().density;
                return Math.round(value * density);
            }

            // ================== 内部模型类 ==================

            private static class Schedule {
                private final long id;
                private final String title;
                private final String content;
                private final String status;
                private final String eventTime; // ISO 字符串
                private final String userName;  // 用户标识

                Schedule(long id,
                         String title,
                         String content,
                         String status,
                         String eventTime,
                         String userName) {
                    this.id = id;
                    this.title = title;
                    this.content = content;
                    this.status = status;
                    this.eventTime = eventTime;
                    this.userName = userName;
                }

                long getId() { return id; }

                String getTitle() { return title; }

                String getContent() { return content; }

                String getStatus() { return status; }

                String getEventTime() { return eventTime; }

                String getUserName() { return userName; }
            }

            private static class ScheduleDbHelper extends SQLiteOpenHelper {
                private static final String DB_NAME = "schedule.db";
                private static final int DB_VERSION = 1; // 升级版本
                private static final String TABLE = "schedules";
                private static final String COL_ID = "_id";
                private static final String COL_TITLE = "title";
                private static final String COL_CONTENT = "content";
                private static final String COL_STATUS = "status";
                private static final String COL_TIME = "event_time";
                private static final String COL_USER = "user_name";

                ScheduleDbHelper(@Nullable Context context) {
                    super(context, DB_NAME, null, DB_VERSION);
                }

                @Override
                public void onCreate(SQLiteDatabase db) {
                    // 增加 user_name 列
                    String sql = "CREATE TABLE " + TABLE + " (" +
                            COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                            COL_TITLE + " TEXT NOT NULL," +
                            COL_CONTENT + " TEXT NOT NULL," +
                            COL_STATUS + " TEXT NOT NULL," +
                            COL_TIME + " TEXT NOT NULL," +
                            COL_USER + " TEXT NOT NULL)";
                    db.execSQL(sql);
                }

                @Override
                public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                    // 简单策略：重建表
                    db.execSQL("DROP TABLE IF EXISTS " + TABLE);
                    onCreate(db);
                }

                long insertSchedule(Schedule schedule) {
                    SQLiteDatabase db = getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put(COL_TITLE, schedule.getTitle());
                    values.put(COL_CONTENT, schedule.getContent());
                    values.put(COL_STATUS, schedule.getStatus());
                    values.put(COL_TIME, schedule.getEventTime());
                    values.put(COL_USER, schedule.getUserName());
                    return db.insert(TABLE, null, values);
                }

                List<Schedule> loadSchedulesDescForUser(String userName) {
                    List<Schedule> data = new ArrayList<>();
                    if (userName == null || userName.trim().isEmpty()) {
                        return data;
                    }
                    SQLiteDatabase db = getReadableDatabase();
                    String selection = COL_USER + " = ?";
                    String[] args = new String[]{ userName };
                    try (Cursor cursor = db.query(
                            TABLE,
                            null,
                            selection,
                            args,
                            null,
                            null,
                            COL_TIME + " DESC"   // 按 ISO 字符串倒序 == 时间倒序
                    )) {
                        while (cursor.moveToNext()) {
                            long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
                            String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
                            String content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT));
                            String status = cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS));
                            String time = cursor.getString(cursor.getColumnIndexOrThrow(COL_TIME));
                            String uName = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER));
                            data.add(new Schedule(id, title, content, status, time, uName));
                        }
                    }
                    return data;
                }
            }
        }