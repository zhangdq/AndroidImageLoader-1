
package com.darrenmowat.androidimageloader.imageloader.caches;

import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Map;

public class DrawableLruCache {

    private final LinkedHashMap<String, BitmapDrawable> map;

    private int size;
    private int maxSize;

    private int putCount;
    private int evictionCount;
    private int hitCount;
    private int missCount;
    private int shrinkCount;

    private int shrinkAttempts;
    private long lastShrinkOverflow;

    private boolean alwaysClear = false;

    private static final int ATTEMPT_THRESHOLD = 4;

    private String cacheName;

    public DrawableLruCache(int maxSize, String name) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0 " + toString());
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<String, BitmapDrawable>(0, 0.75f, true);
        if (name == null) name = "";
        cacheName = name;
    }

    /**
     * Returns the value for {@code key} if it exists in the cache or can be
     * created by {@code #create}. If a value was returned, it is moved to the
     * head of the queue. This returns null if a value is not cached and cannot
     * be created.
     */
    public final BitmapDrawable get(String key) {
        // Log.v("BoothrInformation", toString());
        // Log.v("BoothrInformation", memStat());
        if (key == null) {
            return null;
        }

        BitmapDrawable mapValue;
        synchronized (this) {
            mapValue = map.get(key);
            if (mapValue != null) {
                hitCount++;
                return mapValue;
            }
            missCount++;
        }
        return null;
    }

    /**
     * Caches {@code value} for {@code key}. The value is moved to the head of
     * the queue.
     * 
     * @return the previous value mapped by {@code key}.
     */
    public void put(String key, BitmapDrawable value) {
        if (key == null || value == null) {
            return;
        }

        BitmapDrawable previous;
        synchronized (this) {
            putCount++;
            size += safeSizeOf(key, value);
            previous = map.put(key, value);
            if (previous != null) {
                size -= safeSizeOf(key, previous);
            }
        }

        if (previous != null) {
            entryRemoved(false, key, previous, value, false);
        }
        trimToSize(maxSize, false);
    }

    /**
     * @param maxSize the maximum size of the cache before returning. May be -1
     *            to evict even 0-sized elements.
     */
    private void trimToSize(int maxSize, boolean recycle) {
        while (true) {
            String key;
            BitmapDrawable value;
            synchronized (this) {
                if (size < 0 || (map.isEmpty() && size != 0)) {
                    throw new IllegalStateException(getClass().getName()
                            + ".sizeOf() is reporting inconsistent results!  " + toString());
                }

                if (size <= maxSize || map.isEmpty()) {
                    break;
                }

                Map.Entry<String, BitmapDrawable> toEvict = map.entrySet().iterator().next();
                key = toEvict.getKey();
                value = toEvict.getValue();
                map.remove(key);
                size -= safeSizeOf(key, value);
                evictionCount++;
            }

            entryRemoved(true, key, value, null, recycle);
        }
        // Log.v("BoothrInformation", "Finished trimming cache to " + maxSize);
        // Log.v("BoothrInformation", toString());
        // Log.v("BoothrInformation", memStat());
    }

    /**
     * Removes the entry for {@code key} if it exists.
     * 
     * @return the previous value mapped by {@code key}.
     */
    public final BitmapDrawable remove(String key) {
        if (key == null) {
            return null;
        }

        BitmapDrawable previous;
        synchronized (this) {
            previous = map.remove(key);
            if (previous != null) {
                size -= safeSizeOf(key, previous);
            }
        }

        if (previous != null) {
            entryRemoved(false, key, previous, null, false);
        }

        return previous;
    }

    /**
     * Called for entries that have been evicted or removed. This method is
     * invoked when a value is evicted to make space, removed by a call to
     * {@link #remove}, or replaced by a call to {@link #put}. The default
     * implementation does nothing.
     * <p/>
     * <p>
     * The method is called without synchronization: other threads may access
     * the cache while this method is executing.
     * 
     * @param evicted true if the entry is being removed to make space, false if
     *            the removal was caused by a {@link #put} or {@link #remove}.
     * @param newValue the new value for {@code key}, if it exists. If non-null,
     *            this removal was caused by a {@link #put}. Otherwise it was
     *            caused by an eviction or a {@link #remove}.
     */
    protected synchronized void entryRemoved(boolean evicted, String key, BitmapDrawable oldValue,
            BitmapDrawable newValue, boolean recycle) {
        if (recycle) {
            if (oldValue != null) {
                if (oldValue.getBitmap() != null && !oldValue.getBitmap().isRecycled()) {
                    oldValue.getBitmap().recycle();
                    Log.v("BoothrInformation", "Recycled Bitmap");
                }
                oldValue = null;
            }
            if (newValue != null) {
                if (newValue.getBitmap() != null && !newValue.getBitmap().isRecycled()) {
                    newValue.getBitmap().recycle();
                    Log.v("BoothrInformation", "Recycled Bitmap");
                }
                newValue = null;
            }
        }
    }

    private int safeSizeOf(String key, BitmapDrawable value) {
        int result = sizeOf(key, value);
        if (result < 0) {
            throw new IllegalStateException("Negative size: " + key + "=" + value + " "
                    + toString());
        }
        return result;
    }

    /**
     * Returns the size of the entry for {@code key} and {@code value} in
     * user-defined units. The default implementation returns 1 so that size is
     * the number of entries and max size is the maximum number of entries.
     * <p/>
     * <p>
     * An entry's size must not change while it is in the cache.
     */
    protected int sizeOf(String key, BitmapDrawable value) {
        return 1;
    }

    private static final long TIME = 30000;

    public synchronized void shrink() {
        int s = size();
        // The cache paniced on this phone, always clear instead of shrinking
        if (alwaysClear) {
            clear();
            return;
        }
        if (shrinkAttempts > ATTEMPT_THRESHOLD) {
            clear();
            if (lastShrinkOverflow + TIME > System.currentTimeMillis()){
                alwaysClear = true;
            }
            lastShrinkOverflow = System.currentTimeMillis();
            shrinkAttempts = 0;
        } else {
            int shrinkBy = Math.round(size / 2);
            if (shrinkBy < size && shrinkBy > 0) {
                trimToSize(size() - shrinkBy, false);
                shrinkAttempts = 0;
                shrinkCount++;
            }
        }
        Log.v("AndroidImageLoader", cacheName + " was shrunk from " + s + " to " + size);
    }

    public synchronized void clear() {
        int s = size;
        evictAll();
        Log.v("AndroidImageLoader", cacheName + " was cleared from " + s);
    }

    /**
     * Clear the cache, calling {@link #entryRemoved} on each removed entry.
     */
    public final void evictAll() {
        trimToSize(-1, false); // -1 will evict 0-sized elements
    }

    /**
     * For caches that do not override {@link #sizeOf}, this returns the number
     * of entries in the cache. For all other caches, this returns the sum of
     * the sizes of the entries in this cache.
     */
    public synchronized final int size() {
        return size;
    }

    /**
     * For caches that do not override {@link #sizeOf}, this returns the maximum
     * number of entries in the cache. For all other caches, this returns the
     * maximum sum of the sizes of the entries in this cache.
     */
    public synchronized final int maxSize() {
        return maxSize;
    }

    /**
     * Returns the number of times {@link #get} returned a value.
     */
    public synchronized final int hitCount() {
        return hitCount;
    }

    public synchronized final int shrinkCount() {
        return shrinkCount;
    }

    /**
     * Returns the number of times {@link #get} returned null or required a new
     * value to be created.
     */
    public synchronized final int missCount() {
        return missCount;
    }

    public synchronized final int shrinkAttempts() {
        return shrinkAttempts;
    }

    /**
     * Returns the number of times {@link #put} was called.
     */
    public synchronized final int putCount() {
        return putCount;
    }

    /**
     * Returns the number of values that have been evicted.
     */
    public synchronized final int evictionCount() {
        return evictionCount;
    }

    /**
     * Returns a copy of the current contents of the cache, ordered from least
     * recently accessed to most recently accessed.
     */
    public synchronized final Map<String, BitmapDrawable> snapshot() {
        return new LinkedHashMap<String, BitmapDrawable>(map);
    }

    @Override
    public synchronized final String toString() {
        int accesses = hitCount + missCount;
        int hitPercent = accesses != 0 ? (100 * hitCount / accesses) : 0;
        return String
                .format("LruCache[maxSize=%d, currentSize=%d, saturated=%b, hits=%d, misses=%d, hitRate=%d%%, shrink=%d, alwaysClear=%b]",
                        maxSize, size, (maxSize == size), hitCount, missCount, hitPercent,
                        shrinkCount, alwaysClear);
    }

    public void recycle() {
        trimToSize(-1, true);
    }
}
