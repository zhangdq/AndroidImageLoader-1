
*This library is no longer maintained. Checkout [NetworkImageView][2] instead*

Android Image Loader
====================

A simple Image Loader for Android which user Thread Pools to load files from the filesystem & download them from the Internet.

Android Image Loader uses a 2 tier cache to cache images in memory and on internal/external storage.


Usage
-----

Add AndroidImageLoader as an Android Library Project to your project.

First set the default drawable that ImageLoader should use. This should be done once when your application starts (i.e. In your Application class):

	ImageLoader.setDefaultDrawableId(R.drawable.icon);


Secondly:

	ImageLoader.getInstance().setImage(url, imageView, activity);
	
Or if you don't want a loading drawable:
	
	boolean setDefDrawable = false;
	ImageLoader.getInstance().setImage(url, imageView, activity, setDefDrawable);

    
Thirdly:
	
	...Thats it!
	
The image will be downloaded, saved to the disk & resized to best support the users screen size. 

You can also set custom code to display your bitmaps once they have been loaded / downloaded.

	public class CustomBitmapDisplayer implements BitmapDisplayer {

    @Override
    public void displayBitmap(ImageView imageView, Context context, Bitmap bitmap, 		String url) {
        	// Do something
    	}
	}
	
This is useful for getting notified when an Image has been downloaded. For example in Boothr I use a CustomBitmapDisplayer to notify me when an image has been downloaded for a Home Screen Widget. I can then notify the widget that the image is available.

	
Android Image Loader also has methods to clean out old files from the on disk image cache. You can delete images that haven't been used for 2 days by:

	ImageLoader.getInstance().cleanImageCache(context);
	
Or you can specify how old images can be before they are deleted:

	long time = 3 * 86400000; // 3 Days
	ImageLoader.getInstance().cleanImageCache(context, time);

These methods run on a background thread so they can be called from the UI thread.

Demo
-----
There is a demo application & an apk in the repository. 

Licence
-----

The code in this project is licensed under the Apache Software License 2.0.

Contributing
------------

1. Fork it.
2. Create a branch (`git checkout -b my_imageloader`)
3. Commit your changes (`git commit -am "Fixed something"`)
4. Push to the branch (`git push origin my_imageloader`)
5. Create an [Issue][1] with a link to your branch
6. Enjoy a refreshing Diet Coke and wait

[1]: https://github.com/DarrenMowat/AndroidImageLoader/issues
[2]: https://github.com/DarrenMowat/NetworkImageView