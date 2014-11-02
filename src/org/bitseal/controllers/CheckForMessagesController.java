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
	 * 
	 */
	public void checkServerForMessages()
	{
		ServerCommunicator servCom = new ServerCommunicator();
		servCom.checkServerForNewMsgs();
	}
	
	/**
	 * Processes one or more msg payloads that have been sent to me
	 *  
	 * @return An int representing the number of new messages successfully 
	 * received (i.e. successfully decrypted and authenticated). 
	 */
	public int processIncomingMessages()
	{
		IncomingMessageProcessor inMsgProc = new IncomingMessageProcessor();
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
			Message decryptedMessage = inMsgProc.processReceivedMsg(p);
			
			if (decryptedMessage != null) // If the message was decrypted and authenticated successfully
			{
				newMessagesReceived ++;
				
				// Save the decrypted Message to the database
				MessageProvider msgProv = MessageProvider.get(App.getContext());
				msgProv.addMessage(decryptedMessage);
				
				// Update the UI
				Intent intent = new Intent(UI_NOTIFICATION);
				App.getContext().sendBroadcast(intent);
			}
			
			processedMsgs.add(p);
		}
		
		// Update all the processed Payload records so that they won't be processed again
		for (Payload p : processedMsgs)
		{
			p.setProcessingComplete(true);
			payProv.updatePayload(p);
		}
		
		return newMessagesReceived;
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
		AckProcessor ackProc = new AckProcessor();
		return ackProc.sendAcknowledgments();
	}
}