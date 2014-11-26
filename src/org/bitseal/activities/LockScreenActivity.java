package org.bitseal.activities;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import java.security.GeneralSecurityException;

import org.bitseal.R;

import android.annotation.SuppressLint;
import android.app.Activity;
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
    
    /** The default passphrase for the database. This is NOT intended to provided any security value, 
     * but rather to give us an easy default value to work with when the user has chosen not to set
     * their own passphrase. */
    public static final String DEFAULT_DATABASE_PASSPHRASE = "default123";
    
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
				}

			}
		});
	}
	
    @Override
    protected void onStop()
    {
    	super.onStop();
    	mCacheWordHandler.disconnectFromService();
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
            	mCacheWordHandler.connectToService();
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
        	}
        }
    }
	
	@SuppressLint("InlinedApi")
	@Override
	public void onCacheWordLocked()
	{
		Log.d(TAG, "TEMPORARY: LockScreenActivity.onCacheWordLocked() called.");
		
		// We are already at the lock screen activity, so there's nothing to do here
	}

	@Override
	public void onCacheWordOpened()
	{
		Log.d(TAG, "TEMPORARY: LockScreenActivity.onCacheWordOpened() called.");
		
		// TODO: At this stage in your app you may call getCachedSecrets() to retrieve the unencrypted secrets from CacheWord.
		
		Intent intent = new Intent(getBaseContext(), InboxActivity.class);
        startActivityForResult(intent, 0);
	}
	
	@Override
	public void onCacheWordUninitialized()
	{
		Log.d(TAG, "TEMPORARY: LockScreenActivity.onCacheWordUninitialized() called.");
		
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