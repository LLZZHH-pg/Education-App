package com.example.test1;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIService extends Service {
    public static final String ACTION_AI_REPLY = "com.example.AI_REPLY";
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String subject = intent.getStringExtra("subject");
        String username = LoginManager.getUsername(this);
        List<String> allSubjects = LoginManager.getSubjectsList(this);

        // 异步处理，防止阻塞主线程
        new Thread(() -> {
            String prompt = buildPrompt(username, subject, allSubjects);
            callZhipuAPI(prompt);
        }).start();

        return START_NOT_STICKY;
    }

    private String buildPrompt(String username, String selectedSubject, List<String> allSubjects) {
        ScoreDatabaseHelper dbHelper = new ScoreDatabaseHelper(this, username, allSubjects);
        Cursor cursor = dbHelper.getWritableDatabase().query("score_table", null, null, null, null, null, "id ASC");

        StringBuilder dataBuilder = new StringBuilder();
        if ("全部".equals(selectedSubject)) {
            dataBuilder.append("以下是所有学科的成绩记录：\n");
            for (String sub : allSubjects) {
                dataBuilder.append("[").append(sub).append("]: ").append(getSubjectDataString(cursor, sub)).append("\n");
            }
            return "这是我全部科目的历次考试成绩，请老师帮我分析并给出学习建议。" + dataBuilder;
        } else {
            dataBuilder.append("以下是").append(selectedSubject).append("学科的成绩记录：").append(getSubjectDataString(cursor, selectedSubject));
            return "这是我" + selectedSubject + "学科的历次成绩，请老师帮我分析并给出学习建议。" + dataBuilder;
        }
    }

    private String getSubjectDataString(Cursor cursor, String subject) {
        List<String> list = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                String exam = cursor.getString(cursor.getColumnIndexOrThrow("exam_name"));
                float score = cursor.getFloat(cursor.getColumnIndexOrThrow(subject));
                String time = cursor.getString(cursor.getColumnIndexOrThrow("upload_time"));
                list.add(String.format("[%s, %.1f, %s]", exam, score, time));
            } while (cursor.moveToNext());
        }
        return list.toString();
    }

    private void callZhipuAPI(String prompt) {
        String apiKey = "1e3840be35544ddeb94982b1706e2a04.eHUbxcshMkJijBSZ";
        String url = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

        try {
            // 1. 构建请求 JSON
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "glm-4.7"); // 或 glm-4-plus 等

            JSONArray messages = new JSONArray();
            // 系统提示词
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content",
                    "你是一个专业的高中教师和教育分析员，请根据提供的成绩数据，从考试学科、考试时间点、考试时间间隔、考试时间点学生正在学东西的重点知识等角度，全面分析学生上传的学科成绩，中肯精炼的给出分析结论，并以温和的语气提出学习建议。");
            messages.put(systemMsg);

            // 用户拼接好的数据提示词
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.put(userMsg);

            jsonBody.put("messages", messages);
            jsonBody.put("stream", false); // 简单起见，关闭流式传输，直接获取完整结果

            // 2. 发送请求
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);

            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();

                    // 3. 解析返回的 JSON 获取 AI 内容
                    JSONObject resJson = new JSONObject(responseData);
                    String aiContent = resJson.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    // 4. 通过广播发送结果
                    Intent intent = new Intent(ACTION_AI_REPLY);
                    intent.putExtra("response", aiContent);
                    sendBroadcast(intent);
                } else {
                    sendErrorBroadcast("请求失败: " + response.code());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorBroadcast("网络异常: " + e.getMessage());
        }
    }

    private void sendErrorBroadcast(String errorMsg) {
        Intent intent = new Intent(ACTION_AI_REPLY);
        intent.putExtra("response", "AI 助手出错啦：" + errorMsg);
        sendBroadcast(intent);
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
