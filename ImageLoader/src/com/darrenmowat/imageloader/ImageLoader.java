package com.darrenmowat.imageloader;

import com.darrenmowat.imageloader.cache.BitmapCache;
import com.darrenmowat.imageloader.util.Util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * This class does not support Pre loading of images
 */
public class ImageLoader implements DownloadCallback {
    
    private String tag;

    private static int defaultDrawable = -1;
    private static Drawable def;

    private BitmapCache cache;
    
    private ExecutorService storagePool;
    private ExecutorService downloadPool;
    
    private boolean hasSetGlobalScreenSize;
    
    public ImageLoader() {
        this("ImageLoader");
    }

    public ImageLoader(String tag) {
        this.tag = tag;
        hasSetGlobalScreenSize = false;
        // Load threads are short lived
        // They just load a file from the disk
        storagePool = Executors.newCachedThreadPool();
        // We have 2 threads that can do downloads 
        // off the UI thread. These will be longer lived
        // Than the file loader threads
        downloadPool = Executors.newFixedThreadPool(3);
        // In memory cache
        cache = BitmapCache.getInstance();
    }
    
    public void setImage(String url, ImageView imageView, Context context) {
        setImage(url, imageView, context, new StandardBitmapDisplayer(), true);
    }
    
    public void setImage(String url, ImageView imageView, Context context, BitmapDisplayer bitmapDisplayer) {
        setImage(url, imageView, context, bitmapDisplayer, true);
    }
    
    public void setImage(String url, ImageView imageView, Context context, BitmapDisplayer bitmapDisplayer, boolean setPendingImage) {
        // Don't support preloading yet
        if(context == null || imageView == null) {
            throw new IllegalArgumentException("Activtiy or ImageView was null.");
        }
        // bitmapDisplayer could be passed in as null
        // Assume the user wants to use a StandardBitmapDisplayer
        if(bitmapDisplayer == null) {
            bitmapDisplayer = new StandardBitmapDisplayer();
        }
        // Set the ImageViews tag to the Url. 
        // This helps us check if the ImageView still requires this image
        // Might not due to View Recycling
        imageView.setTag(url);
        if(cache.contains(url)) {
            Bitmap bm = cache.get(url);
            if(bm != null && !bm.isRecycled()) {
                imageView.setImageBitmap(bm);
                return;
            } else {
                cache.remove(url);
            }
        }
        if(setPendingImage) {
            if(def == null && defaultDrawable != -1) {
                def = context.getResources().getDrawable(defaultDrawable);
            }
            if(defaultDrawable != -1) {
                // User has set the defaultDrawable
                imageView.setImageDrawable(def);
            }
        }
        if(!hasSetGlobalScreenSize) {
            Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
            if (display != null) {
                if (display.getHeight() > 0 && display.getWidth() > 0) {
                    int screenWidth = display.getWidth();
                    ImageLoadRunnable.setScreenWidth(screenWidth);
                    ImageDownloadRunnable.setScreenWidth(screenWidth);
                    hasSetGlobalScreenSize = true;
                }
            }
        }
        ImageLoadRunnable fileLoader = new ImageLoadRunnable(url, imageView, context, this, bitmapDisplayer);
        storagePool.execute(fileLoader);
    }
    
    public File getFile(String url, Activity activity) {
        File storage = Util.getStorageDirectory(activity);
        File image = new File(storage, String.valueOf(url.hashCode()));
        return image;
    }
    
    @Override
    public void needsDownloaded(String url, ImageView imageView, Context context, BitmapDisplayer bitmapDisplayer) {
        ImageDownloadRunnable fileDownloader = new ImageDownloadRunnable(url, imageView, context, bitmapDisplayer);
        downloadPool.execute(fileDownloader);
    }
      
    /*
     * Clear the In Memory Cache
     */
    public void clearInMemoryCache() {
        cache.clear();
    }
    
    /*
     * Delete every cached image stored on the filesystem
     */
    public void clearDiskCache(Context context) {
        storagePool.execute(new CacheCleanRunnable(context, true));
    }
    
    /*
     * Delete images which haven't been modified in the last 2 days
     */
    public void cleanImageCache(Context context) {
        storagePool.execute(new CacheCleanRunnable(context));
    }
    
    /*
     * Delete images which haven't been modified in the 
     * {time} milliseconds
     */
    public void cleanImageCache(Context context, long time) {
        storagePool.execute(new CacheCleanRunnable(context));
    }
    
    public static void setDefaultDrawableId(int id) {
        defaultDrawable = id;
    }
    
    private static ImageLoader mInstance; 
    
    public static ImageLoader getInstance() {
        if(mInstance == null) {
            mInstance = new ImageLoader();
        }
        return mInstance;
    }
    
    private void log(String msg) {
        Log.v(tag, msg);
    }
}
