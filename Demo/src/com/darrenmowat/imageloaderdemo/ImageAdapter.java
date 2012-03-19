package com.darrenmowat.imageloaderdemo;

import com.darrenmowat.imageloader.ImageLoader;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.ArrayList;

public class ImageAdapter extends BaseAdapter {
    
    private Activity activity;
    public ArrayList<String> urls;
    
    public ImageAdapter(ArrayList<String> urls, Activity activity) {
        this.activity = activity;
        this.urls = urls;
    }

    @Override
    public int getCount() {
        return urls.size();
    }

    @Override
    public Object getItem(int position) {
        if(position >= urls.size()) {
            return null;
        }
        return urls.get(position);
    }

    @Override
    public long getItemId(int position) {
        if(position >= urls.size()) {
            return 0;
        }
        return urls.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView iv;
        if (convertView == null)
            convertView = iv = new ImageView(activity);
        else
            iv = (ImageView)convertView;
        
        ImageLoader.getInstance().setImage(urls.get(position), iv, activity);
        
        return iv;
    }

    
}
