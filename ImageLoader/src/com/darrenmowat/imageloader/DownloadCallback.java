package com.darrenmowat.imageloader;

import android.content.Context;
import android.widget.ImageView;

public interface DownloadCallback {

    public void needsDownloaded(String url, ImageView imageView, Context context, BitmapDisplayer bitmapDisplayer);
    
}
