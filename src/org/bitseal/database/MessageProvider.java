package org.bitseal.database;

import java.util.ArrayList;

import org.bitseal.data.Message;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * A singleton class which controls the creation, reading, updating, and
 * deletion of stored Message objects. 
 * 
 * @author Jonathan Coe
 */

public class MessageProvider
{
	/** 
	 * This is the maximum age of an object (in seconds) that PyBitmessage will accept.
	 * In this instance we use it as the period for which identical messages received will
	 * be treated as duplicates and ignored. 
	 * */
	private static final int PYBITMESSAGE_NEW_OBJECT_ACCEPTANCE_PERIOD = 216000;

    private static MessageProvider sMessageProvider;
    
    private Context mAppContext;
    private static ContentResolver mContentResolver;
    
    private static final String TAG = "MESSAGE_PROVIDER"; 

    private MessageProvider(Context appContext)
    {
        mAppContext = appContext;
        mContentResolver = mAppContext.getContentResolver();
    }
    
    /**
     * Returns an instance of this singleton class. 
     * 
     * @param c - A Context object for the currently running application
     */
    public static MessageProvider get(Context c)
    {
        if (sMessageProvider == null) 
        {
        	Context appContext = c.getApplicationContext();
        	sMessageProvider = new MessageProvider(appContext);
        }
        
        return sMessageProvider;
    }
    
    /**
     * Takes an Message object and adds it to the app's 
     * SQLite database as a new record, returning the ID of the 
     * newly created record.  
     * 
     * @param m - The Message object to be added
     * 
     * @return id - A long value representing the ID of the newly
     * created record
     */
    public long addMessage(Message m)
    {
    	int belongsToMe = 0;
    	if (m.belongsToMe() == true)
    	{
    		belongsToMe = 1;
    	}
    	
    	ContentValues values = new ContentValues();
    	values.put(MessagesTable.COLUMN_MSG_PAYLOAD_ID, m.getMsgPayloadId());
    	values.put(MessagesTable.COLUMN_ACK_PAYLOAD_ID, m.getAckPayloadId());
    	values.put(MessagesTable.COLUMN_BELONGS_TO_ME, belongsToMe);
    	values.put(MessagesTable.COLUMN_READ, m.hasBeenRead());
    	values.put(MessagesTable.COLUMN_STATUS, m.getStatus());
    	values.put(MessagesTable.COLUMN_TIME, m.getTime());
    	values.put(MessagesTable.COLUMN_TO_ADDRESS, m.getToAddress());
    	values.put(MessagesTable.COLUMN_FROM_ADDRESS, m.getFromAddress());
    	values.put(MessagesTable.COLUMN_SUBJECT, m.getSubject());
    	values.put(MessagesTable.COLUMN_BODY, m.getBody());
			
		Uri insertionUri = mContentResolver.insert(DatabaseContentProvider.CONTENT_URI_MESSAGES, values);
    	Log.i(TAG, "Message with subject " + m.getSubject() + " saved to database");
    	
		// Parse the ID of the newly created record from the insertion Uri
		String uriString = insertionUri.toString();
		String idString = uriString.substring(uriString.indexOf("/") + 1);
		long id = Long.parseLong(idString);
		return id;
    }
    
    /**
     * Finds all Messages in the application's database that match the given field
     * 
     * @param columnName - A String specifying the name of the column in the database that 
     * should be used to find matching records. See the MessagesTable class to find
     * the relevant column name. 
     * @param searchString - A String specifying the value to search for. There are 4 use cases
     * for this:<br>
     * 1) The value to search for is a String (e.g. A label from the UI). In this case the value 
     * can be passed in directly.<br>
     * 2) The value to search for is an int or long. In this case you should use String.valueOf(x)
     * and pass in the resulting String.<br>
     * 3) The value to search for is a boolean. In this case you should pass in the String "0" for 
     * false or the String "1" for true. <br>
     * 4) The value to search for is a byte[]. In this case you should encode the byte[] into a 
     * Base64 encoded String using the class android.util.Base64 and pass in the resulting String.<br><br>
     * 
     * <b>NOTE:</b> The above String conversion is very clumsy, but seems to be necessary. See 
     * https://stackoverflow.com/questions/20911760/android-how-to-query-sqlitedatabase-with-non-string-selection-args
     * 
     * @return An ArrayList containing Message objects populated with the data from
     *  the database search
     */
    public ArrayList<Message> searchMessages(String columnName, String searchString)
    {
    	ArrayList<Message> matchingRecords = new ArrayList<Message>();

    	// Specify which colums from the table we are interested in
		String[] projection = {
				MessagesTable.COLUMN_ID, 
				MessagesTable.COLUMN_MSG_PAYLOAD_ID,
				MessagesTable.COLUMN_ACK_PAYLOAD_ID,
				MessagesTable.COLUMN_BELONGS_TO_ME,
				MessagesTable.COLUMN_READ,
				MessagesTable.COLUMN_STATUS,
				MessagesTable.COLUMN_TIME,
				MessagesTable.COLUMN_TO_ADDRESS,
				MessagesTable.COLUMN_FROM_ADDRESS,
				MessagesTable.COLUMN_SUBJECT,
				MessagesTable.COLUMN_BODY};
		
		// Query the database via the ContentProvider
		Cursor cursor = mContentResolver.query(
				DatabaseContentProvider.CONTENT_URI_MESSAGES, 
				projection, 
				MessagesTable.TABLE_MESSAGES + "." + columnName + " = ? ", 
				new String[]{searchString}, 
				null);
			
		if (cursor.moveToFirst())
    	{
    	    do 
    	    {
    	        long id = cursor.getLong(0);
    	        long msgPayloadId = cursor.getLong(1);
    	        long ackPayloadId = cursor.getLong(2);
    	        
    	        int belongsToMeValue = cursor.getInt(3);
    	        boolean belongsToMe = false;
    	        if (belongsToMeValue == 1)
    	        {
    	        	belongsToMe = true;
    	        }
    	        
    	        int readValue = cursor.getInt(4);
    	        boolean read = false;
    	        if (readValue == 1)
    	        {
    	        	read = true;
    	        }
    	        
    	        String status = cursor.getString(5);
    	        long time = cursor.getLong(6);
    	        String toAddress = cursor.getString(7);
    	        String fromAddress = cursor.getString(8);
    	        String subject = cursor.getString(9);
    	        String body = cursor.getString(10);
    	      
    	        Message m = new Message();
    	        m.setId(id);
    	        m.setMsgPayloadId(msgPayloadId);
    	        m.setAckPayloadId(ackPayloadId);
    	        m.setBelongsToMe(belongsToMe);
    	        m.setRead(read);
    	        m.setStatus(status);
    	        m.setTime(time);
    	        m.setToAddress(toAddress);
    	        m.setFromAddress(fromAddress);
    	        m.setSubject(subject);
    	        m.setBody(body);
    	      
    	        matchingRecords.add(m);
    	    } 
    	    while (cursor.moveToNext());
    	}
			
		else
		{
			Log.i(TAG, "Unable to find any Messages with the value " + searchString + " in the " + columnName + " column");
			cursor.close();
			return matchingRecords;
		}
		
		cursor.close();
    	return matchingRecords;
     }
    
    /**
     * Searches the database for the Message with the given ID.
     * This method will return exactly one Message object or throw
     * a RuntimeException.
     * 
     * @param id - A long value representing the Message's ID.
     * 
     * @return The Message object with the given ID. 
     */
    public Message searchForSingleRecord(long id)
    {
    	ArrayList<Message> retrievedRecords = searchMessages(MessagesTable.COLUMN_ID, String.valueOf(id));
    	
    	if (retrievedRecords.size() != 1)
		{
			throw new RuntimeException("There should be exactly 1 record found in this search. Instead " + retrievedRecords.size() + " records were found");
		}
		else
		{
			return retrievedRecords.get(0);
		}
    }

    /**
     * Returns an ArrayList containing all the Messages stored in the 
     * application's database
     * 
     * @return An ArrayList containing one Message object for
     * each record in the Messages table.
     */
    public ArrayList<Message> getAllMessages()
    {
    	ArrayList<Message> messages = new ArrayList<Message>();
    	
        // Specify which colums from the table we are interested in
		String[] projection = {
				MessagesTable.COLUMN_ID, 
				MessagesTable.COLUMN_MSG_PAYLOAD_ID,
				MessagesTable.COLUMN_ACK_PAYLOAD_ID,
				MessagesTable.COLUMN_BELONGS_TO_ME,
				MessagesTable.COLUMN_READ,
				MessagesTable.COLUMN_STATUS,
				MessagesTable.COLUMN_TIME,
				MessagesTable.COLUMN_TO_ADDRESS,
				MessagesTable.COLUMN_FROM_ADDRESS,
				MessagesTable.COLUMN_SUBJECT,
				MessagesTable.COLUMN_BODY};
		
		// Query the database via the ContentProvider
		Cursor cursor = mContentResolver.query(
				DatabaseContentProvider.CONTENT_URI_MESSAGES, 
				projection,
				null,
				null,
				null);
    	
    	if (cursor.moveToFirst())
    	{
    	   do 
    	   {
	   	        long id = cursor.getLong(0);
	   	        long msgPayloadId = cursor.getLong(1);
	   	        long ackPayloadId = cursor.getLong(2);
	   	        
	   	        int belongsToMeValue = cursor.getInt(3);
	   	        boolean belongsToMe = false;
	   	        if (belongsToMeValue == 1)
	   	        {
	   	        	belongsToMe = true;
	   	        }
	   	        
	   	        int readValue = cursor.getInt(4);
	   	        boolean read = false;
	   	        if (readValue == 1)
	   	        {
	   	        	read = true;
	   	        }
	   	        
	   	        String status = cursor.getString(5);
	   	        long time = cursor.getLong(6);
	   	        String toAddress = cursor.getString(7);
	   	        String fromAddress = cursor.getString(8);
	   	        String subject = cursor.getString(9);
	   	        String body = cursor.getString(10);
	   	      
	   	        Message m = new Message();
	   	        m.setId(id);
	   	        m.setMsgPayloadId(msgPayloadId);
	   	        m.setAckPayloadId(ackPayloadId);
	   	        m.setBelongsToMe(belongsToMe);
	   	        m.setRead(read);
	   	        m.setStatus(status);
	   	        m.setTime(time);
	   	        m.setToAddress(toAddress);
	   	        m.setFromAddress(fromAddress);
	   	        m.setSubject(subject);
	   	        m.setBody(body);
    	      
    	        messages.add(m);
    	   } 
    	   while (cursor.moveToNext());
    	}
    	
		cursor.close();
    	return messages;
    }
    
    /**
     * Updates the database record for a given Message object<br><br>
     * 
     * <b>NOTE:</b> This method uses the given Message's ID field to determine
     * which record in the database to update
     * 
     * @param m - The Message object to be updated
     */
    public void updateMessage(Message m)
    {
    	int belongsToMe = 0;
    	if (m.belongsToMe() == true)
    	{
    		belongsToMe = 1;
    	}
    	
    	ContentValues values = new ContentValues();
    	values.put(MessagesTable.COLUMN_MSG_PAYLOAD_ID, m.getMsgPayloadId());
    	values.put(MessagesTable.COLUMN_ACK_PAYLOAD_ID, m.getAckPayloadId());
    	values.put(MessagesTable.COLUMN_BELONGS_TO_ME, belongsToMe);
    	values.put(MessagesTable.COLUMN_READ, m.hasBeenRead());
    	values.put(MessagesTable.COLUMN_STATUS, m.getStatus());
    	values.put(MessagesTable.COLUMN_TIME, m.getTime());
    	values.put(MessagesTable.COLUMN_TO_ADDRESS, m.getToAddress());
    	values.put(MessagesTable.COLUMN_FROM_ADDRESS, m.getFromAddress());
    	values.put(MessagesTable.COLUMN_SUBJECT, m.getSubject());
    	values.put(MessagesTable.COLUMN_BODY, m.getBody());
		
		long id = m.getId();
    	
		// Query the database via the ContentProvider and update the record with the matching ID
    	mContentResolver.update(DatabaseContentProvider.CONTENT_URI_MESSAGES,
    			values, 
    			MessagesTable.COLUMN_ID + " = ? ", 
				new String[]{String.valueOf(id)});
    	
    	Log.i(TAG, "Message ID " + id + " updated");
    }
    
    /**
     * Deletes a Message object from the application's SQLite database<br><br>
     * 
     * <b>NOTE:</b> This method uses the given Message's ID field to determine
     * which record in the database to delete
     * 
     * @param m - The Message object to be deleted
     */
    public void deleteMessage(Message m)
    {
		long id = m.getId();
		
		// Query the database via the ContentProvider and delete the record with the matching ID
		int recordsDeleted = mContentResolver.delete(
				DatabaseContentProvider.CONTENT_URI_MESSAGES, 
				MessagesTable.COLUMN_ID + " = ? ", 
				new String[]{String.valueOf(id)});
    	
    	Log.i(TAG, recordsDeleted + " Message(s) deleted from database");
    }
    
    /**
     * Deletes all Messages from the database
     */
    public void deleteAllMessages()
    {
		// Query the database via the ContentProvider and delete all the records
		int recordsDeleted = mContentResolver.delete(
				DatabaseContentProvider.CONTENT_URI_MESSAGES, 
				null, 
				null);
    	
    	Log.i(TAG, recordsDeleted + " Message(s) deleted from database");
    }
    
	/**
	 * Takes a Message and determines whether it is a duplicate of 
	 * any messages that are already in the inbox. This takes account
	 * of the to address, from address, subject, body, and received time
	 * of the message. 
	 * 
	 * @param message - The Message object to be checked
	 * 
	 * @return A boolean indicating whether or not the message is a duplicate
	 */
    public boolean detectDuplicateMessage(Message message)
    {
    	// Extract the data that we will use to make the duplicate comparison from the Message provided
    	String toAddress = message.getToAddress();
    	String fromAddress = message.getFromAddress();
    	String subject = message.getSubject();
    	String body = message.getBody();
    	long receivedTime = message.getTime();
    	
    	// Work out the time value to use in searching for duplicates. We will only consider a 
    	// Message to be a duplicate if we received it within a certain period of time, a period
    	// defined precisely by PYBITMESSAGE_NEW_OBJECT_ACCEPTANCE_PERIOD.
    	long receivedSinceTime = receivedTime - PYBITMESSAGE_NEW_OBJECT_ACCEPTANCE_PERIOD;
    	// Get the String representation of the time value
    	String receivedSinceString = String.valueOf(receivedSinceTime);
    	
    	// Build the selection statement we will use
    	String selection = 
    			MessagesTable.TABLE_MESSAGES + "." + MessagesTable.COLUMN_TO_ADDRESS + " = ? AND " +
				MessagesTable.TABLE_MESSAGES + "." + MessagesTable.COLUMN_FROM_ADDRESS + " = ? AND " +
				MessagesTable.TABLE_MESSAGES + "." + MessagesTable.COLUMN_SUBJECT + " = ? AND " +
				MessagesTable.TABLE_MESSAGES + "." + MessagesTable.COLUMN_BODY + " = ? AND " +
				MessagesTable.TABLE_MESSAGES + "." + MessagesTable.COLUMN_TIME + " > ?";
    	
    	// Build the String[] of selection arguments we will use
    	String[] selectionArgs = new String[]{toAddress, fromAddress, subject, body, receivedSinceString};
    	
        // Specify which colums from the table we are interested in
		String[] projection = {
				MessagesTable.COLUMN_ID, 
				MessagesTable.COLUMN_MSG_PAYLOAD_ID,
				MessagesTable.COLUMN_ACK_PAYLOAD_ID,
				MessagesTable.COLUMN_BELONGS_TO_ME,
				MessagesTable.COLUMN_READ,
				MessagesTable.COLUMN_STATUS,
				MessagesTable.COLUMN_TIME,
				MessagesTable.COLUMN_TO_ADDRESS,
				MessagesTable.COLUMN_FROM_ADDRESS,
				MessagesTable.COLUMN_SUBJECT,
				MessagesTable.COLUMN_BODY};
		
		// Query the database via the ContentProvider
		Cursor cursor = mContentResolver.query(
				DatabaseContentProvider.CONTENT_URI_MESSAGES, 
				projection,
				selection, 
				selectionArgs, 
				null);
    	
		int counter = 0;
    	if (cursor.moveToFirst())
    	{
    	   while (cursor.moveToNext());
    	   {
    		   counter ++;
    	   }
    	   
    	   Log.d(TAG, "Found " + counter + " duplicates of message with subject " + subject + " and to address " + toAddress);
   		   cursor.close();
    	   return true;
    	}
    	else
    	{
    		Log.i(TAG, "Found no duplicates for the message provided");
    		cursor.close();
    		return false;
    	}
    }
}