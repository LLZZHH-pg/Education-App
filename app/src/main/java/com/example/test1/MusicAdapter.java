package com.example.test1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.List;

public class MusicAdapter extends ArrayAdapter<MusicItem> {
    private final int resourceId;

    public MusicAdapter(Context context, List<MusicItem> objects) {
        super(context, android.R.layout.simple_list_item_1, objects);
        resourceId = R.layout.music_list_item;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        MusicItem music = getItem(position);
        View view;
        ViewHolder viewHolder;

        if (convertView == null) {
            viewHolder = new ViewHolder();
            view = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);

        } else {
            view = convertView;
            viewHolder = (view.getTag() instanceof ViewHolder) ? (ViewHolder) view.getTag() : new ViewHolder();
        }
        viewHolder.albumArt = view.findViewById(R.id.album_art);
        viewHolder.musicTitle = view.findViewById(R.id.music_title);
        if (music != null) {
            viewHolder.musicTitle.setText(music.getTitle());
            String path = music.getAlbum();
            if (path != null && !path.isEmpty()) {
                File imgFile = new File(path);
                if (imgFile.exists()&&imgFile.canRead()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    if (bitmap != null) {
                        viewHolder.albumArt.setImageBitmap(bitmap);
                    }
                }
//                try {
//                    Uri uri = Uri.parse(path);
//                    viewHolder.albumArt.setImageURI(uri);
//                } catch (Exception e) {
//                    viewHolder.albumArt.setImageResource(R.drawable.ic_music_note);
//                }
            }
        }
        view.setTag(viewHolder);
        return view;
    }

    static class ViewHolder {
        ImageView albumArt;
        TextView musicTitle;
    }
}