package org.bitseal.core;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import org.bitseal.crypt.PRNGFixes;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.util.Log;

public class App extends Application implements ICacheWordSubscriber 
{
    /**
     * Keeps a reference of the application context 
     */
    private static Context sContext;
    
    private CacheWordHandler mCacheWordHandler;
    
    private static final String TAG = "APP";
    
    @Override
    public void onCreate() 
    {
        super.onCreate();
        sContext = getApplicationContext();
        
        PRNGFixes.apply();
        
		// Start and subscribe to the CacheWordService
        mCacheWordHandler = new CacheWordHandler(sContext, this);
        mCacheWordHandler.connectToService();
    }

    /**
     * Returns the application context. <br><br>
     * 
     * <b>NOTE!!!</b> There is no guarantee that the normal, non-static onCreate() will have been called before
     * this method is called. This means that this method can sometimes return null, particularly if called when the 
     * app has been running for a short time, e.g. during unit testing. 
     *
     * @return application context
     */
    public static Context getContext() 
    {
        return sContext;
    }
    
	@SuppressLint("InlinedApi")
	@Override
	public void onCacheWordLocked()
	{
		Log.d(TAG, "TEMPORARY: App.onCacheWordLocked() called.");
			
		// We don't want to do anything here - if we do then the lock screen activity is always launched as soon
		// as the user's device finishes booting
	}

	@Override
	public void onCacheWordOpened()
	{
		Log.d(TAG, "TEMPORARY: App.onCacheWordOpened() called.");
	}

	@Override
	public void onCacheWordUninitialized()
	{
		Log.d(TAG, "TEMPORARY: App.onCacheWordUninitialized() called.");
		
	    // Database encryption is currently not enabled by default, so there is nothing to do here
	}
}