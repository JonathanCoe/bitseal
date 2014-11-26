package org.bitseal.core;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import org.bitseal.activities.LockScreenActivity;
import org.bitseal.crypt.PRNGFixes;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

public class App extends Application implements ICacheWordSubscriber 
{
    /**
     * Keeps a reference of the application context
     */
    private static Context sContext;
    
    /** The key for a boolean variable that records whether or not a user-defined database encryption passphrase has been saved */
    private static final String KEY_DATABASE_PASSPHRASE_SAVED = "databasePassphraseSaved";
    
    private CacheWordHandler mCacheWordHandler;
    
    private static final String TAG = "APP";

    @Override
    public void onCreate() 
    {
        super.onCreate();
        sContext = getApplicationContext();
        
        PRNGFixes.apply();
        
        // Check whether the user has set a database encryption passphrase
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		boolean databasePassphraseSaved= prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false);
		if (databasePassphraseSaved)
		{
			// Start and subscribe to the CacheWordService
	        mCacheWordHandler = new CacheWordHandler(sContext, this);
	        mCacheWordHandler.connectToService();
		}
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
			
		// Start the 'lock screen' activity
        Intent intent = new Intent(sContext, LockScreenActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) // FLAG_ACTIVITY_CLEAR_TASK only exists in API 11 and later
        {
        	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);// Clear the stack of activities
        }
        else
        {
        	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        sContext.startActivity(intent);
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