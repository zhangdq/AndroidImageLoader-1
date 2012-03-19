package com.darrenmowat.boothr.data.imageloader.util;

import android.os.Environment;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ImageLoaderUtils {
    
    public static String getImageFileName(String url) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.reset();
            digest.update(url.getBytes());
            byte[] a = digest.digest();
            int len = a.length;
            StringBuilder sb = new StringBuilder(len << 1);
            for (int i = 0; i < len; i++) {
                sb.append(Character.forDigit((a[i] & 0xf0) >> 4, 16));
                sb.append(Character.forDigit(a[i] & 0x0f, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return url.hashCode() + "";
        }
    }

    
    public static boolean hasStorage(boolean requireWriteAccess) {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            if (requireWriteAccess) {
                boolean writable = Environment.getExternalStorageState().equals(
                        Environment.MEDIA_MOUNTED_READ_ONLY);
                return !writable;
            } else {
                return true;
            }
        } else if (!requireWriteAccess && Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public static File getExternalCacheDirectory() {
        File f = new File("/sdcard/android/data/com.darrenmowat.boothr/cache/");
        if (!f.exists()) f.mkdirs();
        return f;
    }

    public static File getInternalCacheDirectory() {
        File f = new File("/data/data/com.darrenmowat.boothr/cache/");
        if (!f.exists()) f.mkdirs();
        return f;
    }
    
}
