
package com.darrenmowat.imageloader;

import com.darrenmowat.imageloader.cache.BitmapCache;
import com.darrenmowat.imageloader.util.Util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;

public class ImageLoadRunnable implements Runnable {

    private String url;
    private ImageView imageView;
    private Context context;
    private DownloadCallback downloadCallback;

    private BitmapCache cache;
    private BitmapDisplayer bitmapDisplayer;
    
    private static int screenWidth = 800;
    
    public ImageLoadRunnable(String url, ImageView imageView, Context context,
            DownloadCallback downloadCallback, BitmapDisplayer bitmapDisplayer) {
        this.url = url;
        this.imageView = imageView;
        this.context = context;
        this.downloadCallback = downloadCallback;
        this.bitmapDisplayer = bitmapDisplayer;
        cache = BitmapCache.getInstance();
    }

    @Override
    public void run() {
        // Check this ImageView is still waiting on this Image
        // Before we go and download it.
        String tag = (String) imageView.getTag();
        if(tag == null || !tag.equals(url)) {
            // ImageView no longer waiting on this image
            return;
        }
        
        // Just double check the Cache. Another thread could just have loaded/downloaded
        // the image we need
        if(cache.contains(url)) {
            Bitmap bm = cache.get(url);
            if(bm != null && !bm.isRecycled()) {
                bitmapDisplayer.displayBitmap(imageView, context, bm, url);
                return;
            } else {
                cache.remove(url);
            }
        }
        
        boolean needsDownloaded = false;
        File storage = Util.getStorageDirectory(context);
        File image = new File(storage, String.valueOf(url.hashCode()));
        if(image.exists()) {
            try {
                Bitmap bitmap = Util.loadDrawableFromStream(image, screenWidth);
                if(bitmap != null) {
                    bitmapDisplayer.displayBitmap(imageView, context, bitmap, url);
                    cache.put(url, bitmap);
                } else {
                    needsDownloaded = true;
                }
            } catch (IOException e) {
                // Probably a probem with the File.
                // Delete it.
                image.delete();
                needsDownloaded = true;
            }
        } else {
            needsDownloaded = true;
        }
        if (downloadCallback != null && needsDownloaded) {
            downloadCallback.needsDownloaded(url, imageView, context, bitmapDisplayer);
        }
    }
    
    public static void setScreenWidth(int width) {
        screenWidth = width;
    }
    
    private void log(String msg) {
        Log.v("FileLoader", msg);
    }
    
}
