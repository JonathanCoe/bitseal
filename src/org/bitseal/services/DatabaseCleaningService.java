package org.bitseal.services;

import org.bitseal.database.PayloadProvider;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class DatabaseCleaningService extends IntentService
{
	public static final String EXTRA_RUN_DATABASE_CLEANING_ROUTINE = "extraRunDatabaseCleaningRoutine";
	
	/** A key for recording the last time the database cleaning routine was run */
	public static final String LAST_DATABASE_CLEAN_TIME = "lastDatabaseCleanTime";
	
	/** This is the maximum age of an object (in seconds) that PyBitmessage will accept. */
	private static final int PYBITMESSAGE_NEW_OBJECT_ACCEPTANCE_PERIOD = 216000;
	
	public static final String TAG = "DATABASE_CLEANING_SERVICE";
	
	public DatabaseCleaningService()
	{
		super("DatabaseCleaningService");
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{	
		if (intent.hasExtra(EXTRA_RUN_DATABASE_CLEANING_ROUTINE))
		{
			cleanDatabase();
		}
	}
	
	/**
	 * Deletes old, no-longer-needed data from the database. 
	 */
	private void cleanDatabase()
	{
		try
		{
			Log.d(TAG, "Running database cleaning routine");
			
			// Delete any Payloads in the database older than the time defined by PYBITMESSAGE_NEW_OBJECT_ACCEPTANCE_PERIOD
			long currentTime = System.currentTimeMillis() / 1000;
			long deletionTime = currentTime - PYBITMESSAGE_NEW_OBJECT_ACCEPTANCE_PERIOD;
			
			Log.i(TAG, "Deleting any Payloads with a time value earlier than " + deletionTime + " from the database");
			
			PayloadProvider payProv = PayloadProvider.get(getApplicationContext());
			payProv.deletePayloadsCreatedBefore(deletionTime);
			
			// Update the 'last data clean time'
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			SharedPreferences.Editor editor = prefs.edit();
		    editor.putLong(LAST_DATABASE_CLEAN_TIME, currentTime);
		    editor.commit();
		    Log.i(TAG, "Updated last database clean time to " + currentTime);
		}
		catch (Exception e)
		{
			Log.e(TAG, "Exception occurred in DatabaseCleaningService.cleanDatabase().\n"
					+ "The exception message was: " + e.getMessage());
		}
	}
}