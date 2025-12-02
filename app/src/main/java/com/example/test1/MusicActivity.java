package com.example.test1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class MusicActivity extends AppCompatActivity {
    private TextView musicTitle;
    private ImageView albumArt;
    private Button playPauseBtn;
    private boolean isPlaying = false;
    private String musicName;
    private String albumPath;
    private String currentMusicPath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        musicTitle = findViewById(R.id.music_title);
        albumArt = findViewById(R.id.album_art);
        playPauseBtn = findViewById(R.id.play_pause_btn);

        // 获取传递的音乐信息
        Intent intent = getIntent();
        musicName = intent.getStringExtra("music_name");
        albumPath = intent.getStringExtra("music_album");
        currentMusicPath = intent.getStringExtra("music_path");

        // 设置界面内容
        if (musicName != null) {
            musicTitle.setText(musicName);
        }
        // 设置专辑封面
        if(albumPath!=null&&!albumPath.isEmpty()){
            File imgFile = new File(albumPath);
            if (imgFile.exists()&&imgFile.canRead()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                if (bitmap != null) {
                    albumArt.setImageBitmap(bitmap);
                }
            }
        }

        // 设置按钮点击事件
        playPauseBtn.setOnClickListener(v -> {
            if (isPlaying) {
                pauseMusic();
            } else {
                playMusic();
            }
        });

        // 启动服务
        startMusicService();
    }

    private void startMusicService() {
        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.putExtra("music_path", currentMusicPath);
        serviceIntent.setAction("PLAY");
        startService(serviceIntent);
        isPlaying = true;
        updatePlayButton();
    }

    private void playMusic() {
        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction("PLAY");
        startService(serviceIntent);
        isPlaying = true;
        updatePlayButton();
    }

    private void pauseMusic() {
        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction("PAUSE");
        startService(serviceIntent);
        isPlaying = false;
        updatePlayButton();
    }

    private void updatePlayButton() {
        if (isPlaying) {
            playPauseBtn.setText("暂停");
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(
                    android.R.drawable.ic_media_pause, 0, 0, 0);
        } else {
            playPauseBtn.setText("播放");
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(
                    android.R.drawable.ic_media_play, 0, 0, 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止服务
        Intent serviceIntent = new Intent(this, MusicService.class);
        stopService(serviceIntent);
    }
}