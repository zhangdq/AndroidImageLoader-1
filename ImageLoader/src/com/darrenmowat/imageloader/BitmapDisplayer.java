package com.darrenmowat.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

public interface BitmapDisplayer {

    public void displayBitmap(ImageView imageView, Context context, Bitmap bitmap, String url);
    
}
