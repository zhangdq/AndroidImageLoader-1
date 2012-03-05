
package com.darrenmowat.imageloader;

import com.darrenmowat.imageloader.cache.BitmapCache;
import com.darrenmowat.imageloader.util.Util;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;

public class ImageLoadRunnable implements Runnable {

    private String url;
    private ImageView imageView;
    private Activity activity;
    private DownloadCallback downloadCallback;

    private BitmapCache cache;
    
    private static int screenWidth = 800;
    
    public ImageLoadRunnable(String url, ImageView imageView, Activity activity,
            DownloadCallback downloadCallback) {
        this.url = url;
        this.imageView = imageView;
        this.activity = activity;
        this.downloadCallback = downloadCallback;
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
                activity.runOnUiThread(new BitmapDisplayer(imageView, bm, url));
                return;
            } else {
                cache.remove(url);
            }
        }
        
        boolean needsDownloaded = false;
        File storage = Util.getStorageDirectory(activity);
        File image = new File(storage, String.valueOf(url.hashCode()));
        if(image.exists()) {
            try {
                Bitmap bitmap = Util.loadDrawableFromStream(image, screenWidth);
                if(bitmap != null) {
                    activity.runOnUiThread(new BitmapDisplayer(imageView, bitmap, url));
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
            downloadCallback.needsDownloaded(url, imageView, activity);
        }
    }
    
    public static void setScreenWidth(int width) {
        screenWidth = width;
    }
    
    private void log(String msg) {
        Log.v("FileLoader", msg);
    }
    
}
