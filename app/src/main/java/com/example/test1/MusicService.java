package com.example.test1;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private MediaPlayer mediaPlayer;
    private String currentMusicPath;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            currentMusicPath = intent.getStringExtra("music_path");

            switch (action) {
                case "PLAY":
                    playMusic();
                    break;
                case "PAUSE":
                    pauseMusic();
                    break;
            }
        }
        return START_STICKY;
    }

    private void playMusic() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(currentMusicPath);
                mediaPlayer.prepare();
                mediaPlayer.start();
                Log.d(TAG, "开始播放: " + currentMusicPath);
            } catch (IOException e) {
                Log.e(TAG, "播放失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                Log.d(TAG, "继续播放");
            }
        }
    }

    private void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            Log.d(TAG, "暂停播放");
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        Log.d(TAG, "服务销毁");
    }
}