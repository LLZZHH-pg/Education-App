package com.example.test1;

import android.annotation.SuppressLint;
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
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS) // 连接超时
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)    // 读取超时
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)   // 写入超时
            .build();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String subject = intent.getStringExtra("subject");
        String username = LoginManager.getUsername(this);
        List<String> allSubjects = LoginManager.getSubjectsList(this);

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

    @SuppressLint("DefaultLocale")
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
        android.util.Log.d("AI_DEBUG", "开始请求 Zhipu API");
        try {
            // 1. 构建请求 JSON
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "glm-4.7");

            JSONArray messages = new JSONArray();
            // 系统提示词
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content",
                    "你是一个专业的高中教师和教育分析员，请根据提供的成绩数据，从考试学科、考试时间点、考试时间间隔、考试时间点学生正在学东西的重点知识等角度，全面分析学生上传的学科成绩，中肯精炼的给出分析结论，并以温和的语气提出学习建议。给出分析结果和学习建议时都不需要开场白，直接叙述内容。分数为0的考试记录定义为没有录入数据，直接忽略。");
            messages.put(systemMsg);

            // 用户拼接好的数据提示词
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.put(userMsg);

            jsonBody.put("messages", messages);
            jsonBody.put("stream", true);
            jsonBody.put("temperature", 1.0);

            // 2. 发送请求
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);

            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .post(body)
                    .build();
            android.util.Log.d("AI_DEBUG", "即将执行网络请求");

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    sendResult("服务器响应错误: " + response.code());
                    return;
                }

                // SSE: data: {...}\n\n ，最后可能会有 data: [DONE]
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(response.body().byteStream(), java.nio.charset.StandardCharsets.UTF_8)
                );

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    if (!line.startsWith("data:")) continue;

                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }

                    try {
                        JSONObject chunk = new JSONObject(data);
                        JSONArray choices = chunk.optJSONArray("choices");
                        if (choices == null || choices.length() == 0) continue;

                        JSONObject first = choices.getJSONObject(0);

                        // 兼容常见流式字段: delta / message
                        String deltaText = "";
                        if (first.has("delta")) {
                            JSONObject delta = first.getJSONObject("delta");
                            deltaText = delta.optString("content", "");
                        } else if (first.has("message")) {
                            JSONObject msg = first.getJSONObject("message");
                            deltaText = msg.optString("content", "");
                        }

                        if (deltaText != null && !deltaText.isEmpty()) {
                            // 增量推送，不结束
                            sendResult(deltaText);
                        }
                    } catch (Exception parseEx) {
                        android.util.Log.e("AI_DEBUG", "流式分片解析失败: " + data, parseEx);
                    }
                }


            }
            catch (Exception e) {
                android.util.Log.e("AI_DEBUG", "解析或网络异常", e);
                sendResult("处理失败: " + e.getMessage());
            }
        } catch (Exception e) {
            android.util.Log.e("AI_DEBUG", "调用异常", e);
            sendResult("网络异常: " + e.getMessage());
        }
    }

    private void sendResult(String content) {
        Intent intent = new Intent(ACTION_AI_REPLY);
        intent.putExtra("response", content);

        androidx.localbroadcastmanager.content.LocalBroadcastManager
                .getInstance(this).sendBroadcast(intent);
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
