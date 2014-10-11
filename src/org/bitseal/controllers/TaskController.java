package org.bitseal.controllers;

import java.util.ArrayList;

import org.bitseal.core.App;
import org.bitseal.core.BehaviourBitfieldProcessor;
import org.bitseal.core.QueueRecordProcessor;
import org.bitseal.data.Address;
import org.bitseal.data.Message;
import org.bitseal.data.Payload;
import org.bitseal.data.Pubkey;
import org.bitseal.data.QueueRecord;
import org.bitseal.database.AddressProvider;
import org.bitseal.database.MessageProvider;
import org.bitseal.database.MessagesTable;
import org.bitseal.database.PayloadProvider;
import org.bitseal.network.NetworkHelper;
import org.bitseal.services.BackgroundService;

import android.content.Intent;
import android.util.Log;

/**
 * This class coordinates the work of the other "Controller" classes, such as
 * SendMessageController. 
 * 
 * @author Jonathan Coe
 */
public class TaskController
{
	/** Used when broadcasting Intents to the UI so that it can refresh the data it is displaying */
	public static final String UI_NOTIFICATION = "uiNotification";
		
	private static final String TAG = "TASK_CONTROLLER";
	
	/**
	 * Creates a new set of identity data and attempts to disseminate the public part
	 * of that data to the rest of the Bitmessage network. The identity data consists of
	 * a pubkey for a given Bitmessage address and a paylod for that pubkey which
	 * can be sent around the network. 
	 * 
	 * @param queueRecord0 - A QueueRecord object for the task of creating and disseminating
	 * the identity data
	 * @param doPOW - A boolean indicating whether or not to do POW for the pubkey
	 * of the identity we are creating
	 * 
	 * @return A boolean indicating whether all parts of this task were completed successfully
	 */
	public boolean createIdentity(QueueRecord queueRecord0, boolean doPOW)
	{
		Log.i(TAG, "TaskController.createIdentity() called");
		
		CreateIdentityController controller = new CreateIdentityController();
		QueueRecordProcessor queueProc = new QueueRecordProcessor();
		
		// Attempt to retrieve the Address from the database. If it has been deleted by the user
		// then we should abort the identity creation process.
		Address address = null;
		try
		{
			AddressProvider addProv = AddressProvider.get(App.getContext());
			address = addProv.searchForSingleRecord(queueRecord0.getObject0Id());
		}
		catch (RuntimeException e)
		{
			Log.e(TAG, "While running TaskController.createIdentity(), the attempt to retrieve the Address object from the database failed.\n"
					+ "The identity creation process will therefore be aborted.");
			queueProc.deleteQueueRecord(queueRecord0);
			return false;
		}
		
		// Attempt to create the pubkey and pubkey payload for a new identity
		Payload pubkeyPayload = null;
		try
		{
			pubkeyPayload = controller.generatePubkeyData(address, doPOW);
		}
		catch (RuntimeException runEx)
		{
			Log.e(TAG, "While running TaskController.createIdentity(), CreateIdentityController.generateIdentityData() threw a RuntimeExecption. \n" +
					"The exception message was: " + runEx.getMessage());
			queueProc.updateQueueRecordAfterFailure(queueRecord0); // Update the QueueRecord to record the failed attempt
			return false; // If we failed to create the identity data because of an exception, leave the QueueRecord for that
					  // task in place so that it can be attempted again later
		}
		
		// If we successfully generated the identity data, delete the QueueRecord for that task and
		// create a new QueueRecord to dissemiante the pubkey of that identity
		queueProc.deleteQueueRecord(queueRecord0);
		QueueRecord queueRecord1 = queueProc.createAndSaveQueueRecord(BackgroundService.TASK_DISSEMINATE_PUBKEY, pubkeyPayload, null);
		
		// First check whether an Internet connection is available. If not, the QueueRecord for the
		// 'disseminate pubkey' task will be saved (as above) and processed later
		if (NetworkHelper.checkInternetAvailability() == true)
		{
			// Attempt to disseminate the pubkey for the newly generated identity
			return disseminatePubkey(queueRecord1, pubkeyPayload, doPOW);
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Attempts to disseminate a pubkey payload to the Bitmessage network. 
	 * 
	 * @param queueRecord0 - A QueueRecord object for this task
	 * @param pubkeyPayload - A Payload object containing the pubkey payload to be disseminated
	 * @param POWDone - A boolean indicating whether or not POW has been done for this pubkey
	 * 
	 * @return A boolean indicating whether or not the pubkey payload was successfully
	 * dissemianted to the rest of the network
	 */
	public boolean disseminatePubkey(QueueRecord queueRecord0, Payload pubkeyPayload, boolean POWDone)
	{
		Log.i(TAG, "TaskController.disseminatePubkey() called");
		
		CreateIdentityController controller = new CreateIdentityController();
		QueueRecordProcessor queueProc = new QueueRecordProcessor();
		
		boolean success = false;
		// Attempt to disseminate the pubkey payload of the new identity
		try
		{
			success = controller.disseminatePubkey(pubkeyPayload, POWDone);
		}
		catch (RuntimeException runEx)
		{
			Log.e(TAG, "While running TaskController.disseminatePubkey(), CreateIdentityController.disseminatePubkey() threw a RuntimeExecption. \n" +
					"The exception message was: " + runEx.getMessage());
			queueProc.updateQueueRecordAfterFailure(queueRecord0); // Update the QueueRecord to record the failed attempt
			return false; // If we failed to disseminate the pubkey because of an exception, leave the QueueRecord for that
					  // task in place so that it can be attempted again later
		}
		
		// If we successfully generated the identity data
		if (success == true)
		{
			// Delete the successfully disseminated payload
			PayloadProvider payProv = PayloadProvider.get(App.getContext());
			payProv.deletePayload(pubkeyPayload);
			
			queueProc.deleteQueueRecord(queueRecord0);
			return true;
		}
		else
		{
			queueProc.updateQueueRecordAfterFailure(queueRecord0);
			return false;
		}
	}
	
	/**
	 * This method takes a scheduled 'send message' task and does all the work
	 * necessary to send the message. If the process cannot be completed succesfully,
	 * then a QueueRecord will be saved to the database so that it can be completed 
	 * later.  
	 * 
	 * @param queueRecord0 - A QueueRecord for the task of sending this message
	 * @param messageToSend - A Message object containing the message to send
	 * @param doPOW - A boolean indicating whether or not to do POW for this message
	 * 
	 * @return A boolean indicating whether or not the entire process of creating and 
	 * sending the message was completed successfully. 
	 */
	public boolean sendMessage(QueueRecord queueRecord0, Message messageToSend, boolean doPOW)
	{
		Log.i(TAG, "TaskController.sendMessage() called");
		
		String toAddress = messageToSend.getToAddress();
		
		// Attempt to retrieve the pubkey of the address that the message is to be sent to
		SendMessageController controller = new SendMessageController();
		QueueRecordProcessor queueProc = new QueueRecordProcessor();
		Pubkey toPubkey = null;
		try
		{
			Object retrievalResult = null;
			// If we have already have getpubkey object created during a previous attempt to retrieve this 
			// pubkey, pass it to the SendMessageController so it can be reused if necessary
			if (queueRecord0.getObject1Id() != 0)
			{
				PayloadProvider payProv = PayloadProvider.get(App.getContext());
				Payload getpubkeyPayload = payProv.searchForSingleRecord(queueRecord0.getObject1Id());
				retrievalResult = controller.retrieveToPubkey(toAddress, getpubkeyPayload);
			}
			else
			{
				retrievalResult = controller.retrieveToPubkey(toAddress, null);
			}
			
			if (retrievalResult instanceof Payload)
			{
				// We were unable to retrieve the pubkey, and in response have created a new getpubkey
				// object and sent it out to the network. Now we will modify the QueueRecord for this task
				// to include a reference to the getpubkey Payload, so that we can reuse it if necessary. 
				queueRecord0.setObject1Type(QueueRecord.QUEUE_RECORD_OBJECT_TYPE_PAYLOAD);
				queueRecord0.setObject1Id(((Payload) retrievalResult).getId());
				queueProc.updateQueueRecord(queueRecord0);
				queueProc.updateQueueRecordAfterFailure(queueRecord0); // Update the QueueRecord to record the failed attempt
				return false; // If we failed to retrieve the pubkey, leave the QueueRecord for that
							  // task in place so that it can be attempted again later
			}
			else
			{
				toPubkey = (Pubkey) retrievalResult;
			}
		}
		catch (RuntimeException runEx)
		{
			Log.e(TAG, "While running TaskController.sendMessage(), SendMessageController.retrieveToPubkey() threw a RuntimeExecption. \n" +
					"The exception message was: " + runEx.getMessage());
			queueProc.updateQueueRecordAfterFailure(queueRecord0); // Update the QueueRecord to record the failed attempt
			return false; // If we failed to retrieve the pubkey, leave the QueueRecord for that
						  // task in place so that it can be attempted again later
		}
		
		// If we successfully retrieved the pubkey, delete the 'retrieve pubkey' QueueRecord and create a new one for the 
		// next stage of this task
		queueProc.deleteQueueRecord(queueRecord0);
		QueueRecord queueRecord1 = queueProc.createAndSaveQueueRecord(BackgroundService.TASK_PROCESS_OUTGOING_MESSAGE, messageToSend, toPubkey);
		
		return processOutgoingMessage(queueRecord1, messageToSend, toPubkey, doPOW);
	}
	
	/**
	 * Processes a Message object, extracting the data from it and using that data to
	 * create a new msg payload that is ready to be sent out to the Bitmessage network.
	 * 
	 * @param queueRecord0 - A QueueRecord object for this task
	 * @param messageToSend - A Message object containing the data of the message that is to be sent
	 * @param toPubkey - A Pubkey object containing the pubkey data of the destination address
	 * @param doPOW - A boolean indicating whether or not to do POW for this message
	 * 
	 * @return A boolean indicating whether or not the Message was successfully processed
	 */
	public boolean processOutgoingMessage (QueueRecord queueRecord0, Message messageToSend, Pubkey toPubkey, boolean doPOW)
	{
		Log.i(TAG, "TaskController.processOutgoingMessage() called");
		
		SendMessageController controller = new SendMessageController();
		QueueRecordProcessor queueProc = new QueueRecordProcessor();
		
		// Update the status of the Message and then prompt the UI to update the list of sent messages it is displaying
		messageToSend.setStatus(Message.STATUS_CONSTRUCTING_PAYLOAD);
		MessageProvider msgProv = MessageProvider.get(App.getContext());
		msgProv.updateMessage(messageToSend);	
		Intent intent = new Intent(UI_NOTIFICATION);
		App.getContext().sendBroadcast(intent);
		
		// Attempt to construct the message payload
		Payload msgPayload = null;
		try
		{
			msgPayload = controller.processOutgoingMessage(messageToSend, toPubkey, doPOW);
		}
		catch (Exception e)
		{
			Log.e(TAG, "While running TaskController.processOutgoingMessage(), SendMessageController.processOutgoingMessage() threw an Exception. \n" +
					"The exception message was: " + e.getMessage());
			queueProc.updateQueueRecordAfterFailure(queueRecord0); // Update the QueueRecord to record the failed attempt
			return false; // If we failed to process the outgoing message, leave the QueueRecord for that
						  // task in place so that it can be attempted again later
		}
		
		// If we successfully created the message payload, delete the 'process outgoing message' QueueRecord and create a new one for the 
		// next stage of this task
		queueProc.deleteQueueRecord(queueRecord0);
		QueueRecord queueRecord1 = queueProc.createAndSaveQueueRecord(BackgroundService.TASK_DISSEMINATE_MESSAGE, msgPayload, toPubkey);	
		
		// Update the "status" and "correspondingPayloadId" fields of the Message and 
		// then prompt the UI to update the list of sent messages it is displaying
		messageToSend.setStatus(Message.STATUS_SENDING_MESSAGE);
		messageToSend.setMsgPayloadId(msgPayload.getId());
		msgProv = MessageProvider.get(App.getContext());
		msgProv.updateMessage(messageToSend);
		App.getContext().sendBroadcast(intent);
		
		// First check whether an Internet connection is available. If not, the QueueRecord for the
		// 'disseminate message' task will be saved (as above) and processed later
		if (NetworkHelper.checkInternetAvailability() == true)
		{
			return disseminateMessage(queueRecord1, msgPayload, toPubkey, doPOW);
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Attempts to disseminate a msg payload to the rest of the Bitmessage network. 
	 * 
	 * @param queueRecord0 - A QueueRecord object for this task
	 * @param msgPayload - A Payload object containing the msg payload to be sent
	 * @param toPubkey - A Pubkey object containing the pubkey data of the destination address
	 * @param POWDone - A boolean indicating whether or not POW has been done for this message
	 * 
	 * @return A boolean indicating whether or not the msg payload was successfully disseminated 
	 * to the rest of the Bitmessage network
	 */
	public boolean disseminateMessage(QueueRecord queueRecord0, Payload msgPayload, Pubkey toPubkey, boolean POWDone)
	{
		Log.i(TAG, "TaskController.disseminateMessage() called");
		
		SendMessageController controller = new SendMessageController();
		QueueRecordProcessor queueProc = new QueueRecordProcessor();
		// Attempt to disseminate the message
		boolean success = false;
		try
		{
			success = controller.disseminateMessage(msgPayload, toPubkey, POWDone);
		}
		catch (RuntimeException runEx)
		{
			Log.e(TAG, "While running TaskController.disseminateMessage(), SendMessageController.disseminateMessage() threw a RuntimeExecption. \n" +
					"The exception message was: " + runEx.getMessage());
			queueProc.updateQueueRecordAfterFailure(queueRecord0); // Update the QueueRecord to record the failed attempt
			return false; // If we failed to disseminate the message, leave the QueueRecord for that
						  // task in place so that it can be attempted again later
		}
		
		if (success == true)
		{
			// Delete the "disseminate message" QueueRecord, 
			queueProc.deleteQueueRecord(queueRecord0);
			
			// Delete the successfully disseminated payload
			PayloadProvider payProv = PayloadProvider.get(App.getContext());
			payProv.deletePayload(msgPayload);
			
			// Update the status of the original Message that the payload was derived from, either to 'Message sent' or
			// to "Message sent, no acknowledgment expected', depending on the behaviour bitfield of the destination pubkey
			MessageProvider msgProv = MessageProvider.get(App.getContext());
			ArrayList<Message> retrievedMessages = msgProv.searchMessages(MessagesTable.COLUMN_MSG_PAYLOAD_ID, String.valueOf(msgPayload.getId()));
			if (retrievedMessages.size() == 1)
			{
				Message originalMessage = retrievedMessages.get(0);
				
				// Check whether we should expect an acknowledgment and update the original Message's status accordingly
				boolean sendsAcks = BehaviourBitfieldProcessor.checkSendsAcks(toPubkey.getBehaviourBitfield());
				if (sendsAcks == true)
				{
					originalMessage.setStatus(Message.STATUS_MSG_SENT);
				}
				else
				{
					originalMessage.setStatus(Message.STATUS_MSG_SENT_NO_ACK_EXPECTED);
				}
				
				msgProv.updateMessage(originalMessage);
				
				// Send a broadcast to the UI so that it can update itself
				Intent intent = new Intent(UI_NOTIFICATION);
				App.getContext().sendBroadcast(intent);
			}
			else
			{
				Log.e(TAG, "There should be exactly 1 result from this search. Instead " + retrievedMessages.size() + " records were found");
			}
		}
		
		return success;
	}
	
	/**
	 * Takes all Address objects saved in the database and checks with servers 
	 * to retrieve any new messages that have been sent to them. If any new messages
	 * are retrieved, they are processed, and acknowledgments for them are sent. <br><br>
	 * 
	 * Note that we do NOT create QueueRecords for the 'check for messages and send acks'
	 * task, because it is a default action that will be carried out regularly anyway. See 
	 * BackgroundService.processTasks().
	 */
	public void checkForMessagesAndSendAcks()
	{
		Log.i(TAG, "TaskController.checkForMessagesAndAcks() called");
		
		// Run the new message downloading thread.
	    MessageDownloadThread.getInstance().startThread();
	    
	    // Run the new message processing thread.
	    MessageProcessingThread.getInstance().startThread();
	}
	
	/**
	 * Checks the database for any Payload objects containing new msgs and
	 * processes any that are found.
	 * 
	 * @return An int representing the number of new messages successfully 
	 * processed (zero to many)
	 */
	public int processIncomingMessages()
	{
		Log.i(TAG, "TaskController.processIncomingMessages called");
		
		CheckForMessagesController controller = new CheckForMessagesController();
		int messagesProcessed = 0;
		
		try
		{
			messagesProcessed = controller.processIncomingMessages();
		}
		catch (RuntimeException runEx)
		{
			Log.e(TAG, "While running TaskController.processIncomingMessages(), CheckForMessagesController.processIncomingMessages() threw a RuntimeExecption. \n" +
					"The exception message was: " + runEx.getMessage());
			return 0;
		}
		
		return messagesProcessed;
	}
	
	/**
	 * Attempts to send any outstanding acknowledgments for messages
	 * that I have received. <br><br>
	 */
	public void sendAcknowledgments()
	{
		Log.i(TAG, "TaskController.sendAcknowledgments() called");
		
		// Attempt to send any acknowledgments that are waiting to be sent by me
		try
		{
			CheckForMessagesController controller = new CheckForMessagesController();
			controller.sendAcknowledgments();
		}
		catch (RuntimeException runEx)
		{
			Log.e(TAG, "While running TaskController.sendAcknowledgments(), CheckForMessagesController.sendAcknowledgments() threw a RuntimeExecption. \n" +
					"The exception message was: " + runEx.getMessage());
		}
	}
	
	/**
	 * Takes all Pubkeys objects saved in the database and checks whether any
	 * of them need to be disseminated again. If yes, they are disseminated
	 * again. 
	 * 
	 * Note that we do NOT create QueueRecords for this task, because it is a default action
	 * that will be carried out regularly anyway. See BackgroundService.processTasks().
	 * 
	 * @param doPOW - A boolean indicating whether or not to do POW for the updated
	 * pubkey payload
	 */
	public void checkIfPubkeyDisseminationIsDue(boolean doPOW)
	{
		Log.i(TAG, "TaskController.checkIfPubkeyDisseminationIsDue() called");
						
		ReDisseminatePubkeysController controller = new ReDisseminatePubkeysController();
		ArrayList<Pubkey> pubkeysToReDisseminate = new ArrayList<Pubkey>();
		try
		{
			pubkeysToReDisseminate = controller.checkIfPubkeyDisseminationIsDue();
		}
		catch (RuntimeException runEx)
		{
			Log.e(TAG, "While running TaskController.checkIfPubkeyDisseminationIsDue(), ReDisseminatePubkeysController.checkIfPubkeyDisseminationIsDue() threw a RuntimeExecption. \n" +
					"The exception message was: " + runEx.getMessage());
		}
		
		if (pubkeysToReDisseminate.size() > 0)
		{
			reDisseminatePubkeys(pubkeysToReDisseminate, doPOW);
		}
		else
		{
			Log.d(TAG, "None of our pubkeys are due to be re-disseminated");
		}
	}
	
	/**
	 * Re-disseminates any supplied Pubkeys. 
	 * 
	 * @param pubkeysToReDisseminate - The Pubkeys to be re-disseminated
	 * @param doPOW - A boolean indicating whether or not to do POW for the updated
	 * pubkey payload
	 */
	public void reDisseminatePubkeys(ArrayList<Pubkey> pubkeysToReDisseminate, boolean doPOW)
	{
		ReDisseminatePubkeysController controller = new ReDisseminatePubkeysController();
		try
		{
			for (Pubkey p : pubkeysToReDisseminate)
			{
				Payload updatedPayload = controller.reDisseminatePubkeys(p, doPOW);
				
				// Create a new QueueRecord to re-dissemiante the pubkey
				QueueRecordProcessor queueProc = new QueueRecordProcessor();
				QueueRecord queueRecord = queueProc.createAndSaveQueueRecord(BackgroundService.TASK_DISSEMINATE_PUBKEY, updatedPayload, null);
				
				// First check whether an Internet connection is available. If not, the QueueRecord for the
				// 'disseminate pubkey' task will be saved (as above) and processed later
				if (NetworkHelper.checkInternetAvailability() == true)
				{
					// Attempt to disseminate the pubkey for the newly generated identity
					disseminatePubkey(queueRecord, updatedPayload, doPOW);
				}
			}
		}
		catch (RuntimeException runEx)
		{
			Log.e(TAG, "While running TaskController.reDisseminatePubkeys(), ReDisseminatePubkeysController.reDisseminatePubkeys() threw a RuntimeExecption. \n" +
					"The exception message was: " + runEx.getMessage());
		}
	}
}