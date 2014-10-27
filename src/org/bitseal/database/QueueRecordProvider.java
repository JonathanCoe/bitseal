package org.bitseal.database;

import java.util.ArrayList;

import org.bitseal.data.QueueRecord;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * A singleton class which controls the creation, reading, updating, and
 * deletion of stored QueueRecord objects. 
 * 
 * @author Jonathan Coe
 */

public class QueueRecordProvider
{
    private static final String TAG = "QUEUE_RECORD_PROVIDER"; 

    private static QueueRecordProvider sQueueRecordProvider;
    
    private Context mAppContext;
    private static ContentResolver mContentResolver;

    private QueueRecordProvider(Context appContext)
    {
        mAppContext = appContext;
        mContentResolver = mAppContext.getContentResolver();
    }
    
    /**
     * Returns an instance of this singleton class. 
     * 
     * @param c - A Context object for the currently running application
     */
    public static QueueRecordProvider get(Context c)
    {
        if (sQueueRecordProvider == null) 
        {
        	Context appContext = c.getApplicationContext();
        	sQueueRecordProvider = new QueueRecordProvider(appContext);
        }
        
        return sQueueRecordProvider;
    }
    
    /**
     * Takes an QueueRecord object and adds it to the app's 
     * SQLite database as a new record, returning the ID of the 
     * newly created record. 
     * 
     * @param q - The QueueRecord object to be added
     * 
     * @return id - A long value representing the ID of the newly
     * created record
     */
    public long addQueueRecord(QueueRecord q)
    {	
    	ContentValues values = new ContentValues();
    	values.put(QueueRecordsTable.COLUMN_TASK, q.getTask());
    	values.put(QueueRecordsTable.COLUMN_TRIGGER_TIME, q.getTriggerTime());
    	values.put(QueueRecordsTable.COLUMN_RECORD_COUNT, q.getRecordCount());
    	values.put(QueueRecordsTable.COLUMN_LAST_ATTEMPT_TIME, q.getLastAttemptTime());
    	values.put(QueueRecordsTable.COLUMN_ATTEMPTS, q.getAttempts());
    	values.put(QueueRecordsTable.COLUMN_OBJECT_0_ID, q.getObject0Id());
    	values.put(QueueRecordsTable.COLUMN_OBJECT_0_TYPE, q.getObject0Type());
    	values.put(QueueRecordsTable.COLUMN_OBJECT_1_ID, q.getObject1Id());
    	values.put(QueueRecordsTable.COLUMN_OBJECT_1_TYPE, q.getObject1Type());
			
		Uri insertionUri = mContentResolver.insert(DatabaseContentProvider.CONTENT_URI_QUEUE_RECORDS, values);	
    	Log.i(TAG, "QueueRecord with task " + q.getTask() + " and number of attempts " + q.getAttempts() + " saved to database");
    	
		// Parse the ID of the newly created record from the insertion URI
		String uriString = insertionUri.toString();
		String idString = uriString.substring(uriString.indexOf("/") + 1);
		long id = Long.parseLong(idString);
		return id;
    }
    
    /**
     * Finds all QueueRecords in the application's database that match the given field
     * 
     * @param columnName - A String specifying the name of the column in the database that 
     * should be used to find matching records. See the QueueRecordsTable class to find
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
     * @return An ArrayList containing QueueRecord objects populated with the data from
     *  the database search
     */
    public ArrayList<QueueRecord> searchQueueRecords(String columnName, String searchString)
    {
    	ArrayList<QueueRecord> matchingRecords = new ArrayList<QueueRecord>();

    	// Specify which columns from the table we are interested in
		String[] projection = {
				QueueRecordsTable.COLUMN_ID, 
				QueueRecordsTable.COLUMN_TASK,
				QueueRecordsTable.COLUMN_TRIGGER_TIME,
				QueueRecordsTable.COLUMN_RECORD_COUNT,
				QueueRecordsTable.COLUMN_LAST_ATTEMPT_TIME,
				QueueRecordsTable.COLUMN_ATTEMPTS,
				QueueRecordsTable.COLUMN_OBJECT_0_ID,
				QueueRecordsTable.COLUMN_OBJECT_0_TYPE, 
				QueueRecordsTable.COLUMN_OBJECT_1_ID,
				QueueRecordsTable.COLUMN_OBJECT_1_TYPE};
		
		// Query the database via the ContentProvider
		Cursor cursor = mContentResolver.query(
				DatabaseContentProvider.CONTENT_URI_QUEUE_RECORDS, 
				projection, 
				QueueRecordsTable.TABLE_QUEUE_RECORDS + "." + columnName + " = ? ", 
				new String[]{searchString}, 
				null);
			
		if (cursor.moveToFirst())
    	{
    	    do 
    	    {
    	        long id = cursor.getLong(0);
    	        String task = cursor.getString(1);
    	        long triggerTime = cursor.getLong(2);
    	        int completionCount = cursor.getInt(3);
    	        long lastAttemptTime = cursor.getLong(4);
    	        int attempts = cursor.getInt(5);
    	        long object0Id = cursor.getLong(6);
    	        String object0Type = cursor.getString(7);
    	        long object1Id = cursor.getLong(8);
    	        String object1Type = cursor.getString(9);
    	      
    	        QueueRecord q = new QueueRecord();
    	        q.setId(id);
    	        q.setTask(task);
    	        q.setTriggerTime(triggerTime);
    	        q.setRecordCount(completionCount);
    	        q.setLastAttemptTime(lastAttemptTime);
    	        q.setAttempts(attempts);
    	        q.setObject0Id(object0Id);
    	        q.setObject0Type(object0Type);
    	        q.setObject1Id(object1Id);
    	        q.setObject1Type(object1Type);
    	      
    	        matchingRecords.add(q);
    	    } 
    	    while (cursor.moveToNext());
    	}
			
		else
		{
			Log.i(TAG, "Unable to find any QueueRecords with the value " + searchString + " in the " + columnName + " column");
			return matchingRecords;
		}
		
		cursor.close();
	
    	return matchingRecords;
     }

    /**
     * Searches the database for the QueueRecord with the given ID.
     * This method will return exactly one QueueRecord object or throw
     * a RuntimeException.
     * 
     * @param id - A long value representing the QueueRecord's ID.
     * 
     * @return The QueueRecord object with the given ID. 
     */
    public QueueRecord searchForSingleRecord(long id)
    {
    	ArrayList<QueueRecord> retrievedRecords = searchQueueRecords(QueueRecordsTable.COLUMN_ID, String.valueOf(id));
    	
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
     * Returns an ArrayList containing all the QueueRecords stored in the 
     * application's database
     * 
     * @return An ArrayList containing one QueueRecord object for
     * each record in the QueueRecords table.
     */
    public ArrayList<QueueRecord> getAllQueueRecords()
    {
    	ArrayList<QueueRecord> queueRecords = new ArrayList<QueueRecord>();
    	
        // Specify which columns from the table we are interested in
		String[] projection = {
				QueueRecordsTable.COLUMN_ID, 
				QueueRecordsTable.COLUMN_TASK,
				QueueRecordsTable.COLUMN_TRIGGER_TIME,
				QueueRecordsTable.COLUMN_RECORD_COUNT,
				QueueRecordsTable.COLUMN_LAST_ATTEMPT_TIME,
				QueueRecordsTable.COLUMN_ATTEMPTS,
				QueueRecordsTable.COLUMN_OBJECT_0_ID,
				QueueRecordsTable.COLUMN_OBJECT_0_TYPE, 
				QueueRecordsTable.COLUMN_OBJECT_1_ID,
				QueueRecordsTable.COLUMN_OBJECT_1_TYPE};
		
		// Query the database via the ContentProvider
		Cursor cursor = mContentResolver.query(
				DatabaseContentProvider.CONTENT_URI_QUEUE_RECORDS, 
				projection,
				null,
				null,
				null);
    	
    	if (cursor.moveToFirst())
    	{
    	    do 
    	    {
    	        long id = cursor.getLong(0);
    	        String task = cursor.getString(1);
    	        long triggerTime = cursor.getLong(2);
    	        int completionCount = cursor.getInt(3);
    	        long lastAttemptTime = cursor.getLong(4);
    	        int attempts = cursor.getInt(5);
    	        long object0Id = cursor.getLong(6);
    	        String object0Type = cursor.getString(7);
    	        long object1Id = cursor.getLong(8);
    	        String object1Type = cursor.getString(9);
    	      
    	        QueueRecord q = new QueueRecord();
    	        q.setId(id);
    	        q.setTask(task);
    	        q.setTriggerTime(triggerTime);
    	        q.setRecordCount(completionCount);
    	        q.setLastAttemptTime(lastAttemptTime);
    	        q.setAttempts(attempts);
    	        q.setObject0Id(object0Id);
    	        q.setObject0Type(object0Type);
    	        q.setObject1Id(object1Id);
    	        q.setObject1Type(object1Type);
    	      
    	        queueRecords.add(q);
    	    } 
    	    while (cursor.moveToNext());
    	}
    	
    	return queueRecords;
    }
    
    /**
     * Updates the database record for a given QueueRecord object<br><br>
     * 
     * <b>NOTE:</b> This method uses the given QueueRecord's ID field to determine
     * which record in the database to update
     * 
     * @param q - The QueueRecord object to be updated
     */
    public void updateQueueRecord(QueueRecord q)
    {
    	ContentValues values = new ContentValues();
    	values.put(QueueRecordsTable.COLUMN_TASK, q.getTask());
    	values.put(QueueRecordsTable.COLUMN_TRIGGER_TIME, q.getTriggerTime());
    	values.put(QueueRecordsTable.COLUMN_RECORD_COUNT, q.getRecordCount());
    	values.put(QueueRecordsTable.COLUMN_LAST_ATTEMPT_TIME, q.getLastAttemptTime());
    	values.put(QueueRecordsTable.COLUMN_ATTEMPTS, q.getAttempts());
    	values.put(QueueRecordsTable.COLUMN_OBJECT_0_ID, q.getObject0Id());
    	values.put(QueueRecordsTable.COLUMN_OBJECT_0_TYPE, q.getObject0Type());
    	values.put(QueueRecordsTable.COLUMN_OBJECT_1_ID, q.getObject1Id());
    	values.put(QueueRecordsTable.COLUMN_OBJECT_1_TYPE, q.getObject1Type());
		
		long id = q.getId();
		String task = q.getTask();
		int attempts = q.getAttempts();
    	
		// Query the database via the ContentProvider and update the record with the matching ID
    	mContentResolver.update(DatabaseContentProvider.CONTENT_URI_QUEUE_RECORDS,
    			values, 
    			QueueRecordsTable.COLUMN_ID + " = ? ", 
				new String[]{String.valueOf(id)});
    	
    	Log.i(TAG, "QueueRecord with ID " + id + ", task '" + task + "', and number of attempts " + attempts + " updated");
    }
    
    /**
     * Deletes a QueueRecord object from the application's SQLite database<br><br>
     * 
     * <b>NOTE:</b> This method uses the given QueueRecord's ID field to determine
     * which record in the database to delete
     * 
     * @param m - The QueueRecord object to be deleted
     */
    public void deleteQueueRecord(QueueRecord q) 
    {
		long id = q.getId();
		
		// Query the database via the ContentProvider and delete the record with the matching ID
		int recordsDeleted = mContentResolver.delete(
				DatabaseContentProvider.CONTENT_URI_QUEUE_RECORDS, 
				QueueRecordsTable.COLUMN_ID + " = ? ", 
				new String[]{String.valueOf(id)});
    	
    	Log.i(TAG, recordsDeleted + " QueueRecord(s) with task " + q.getTask() + " and number of attempts " + q.getAttempts() + 
    			" deleted from the database.");
    }
    
    /**
     * Deletes all QueueRecords from the database
     */
    public void deleteAllQueueRecords()
    {
		// Query the database via the ContentProvider and delete all the records
		int recordsDeleted = mContentResolver.delete(
				DatabaseContentProvider.CONTENT_URI_QUEUE_RECORDS, 
				null, 
				null);
    	
    	Log.i(TAG, "All QueueRecords deleted deleted from the database. Total number deleted: " + recordsDeleted);
    }
}


