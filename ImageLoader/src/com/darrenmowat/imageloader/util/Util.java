package com.darrenmowat.imageloader.util;

import com.darrenmowat.imageloader.cache.BitmapCache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Util {

    
    public static Bitmap loadDrawableFromStream(File f, int screenWidth) throws IOException {
        FileInputStream stream = new FileInputStream(f);
        Bitmap bitmap = null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream, null, o);
        stream.close();
        stream = null;
        stream = new FileInputStream(f);
        int scale = 1;
        if (o.outWidth > screenWidth) {
            scale = (int) Math.pow(
                    2,
                    (int) Math.round(Math.log(screenWidth
                            / (double) Math.max(o.outHeight, o.outWidth))
                            / Math.log(0.5)));
        }
        // Decode with inSampleSize to reduce memory used
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        o2.inPreferredConfig = Bitmap.Config.ARGB_8888;
        o2.inTempStorage = new byte[16 * 1024];
        try {
            bitmap = BitmapFactory.decodeStream(stream, null, o2);
            f.setLastModified(System.currentTimeMillis());
        } catch (OutOfMemoryError e) {
            BitmapCache.getInstance().clear();
            try {
                bitmap = BitmapFactory.decodeStream(stream, null, o2);
            } catch (OutOfMemoryError failed) {
                return null;
            }
        } finally {
            stream.close();
        }
        return bitmap;
    }

    public static File getStorageDirectory(Context context) {
        boolean mExternalStorageAvailable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mExternalStorageAvailable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            mExternalStorageAvailable = true;
        } else {
            mExternalStorageAvailable = false;
        }
        if (mExternalStorageAvailable) {
            String packageName = context.getPackageName();
            File externalPath = Environment.getExternalStorageDirectory();
            File appFiles = new File(externalPath.getAbsolutePath() + "/Android/data/"
                    + packageName + "/images");
            appFiles.mkdirs();
            return appFiles;
        } else {
            File appFiles = context.getCacheDir();
            appFiles.mkdirs();
            return appFiles;
        }
    }
    
}
