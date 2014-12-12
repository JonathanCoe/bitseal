package org.bitseal.activities;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import java.security.GeneralSecurityException;
import java.util.Timer;
import java.util.TimerTask;

import org.bitseal.R;
import org.bitseal.services.NotificationsService;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
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
    
    private Context mActivityContext;
    
    private CacheWordHandler mCacheWordHandler;
    
    private TimerTask mUnlockAttemptTimerTask;
    
    /** Records whether onCacheWordOpened() has been called during the lifetime of this activity */
    private boolean mOnCacheWordOpenedCalled;
    
    /** This variable is used as a flag to detect times when attempts to unlock the database hang without giving any result */
    private boolean mLastUnlockAttemptCompleted;
    
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
		
		mActivityContext = this;
		
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
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
					{
						// Post Jelly Bean devices should generally be fast enough that the app will either unlock or show an 'invalid' message
						// almost instantly, so showing this message is only useful on older, slower devices. 
						Toast.makeText(getBaseContext(), "Checking passphrase...", Toast.LENGTH_SHORT).show();
					}
					
					new AttemptUnlockTask().execute(new String[]{enteredPassphrase}); // Attempt to unlock the app using the passphrase entered by the user
				}
				else
				{
					Toast.makeText(getBaseContext(), "Invalid passphrase", Toast.LENGTH_SHORT).show();
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
			Log.i(TAG, "The passphrase entered is too short - only " + passphrase.length() + " characters in length.\n" +
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
        	Log.i(TAG, "LockScreenActivity.AttemptUnlockTask.execute() called");
        	
        	// Sometimes the call to mCacheWordHandler.setPassphrase() can hang indefinitely, with no result or exception.
        	// Therefore we will set a timer task to detect when this happens and handle it.
        	mLastUnlockAttemptCompleted = false;
        	mUnlockAttemptTimerTask = new TimerTask() 
        	{          
        	    @Override
        	    public void run()
        	    {
        	    	if (mLastUnlockAttemptCompleted == false)
        	    	{
            	    	Log.e(TAG, "We detected that the last unlock attempt hung, without giving any result. We will now attempt to handle this error.");
            	    	unlockIcon.setClickable(true);
            			mCacheWordHandler = new CacheWordHandler(mActivityContext);
            			mCacheWordHandler.connectToService();
        	    	}
        	    }
        	};
        	new Timer().schedule(mUnlockAttemptTimerTask, 4000); // Run after 4 seconds
    		
    		try 
            {
    			mCacheWordHandler.setPassphrase(enteredPassphrase[0].toCharArray());
            }
            catch (GeneralSecurityException e) // If the passphrase is invalid, a GeneralSecurityException will be thrown
            {
                Log.i(TAG, "The passphrase entered by the user was invalid, resulting in a GeneralSecurityException");
                mLastUnlockAttemptCompleted = true;
                return false;
            }
            catch (Exception e)
            {
                Log.e(TAG, "Cacheword passphrase verification failed, throwing an unknown exception. The exception message was:\n"
                		+ e.getMessage());
                mLastUnlockAttemptCompleted = true;
                return false;
            }
    		mLastUnlockAttemptCompleted = true;
            return true;
        }
        
        @Override
        protected void onPostExecute(Boolean passphraseCorrect) 
        {
        	unlockIcon.setClickable(true);
        	
        	if (passphraseCorrect)
        	{
        		openBitseal();
        	}
        	else
        	{
        		Toast.makeText(getBaseContext(), "Invalid passphrase", Toast.LENGTH_SHORT).show();
        	}
        }
    }
    
    /**
     * Checks whether the correct conditions to open the app have been met and, if so, 
     * opens the app. 
     */
    @SuppressLint("InlinedApi")
	private void openBitseal()
    {
		// Check whether onCacheWordOpened() has been called
    	if (mOnCacheWordOpenedCalled)
		{
			// Reset this flag
			mOnCacheWordOpenedCalled = false;
			
			// Cancel the 'handle hung unlock attempts' timer task
			mUnlockAttemptTimerTask.cancel();
			
			// Clear any 'unlock' notifications
			NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.cancel(NotificationsService.getUnlockNotificationId());
			
			// Open the Inbox Activity
			Intent intent = new Intent(getBaseContext(), InboxActivity.class);
			intent.putExtra(EXTRA_DATABASE_UNLOCKED, true);
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
			Log.e(TAG, "LockScreenActivity.openBitseal() was called, but onCacheWordOpened() has not yet been called!");
			// Attempt to handle this failure
			mCacheWordHandler = new CacheWordHandler(mActivityContext);
			mCacheWordHandler.connectToService();
			new AttemptUnlockTask().execute(new String[]{enterPassphraseEditText.getText().toString()});
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
	
	@Override
	public void onCacheWordLocked()
	{
		Log.i(TAG, "LockScreenActivity.onCacheWordLocked() called.");
		// We are already at the lock screen activity, so there's nothing to do here
	}

	@SuppressLint("InlinedApi")
	@Override
	public void onCacheWordOpened()
	{
		Log.i(TAG, "LockScreenActivity.onCacheWordOpened() called.");
		mOnCacheWordOpenedCalled = true;
	}
	
	@Override
	public void onCacheWordUninitialized()
	{
		Log.i(TAG, "LockScreenActivity.onCacheWordUninitialized() called.");
		// Nothing to do here
	}
}