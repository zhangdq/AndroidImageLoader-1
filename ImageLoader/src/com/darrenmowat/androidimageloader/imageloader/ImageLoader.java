
package com.darrenmowat.androidimageloader.imageloader;



import com.darrenmowat.androidimageloader.imageloader.caches.ImageCache;
import com.darrenmowat.androidimageloader.imageloader.util.ImageLoaderUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Stack;

public class ImageLoader {

    private static int defaultDrawable = R.drawable.icon;
    private static Drawable def;

    private ImageCache images;

    private PhotosQueue mDownloadQueue;
    private PhotosLoader mDownloadThread;

    private FileQueue mFileQueue;
    private FileLoader mFileLoaderThread;

    private ImageLoaderManager ilManagerThread;

    private static final boolean DEBUG = false;

    private boolean hasSize = false;

    private long lastImageSet;

    // Desire / Nexus One Screen Size
    private int screenWidth = 480;

    private static int memoryClass;

    public ImageLoader() {
        lastImageSet = System.currentTimeMillis();
        init();
    }

    private void init() {
        if (images == null) {
            ImageCache.setMemoryClass(memoryClass);
            images = ImageCache.getInstance();
        }
        if (mDownloadThread == null || mDownloadThread.isInterrupted()) {
            Log.v("ImageLoader Manager", "Started Image Download Thread");
            mDownloadQueue = new PhotosQueue();
            mDownloadThread = new PhotosLoader();
            mDownloadThread.setPriority(Thread.NORM_PRIORITY - 1);
            mDownloadThread.setName("ImageLoader: Image Downloader");
            mDownloadThread.start();
        }
        if (mFileLoaderThread == null || mFileLoaderThread.isInterrupted()) {
            Log.v("ImageLoader Manager", "Started Image File Loader Thread");
            mFileQueue = new FileQueue();
            mFileLoaderThread = new FileLoader();
            mFileLoaderThread.setPriority(Thread.NORM_PRIORITY);
            mFileLoaderThread.setName("ImageLoader: Image File Loader");
            mFileLoaderThread.start();
        }
        if (ilManagerThread == null || ilManagerThread.isInterrupted()) {
            Log.v("ImageLoader Manager", "Started Image Loader Manager Thread");
            ilManagerThread = new ImageLoaderManager();
            ilManagerThread.setPriority(Thread.MIN_PRIORITY);
            ilManagerThread.setName("ImageLoader: Thread Manager");
            ilManagerThread.start();
        }
    }

    public static void setMemoryClass(int m) {
        memoryClass = m;
    }

    public BitmapDrawable getImageFromCache(String u) {
        if (u == null) return null;
            return images.get(u);
    }

    public void removeImageFromCache(String u) {
        if (u.contains("avatars")) {
            avatars.remove(u);
        } else {
            images.remove(u);
        }
    }

    public void putImageInCache(String u, BitmapDrawable b) {
            images.put(u, b);
    }

    private int REQUIRED_SIZE = 600;

    public void setImage(String url, ImageView imageView, Activity a) {
        setImage(url, imageView, a, true);
    }

    public void setImage(String url, ImageView imageView, Activity a, boolean showIcon) {
        lastImageSet = System.currentTimeMillis();
        init();
        if (!hasSize && a != null) {
            // Get this phones display metrics so we can resize photos for this
            // screen
            Display display = ((WindowManager) a.getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
            if (display != null) {
                if (display.getHeight() > 0 && display.getWidth() > 0) {
                    screenWidth = display.getWidth();
                    REQUIRED_SIZE = screenWidth;
                    hasSize = true;
                }
            }
        }
        if (imageView != null) {
            imageView.setTag(url);
        }
        BitmapDrawable d = getImageFromCache(url);
        if (d != null) {
            if (d.getBitmap() != null && !d.getBitmap().isRecycled()) {
                if (imageView != null) {
                    imageView.setAdjustViewBounds(true);
                    imageView.setImageDrawable(d);
                    return;
                }
            } else {
                removeImageFromCache(url);
            }
        }
        if(url.trim().equals(""))
            return; // This url is empty
        queuePhoto(url, a, imageView);
        if (showIcon && a != null && imageView != null) {
            if (def == null) 
                def = a.getResources().getDrawable(defaultDrawable);
            imageView.setImageDrawable(def);

        }
    }

    private PhotoToLoad p;

    private void queuePhoto(String url, Activity activity, ImageView imageView) {
        // This ImageView may be used for other images before. So there may be
        // some old tasks in the queue. We need to discard them.
        if (imageView != null) {
            mFileQueue.cleanQueue(imageView);
        }
        p = new PhotoToLoad(url, imageView, activity);
        synchronized (mFileQueue.filesToLoad) {
            mFileQueue.filesToLoad.push(p);
            mFileQueue.filesToLoad.notifyAll();
        }
    }

    private BitmapDrawable downloadImage(PhotoToLoad p) {
        if (getImageFromCache(p.getUrl()) != null) {
            if (getImageFromCache(p.getUrl()).getBitmap() != null
                    && !getImageFromCache(p.getUrl()).getBitmap().isRecycled()) {
                return getImageFromCache(p.getUrl());
            } else {
                removeImageFromCache(p.getUrl()); // It has been recycled
            }
        }
        final String filename = ImageLoaderUtils.getImageFileName(p.getUrl());
        final File file;
        boolean extRead = ImageLoaderUtils.hasStorage(false);
        if (extRead) // don't need to read from it
            file = new File(ImageLoaderUtils.getExternalCacheDirectory(), filename);
        else
            file = new File(ImageLoaderUtils.getInternalCacheDirectory(), filename);

        try {
            // About to try and download the file, increment it's download tries
            p.incrementDownloadTryCount();
            HttpGet get = new HttpGet(p.getUrl());
            HttpResponse resp = HttpManager.execute(get);
            int status = resp.getStatusLine().getStatusCode();
            if (status != HttpURLConnection.HTTP_OK) {
                return null;
            }
            HttpEntity entity = resp.getEntity();
            if (entity != null) {
                BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
                InputStream is = bufHttpEntity.getContent();
                FileOutputStream fos = new FileOutputStream(file);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inTempStorage = new byte[16 * 1024];
                Bitmap temp = null;
                try {
                    temp = BitmapFactory.decodeStream(new FlushedInputStream(is), null, options);
                } catch (OutOfMemoryError e) {
                    images.shrink();
                    notifyCacheCleared();
                    System.gc();
                    try {
                        Thread.sleep(400); // Sleep for a second, allow some
                                           // memory to be reclaimed
                    } catch (InterruptedException e2) {
                    }
                    try {
                        temp = BitmapFactory
                                .decodeStream(new FlushedInputStream(is), null, options);
                    } catch (OutOfMemoryError e1) {
                        System.gc();
                        temp = BitmapFactory
                                .decodeStream(new FlushedInputStream(is), null, options);
                    }
                }
                is.close();
                entity.consumeContent();
                temp.compress(CompressFormat.PNG, 100, fos);
                temp.recycle();
                temp = null;
                fos.close();
                return loadDrawableFromStream(file);
            } else
                return null;
        } catch (Exception ex) {
            retryDownload(p, ex);
        }
        return null;
    }

    private void retryDownload(PhotoToLoad p, Exception ex) {
        if (!p.tooManyTries()) {
            Log.v(Boothr.getINFO(), "Retrying image at " + p.getUrl(), ex);
            synchronized (mDownloadQueue.photosToLoad) {
                mDownloadQueue.photosToLoad.push(p);
                mDownloadQueue.photosToLoad.notifyAll();
            }
        } else {
            Log.v(Boothr.getError(),
                    "Couldn't download image from DailyBooth after " + p.getDownloadTries()
                            + " tries : " + p.getUrl(), ex);
        }
    }

    private BitmapDrawable loadDrawableFromStream(File f) throws IOException {
        FileInputStream stream = new FileInputStream(f);
        Bitmap bitmap = null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream, null, o);
        stream.close();
        stream = null;
        stream = new FileInputStream(f);
        int scale = 1;
        if (o.outHeight > REQUIRED_SIZE || o.outWidth > REQUIRED_SIZE) {
            scale = (int) Math.pow(
                    2,
                    (int) Math.round(Math.log(REQUIRED_SIZE
                            / (double) Math.max(o.outHeight, o.outWidth))
                            / Math.log(0.5)));
        }
        // Decode with inSampleSize to reduce memory used
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        o2.inTempStorage = new byte[16 * 1024];
        try {
            bitmap = BitmapFactory.decodeStream(stream, null, o2);
            f.setLastModified(System.currentTimeMillis());
        } catch (OutOfMemoryError e) {
            // Going to attempt to fix this sit
            avatars.shrink();
            images.shrink();
            notifyCacheCleared();
            System.gc();
            log(e);
            try {
                Thread.sleep(400); // Sleep for a bit, allow some memory to be
                                   // reclaimed
            } catch (InterruptedException e2) {
                log(e2);
            }
            try {
                bitmap = BitmapFactory.decodeStream(stream, null, o2);
            } catch (OutOfMemoryError e1) {
                System.gc();
                bitmap = BitmapFactory.decodeStream(stream, null, o2);
                log(e1);
            }
        } finally {
            stream.close();
        }
        return new BitmapDrawable(bitmap);
    }

    class PhotosQueue {

        private final Stack<PhotoToLoad> photosToLoad = new Stack<PhotoToLoad>();

        // removes all instances of this ImageView
        public void cleanQueue(ImageView image) {
            try {
                synchronized (photosToLoad) {
                    for (int j = 0; j < photosToLoad.size();) {
                        if (photosToLoad.get(j).getImageView() == null
                                || photosToLoad.get(j).getImageView() == image) {
                            photosToLoad.remove(j);
                        } else {
                            ++j;
                        }
                    }
                }
            } catch (Exception e) {
            }
        }

        /*
         * Check the stack to see if any other ImageView is waiting for this
         * image.
         */
        public void checkQueue(String url, BitmapDrawable b) {
            try {
                synchronized (photosToLoad) {
                    for (int j = 0; j < photosToLoad.size();) {
                        if (photosToLoad.get(j).getImageView().getTag().equals(url)
                                && photosToLoad.get(j).getActivity() != null) {
                            if (photosToLoad.get(j).getImageView() != null) {
                                String tag = (String) photoToLoad.getImageView().getTag();
                                if (tag != null && tag.equals(photosToLoad.get(j).getUrl())) {
                                    BitmapDisplayer bd = new BitmapDisplayer(b, photosToLoad.get(j)
                                            .getImageView());
                                    photosToLoad.get(j).getActivity().runOnUiThread(bd);
                                }
                            }
                            photosToLoad.remove(j);
                        } else
                            j++;
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    class BitmapDisplayer implements Runnable {

        BitmapDrawable bitmap;
        ImageView imageView;

        public BitmapDisplayer(BitmapDrawable b, ImageView i) {
            bitmap = b;
            imageView = i;
        }

        @Override
        public void run() {
            if (bitmap != null)
                imageView.setImageDrawable(bitmap);
            else
                imageView.setImageResource(defaultDrawable);
            imageView.setAdjustViewBounds(true);
        }
    }

    private PhotoToLoad photoToLoad;

    class PhotosLoader extends Thread {
        
        boolean killed = false;
        
        public void kill() {
            killed = true;
        }
        
        @Override
        public void run() {
            try {
                while (true) {
                    // thread waits until there are any images to load in the
                    // queue
                    if (mDownloadQueue.photosToLoad.size() == 0)
                        synchronized (mDownloadQueue.photosToLoad) {
                            mDownloadQueue.photosToLoad.wait();
                        }
                    if (mDownloadQueue.photosToLoad.size() != 0) {
                        synchronized (mDownloadQueue.photosToLoad) {
                            photoToLoad = mDownloadQueue.photosToLoad.pop();
                        }
                        // The ImageView could have been GC'd whilst in the
                        // queue.
                        // This helps to purge out unnecessary downloads
                        if (photoToLoad.getImageView() != null) {
                            BitmapDrawable b = downloadImage(photoToLoad);
                            putImageInCache(photoToLoad.getUrl(), b);
                            // The ImageView could have been GC'd during the
                            // download
                            if (photoToLoad.getImageView() != null) {
                                String tag = (String) photoToLoad.getImageView().getTag();
                                if (tag != null && tag.equals(photoToLoad.getUrl())) {
                                    if (photoToLoad.getActivity() != null) {
                                        BitmapDisplayer bd = new BitmapDisplayer(b,
                                                photoToLoad.getImageView());
                                        photoToLoad.getActivity().runOnUiThread(bd);
                                    } else {
                                        Intent update = new Intent();
                                        update.setAction(WidgetUtils.UPDATE_WIDGET);
                                        photoToLoad.getImageView().getContext()
                                                .sendBroadcast(update);
                                    }
                                }
                            }
                            mDownloadQueue.checkQueue(photoToLoad.getUrl(), b);
                        }
                    }
                    if (killed || interrupted()) {
                        return;
                    }
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    class FileQueue {

        private final Stack<PhotoToLoad> filesToLoad = new Stack<PhotoToLoad>();

        // removes all instances of this ImageView
        public void cleanQueue(ImageView image) {
            try {
                synchronized (filesToLoad) {
                    for (int j = 0; j < filesToLoad.size();) {
                        if (filesToLoad.get(j).getImageView() == null
                                || filesToLoad.get(j).getImageView() == image) {
                            filesToLoad.remove(j);
                        } else {
                            ++j;
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    private PhotoToLoad fileToLoad;

    class FileLoader extends Thread {
        
        boolean killed = false;
        
        public void kill() {
            killed = true;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    // thread waits until there are any images to load in the
                    // queue
                    if (mFileQueue.filesToLoad.size() == 0)
                    // TODO: Stack can become empty between the test above and
                    // synchronising
                    // with it. Should the test also be synchronised?
                        synchronized (mFileQueue.filesToLoad) {
                            mFileQueue.filesToLoad.wait();
                        }
                    if (mFileQueue.filesToLoad.size() != 0) {
                        synchronized (mFileQueue.filesToLoad) {
                            fileToLoad = mFileQueue.filesToLoad.pop();
                        }
                        BitmapDrawable b = null;
                        if (getImageFromCache(fileToLoad.getUrl()) != null) {
                            if (getImageFromCache(fileToLoad.getUrl()).getBitmap() != null
                                    && !getImageFromCache(fileToLoad.getUrl()).getBitmap()
                                            .isRecycled()) {
                                b = getImageFromCache(fileToLoad.getUrl());
                            } else {
                                removeImageFromCache(fileToLoad.getUrl()); // It
                                                                           // has
                                // been
                                // recycled
                            }
                        }
                        final String filename = ImageLoaderUtils.getImageFileName(fileToLoad
                                .getUrl());
                        final File file;
                        boolean extRead = ImageLoaderUtils.hasStorage(false);
                        if (extRead) // don't need to read from it
                            file = new File(ImageLoaderUtils.getExternalCacheDirectory(), filename);
                        else
                            file = new File(ImageLoaderUtils.getInternalCacheDirectory(), filename);
                        if (file.exists()) {
                            try {
                                BitmapDrawable drawable = loadDrawableFromStream(file);
                                if (drawable != null) {
                                    b = drawable;
                                    putImageInCache(fileToLoad.getUrl(), b);
                                    if (fileToLoad.getImageView() != null) {
                                        String tag = (String) fileToLoad.getImageView().getTag();
                                        if (tag != null && tag.equals(fileToLoad.getUrl())) {
                                            if (fileToLoad.getActivity() != null) {
                                                BitmapDisplayer bd = new BitmapDisplayer(b,
                                                        fileToLoad.getImageView());
                                                fileToLoad.getActivity().runOnUiThread(bd);
                                            } else {
                                                Intent update = new Intent();
                                                update.setAction(WidgetUtils.UPDATE_WIDGET);
                                                fileToLoad.getImageView().getContext()
                                                        .sendBroadcast(update);
                                            }
                                        }
                                    }
                                } else {
                                    file.delete();
                                }
                            } catch (Exception ex) {
                            }
                        }
                        if (fileToLoad.getImageView() != null) {
                            mDownloadQueue.cleanQueue(fileToLoad.getImageView());
                        }
                        synchronized (mDownloadQueue.photosToLoad) {
                            mDownloadQueue.photosToLoad.push(fileToLoad);
                            mDownloadQueue.photosToLoad.notifyAll();
                        }
                    }
                    if (killed || interrupted()) {
                        return;
                    }
                }
            } catch (InterruptedException e) {
                return;

            }
        }
    }

    /* Methods for controlling the state of the threads */

    public void stopThreads() {
        Log.v("ImageLoader Manager", "Stopping Threads...");
        if (mDownloadThread != null && !mDownloadThread.isInterrupted()) {
            mDownloadThread.kill();
            mDownloadThread.interrupt();
            mDownloadThread = null;
            Log.v("ImageLoader Manager", "Stopped mDownloadThread");
        }
        synchronized (mDownloadQueue.photosToLoad) {
            mDownloadQueue.photosToLoad.clear();
        }
        if (mFileLoaderThread != null && !mFileLoaderThread.isInterrupted()) {
            mFileLoaderThread.kill();
            mFileLoaderThread.interrupt();
            mFileLoaderThread = null;
            Log.v("ImageLoader Manager", "Stopped mFileLoaderThread");
        }
        synchronized (mFileQueue.filesToLoad) {
            mFileQueue.filesToLoad.clear();
        }
        if (ilManagerThread != null && !ilManagerThread.isInterrupted()) {
            ilManagerThread.kill();
            ilManagerThread.interrupt();
            ilManagerThread = null;
            Log.v("ImageLoader Manager", "Stopped ilManagerThread");
        }
    }

    private CacheClearedListener mCacheListener = null;

    public void setCacheClearedListener(CacheClearedListener c) {
        mCacheListener = c;
    }

    public CacheClearedListener getCacheClearedListener() {
        return mCacheListener;
    }

    public void notifyCacheCleared() {
        if (mCacheListener != null) mCacheListener.cacheCleared();
    }

    private void log(Throwable e) {
        Log.v("BoothrInformation", "Exception", e);
    }

    @Override
    public String toString() {
        return "FileQueue: " + mFileQueue.filesToLoad.size() + " DownloadQueue: "
                + mDownloadQueue.photosToLoad.size();
    }

    class ImageLoaderManager extends Thread {
        
        boolean killed = false;
        
        public void kill() {
            killed = true;
        }

        @Override
        public void run() {
            try {
                Log.v("ImageLoader Manager", "Running");
                while (true) {
                    if (System.currentTimeMillis() > (lastImageSet + (1 * 60 * 1000))) {
                        stopThreads();
                    }
                    // This thread only wakes up every 2 minutes
                    Thread.sleep(1 * 60 * 1000);
                    if (killed || interrupted()) {
                        return;
                    }
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

}
