package org.bitseal.services;

import java.lang.Thread.UncaughtExceptionHandler;

import org.bitseal.activities.SplashScreenActivity;
import org.bitseal.core.App;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Handles any uncaught exceptions thrown by Bitseal.
 * 
 * @author Jonathan Coe
 */
public class ExceptionHandler implements UncaughtExceptionHandler
{   
	/** The key for a SharedPreferences flag used to prevent an infinite loop when we catch an application crash */
	public static final String UNCAUGHT_EXCEPTION_HANDLED = "uncaughtExceptionHandled";
	
	private static final String TAG = "EXCEPTION_HANDLER";
	
	@Override
    public void uncaughtException(Thread thread, Throwable ex)
	{
		Log.e(TAG, "Handling an uncaught exception thrown by thread " + thread.getName() + ". The exception message was:\n"
				+ ex.getMessage());
		
		// Check the 'uncaught exception handled' flag
		Context appContext = App.getContext();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
		
		// If the flag is not set, attempt to handle this crash. Otherwise we will allow it to proceed. 
		if (prefs.getBoolean(UNCAUGHT_EXCEPTION_HANDLED, false) == false)
		{
			// Set the 'uncaught exception handled' shared preference flag to true
			SharedPreferences.Editor editor = prefs.edit();
		    editor.putBoolean(UNCAUGHT_EXCEPTION_HANDLED, true);
		    editor.commit();
		    
		    // Create a PendingIntent to open the splash screen activity
		    Log.i(TAG, "Preparing to launch splash screen activity");
		    Intent intent = new Intent(appContext, SplashScreenActivity.class);
		    intent.putExtra(SplashScreenActivity.EXTRA_DELAY_APP_OPENING, true);
		    PendingIntent pendingIntent = PendingIntent.getActivity(appContext, 0, intent, 0);
		    
		    // Schedule the PendingIntent to fire almost immediately
		    AlarmManager mgr = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
		    mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 10, pendingIntent);
		    
		    // Shut down the application
		    System.exit(2);
		}
		else
		{
			Log.e(TAG, "Uncaught exception handled flag was set to true. Allowing crash to proceed in order to avoid an infinite loop of exception handling.");
		}
    }
}