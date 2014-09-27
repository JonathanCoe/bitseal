package org.bitseal.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
	private static final String TAG = "BOOT_SIGNAL_RECEIVER";
	
	@Override
    public void onReceive(Context context, Intent i) 
    {
		Log.i(TAG, "BootSignalReceiver.onReceive() called.");
		    	
		// Start the BackgroundService for the first time
		Intent firstStartIntent = new Intent(context, BackgroundService.class);
		firstStartIntent.putExtra(BackgroundService.PERIODIC_BACKGROUND_PROCESSING_REQUEST, BackgroundService.BACKGROUND_PROCESSING_REQUEST);
		context.startService(firstStartIntent);
    }
}