package org.bitseal.services;

import info.guardianproject.cacheword.CacheWordHandler;

import org.bitseal.activities.LockScreenActivity;
import org.bitseal.activities.SplashScreenActivity;
import org.bitseal.core.App;
import org.bitseal.database.DatabaseContentProvider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Handles the process of locking app.
 * 
 * @author Jonathan Coe
 */
@SuppressLint("InlinedApi")
public class AppLockHandler
{
	/**
	 * Does all the work necessary to securely lock the application.
	 * 
	 * @param cacheWordHandler - An instance of CacheWordHandler to be
	 * provided by the caller of this method
	 */
	public static void runLockRoutine(CacheWordHandler cacheWordHandler)
	{
		// Destroy the cached database encryption key
    	cacheWordHandler.lock();
    	    	
    	// Close the database
    	DatabaseContentProvider.closeDatabase();
    	
    	// Open the splash screen activity. Doing this makes the process of restarting the app appear much smoother to the user. 
    	Context appContext = App.getContext();
        Intent intent0 = new Intent(appContext, SplashScreenActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) // FLAG_ACTIVITY_CLEAR_TASK only exists in API 11 and later 
        {
        	intent0.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);// Clear the stack of activities
        }
        else
        {
        	intent0.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        appContext.startActivity(intent0);
    	
    	// Exit the app, releasing all resources
    	System.exit(0);
    	
    	// Restart the app, opening the lock screen activity
        Intent intent1 = new Intent(appContext, LockScreenActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) // FLAG_ACTIVITY_CLEAR_TASK only exists in API 11 and later 
        {
        	intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);// Clear the stack of activities
        }
        else
        {
        	intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        appContext.startActivity(intent1);
	}
}