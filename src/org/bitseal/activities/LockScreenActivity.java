package org.bitseal.activities;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import java.security.GeneralSecurityException;

import org.bitseal.R;
import org.bitseal.services.NotificationsService;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * The Activity class for the app's 'Lock' screen. 
 * 
 * @author Jonathan Coe
 */
public class LockScreenActivity extends Activity implements ICacheWordSubscriber
{
    private EditText enterPassphraseEditText;
    
    private ImageView unlockIcon;
    
    private CacheWordHandler mCacheWordHandler;
    
    /** The minimum length we will allow for a database encryption passphrase */
    private static final int MINIMUM_PASSPHRASE_LENGTH = 8;
    
    /** Signals to the InboxActivity that the database has just been unlocked, so it should not redirect
     * the user to the lock screen. */
    public static final String EXTRA_DATABASE_UNLOCKED = "lockScreenActivity.DATABASE_UNLOCKED";
    
    private static final String TAG = "LOCK_SCREEN_ACTIVITY";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lock_screen);
		
		mCacheWordHandler = new CacheWordHandler(this);
		mCacheWordHandler.connectToService();
		
		enterPassphraseEditText = (EditText) findViewById(R.id.lock_screen_enter_passphrase_edittext);
		
		unlockIcon = (ImageView) findViewById(R.id.lock_screen_unlock_icon_imageview);
		unlockIcon.setOnClickListener(new View.OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Lock screen unlock button clicked");
				
				unlockIcon.setClickable(false);
				
				String enteredPassphrase = enterPassphraseEditText.getText().toString();
				
				// Validate the passphrase entered by the user
				if (validatePassphrase(enteredPassphrase))
				{
					Toast.makeText(getBaseContext(), "Checking passphrase...", Toast.LENGTH_LONG).show();
					
					// Attempt to unlock the app using the passphrase entered by the user
					new AttemptUnlockTask().execute(new String[]{enteredPassphrase});
				}
				else
				{
					Toast.makeText(getBaseContext(), "The passphrase must be at least " + MINIMUM_PASSPHRASE_LENGTH + " characters long", Toast.LENGTH_SHORT).show();
					unlockIcon.setClickable(true);
				}

			}
		});
	}
    
    /**
     * Validates a passphrase entered by the user
     * 
     * @param passphrase - The passphrase
     * 
     * @return A boolean indicating whether or not the passphrase is valid
     */
    private boolean validatePassphrase(String passphrase)
    {
		// Check the length of the passphrase
		if (passphrase.length() < MINIMUM_PASSPHRASE_LENGTH)
		{
			Log.e(TAG, "The passphrase entered is too short - only " + passphrase.length() + " characters in length.\n" +
					"The passphrase must be at least " + MINIMUM_PASSPHRASE_LENGTH + " characters in length");
			return false;
		}
		return true;
    }
    
	/**
	 * Attempts to unlock the encrypted database using the passphrase provided.
	 */
    class AttemptUnlockTask extends AsyncTask<String, Void, Boolean> 
    {
        @Override
    	protected Boolean doInBackground(String... enteredPassphrase)
        {
        	try 
            {
            	mCacheWordHandler.setPassphrase(enteredPassphrase[0].toCharArray());
            }
            catch (GeneralSecurityException e) 
            {
                Log.e(TAG, "Cacheword passphrase verification failed: " + e.getMessage());
                return false;
            }
            
            return true;
        }
        
        @Override
        protected void onPostExecute(Boolean result) 
        {
        	if (result == false)
        	{
        		Toast.makeText(getBaseContext(), "Invalid passphrase", Toast.LENGTH_SHORT).show();
        		unlockIcon.setClickable(true);
        	}
        }
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
	
	@SuppressLint("InlinedApi")
	@Override
	public void onCacheWordLocked()
	{
		Log.i(TAG, "LockScreenActivity.onCacheWordLocked() called.");
		// We are already at the lock screen activity, so there's nothing to do here
	}

	@Override
	public void onCacheWordOpened()
	{
		Log.i(TAG, "LockScreenActivity.onCacheWordOpened() called.");
		
		// Clear any 'unlock' notifications
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(NotificationsService.getUnlockNotificationId());
		
		// Open the Inbox Activity
		Intent intent = new Intent(getBaseContext(), InboxActivity.class);
		intent.putExtra(EXTRA_DATABASE_UNLOCKED, true);
        startActivityForResult(intent, 0);
	}
	
	@Override
	public void onCacheWordUninitialized()
	{
		// Nothing to do here
	}
}