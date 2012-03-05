package com.darrenmowat.imageloader;

import android.app.Activity;
import android.widget.ImageView;

public interface DownloadCallback {

    public void needsDownloaded(String url, ImageView imageView, Activity activity);
    
}
