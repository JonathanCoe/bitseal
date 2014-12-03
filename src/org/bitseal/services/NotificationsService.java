package org.bitseal.services;

import org.bitseal.R;
import org.bitseal.activities.InboxActivity;
import org.bitseal.activities.LockScreenActivity;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class NotificationsService extends IntentService
{
	public static final String EXTRA_NEW_MESSAGES_NOTIFICATION_CLEARED = "extraNewMessagesNotificationCleared";
	public static final String KEY_NEW_MESSAGES_NOTIFICATION_CURRENTLY_DISPLAYED = "keyNewMessagesNotificationCurrentlyDisplayed";
	public static final String EXTRA_DISPLAY_NEW_MESSAGES_NOTIFICATION = "displayNewMessagesNotification";
	
	public static final String EXTRA_DISPLAY_UNLOCK_NOTIFICATION = "extraDisplayUnlockNotification";
	
	/** A random value used as a unique ID number of the 'new messages received' notification */
	private static final int NEW_MESSAGES_NOTIFICATION_ID = 43282372;
	
	/** A random value used as a unique ID number of the 'new messages received' notification */
	private static final int UNLOCK_NOTIFICATION_ID = 749385039; 
	
	/** A key for storing the number of new messages referred to in the current 'new messages received' notification */
	private static final String NEW_MESSAGES_NOTIFICATION_KEY = "newMessagesNotificationKey";
	
	private static final String UNLOCK_NOTIFICATION_TITLE = "Unlock Bitseal";
	private static final String UNLOCK_NOTIFICATION_TEXT = "Touch to unlock Bitseal";
	
	public static final String TAG = "NOTIFICATIONS_SERVICE";
	
	public NotificationsService()
	{
		super("NotificationsService");
	}
	
	public static int getNewMessagesNotificationId()
	{
		return NEW_MESSAGES_NOTIFICATION_ID;
	}
	
	public static int getUnlockNotificationId()
	{
		return UNLOCK_NOTIFICATION_ID;
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		if (intent.hasExtra(EXTRA_NEW_MESSAGES_NOTIFICATION_CLEARED))
		{
			// Set the 'new messages notification currently displayed' shared preference to false
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			SharedPreferences.Editor editor = prefs.edit();
		    editor.putBoolean(KEY_NEW_MESSAGES_NOTIFICATION_CURRENTLY_DISPLAYED, false);
		    editor.commit();
		    
		    Log.i(TAG, "Recorded dismissal of new messages notification");
		}
		else if (intent.hasExtra(EXTRA_DISPLAY_NEW_MESSAGES_NOTIFICATION))
		{
			int newMessages = intent.getExtras().getInt(EXTRA_DISPLAY_NEW_MESSAGES_NOTIFICATION);
			
			displayNewMessagesNotification(newMessages);
		}
		else if (intent.hasExtra(EXTRA_DISPLAY_UNLOCK_NOTIFICATION))
		{
			displayUnlockNotification();
		}
	}
	
	/**
	 * Shows an Android notification that new messages have been received
	 * 
	 * @param newMessages - An int representing the number of new messages received
	 */
	private void displayNewMessagesNotification(int newMessages)
	{		
		int notificationNewMessages; // The number of new messages that this notification will advertise to the user
		
		// Check whether there is already a 'new messages' notification being displayed to the user
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		boolean newMessagesNotificationCurrentlyDisplayed = prefs.getBoolean(NotificationsService.KEY_NEW_MESSAGES_NOTIFICATION_CURRENTLY_DISPLAYED, false);
		
		// If there is, cancel the current notification and show a new one that takes account of the number of messages referred to in the last notification
		if (newMessagesNotificationCurrentlyDisplayed == true)
		{
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.cancel(NEW_MESSAGES_NOTIFICATION_ID);
			
			int oldNotificationMessages = prefs.getInt(NEW_MESSAGES_NOTIFICATION_KEY, 0);
			notificationNewMessages = newMessages + oldNotificationMessages;
		}
		else
		{
			notificationNewMessages = newMessages;
		}
		
		Log.i(TAG, "Showing notification for " + notificationNewMessages + " new message(s) received");
		
		// Create and display the new notification
		if (notificationNewMessages == 1)
		{
			displayNewMessagesNotification("1 new message received", "Touch to open Bitseal inbox");
		}
		else
		{
			displayNewMessagesNotification(notificationNewMessages + " new messages received", "Touch to open Bitseal inbox");
		}
		
		// Store the number of new messages referred to in this notification so that it can be updated later
		SharedPreferences.Editor editor = prefs.edit();
	    editor.putInt(NEW_MESSAGES_NOTIFICATION_KEY, notificationNewMessages);
	    editor.commit();
	    
	    // Set the 'new messages notification currently displayed' shared preference to true
	    editor.putBoolean(NotificationsService.KEY_NEW_MESSAGES_NOTIFICATION_CURRENTLY_DISPLAYED, true);
	    editor.commit();
	}
	
	/**
	 * Displays an Android notification to the user, informing them that new messages
	 * have been received.<br><br>
	 * 
	 * Note: This method is adapted from the example here: <br>
	 * https://developer.android.com/guide/topics/ui/notifiers/notifications.html
	 * 
	 * @param title - A String containing the title of the notification to display
	 * @param text - A String containing the text of the notification to display
	 */
	private void displayNewMessagesNotification(String title, String text)
	{
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setContentTitle(title);
		builder.setContentText(text);
		builder.setAutoCancel(true);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			builder.setSmallIcon(R.drawable.notification_icon_lollipop);
			builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC); // All we show is the number of new messages received
		}
		else
		{
			builder.setSmallIcon(R.drawable.notification_icon);
		}
		
		// Set a sound for the notification
		Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		builder.setSound(alarmSound);
		
		// Creates an intent to open the Inbox Activity if the user selects the notification
		Intent openNotificationIntent = new Intent(this, InboxActivity.class);
		openNotificationIntent.putExtra(NotificationsService.EXTRA_NEW_MESSAGES_NOTIFICATION_CLEARED, true);
		
		// Creates an intent to run the notifications service if the user clears the notification
		Intent clearNotificationIntent = new Intent(getApplicationContext(), NotificationsService.class);
		clearNotificationIntent.putExtra(NotificationsService.EXTRA_NEW_MESSAGES_NOTIFICATION_CLEARED, true);
		PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, clearNotificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		builder.setDeleteIntent(pendingIntent);
		
		// The stack builder object will contain an artificial back stack for the
		// started Activity. This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(InboxActivity.class);
		
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(openNotificationIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(resultPendingIntent);
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		// The notification ID number allows you to update the notification later on.
		notificationManager.notify(NEW_MESSAGES_NOTIFICATION_ID, builder.build());
	}
	
	/**
	 * Displays an Android notification to the user, informing them that the app
	 * needs to be unlocked (decrypting the database). 
	 * 
	 * Note: This method is adapted from the example here: <br>
	 * https://developer.android.com/guide/topics/ui/notifiers/notifications.html
	 */
	private void displayUnlockNotification()
	{
		NotificationCompat.Builder builder =
		        new NotificationCompat.Builder(this)
		        .setSmallIcon(R.drawable.notification_icon)
		        .setContentTitle(UNLOCK_NOTIFICATION_TITLE)
		        .setContentText(UNLOCK_NOTIFICATION_TEXT);
		
		builder.setAutoCancel(true);
		
		// Set a sound for the notification
		Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		builder.setSound(alarmSound);
		
		// Creates an intent to open the lock screen activity if the user selects the notification
		Intent openNotificationIntent = new Intent(this, LockScreenActivity.class);
		openNotificationIntent.putExtra(NotificationsService.EXTRA_NEW_MESSAGES_NOTIFICATION_CLEARED, true);
		
		// Creates an intent to run the notifications service if the user clears the notification
		Intent clearNotificationIntent = new Intent(getApplicationContext(), NotificationsService.class);
		clearNotificationIntent.putExtra(NotificationsService.EXTRA_NEW_MESSAGES_NOTIFICATION_CLEARED, true);
		PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, clearNotificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		builder.setDeleteIntent(pendingIntent);
		
		// The stack builder object will contain an artificial back stack for the
		// started Activity. This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(LockScreenActivity.class);
		
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(openNotificationIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(resultPendingIntent);
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		// The notification ID number allows you to update the notification later on.
		notificationManager.notify(UNLOCK_NOTIFICATION_ID, builder.build());
	}
}