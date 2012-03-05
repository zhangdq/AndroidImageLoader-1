package com.darrenmowat.imageloader;

import android.graphics.Bitmap;
import android.widget.ImageView;

public class BitmapDisplayer implements Runnable{

    private ImageView imageView;
    private Bitmap bitmap;
    private String url;
    
    public BitmapDisplayer(ImageView imageView, Bitmap bitmap, String url) {
        this.imageView = imageView;
        this.bitmap = bitmap;
        this.url = url;
    }
    
    @Override
    public void run() {
        // This should be run on the UI thread!
        String tag = (String) imageView.getTag();
        if(tag == null || !tag.equals(url)) {
            // ImageView not longer waiting on this image
            return;
        }
        imageView.setImageBitmap(bitmap); 
        imageView.setAdjustViewBounds(true);
    }
}
