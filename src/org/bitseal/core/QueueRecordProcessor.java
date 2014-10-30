package org.bitseal.core;

import org.bitseal.data.Address;
import org.bitseal.data.Message;
import org.bitseal.data.Payload;
import org.bitseal.data.Pubkey;
import org.bitseal.data.QueueRecord;
import org.bitseal.database.QueueRecordProvider;

/**
 * This class provides some convenient methods for handling QueueRecord objects
 * 
 * @author Jonathan Coe
 */
public class QueueRecordProcessor
{
	// The tasks for performing the first major function of the application: sending messages
	public static final String TASK_SEND_MESSAGE = "sendMessage";
	public static final String TASK_PROCESS_OUTGOING_MESSAGE = "processOutgoingMessage";
	public static final String TASK_DISSEMINATE_MESSAGE = "disseminateMessage";
		
	// The tasks for performing the third major function of the application: creating a new identity
	public static final String TASK_CREATE_IDENTITY = "createIdentity";
	public static final String TASK_DISSEMINATE_PUBKEY = "disseminatePubkey";
	
	/**
	 * Creates a new QueueRecord, saves it to the database, and returns it
	 * to the caller. 
	 * 
	 * @param task - A String representing the task which this QueueRecord
	 * is for. 
	 * @param triggerTime - The 'trigger time' to use for this QueueRecord. If it should be
	 * processed immediately, set this to zero.
	 * @param recordCount - The number of QueueRecords which have already been created for this task
	 * @param object0 - The first Object which this QueueRecord should have a reference
	 * to, if any
	 * @param object1 - The second Object which this QueueRecord should have a reference
	 * to, if any
	 * @param object2 - The third Object which this QueueRecord should have a reference
	 * to, if any
	 * 
	 * @return A QueueRecord object for the given task and data
	 */
	public QueueRecord createAndSaveQueueRecord(String task, long triggerTime, int recordCount, Object object0, Object object1, Object object2)
	{
		QueueRecord q = new QueueRecord();
		q.setTask(task);
		q.setTriggerTime(triggerTime);
		q.setRecordCount(recordCount);
		q.setLastAttemptTime(System.currentTimeMillis() / 1000); // The current time in seconds
		q.setAttempts(0);
		
		// Set the QueueRecord's four object ID and object type fields, according to the given task
		if (task == TASK_SEND_MESSAGE)
		{
			// Set object 0 ID and Type
			Message messageToSend = (Message) object0;
			q.setObject0Id(messageToSend.getId());
			q.setObject0Type(QueueRecord.QUEUE_RECORD_OBJECT_TYPE_MESSAGE);
		}
		
		else if (task == TASK_PROCESS_OUTGOING_MESSAGE)
		{
			// Set object 0 ID and Type
			Message messageToSend = (Message) object0;
			q.setObject0Id(messageToSend.getId());
			q.setObject0Type(QueueRecord.QUEUE_RECORD_OBJECT_TYPE_MESSAGE);
			
			// Set object 1 ID and Type
			Pubkey toPubkey = (Pubkey) object1;
			q.setObject1Id(toPubkey.getId());
			q.setObject1Type(QueueRecord.QUEUE_RECORD_OBJECT_TYPE_PUBKEY);
		}
		
		else if (task == TASK_DISSEMINATE_MESSAGE)
		{
			// Set object 0 ID and Type
			Message sentMessage = (Message) object0;
			q.setObject0Id(sentMessage.getId());
			q.setObject0Type(QueueRecord.QUEUE_RECORD_OBJECT_TYPE_MESSAGE);
			
			// Set object 1 ID and Type
			Payload payloadToSend = (Payload) object1;
			q.setObject1Id(payloadToSend.getId());
			q.setObject1Type(QueueRecord.QUEUE_RECORD_OBJECT_TYPE_PAYLOAD);
			
			// Set object 2 ID and Type
			Pubkey toPubkey = (Pubkey) object2;
			q.setObject2Id(toPubkey.getId());
			q.setObject2Type(QueueRecord.QUEUE_RECORD_OBJECT_TYPE_PUBKEY);
		}
				
		else if (task == TASK_CREATE_IDENTITY)
		{
			// Set object 0 ID and Type
			Address address = (Address) object0;
			q.setObject0Id(address.getId());
			q.setObject0Type(QueueRecord.QUEUE_RECORD_OBJECT_TYPE_ADDRESS);
		}
		
		else if (task == TASK_DISSEMINATE_PUBKEY)
		{
			// Set object 0 ID and Type
			Payload payloadToSend = (Payload) object0;
			q.setObject0Id(payloadToSend.getId());
			q.setObject0Type(QueueRecord.QUEUE_RECORD_OBJECT_TYPE_PAYLOAD);
		}
		
		else
		{
			throw new RuntimeException("QueueRecordProcessor.createAndSaveQueueRecord() was called with an invalid task parameter. The " +
					"invalid task parameter was " + task);
		}
		
		// Save the QueueRecord we have created to the database
		long queueRecordID = saveQueueRecord(q);
		
		// Finally, set the ID field of the new QueueRecord with the ID created by the SQLite database
		q.setId(queueRecordID);
		
		return q;
	}
	
	/**
	 * Updates a given QueueRecord, over-writing the old version
	 * of it in the database. 
	 * 
	 * @param q - The QueueRecord to be updated in the database
	 */
	public void updateQueueRecord(QueueRecord q)
	{
		QueueRecordProvider queueProv = QueueRecordProvider.get(App.getContext());
		queueProv.updateQueueRecord(q);
	}
	
	/**
	 * Updates a given QueueRecord's lastAttemptTime and
	 * numberOfAttempts values, then saves the updated QueueRecord
	 * to the database. This method should be used when the task
	 * of a QueueRecord has been attempted and failed. 
	 * 
	 * @param q - The QueueRecord to update
	 * 
	 * @return The updated QueueRecord object
	 */
	public QueueRecord updateQueueRecordAfterFailure (QueueRecord q)
	{
		q.setLastAttemptTime(System.currentTimeMillis() / 1000); // The current time in seconds	
		int attempts = q.getAttempts();	
		q.setAttempts(attempts + 1);
		updateQueueRecord(q);
		return q;
	}
	
	/**
	 * Saves the given QueueRecord to the database. 
	 * 
	 * @param q - The QueueRecord to save
	 * 
	 * @return A long containing the id generated by the database
	 */
	public long saveQueueRecord (QueueRecord q)
	{
		QueueRecordProvider queueProv = QueueRecordProvider.get(App.getContext());
		long queueRecordID = queueProv.addQueueRecord(q);
		return queueRecordID;
	}
	
	/**
	 * Deletes a given QueueRecord from the database. This method
	 * should be used when the task of the given QueueRecord has
	 * been successfully completed
	 * 
	 * @param q - The QueueRecord object to delete from the
	 * database
	 */
	public void deleteQueueRecord (QueueRecord q)
	{
		QueueRecordProvider queueProv = QueueRecordProvider.get(App.getContext());
		queueProv.deleteQueueRecord(q);
	}
}