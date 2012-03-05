package com.darrenmowat.imageloader.cache;

import android.graphics.Bitmap;

import java.lang.ref.SoftReference;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Using SoftReferences as a cache in android is not very efficient.
 * Almost guaranteed that the image your looking for in the cache 
 * won't be here.
 * 
 * However as it uses SoftReferences it means that currently referenced images
 * i.e. by an ImageView can't be GC'd
 */
public class BitmapCache extends Observable{

    private ConcurrentHashMap<String, SoftReference<Bitmap>> cache;
    
    public BitmapCache() {
        cache = new ConcurrentHashMap<String, SoftReference<Bitmap>>();
    }
    
    public void put(String key, Bitmap value) {
        if(key == null || value == null) {
            return;
        }
        // Put's are usually done on a background thread
        // This will keep the HashMap tidy
        // With no collected SoftReferences
        purgeGarbageCollectedValues();
        cache.putIfAbsent(key, new SoftReference<Bitmap>(value));
    }
    
    public boolean contains(String key) {
        return get(key) != null;
    }
    
    public Bitmap get(String key) {
        if(key == null) {
            return null;
        }
        SoftReference<Bitmap> ref = cache.get(key);
        if(ref == null) {
            return null;
        }
        // If the Bitmap has been GC'd
        if(ref.get() == null) {
            cache.remove(key);
            return null;
        }
        return ref.get();
    }
    
    public void remove(String key) {
        if(key == null) {
            return;
        }
        cache.remove(key);
    }
    
    public void clear() {
        cache.clear();
        setChanged();
        notifyObservers();
    }
    
    public void purgeGarbageCollectedValues() {
        Set<String> keys = cache.keySet();
        for(String key : keys) {
            if(cache.containsKey(key)) {
                SoftReference<Bitmap> ref = cache.get(key);
                if(ref == null || ref.get() == null) {
                    // This reference has been Garbage Collected
                    cache.remove(key);
                }
            }
        }
    }
    
    
    private static BitmapCache mInstance;
    
    public static BitmapCache getInstance() {
        if(mInstance == null) {
            mInstance = new BitmapCache();
        } 
        return mInstance;
    }
}
