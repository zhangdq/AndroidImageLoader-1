
package com.darrenmowat.imageloader;

import com.darrenmowat.imageloader.cache.BitmapCache;
import com.darrenmowat.imageloader.util.FlushedInputStream;
import com.darrenmowat.imageloader.util.Util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageDownloadRunnable implements Runnable {

    private String url;
    private ImageView imageView;
    private Activity activity;

    private BitmapCache cache;

    private static int screenWidth = 800;

    public ImageDownloadRunnable(String url, ImageView imageView, Activity activity) {
        this.url = url;
        this.imageView = imageView;
        this.activity = activity;
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
        
        Bitmap bitmap = downloadImage(url);
        
        if(bitmap == null) {
            // Failed
            return;
        }
        
        activity.runOnUiThread(new BitmapDisplayer(imageView, bitmap, url));
        cache.put(url, bitmap);
    }

    private Bitmap downloadImage(String url) {
        final DefaultHttpClient client = new DefaultHttpClient();
        final HttpGet getRequest = new HttpGet(url);

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                return null;
            }

            final HttpEntity entity = response.getEntity();
            boolean consumed = false;
            if (entity != null) {
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    inputStream = entity.getContent();

                    // TODO: This can throw OutOfMemory
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inTempStorage = new byte[16 * 1024];
                    Bitmap bitmap = BitmapFactory.decodeStream(new FlushedInputStream(inputStream),
                            null, options);
                    
                    entity.consumeContent();
                    consumed = true;
                    
                    if (bitmap == null) {
                        return null;
                    }
                    
                    // Push the file to the Disk
                    File outFile = new File(Util.getStorageDirectory(activity), String.valueOf(url.hashCode()));
                    FileOutputStream out = new FileOutputStream(outFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    // This bitmap is temporary - recycle this
                    bitmap.recycle();
                    
                    
                    return Util.loadDrawableFromStream(outFile, screenWidth);

                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    if (!consumed) {
                        entity.consumeContent();
                    }
                }
            }
        } catch (IOException e) {
            getRequest.abort();
            log("", e);
        } catch (IllegalStateException e) {
            getRequest.abort();
            log("", e);
        } catch (Exception e) {
            getRequest.abort();
            log("", e);
        }
        return null;
    }

    public static void setScreenWidth(int width) {
        screenWidth = width;
    }
    
    private void log(String msg) {
        Log.v("FileDownloader", msg);
    }
    
    private void log(String msg, Exception e) {
        Log.v("FileDownloader", msg, e);
    }
}
