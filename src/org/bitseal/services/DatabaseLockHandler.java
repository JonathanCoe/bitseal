package org.bitseal.services;

import info.guardianproject.cacheword.CacheWordHandler;

import org.bitseal.activities.LockScreenActivity;
import org.bitseal.core.App;
import org.bitseal.database.DatabaseContentProvider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Handles tasks related to locking the database.
 * 
 * @author Jonathan Coe
 */
@SuppressLint("InlinedApi")
public class DatabaseLockHandler
{
	/**
	 * Does all the work necessary to securely lock the database.
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
    	
    	// Open the lock screen activity
    	Context appContext = App.getContext();
        Intent intent = new Intent(appContext, LockScreenActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) // FLAG_ACTIVITY_CLEAR_TASK only exists in API 11 and later 
        {
        	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);// Clear the stack of activities
        }
        else
        {
        	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        appContext.startActivity(intent);
	}
}