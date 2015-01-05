package org.bitseal.controllers;

import java.util.ArrayList;

import org.bitseal.R;
import org.bitseal.core.App;
import org.bitseal.core.BehaviourBitfieldProcessor;
import org.bitseal.core.ObjectProcessor;
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
import org.bitseal.services.MessageStatusHandler;

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
	 * a pubkey for a given Bitmessage address and a payload for that pubkey which
	 * can be sent around the network. 
	 * 
	 * @param inputQueueRecord - A QueueRecord object for the task of creating and disseminating
	 * the identity data
	 * @param doPOW - A boolean indicating whether or not to do POW for the pubkey
	 * of the identity we are creating
	 * 
	 * @return A boolean indicating whether all parts of this task were completed successfully
	 */
	public boolean createIdentity(QueueRecord inputQueueRecord, boolean doPOW)
	{
		Log.i(TAG, "TaskController.createIdentity() called");
		
		QueueRecordProcessor queueProc = new QueueRecordProcessor();
		
		// Attempt to retrieve the Address from the database. If it has been deleted by the user
		// then we should abort the identity creation process.
		Address address = null;
		try
		{
			address = AddressProvider.get(App.getContext()).searchForSingleRecord(inputQueueRecord.getObject0Id());
		}
		catch (RuntimeException e)
		{
			Log.e(TAG, "While running TaskController.createIdentity(), the attempt to retrieve the Address object from the database failed.\n"
					+ "The identity creation process will therefore be aborted.");
			queueProc.deleteQueueRecord(inputQueueRecord);
			return false;
		}
		
		// Attempt to create the pubkey and pubkey payload for a new identity
		Payload pubkeyPayload = null;
		try
		{
			pubkeyPayload = new CreateIdentityController().generatePubkeyData(address, doPOW);
		}
		catch (RuntimeException runEx)
		{
			Log.e(TAG, "While running TaskController.createIdentity(), CreateIdentityController.generateIdentityData() threw a RuntimeExecption. \n" +
					"The exception message was: " + runEx.getMessage());
			queueProc.updateQueueRecordAfterFailure(inputQueueRecord); // Update the QueueRecord to record the failed attempt
			return false; // If we failed to create the identity data because of an exception, leave the QueueRecord for that
					  	  // task in place so that it can be attempted again later
		}
		
		// If we successfully generated the identity data, delete the QueueRecord for that task and
		// create a new QueueRecord to disseminate the pubkey of that identity
		queueProc.deleteQueueRecord(inputQueueRecord);
		QueueRecord newQueueRecord = queueProc.createAndSaveQueueRecord(BackgroundService.TASK_DISSEMINATE_PUBKEY, 0, 0, pubkeyPayload, null, null);
		
		// First check whether an Internet connection is available. If not, the QueueRecord for the
		// 'disseminate pubkey' task will be saved (as above) and processed later
		if (NetworkHelper.checkInternetAvailability() == true)
		{
			// Attempt to disseminate the pubkey for the newly generated identity
			return disseminatePubkey(newQueueRecord, pubkeyPayload, doPOW);
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Attempts to disseminate a pubkey payload to the Bitmessage network. 
	 * 
	 * @param inputQueueRecord - A QueueRecord object for this task
	 * @param pubkeyPayload - A Payload object containing the pubkey payload to be disseminated
	 * @param POWDone - A boolean indicating whether or not POW has been done for this pubkey
	 * 
	 * @return A boolean indicating whether or not the pubkey payload was successfully
	 * disseminated to the rest of the network
	 */
	public boolean disseminatePubkey(QueueRecord inputQueueRecord, Payload pubkeyPayload, boolean POWDone)
	{
		Log.i(TAG, "TaskController.disseminatePubkey() called");
		
		QueueRecordProcessor queueProc = new QueueRecordProcessor();
		boolean success;
		
		// Attempt to disseminate the pubkey payload of the new identity
		try
		{
			success = new CreateIdentityController().disseminatePubkey(pubkeyPayload, POWDone);
		}
		catch (RuntimeException runEx)
		{
			Log.e(TAG, "While running TaskController.disseminatePubkey(), CreateIdentityController.disseminatePubkey() threw a RuntimeExecption. \n" +
					"The exception message was: " + runEx.getMessage());
			queueProc.updateQueueRecordAfterFailure(inputQueueRecord); // Update the QueueRecord to record the failed attempt
			return false; // If we failed to disseminate the pubkey because of an exception, leave the QueueRecord for this
					  	  // task in place so that it can be attempted again later
		}
		
		// If we successfully generated the identity data
		if (success)
		{
			// Delete the successfully disseminated payload and the QueueRecord for disseminating it
			PayloadProvider.get(App.getContext()).deletePayload(pubkeyPayload);
			queueProc.deleteQueueRecord(inputQueueRecord);
			return true;
		}
		else
		{
			queueProc.updateQueueRecordAfterFailure(inputQueueRecord);
			return false;
		}
	}
	
	/**
	 * This method takes a scheduled 'send message' task and does all the work
	 * necessary to send the message.
	 * 
	 * @param inputQueueRecord - A QueueRecord for the task of sending this message
	 * @param message - A Message object containing the message to send
	 * @param doPOW - A boolean indicating whether or not to do POW for this message
	 * @param msgTimeToLive - The 'time to live' value (in seconds) to be used in sending this message
	 * @param getpubkeyTimeToLive - The 'time to live' value (in seconds) to be used if we need to create
	 * a getpubkey object in order to retrieve the destination pubkey for this message
	 * 
	 * @return A boolean indicating whether or not the entire process of creating and 
	 * sending the message was completed successfully. 
	 */
	public boolean sendMessage(QueueRecord inputQueueRecord, Message message, boolean doPOW, long msgTimeToLive, long getpubkeyTimeToLive)
	{
		Log.i(TAG, "TaskController.sendMessage() called");
		
		// Attempt to retrieve the pubkey of the address that the message is to be sent to
		SendMessageController controller = new SendMessageController();
		QueueRecordProcessor queueProc = new QueueRecordProcessor();
		Pubkey toPubkey = null;
		try
		{
			Object retrievalResult = null;
			// If we have already have getpubkey object created during a previous attempt to retrieve this 
			// pubkey, pass it to the SendMessageController so it can be reused if necessary
			if (inputQueueRecord.getObject1Id() != 0)
			{
				PayloadProvider payProv = PayloadProvider.get(App.getContext());
				Payload getpubkeyPayload = payProv.searchForSingleRecord(inputQueueRecord.getObject1Id());
				
				// Check whether If the getpubkey is still valid (its time to live pay have expired)
				boolean getpubkeyValid = new ObjectProcessor().validateObject(getpubkeyPayload.getPayload());
				if (getpubkeyValid)
				{
					// Attempt to retrieve the pubkey using the existing getpubkey object
					retrievalResult = controller.retrievePubkey(message, getpubkeyPayload, getpubkeyTimeToLive);
				}
				else
				{
					// Delete the old (and no longer valid) getpubkey from the database
					payProv.deletePayload(getpubkeyPayload);
					
					// Attempt to retrieve the pubkey by creating and disseminating a new getpubkey object
					retrievalResult = controller.retrievePubkey(message, null, getpubkeyTimeToLive);
				}
			}
			else
			{
				// Attempt to retrieve the pubkey by creating and disseminating a new getpubkey object
				retrievalResult = controller.retrievePubkey(message, null, getpubkeyTimeToLive);
			}
			
			if (retrievalResult instanceof Payload)
			{
				// We were unable to retrieve the pubkey, and in response have created a new getpubkey
				// object and sent it out to the network. Now we will modify the QueueRecord for this task
				// to include a reference to the getpubkey Payload, so that we can reuse it if necessary. 
				inputQueueRecord.setObject1Type(QueueRecord.QUEUE_RECORD_OBJECT_TYPE_PAYLOAD);
				inputQueueRecord.setObject1Id(((Payload) retrievalResult).getId());
				queueProc.updateQueueRecord(inputQueueRecord);
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
			queueProc.updateQueueRecordAfterFailure(inputQueueRecord); // Update the QueueRecord to record the failed attempt
			return false; // If we failed to retrieve the pubkey, leave the QueueRecord for that
						  // task in place so that it can be attempted again later
		}
		
		// If we successfully retrieved the pubkey, delete the 'retrieve pubkey' QueueRecord and create a new one for the 
		// next stage of this task
		queueProc.deleteQueueRecord(inputQueueRecord);
		QueueRecord newQueueRecord = queueProc.createAndSaveQueueRecord(BackgroundService.TASK_PROCESS_OUTGOING_MESSAGE, 0, inputQueueRecord.getRecordCount(), message, toPubkey, null);
		
		return processOutgoingMessage(newQueueRecord, message, toPubkey, doPOW, msgTimeToLive);
	}
	
	/**
	 * Processes a Message object, extracting the data from it and using that data to
	 * create a new msg payload that is ready to be sent out to the Bitmessage network.
	 * 
	 * @param inputQueueRecord - A QueueRecord object for this task
	 * @param message - A Message object containing the data of the message that is to be sent
	 * @param toPubkey - A Pubkey object containing the pubkey data of the destination address
	 * @param doPOW - A boolean indicating whether or not to do POW for this message
	 * @param timeToLive - The 'time to live' value (in seconds) to be used in sending this message
	 * 
	 * @return A boolean indicating whether or not the Message was successfully processed
	 */
	public boolean processOutgoingMessage (QueueRecord inputQueueRecord, Message message, Pubkey toPubkey, boolean doPOW, long timeToLive)
	{
		Log.i(TAG, "TaskController.processOutgoingMessage() called");
		
		QueueRecordProcessor queueProc = new QueueRecordProcessor();
		
		MessageStatusHandler.updateMessageStatus(message, App.getContext().getString(R.string.message_status_constructing_payload));
		
		// Attempt to construct the message payload
		Payload msgPayload = null;
		try
		{
			msgPayload = new SendMessageController().processOutgoingMessage(message, toPubkey, doPOW, timeToLive);
		}
		catch (Exception e)
		{
			Log.e(TAG, "While running TaskController.processOutgoingMessage(), SendMessageController.processOutgoingMessage() threw an Exception. \n" +
					"The exception message was: " + e.getMessage());
			queueProc.updateQueueRecordAfterFailure(inputQueueRecord); // Update the QueueRecord to record the failed attempt
			return false; // If we failed to process the outgoing message, leave the QueueRecord for that
						  // task in place so that it can be attempted again later
		}
		
		// If we successfully created the message payload, delete the 'process outgoing message' QueueRecord and create a new one for the 
		// next stage of this task
		queueProc.deleteQueueRecord(inputQueueRecord);
		QueueRecord newQueueRecord = queueProc.createAndSaveQueueRecord(BackgroundService.TASK_DISSEMINATE_MESSAGE, 0, 0, message, msgPayload, toPubkey);
		
		MessageStatusHandler.updateMessageStatus(message, App.getContext().getString(R.string.message_status_sending_message));
		
		// Update the "correspondingPayloadId" field of the Message
		message.setMsgPayloadId(msgPayload.getId());
		MessageProvider.get(App.getContext()).updateMessage(message);
		
		// First check whether an Internet connection is available. If not, the QueueRecord for the
		// 'disseminate message' task will be processed later
		if (NetworkHelper.checkInternetAvailability() == true)
		{
			return disseminateMessage(newQueueRecord, msgPayload, toPubkey, doPOW);
		}
		else
		{
			MessageStatusHandler.updateMessageStatus(message, App.getContext().getString(R.string.message_status_waiting_for_connection));
			return false;
		}
	}
	
	/**
	 * Attempts to disseminate a msg payload to the rest of the Bitmessage network. 
	 * 
	 * @param inputQueueRecord - A QueueRecord object for this task
	 * @param msgPayload - A Payload object containing the msg payload to be sent
	 * @param toPubkey - A Pubkey object containing the pubkey data of the destination address
	 * @param POWDone - A boolean indicating whether or not POW has been done for this message
	 * 
	 * @return A boolean indicating whether or not the msg payload was successfully disseminated 
	 * to the rest of the Bitmessage network
	 */
	public boolean disseminateMessage(QueueRecord inputQueueRecord, Payload msgPayload, Pubkey toPubkey, boolean POWDone)
	{
		Log.i(TAG, "TaskController.disseminateMessage() called");
		
		// Attempt to disseminate the message
		boolean success;
		try
		{
			success = new SendMessageController().disseminateMessage(msgPayload, toPubkey, POWDone);
		}
		catch (RuntimeException runEx)
		{
			Log.e(TAG, "While running TaskController.disseminateMessage(), SendMessageController.disseminateMessage() threw a RuntimeExecption. \n" +
					"The exception message was: " + runEx.getMessage());
			new QueueRecordProcessor().updateQueueRecordAfterFailure(inputQueueRecord); // Update the QueueRecord to record the failed attempt
			return false; // If we failed to disseminate the message, leave the QueueRecord for that
						  // task in place so that it can be attempted again later
		}
		
		if (success)
		{
			// Delete the "disseminate message" QueueRecord, 
			new QueueRecordProcessor().deleteQueueRecord(inputQueueRecord);
			
			// Delete the successfully disseminated payload
			PayloadProvider.get(App.getContext()).deletePayload(msgPayload);
			
			// Update the status of the original Message that the payload was derived from
			ArrayList<Message> retrievedMessages = MessageProvider.get(App.getContext()).searchMessages(MessagesTable.COLUMN_MSG_PAYLOAD_ID, String.valueOf(msgPayload.getId()));
			if (retrievedMessages.size() == 1)
			{
				Message originalMessage = retrievedMessages.get(0);
				
				// Check whether we should expect an acknowledgement and update the original Message's status accordingly
				if (BehaviourBitfieldProcessor.checkSendsAcks(toPubkey.getBehaviourBitfield()))
				{
					MessageStatusHandler.updateMessageStatus(originalMessage, App.getContext().getString(R.string.message_status_message_sent));
				}
				else
				{
					MessageStatusHandler.updateMessageStatus(originalMessage, App.getContext().getString(R.string.message_status_message_sent_no_ack_expected));
				}
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
		
		try
		{
			// Start the message download thread.
		    MessageDownloadThread.getInstance().startThread();
		}
		catch (Exception e)
		{
			Log.e(TAG, "While running TaskController.checkForMessagesAndSendAcks(), MessageDownloadThread.getInstance().startThread() threw an Exception. \n" +
					"The exception message was: " + e.getMessage());
		}
		
		try
		{
			// Start the message processing thread
			MessageProcessingThread.getInstance().startThread();
		}
		catch (Exception e)
		{
			Log.e(TAG, "While running TaskController.checkForMessagesAndSendAcks(), MessageProcessingThread.getInstance().startThread() threw an Exception. \n" +
					"The exception message was: " + e.getMessage());
		}
	}
	
	/**
	 * Takes all Pubkeys objects saved in the database and checks whether any
	 * of them need to be disseminated again. If yes, they are disseminated
	 * again. <br><br>
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
		
		ArrayList<Address> addressesWithExpiredPubkeys = new ArrayList<Address>();
		try
		{
			addressesWithExpiredPubkeys = new ReDisseminatePubkeysController().checkIfPubkeyDisseminationIsDue();
		}
		catch (RuntimeException runEx)
		{
			Log.e(TAG, "While running TaskController.checkIfPubkeyDisseminationIsDue(), ReDisseminatePubkeysController.checkIfPubkeyDisseminationIsDue() threw a RuntimeExecption. \n"
					 + "The exception message was: " + runEx.getMessage());
		}
		
		if (addressesWithExpiredPubkeys.size() > 0)
		{
			reDisseminatePubkeys(addressesWithExpiredPubkeys, doPOW);
		}
		else
		{
			Log.i(TAG, "None of our pubkeys are due to be re-disseminated");
		}
	}
	
	/**
	 * Re-disseminates the Pubkeys of any given Addresses
	 * 
	 * @param addressesWithExpiredPubkeys - The Addresses which require their pubkeys to
	 * be regenerated and re-disseminated
	 * @param doPOW - A boolean indicating whether or not to do POW for the updated
	 * pubkey payload(s)
	 */
	public void reDisseminatePubkeys(ArrayList<Address> addressesWithExpiredPubkeys, boolean doPOW)
	{
		try
		{
			for (Address a : addressesWithExpiredPubkeys)
			{
				Payload updatedPayload = new ReDisseminatePubkeysController().regeneratePubkey(a, doPOW);
				
				// Create a new QueueRecord to re-disseminate the pubkey
				QueueRecordProcessor queueProc = new QueueRecordProcessor();
				QueueRecord queueRecord = queueProc.createAndSaveQueueRecord(BackgroundService.TASK_DISSEMINATE_PUBKEY, 0, 0, updatedPayload, null, null);
				
				// First check whether an Internet connection is available. If not, the QueueRecord for the
				// 'disseminate pubkey' task will be saved (as above) and processed later
				if (NetworkHelper.checkInternetAvailability() == true)
				{
					// Attempt to disseminate the pubkey for the newly generated identity
					Log.d(TAG, "Re-disseminating the pubkey for address " + a.getAddress());
					disseminatePubkey(queueRecord, updatedPayload, doPOW);
				}
			}
		}
		catch (RuntimeException runEx)
		{
			Log.e(TAG, "A RuntimeException was thrown while running TaskController.reDisseminatePubkeys(). \n" +
					"The exception message was: " + runEx.getMessage());
		}
	}
}