
package com.darrenmowat.androidimageloader.imageloader.caches;

public final class ImageCache extends DrawableLruCache {

    private static ImageCache mInstance;
    private static int memoryClass = 0;

    public ImageCache(final int capacity) {
        super(capacity, "ImageCache");
    }

    public static ImageCache getInstance() {
        if (mInstance == null) {
            mInstance = new ImageCache(getMaxSize(memoryClass));
        }
        return mInstance;
    }

    public static void setMemoryClass(int m) {
        memoryClass = m;
    }

    private static int getMaxSize(int memoryClass) {
        if (memoryClass < 17)
            return 8;
        else if (memoryClass > 17 && memoryClass < 25)
            return 12;
        else if (memoryClass > 25 && memoryClass < 33) return 15;
        return 20;
    }
}
