package com.darrenmowat.boothr.data.imageloader;

import android.app.Activity;
import android.widget.ImageView;

public class PhotoToLoad {

    private String url;
    private ImageView imageView;
    private Activity activity;
    private int downloadTries;

    public PhotoToLoad(String u, ImageView i, Activity a) {
        url = u;
        imageView = i;
        activity = a;
        downloadTries = 0;
    }
    
    public String getUrl() {
        return url;
    }
    
    public ImageView getImageView() {
        return imageView;
    }
    
    public Activity getActivity() {
        return activity;
    }
    
    public int getDownloadTries() {
        return downloadTries;
    }
    

    public boolean tooManyTries() {
        return downloadTries > 3; //
    }

    public void incrementDownloadTryCount() {
        downloadTries++;
    }
}