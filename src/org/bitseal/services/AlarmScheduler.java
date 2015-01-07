package org.bitseal.services;

import org.bitseal.util.TimeUtils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

/**
 * Used to schedule tasks via the Android AlarmManager. We use
 * the CommonsWare AlarmListener to ensure that these tasks will
 * be completed even when the device is asleep and has to be 
 * 'woken up'. 
 * 
 * @author Jonathan Coe
 */
public class AlarmScheduler implements WakefulIntentService.AlarmListener
{
    /**
     * The normal amount of time in seconds between each attempt to start the
     * BackgroundService. e.g. If this value is set to 60, then the BackgroundService
     * will be restarted roughly every minute.
     */
	public static final int BACKGROUND_SERVICE_NORMAL_START_INTERVAL = 60;
	
	private static final String TAG = "ALARM_SCHEDULER";
	
	/**
	 * Schedules an alarm.
	 */
	public void scheduleAlarms(AlarmManager manager, PendingIntent intent, Context context)
	{
		Log.d(TAG, "AlarmScheduler.scheduleAlarms() called");
		Log.d(TAG, "Scheduling a restart of the BackgroundService in roughly " + TimeUtils.getTimeMessage(BACKGROUND_SERVICE_NORMAL_START_INTERVAL));		
		
		// Create an intent that will be used to restart the BackgroundService
		Intent baseIntent = new Intent(context, BackgroundService.class);
		baseIntent.putExtra(BackgroundService.PERIODIC_BACKGROUND_PROCESSING_REQUEST, BackgroundService.BACKGROUND_PROCESSING_REQUEST);
		PendingIntent pendingIntent = PendingIntent.getService(context, 0, baseIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		// Schedule a repeating alarm for restarting the BackgroundService
		long restartIntervalMilliseconds = BACKGROUND_SERVICE_NORMAL_START_INTERVAL * 1000;
		manager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + restartIntervalMilliseconds, restartIntervalMilliseconds, pendingIntent);
	}
	
	/**
	 * Defines what work should be done when the alarm set by
	 * this scheduler is triggered.
	 */
	public void sendWakefulWork(Context context)
	{
		Log.d(TAG, "AlarmScheduler.sendWakefulWork() called");
		
		// Create a new intent that will be used to run BackgroundService.processTasks(), then execute it
		Intent intent = new Intent(context, BackgroundService.class);
		intent.putExtra(BackgroundService.PERIODIC_BACKGROUND_PROCESSING_REQUEST, BackgroundService.BACKGROUND_PROCESSING_REQUEST);
		WakefulIntentService.sendWakefulWork(context, intent);
	}
	
	/**
	 * Sets the maximum time period that can pass between executions of a given
	 * alarm before that we assume that it has been lost and needs to be re-scheduled.  
	 */
	public long getMaxAge()
	{
		Log.d(TAG, "AlarmScheduler.getMaxAge() called");
		
		// Return double the standard restart interval. CommonsWare states that this is a sensible approach. 
		return BACKGROUND_SERVICE_NORMAL_START_INTERVAL * 2000;
	}
}