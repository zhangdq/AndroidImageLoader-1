package com.darrenmowat.imageloaderdemo;

import com.darrenmowat.imageloader.ImageLoader;
import com.darrenmowat.imageloader.cache.BitmapCache;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class ImageAdapter extends BaseAdapter implements Observer{
    
    private Activity activity;
    public ArrayList<String> urls;
    
    public ImageAdapter(ArrayList<String> urls, Activity activity) {
        this.activity = activity;
        this.urls = urls;
        BitmapCache.getInstance().addObserver(this);
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

    /*
     * The cache fires this when it has been cleared.
     * Allows you to refresh your data
     * 
     * (non-Javadoc)
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    @Override
    public void update(Observable observable, Object data) {
        if(observable instanceof BitmapCache) {
            notifyDataSetChanged();
        }
    }
    
}
