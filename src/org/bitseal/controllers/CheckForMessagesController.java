package org.bitseal.controllers;

import java.util.ArrayList;

import org.bitseal.core.AckProcessor;
import org.bitseal.core.App;
import org.bitseal.core.IncomingMessageProcessor;
import org.bitseal.data.Message;
import org.bitseal.data.Payload;
import org.bitseal.database.MessageProvider;
import org.bitseal.database.PayloadProvider;
import org.bitseal.database.PayloadsTable;
import org.bitseal.network.ServerCommunicator;
import org.bitseal.services.NotificationsService;

import android.content.Context;
import android.content.Intent;

/**
 * This class controls the operations necessary to check whether
 * any new messages are available from servers and to process any
 * new messages that are retrieved.
 * 
 * @author Jonathan Coe
 */
public class CheckForMessagesController
{
	/** Used when broadcasting Intents to the UI so that it can refresh the data it is displaying */
	public static final String UI_NOTIFICATION = "uiNotification";
	
	/**
	 * Polls one or more servers to check whether any new messages are available. 
	 */
	public void checkServerForMessages()
	{
		new ServerCommunicator().checkServerForNewMsgs();
	}
	
	/**
	 * Processes one or more msg payloads that have been sent to me
	 *  
	 * @return An int representing the number of new messages successfully processed
	 */
	public int processIncomingMessages()
	{
		int newMessagesReceived = 0;
		
		// Search the database for the Payloads of any possible new msgs
		PayloadProvider payProv = PayloadProvider.get(App.getContext());
		String[] columnNames = new String[]{PayloadsTable.COLUMN_TYPE, PayloadsTable.COLUMN_BELONGS_TO_ME, PayloadsTable.COLUMN_PROCESSING_COMPLETE};
		String[] searchTerms = new String[]{Payload.OBJECT_TYPE_MSG, "0", "0"}; // Zero stands for false in the database
		ArrayList<Payload> msgsToProcess = payProv.searchPayloads(columnNames, searchTerms);
		
		// At this point we have selected all the msg payloads received by me which have not been processed yet. Now process them. 
		ArrayList<Payload> processedMsgs = new ArrayList<Payload>();
		for (Payload p : msgsToProcess)
		{
			Message decryptedMessage = new IncomingMessageProcessor().processReceivedMsg(p);
			
			if (decryptedMessage != null) // If the message was decrypted and authenticated successfully
			{
				newMessagesReceived ++;
				
				// Save the decrypted Message to the database
				MessageProvider.get(App.getContext()).addMessage(decryptedMessage);
				
				// Update the UI
				App.getContext().sendBroadcast(new Intent(UI_NOTIFICATION));
			}
			
			processedMsgs.add(p);
		}
		
		// Update all the processed Payload records so that they won't be processed again
		for (Payload p : processedMsgs)
		{
			p.setProcessingComplete(true);
			payProv.updatePayload(p);
		}
		
		if (newMessagesReceived > 0)
		{
			// Display a notification for any new message(s) we have received
			Context appContext = App.getContext();
			Intent intent = new Intent(appContext, NotificationsService.class);
		    intent.putExtra(NotificationsService.EXTRA_DISPLAY_NEW_MESSAGES_NOTIFICATION, newMessagesReceived);
		    appContext.startService(intent);
		}
		
		return processedMsgs.size();
	}
	
	/**
	 * Attempts to send any acknowledgments that are scheduled to be
	 * sent by me for messages that I have received
	 * 
	 * @return - A boolean indicating whether or not all outstanding acknowledgments
	 * were successfully processed 
	 */
	public boolean sendAcknowledgments()
	{
		return new AckProcessor().sendAcknowledgments();
	}
}