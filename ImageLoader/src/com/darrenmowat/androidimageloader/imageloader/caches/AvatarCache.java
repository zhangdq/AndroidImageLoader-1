
package com.darrenmowat.androidimageloader.imageloader.caches;

/**
 * An Lru Cache used to store small images (Avatars) in Boothr
 * <p/>
 * This is used as the big cache (ImageCache) is cleared too quickly
 */
public class AvatarCache extends DrawableLruCache {

    private static AvatarCache mInstance;
    private static int memoryClass = 0;

    public AvatarCache(final int capacity) {
        super(capacity, "AvatarCache");
    }

    public static AvatarCache getInstance() {
        if (mInstance == null) {
            mInstance = new AvatarCache(getMaxSize(memoryClass));
        }
        return mInstance;
    }

    public static void setMemoryClass(int m) {
        memoryClass = m;
    }

    private static int getMaxSize(int memoryClass) {
        if (memoryClass < 17)
            return 12;
        else if (memoryClass > 17 && memoryClass < 25)
            return 18;
        else if (memoryClass > 25 && memoryClass < 33) return 25;
        return 35;
    }
}
