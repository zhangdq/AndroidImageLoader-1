package com.darrenmowat.imageloader;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

public class StandardBitmapDisplayer implements BitmapDisplayer {
    
    public StandardBitmapDisplayer() {
        // Don't need to initialise anything
    }

    @Override
    public void displayBitmap(final ImageView imageView, Context context, final Bitmap bitmap, String url) {
        if(imageView == null) {
            return;
        }
        String tag = (String) imageView.getTag();
        if(tag == null || !tag.equals(url)) {
            // ImageView not longer waiting on this image
            return;
        }
        // Display the image
        if(context instanceof Activity) {
            Activity a = (Activity) context;
            a.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    imageView.setImageBitmap(bitmap); 
                    // imageView.setAdjustViewBounds(true);
                }
                
            });
        } 
    }
}
