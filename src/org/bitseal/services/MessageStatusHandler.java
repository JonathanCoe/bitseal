package org.bitseal.services;

import org.bitseal.core.App;
import org.bitseal.data.Message;
import org.bitseal.database.MessageProvider;

import android.content.Intent;

/**
 * Class for handling message status updates.
 * 
 * @author Jonathan Coe
 */
public class MessageStatusHandler
{
	/** Used when broadcasting Intents to the UI so that it can refresh the data it is displaying */
	private static final String UI_NOTIFICATION = "uiNotification";
	
	/**
	 * Updates the status of a Message object in the database and prompts the
	 * UI to refresh itself so that the new status will be displayed
	 * 
	 * @param message - The Message object to update the status of
	 * @param status - The status String to use
	 */
	public static void updateMessageStatus(Message message, String status)
	{		
		// Update the status of the Message and then prompt the UI to update the list of sent messages it is displaying
		message.setStatus(status);
		MessageProvider msgProv = MessageProvider.get(App.getContext());
		msgProv.updateMessage(message);	
		Intent intent = new Intent(UI_NOTIFICATION);
		App.getContext().sendBroadcast(intent);
	}
}