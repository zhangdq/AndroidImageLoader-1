
package com.darrenmowat.androidimageloader.imageloader;

public class IL extends ImageLoader {

    private static IL mInstance;
    
    public IL() {
        super();
    }

    public static IL getInstance() {
        if (mInstance == null) {
            mInstance = new IL();
        }
        return mInstance;
    }
    
}
