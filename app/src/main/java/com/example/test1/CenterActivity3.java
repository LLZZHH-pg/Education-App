package com.example.test1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CenterActivity3 extends Fragment {
    private static final int REQUEST_PERMISSION_CODE = 100;
    private ListView musicListView;
    private List<MusicItem> musicList = new ArrayList<>();
    private MusicAdapter adapter;
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page3, container, false);
        musicListView = view.findViewById(R.id.musicListView);

        // 初始化适配器
        adapter = new MusicAdapter(getContext(), musicList);
        musicListView.setAdapter(adapter);

        // 注册权限请求回调
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean granted = true;
                    for (Boolean value : result.values()) {
                        if (!Boolean.TRUE.equals(value)) {
                            granted = false;
                            break;
                        }
                    }
                    if (granted) {
                        loadMusicFiles();
                    } else {
                        Toast.makeText(getContext(), "需要媒体权限来读取音乐和封面", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 检查并请求权限
        checkPermission();

        // 设置点击事件
        musicListView.setOnItemClickListener((parent, view1, position, id) -> {
            MusicItem music = musicList.get(position);
            Intent intent = new Intent(getActivity(), MusicActivity.class);
            intent.putExtra("music_path", music.getPath());
            intent.putExtra("music_album", music.getAlbum());
            intent.putExtra("music_name", music.getTitle());
            intent.putExtra("music_position", position);
            startActivity(intent);
        });

        return view;
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            List<String> need = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                need.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                need.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (!need.isEmpty()) {
                permissionLauncher.launch(need.toArray(new String[0]));
            } else {
                loadMusicFiles();
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
            } else {
                loadMusicFiles();
            }
        }
    }

    private void loadMusicFiles() {
        musicList.clear();

        // 方法1：从Download目录读取
//        loadMusicFromDirectory();

        // 方法2：从媒体库读取（如果需要）
        loadMusicFromMediaStore();

        adapter.notifyDataSetChanged();

        if (musicList.isEmpty()) {
            Toast.makeText(getContext(), "未找到音乐文件", Toast.LENGTH_SHORT).show();
        }
    }

//    private void loadMusicFromDirectory() {
//        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//        if (downloadDir.exists() && downloadDir.isDirectory()) {
//            File[] files = downloadDir.listFiles();
//            if (files != null) {
//                for (File file : files) {
//                    if (isMusicFile(file.getName())) {
//                        MusicItem music = new MusicItem();
//                        music.setTitle(getFileNameWithoutExtension(file.getName()));
//                        music.setPath(file.getAbsolutePath());
//                        String jpgPath = new File(file.getParentFile(), getFileNameWithoutExtension(file.getName()) + ".jpg").getAbsolutePath();
//                        music.setAlbum(jpgPath);
//                        musicList.add(music);
//                    }
//                }
//            }
//        }
//    }

    private void loadMusicFromMediaStore() {
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        Cursor cursor = getActivity().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE + " ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                MusicItem music = new MusicItem();
                music.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)));
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                if (path!=null) {
                    music.setPath(path);
                    File file = new File(path);
                    music.setAlbum(new File(file.getParentFile(), getFileNameWithoutExtension(file.getName()) + ".jpg").getAbsolutePath());
                }
                musicList.add(music);
            }
            cursor.close();
        }
    }

//    private boolean isMusicFile(String fileName) {
//        String[] musicExtensions = {".mp3", ".wav", ".ogg", ".flac", ".m4a", ".aac"};
//        String lowerCaseName = fileName.toLowerCase();
//        for (String ext : musicExtensions) {
//            if (lowerCaseName.endsWith(ext)) {
//                return true;
//            }
//        }
//        return false;
//    }

    private String getFileNameWithoutExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }
}