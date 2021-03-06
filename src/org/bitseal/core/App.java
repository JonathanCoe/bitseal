package org.bitseal.core;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import org.bitseal.crypt.PRNGFixes;
import org.bitseal.services.ExceptionHandler;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

public class App extends Application implements ICacheWordSubscriber 
{
    /**
     * Keeps a reference of the application context 
     */
    private static Context sContext;
    
    private CacheWordHandler mCacheWordHandler;   
    
    @Override
    public void onCreate() 
    {
        super.onCreate();
        sContext = getApplicationContext();
        
        PRNGFixes.apply();
        
		// Start and subscribe to the CacheWordService
        mCacheWordHandler = new CacheWordHandler(sContext, this);
        mCacheWordHandler.connectToService();
        
        // Set up the uncaught exception handler for the application's main thread
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
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
		// Nothing to do here currently
	}

	@Override
	public void onCacheWordOpened()
	{
		// Nothing to do here currently
	}

	@Override
	public void onCacheWordUninitialized()
	{
		// Nothing to do here currently
	}
}