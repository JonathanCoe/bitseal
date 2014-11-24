package org.bitseal.core;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import java.security.GeneralSecurityException;

import org.bitseal.activities.LockScreenActivity;
import org.bitseal.crypt.PRNGFixes;
import org.bitseal.database.DatabaseHelper;

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
    
    private CacheWordHandler mCacheWord;
    
    private static final String TAG = "APP";

    @Override
    public void onCreate() 
    {
        super.onCreate();
        sContext = getApplicationContext();
        
        PRNGFixes.apply();
        
		// Connect to the CacheWord service
        mCacheWord = new CacheWordHandler(getApplicationContext(), this);
        mCacheWord.connectToService();
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
		// This should be handled automatically by the DatabaseHelper class, which is a subclass of SQLCipherOpenHelper
	}

	@Override
	public void onCacheWordUninitialized()
	{
	    // Set the default passphrase for the encrypted SQLite database - this is NOT intended to have any security value, but
	    // rather to give us a convenient default value to use when the user has not yet set a passphrase of their own. 
	    try
		{
			mCacheWord.setPassphrase(DatabaseHelper.DEFAULT_DATABASE_PASSPHRASE.toCharArray());
		}
		catch (GeneralSecurityException e)
		{
			Log.e(TAG, "Attempt to set the default database encryption passphrase failed.\n" + 
					"The GeneralSecurityException message was: " + e.getMessage());
		}
	}
}