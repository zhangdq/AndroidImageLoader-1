package com.darrenmowat.imageloader;

import com.darrenmowat.imageloader.util.Util;

import android.content.Context;
import android.util.Log;

import java.io.File;

public class CacheCleanRunnable implements Runnable {

    private Context context;
    private boolean purgeAll;
    private long deleteOlderThan;

    private static long ONE_DAY =  86400000;
    private static long TWO_DAYS = ONE_DAY * 2;
    
    public CacheCleanRunnable(Context context) {
        this(context, false, TWO_DAYS);
    }
    
    public CacheCleanRunnable(Context context, boolean purgeAll) {
        this(context, purgeAll, TWO_DAYS);
    }
    
    public CacheCleanRunnable(Context context, long deleteOlderThan) {
        this(context, false, deleteOlderThan);
    }
    
    public CacheCleanRunnable(Context context, boolean purgeAll, long time) {
        this.context = context;
        this.purgeAll = purgeAll;
        this.deleteOlderThan = System.currentTimeMillis() - time;
    }
    
    @Override
    public void run() {
        File storage = Util.getStorageDirectory(context);
        if(!storage.exists()) {
            // Sanity Check
            return;
        }
        int cleaned = 0;
        File[] cached = storage.listFiles();
        for(File f : cached) {
            if(f != null) {
                if(purgeAll || f.lastModified() < deleteOlderThan) {
                    Log.v("CacheCleanRunnable", "Deleted: " + f.getAbsolutePath());
                    f.delete();
                    cleaned++;
                }
            }
        }
        Log.v("ImageLoaderCacheCleaner", "Deleted " + cleaned + " cached images");
    }

}
