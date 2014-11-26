package org.bitseal.core;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import java.security.GeneralSecurityException;

import org.bitseal.activities.LockScreenActivity;
import org.bitseal.crypt.PRNGFixes;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class App extends Application implements ICacheWordSubscriber 
{
    /**
     * Keeps a reference of the application context
     */
    private static Context sContext;
    
    private CacheWordHandler mCacheWordHandler;
    
    private static final String TAG = "APP";
    
    /** The default passphrase for the database. This is NOT intended to provided any security value, 
     * but rather to give us an easy default value to work with when the user has chosen not to set
     * their own passphrase. */
    public static final String DEFAULT_DATABASE_PASSPHRASE = "default123";
    
    //TODO: private static final int DATABASE_ENCRYPTION_TIMEOUT_SECONDS = 604800; // Currently set to 1 week

    @Override
    public void onCreate() 
    {
        super.onCreate();
        sContext = getApplicationContext();
        
        PRNGFixes.apply();
        
		// Start and subscribe to the CacheWordService
        mCacheWordHandler = new CacheWordHandler(sContext, this);
        mCacheWordHandler.connectToService();
        
        if (mCacheWordHandler.isCacheWordInitialized() == false)
        {
        	onCacheWordUninitialized();
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
		
		// DONE: You should clear all UI components and data structures containing sensitive information and perhaps show a dedicated lock screen		
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
        
        // TODO: At this stage your app should prompt the user for the passphrase and give it to CacheWord with setCachedSecrets()
	}

	@Override
	public void onCacheWordOpened()
	{
		Log.d(TAG, "TEMPORARY: App.onCacheWordOpened() called.");
		
		// TODO: At this stage in your app you may call getCachedSecrets() to retrieve the unencrypted secrets from CacheWord.
	}

	@Override
	public void onCacheWordUninitialized()
	{
		Log.d(TAG, "TEMPORARY: App.onCacheWordUninitialized() called.");
		
	    // Set the default passphrase for the encrypted SQLite database - this is NOT intended to have any security value, but
	    // rather to give us a convenient default value to use when the user has not yet set a passphrase of their own. 
	    try
		{
	    	mCacheWordHandler.setPassphrase(DEFAULT_DATABASE_PASSPHRASE.toCharArray());
		}
		catch (GeneralSecurityException e)
		{
			Log.e(TAG, "Attempt to set the default database encryption passphrase failed.\n" + 
					"The GeneralSecurityException message was: " + e.getMessage());
		}
	}
}