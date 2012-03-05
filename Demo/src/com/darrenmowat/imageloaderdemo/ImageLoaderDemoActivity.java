
package com.darrenmowat.imageloaderdemo;

import com.darrenmowat.imageloader.ImageLoader;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;

public class ImageLoaderDemoActivity extends ListActivity {

    private ImageAdapter mAdapter;
    private ArrayList<String> urls;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Set the ImageLoaders default drawable
        // This is displayed whilst images are downloading
        ImageLoader.setDefaultDrawableId(R.drawable.ic_launcher);
        
        urls = new ArrayList<String>();

        urls.add("http://cdn.androidpolice.com/wp-content/themes/ap1/images/android1.png");
        urls.add("http://developer.android.com/sdk/images/2.3/home-plain.png");
        urls.add("http://www.bestvpnservice.com/blog/wp-content/uploads/2011/04/android-vpn.jpg");
        urls.add("http://fxtrade.oanda.com/wandacache/1-oanda-android-forex-rate-chart-vert-360-ff9f184301efb73ba9dbf62d6f6aafc9134a770d.png");
        urls.add("http://4.bp.blogspot.com/-9Ez2y71RLWY/TVmisYUffnI/AAAAAAAAABs/iN2LC8JQTNw/s400/android-bumblebee-holo.png");
        urls.add("http://www.thebiblescholar.com/android_awesome.jpg");
        // Duplicates
        urls.add("http://cdn.androidpolice.com/wp-content/themes/ap1/images/android1.png");
        urls.add("http://developer.android.com/sdk/images/2.3/home-plain.png");
        urls.add("http://www.bestvpnservice.com/blog/wp-content/uploads/2011/04/android-vpn.jpg");
        urls.add("http://fxtrade.oanda.com/wandacache/1-oanda-android-forex-rate-chart-vert-360-ff9f184301efb73ba9dbf62d6f6aafc9134a770d.png");
        urls.add("http://4.bp.blogspot.com/-9Ez2y71RLWY/TVmisYUffnI/AAAAAAAAABs/iN2LC8JQTNw/s400/android-bumblebee-holo.png");
        urls.add("http://www.thebiblescholar.com/android_awesome.jpg");
        // More Duplicates
        urls.add("http://cdn.androidpolice.com/wp-content/themes/ap1/images/android1.png");
        urls.add("http://developer.android.com/sdk/images/2.3/home-plain.png");
        urls.add("http://www.bestvpnservice.com/blog/wp-content/uploads/2011/04/android-vpn.jpg");
        urls.add("http://fxtrade.oanda.com/wandacache/1-oanda-android-forex-rate-chart-vert-360-ff9f184301efb73ba9dbf62d6f6aafc9134a770d.png");
        urls.add("http://4.bp.blogspot.com/-9Ez2y71RLWY/TVmisYUffnI/AAAAAAAAABs/iN2LC8JQTNw/s400/android-bumblebee-holo.png");
        urls.add("http://www.thebiblescholar.com/android_awesome.jpg");

        mAdapter = new ImageAdapter(urls, ImageLoaderDemoActivity.this);
        setListAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Clear Disk Cache");
        menu.add("Clear In Memory Cache");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle().equals("Clear Disk Cache")) {
            ImageLoader.getInstance().clearDiskCache(this);
            Toast.makeText(this, "On Disk Cache has been queued for clearing...", Toast.LENGTH_LONG).show();
            return true;
        } else if (item.getTitle().equals("Clear In Memory Cache")) {
            ImageLoader.getInstance().clearInMemoryCache();
            Toast.makeText(this, "In Memory Cache Cleared", Toast.LENGTH_LONG).show();
            return true;
        } 
        return super.onOptionsItemSelected(item);
    }
}
