package org.bitseal.activities;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import org.bitseal.R;
import org.bitseal.database.DatabaseContentProvider;
import org.bitseal.services.NotificationsService;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * The Activity class for the app's 'splash screen'. 
 * 
 * @author Jonathan Coe
 */
public class SplashScreenActivity extends Activity implements ICacheWordSubscriber
{    
    private CacheWordHandler mCacheWordHandler;
    
    /** Signals to the InboxActivity that the database has just been unlocked, so it should not redirect
     * the user to the lock screen. */
    public static final String EXTRA_DATABASE_UNLOCKED = "lockScreenActivity.DATABASE_UNLOCKED";
    
    /** The key for a boolean variable that records whether or not a user-defined database encryption passphrase has been saved */
    private static final String KEY_DATABASE_PASSPHRASE_SAVED = "databasePassphraseSaved";
    
    /** The key for an extra that tells this activity to delay opening the main application for a few seconds */
	public static final String EXTRA_DELAY_APP_OPENING = "delayAppOpening";
	
	/** The amount of time in seconds by which we delay the app's opening when requested to do so */
	private static final int DELAY_OPENING_PERIOD_SECONDS = 3;
    
    private static final String TAG = "SPLASH_SCREEN_ACTIVITY";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash_screen);
		
		mCacheWordHandler = new CacheWordHandler(this);
		mCacheWordHandler.connectToService();
		
		new InitializeDatabaseTask().execute();
	}
    
    @Override
    protected void onStop()
    {
    	super.onStop();
    	if (mCacheWordHandler != null)
    	{
        	mCacheWordHandler.disconnectFromService();
    	}
    }
    
	/**
	 * Initializes the database
	 */
    class InitializeDatabaseTask extends AsyncTask<Void, Void, Void> 
    {
        @SuppressLint("InlinedApi")
		@Override
    	protected Void doInBackground(Void... params)
        {
			// Check whether the user has set a database encryption passphrase
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			if (prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false))
			{
				// Redirect to the lock screen activity
		        Intent intent = new Intent(getBaseContext(), LockScreenActivity.class);
		        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) // FLAG_ACTIVITY_CLEAR_TASK only exists in API 11 and later 
		        {
		        	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);// Clear the stack of activities
		        }
		        else
		        {
		        	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		        }
		        startActivity(intent);
			}
			else
			{
				// Open the database using the default passphrase
				DatabaseContentProvider.attemptGetDefaultEncryptionKey();
			}
			return null;
        }
    }
	
	@SuppressLint("InlinedApi")
	@Override
	public void onCacheWordLocked()
	{
		Log.i(TAG, "SplashScreenActivity.onCacheWordLocked() called.");
		// Nothing to do here
	}

	@Override
	public void onCacheWordOpened()
	{
		Log.i(TAG, "SplashScreenActivity.onCacheWordOpened() called.");
		
		// Clear any 'unlock' notifications
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(NotificationsService.getUnlockNotificationId());
		
		// If the extra to delay opening the main app has been set, wait for a couple of seconds
		if (this.getIntent().hasExtra(EXTRA_DELAY_APP_OPENING))
		{
			Log.i(TAG, "Delaying the opening of the inbox by " + DELAY_OPENING_PERIOD_SECONDS + " seconds");
			new Handler().postDelayed(new Runnable()
			{
                @Override
                public void run()
                {
                	openInboxActivity();
                }
            }, DELAY_OPENING_PERIOD_SECONDS * 1000);
		}
		else
		{
			openInboxActivity();
		}
	}
	
	/** Opens the Inbox Activity */
	private void openInboxActivity()
	{
		Intent intent = new Intent(getBaseContext(), InboxActivity.class);
		intent.putExtra(EXTRA_DATABASE_UNLOCKED, true);
        startActivityForResult(intent, 0);
	}
	
	@Override
	public void onCacheWordUninitialized()
	{
		Log.i(TAG, "SplashScreenActivity.onCacheWordUninitialized() called.");
		// Nothing to do here
	}
}