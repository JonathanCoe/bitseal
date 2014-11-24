package org.bitseal.activities;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import java.security.GeneralSecurityException;

import org.bitseal.R;
import org.bitseal.database.DatabaseHelper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
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
    
    private CacheWordHandler mCacheWord;
    
    private static final String TAG = "LOCK_SCREEN_ACTIVITY";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lock_screen);
		
		// Connect to the CacheWord service
        mCacheWord = new CacheWordHandler(getApplicationContext(), this);
        mCacheWord.connectToService();
		
		enterPassphraseEditText = (EditText) findViewById(R.id.lock_screen_enter_passphrase_edittext);
		
		unlockIcon = (ImageView) findViewById(R.id.lock_screen_unlock_icon_imageview);
		unlockIcon.setOnClickListener(new View.OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Lock screen unlock button clicked");
				
				String enteredPassphrase = enterPassphraseEditText.getText().toString();
				
				if (attemptUnlock(enteredPassphrase))
				{
                    // If the passphrase was valid
					Intent intent = new Intent(getBaseContext(), InboxActivity.class);
                    startActivityForResult(intent, 0);
				}
				else
				{
					Toast.makeText(getApplicationContext(), "Invalid passphrase", Toast.LENGTH_LONG).show();
				}
			}
		});
	}
	
	/**
	 * Attempts to unlock the encrypted database using the passphrase provided
	 * 
	 * @param enteredPassphrase - The passphrase entered by the user
	 * 
	 * @return A boolean indicating whether or not the passphrase was valid
	 */
	private boolean attemptUnlock(String enteredPassphrase)
	{
        // Check the passphrase
        try 
        {
            mCacheWord.setPassphrase(enteredPassphrase.toCharArray());
        } 
        catch (GeneralSecurityException e) 
        {
            Log.e(TAG, "Cacheword passphrase verification failed: " + e.getMessage());
            return false;
        }
        
        // If the password was valid
        return true;
	}
	
	@SuppressLint("InlinedApi")
	@Override
	public void onCacheWordLocked()
	{
		// Start the 'lock screen' activity
        Intent intent = new Intent(getBaseContext(), LockScreenActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) // FLAG_ACTIVITY_CLEAR_TASK only exists in API 11 and later
        {
        	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);// Clear the stack of activities
        }
        startActivityForResult(intent, 0);
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
 	
	@Override
	protected void onStop() 
	{
	    super.onStop();
	    
	    mCacheWord.disconnectFromService();
	}
}