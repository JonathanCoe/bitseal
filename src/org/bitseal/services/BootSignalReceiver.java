package org.bitseal.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * When the device completes its boot sequence, this class starts the 
 * BackgroundService for the first time with a request to do any 
 * pending background processing. 
 * 
 * @author Jonathan Coe
 */
public class BootSignalReceiver extends BroadcastReceiver
{
    /** The key for a boolean variable that records whether or not a user-defined database encryption passphrase has been saved */
    private static final String KEY_DATABASE_PASSPHRASE_SAVED = "databasePassphraseSaved";
	
	private static final String TAG = "BOOT_SIGNAL_RECEIVER";
	
	@Override
    public void onReceive(Context context, Intent i) 
    {
		Log.i(TAG, "BootSignalReceiver.onReceive() called.");
		
        // Check whether the user has set a database encryption passphrase
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean databasePassphraseSaved= prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false);
		if (databasePassphraseSaved)
		{
			// Display a notification for the user to unlock the database
			Intent intent = new Intent(context, NotificationsService.class);
		    intent.putExtra(NotificationsService.EXTRA_DISPLAY_UNLOCK_NOTIFICATION, 0); // The zero is just a placeholder
		    context.startService(intent);
		}
		else
		{
			// Start the BackgroundService
			Intent firstStartIntent = new Intent(context, BackgroundService.class);
			firstStartIntent.putExtra(BackgroundService.PERIODIC_BACKGROUND_PROCESSING_REQUEST, BackgroundService.BACKGROUND_PROCESSING_REQUEST);
			context.startService(firstStartIntent);
		}
    }
}