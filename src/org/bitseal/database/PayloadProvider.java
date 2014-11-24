package org.bitseal.database;

import java.util.ArrayList;

import org.bitseal.data.Payload;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

/**
 * A singleton class which controls the creation, reading, updating, and
 * deletion of stored Payload objects. 
 * 
 * @author Jonathan Coe
 */

public class PayloadProvider
{
    private static final String TAG = "PAYLOAD_PROVIDER"; 

    private static PayloadProvider sPayloadProvider;
    
    private Context mAppContext;
    private static ContentResolver mContentResolver;

    private PayloadProvider(Context appContext)
    {
        mAppContext = appContext;
        mContentResolver = mAppContext.getContentResolver();
    }
    
    /**
     * Returns an instance of this singleton class. 
     * 
     * @param c - A Context object for the currently running application
     */
    public static PayloadProvider get(Context c)
    {
        if (sPayloadProvider == null) 
        {
        	Context appContext = c.getApplicationContext();
        	sPayloadProvider = new PayloadProvider(appContext);
        }
        
        return sPayloadProvider;
    }
    
    /**
     * Takes an Payload object and adds it to the app's 
     * SQLite database as a new record, returning the ID of the 
     * newly created record. 
     * 
     * @param p - The Payload object to be added
     * 
     * @return id - A long value representing the ID of the newly
     * created record
     */
    public long addPayload(Payload p)
    {
    	int belongsToMe = 0;
    	if (p.belongsToMe() == true)
    	{
    		belongsToMe = 1;
    	}
    	
    	int processingComplete = 0;
    	if (p.processingComplete() == true)
    	{
    		processingComplete = 1;
    	}
    	
    	int powDone = 0;
    	if (p.powDone() == true)
    	{
    		powDone = 1;
    	}
    	
    	ContentValues values = new ContentValues();
    	values.put(PayloadsTable.COLUMN_RELATED_ADDRESS_ID, p.getRelatedAddressId());
    	values.put(PayloadsTable.COLUMN_BELONGS_TO_ME, belongsToMe);
    	values.put(PayloadsTable.COLUMN_PROCESSING_COMPLETE, processingComplete);
    	values.put(PayloadsTable.COLUMN_TIME, p.getTime());
    	values.put(PayloadsTable.COLUMN_TYPE, p.getType());
    	values.put(PayloadsTable.COLUMN_ACK, p.isAck());
    	values.put(PayloadsTable.COLUMN_POW_DONE, powDone);   	
    	values.put(PayloadsTable.COLUMN_PAYLOAD, Base64.encodeToString(p.getPayload(), Base64.DEFAULT));
			
		Uri insertionUri = mContentResolver.insert(DatabaseContentProvider.CONTENT_URI_PAYLOADS, values);
    	Log.i(TAG, "Payload with type " + p.getType() + " and time value " + p.getTime() + " saved to database");
    	
		// Parse the ID of the newly created record from the insertion URI
		String uriString = insertionUri.toString();
		String idString = uriString.substring(uriString.indexOf("/") + 1);
		long id = Long.parseLong(idString);
		return id;
    }
    
    /**
     * Finds all Payloads in the application's database that match the given field
     * 
     * @param columnName - A String specifying the name of the column in the database that 
     * should be used to find matching records. See the PayloadsTable class to find
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
     * @return An ArrayList containing Payload objects populated with the data from
     *  the database search
     */
    public ArrayList<Payload> searchPayloads(String columnName, String searchString)
    {
    	ArrayList<Payload> matchingRecords = new ArrayList<Payload>();

    	// Specify which columns from the table we are interested in
		String[] projection = {
				PayloadsTable.COLUMN_ID, 
				PayloadsTable.COLUMN_RELATED_ADDRESS_ID, 
				PayloadsTable.COLUMN_BELONGS_TO_ME,
				PayloadsTable.COLUMN_PROCESSING_COMPLETE, 
				PayloadsTable.COLUMN_TIME,
				PayloadsTable.COLUMN_TYPE,
				PayloadsTable.COLUMN_ACK,
				PayloadsTable.COLUMN_POW_DONE,
				PayloadsTable.COLUMN_PAYLOAD};
		
		// Query the database via the ContentProvider
		Cursor cursor = mContentResolver.query(
				DatabaseContentProvider.CONTENT_URI_PAYLOADS, 
				projection, 
				PayloadsTable.TABLE_PAYLOADS + "." + columnName + " = ? ", 
				new String[]{searchString}, 
				null);
			
		if (cursor.moveToFirst())
    	{
    	    do 
    	    {
    	        long id = cursor.getLong(0);
    	        long relatedAddressId = cursor.getLong(1);   	        
    	        int belongsToMeValue = cursor.getInt(2);
    	        boolean belongsToMe = false;
    	        if (belongsToMeValue == 1)
    	        {
    	        	belongsToMe = true;
    	        }
    	        int processingCompleteValue = cursor.getInt(3);
    	        boolean processingComplete = false;
    	        if (processingCompleteValue == 1)
    	        {
    	        	processingComplete = true;
    	        }  	         	        
    	        long time = cursor.getLong(4);
    	        String type = cursor.getString(5);
    	        int isAckValue = cursor.getInt(6);
    	        boolean isAck = false;
    	        if (isAckValue == 1)
    	        {
    	        	isAck = true;
    	        }
    	        int powDoneValue = cursor.getInt(7);
    	        boolean powDone = false;
    	        if (powDoneValue == 1)
    	        {
    	        	powDone = true;
    	        }    	        
    	        byte[] payload = Base64.decode(cursor.getString(8), Base64.DEFAULT);

    	        Payload p = new Payload();
    	        p.setId(id);
    	        p.setRelatedAddressId(relatedAddressId);
    	        p.setBelongsToMe(belongsToMe);
    	        p.setProcessingComplete(processingComplete);
    	        p.setTime(time);
    	        p.setType(type);
    	        p.setAck(isAck);
    	        p.setPOWDone(powDone);
    	        p.setPayload(payload);

    	        matchingRecords.add(p);
    	    } 
    	    while (cursor.moveToNext());
    	}
		
		cursor.close();
    	return matchingRecords;
     }
    
    /**
     * Finds all Payloads in the application's database that match the given criteria. This 
     * method allows for multiple search terms. 
     * 
     * @param columnNames - The columns in the table to use in the query
     * @param selections - The selections for each column
     * 
     * @return An ArrayList containing Payload objects populated with the data from
     *  the database search
     */
    public ArrayList<Payload> searchPayloads(String[] columnNames, String[] selections)
    {
    	ArrayList<Payload> matchingRecords = new ArrayList<Payload>();

    	// Specify which columns from the table we are interested in
		String[] projection = {
				PayloadsTable.COLUMN_ID, 
				PayloadsTable.COLUMN_RELATED_ADDRESS_ID, 
				PayloadsTable.COLUMN_BELONGS_TO_ME,
				PayloadsTable.COLUMN_PROCESSING_COMPLETE, 
				PayloadsTable.COLUMN_TIME,
				PayloadsTable.COLUMN_TYPE,
				PayloadsTable.COLUMN_ACK,
				PayloadsTable.COLUMN_POW_DONE,
				PayloadsTable.COLUMN_PAYLOAD};
		
		// Build the selection String
		String selectionString = PayloadsTable.TABLE_PAYLOADS + ".";
		int counter = 0;
		for (String columnName : columnNames)
		{
			String stringToAppend = columnName + " = ? ";
			
			if ((counter + 1) != columnNames.length) // If this is not the last column name in the search data
			{
				stringToAppend = stringToAppend + "AND ";
			}
			
			selectionString = selectionString + stringToAppend;
			counter ++;
		}
		
		// Query the database via the ContentProvider
		Cursor cursor = mContentResolver.query(
				DatabaseContentProvider.CONTENT_URI_PAYLOADS, 
				projection, 
				selectionString, 
				selections, 
				null);
			
		if (cursor.moveToFirst())
    	{
			do 
    	    {
    	        long id = cursor.getLong(0);
    	        long relatedAddressId = cursor.getLong(1);   	        
    	        int belongsToMeValue = cursor.getInt(2);
    	        boolean belongsToMe = false;
    	        if (belongsToMeValue == 1)
    	        {
    	        	belongsToMe = true;
    	        }
    	        int processingCompleteValue = cursor.getInt(3);
    	        boolean processingComplete = false;
    	        if (processingCompleteValue == 1)
    	        {
    	        	processingComplete = true;
    	        }  	         	        
    	        long time = cursor.getLong(4);
    	        String type = cursor.getString(5);
    	        int isAckValue = cursor.getInt(6);
    	        boolean isAck = false;
    	        if (isAckValue == 1)
    	        {
    	        	isAck = true;
    	        }
    	        int powDoneValue = cursor.getInt(7);
    	        boolean powDone = false;
    	        if (powDoneValue == 1)
    	        {
    	        	powDone = true;
    	        }    	        
    	        byte[] payload = Base64.decode(cursor.getString(8), Base64.DEFAULT);

    	        Payload p = new Payload();
    	        p.setId(id);
    	        p.setRelatedAddressId(relatedAddressId);
    	        p.setBelongsToMe(belongsToMe);
    	        p.setProcessingComplete(processingComplete);
    	        p.setTime(time);
    	        p.setType(type);
    	        p.setAck(isAck);
    	        p.setPOWDone(powDone);
    	        p.setPayload(payload);

    	        matchingRecords.add(p);
    	    } 
    	    while (cursor.moveToNext());
    	}
		
		cursor.close();
    	return matchingRecords;
     }
    
    /**
     * Searches the database for the Payload with the given ID.
     * This method will return exactly one Payload object or throw
     * a RuntimeException.
     * 
     * @param id - A long value representing the Payload's ID.
     * 
     * @return The Payload object with the given ID. 
     */
    public Payload searchForSingleRecord(long id)
    {
    	ArrayList<Payload> retrievedRecords = searchPayloads(PayloadsTable.COLUMN_ID, String.valueOf(id));
    	
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
     * Returns an ArrayList containing all the Payloads stored in the 
     * application's database
     * 
     * @return An ArrayList containing one Payload object for
     * each record in the Payloads table.
     */
    public ArrayList<Payload> getAllPayloads()
    {
    	
    	
    	ArrayList<Payload> payloads = new ArrayList<Payload>();
    	
        // Specify which columns from the table we are interested in
		String[] projection = {
				PayloadsTable.COLUMN_ID, 
				PayloadsTable.COLUMN_RELATED_ADDRESS_ID, 
				PayloadsTable.COLUMN_BELONGS_TO_ME,
				PayloadsTable.COLUMN_PROCESSING_COMPLETE, 
				PayloadsTable.COLUMN_TIME,
				PayloadsTable.COLUMN_TYPE,
				PayloadsTable.COLUMN_ACK,
				PayloadsTable.COLUMN_POW_DONE,
				PayloadsTable.COLUMN_PAYLOAD};
		
		// Query the database via the ContentProvider
		Cursor cursor = mContentResolver.query(
				DatabaseContentProvider.CONTENT_URI_PAYLOADS, 
				projection, 
				null, 
				null, 
				null);
    	
    	if (cursor.moveToFirst())
    	{
    		do 
    	    {
    	        long id = cursor.getLong(0);
    	        long relatedAddressId = cursor.getLong(1);   	        
    	        int belongsToMeValue = cursor.getInt(2);
    	        boolean belongsToMe = false;
    	        if (belongsToMeValue == 1)
    	        {
    	        	belongsToMe = true;
    	        }
    	        int processingCompleteValue = cursor.getInt(3);
    	        boolean processingComplete = false;
    	        if (processingCompleteValue == 1)
    	        {
    	        	processingComplete = true;
    	        }  	         	        
    	        long time = cursor.getLong(4);
    	        String type = cursor.getString(5);
    	        int isAckValue = cursor.getInt(6);
    	        boolean isAck = false;
    	        if (isAckValue == 1)
    	        {
    	        	isAck = true;
    	        }
    	        int powDoneValue = cursor.getInt(7);
    	        boolean powDone = false;
    	        if (powDoneValue == 1)
    	        {
    	        	powDone = true;
    	        }    	        
    	        byte[] payload = Base64.decode(cursor.getString(8), Base64.DEFAULT);

    	        Payload p = new Payload();
    	        p.setId(id);
    	        p.setRelatedAddressId(relatedAddressId);
    	        p.setBelongsToMe(belongsToMe);
    	        p.setProcessingComplete(processingComplete);
    	        p.setTime(time);
    	        p.setType(type);
    	        p.setAck(isAck);
    	        p.setPOWDone(powDone);
    	        p.setPayload(payload);

    	        payloads.add(p);
    	    } 
    	    while (cursor.moveToNext());
    	}
    	
		cursor.close();
    	return payloads;
    }
    
    /**
     * Updates the database record for a given Payload object<br><br>
     * 
     * <b>NOTE:</b> This method uses the given Payload's ID field to determine
     * which record in the database to update
     * 
     * @param p - The Payload object to be updated
     */
    public void updatePayload(Payload p)
    {
    	int belongsToMe = 0;
    	if (p.belongsToMe() == true)
    	{
    		belongsToMe = 1;
    	}
    	
    	int processingComplete = 0;
    	if (p.processingComplete() == true)
    	{
    		processingComplete = 1;
    	}
    	
    	int powDone = 0;
    	if (p.powDone() == true)
    	{
    		powDone = 1;
    	}
    	
    	ContentValues values = new ContentValues();
    	values.put(PayloadsTable.COLUMN_RELATED_ADDRESS_ID, p.getRelatedAddressId());
    	values.put(PayloadsTable.COLUMN_BELONGS_TO_ME, belongsToMe);
    	values.put(PayloadsTable.COLUMN_PROCESSING_COMPLETE, processingComplete);
    	values.put(PayloadsTable.COLUMN_TIME, p.getTime());
    	values.put(PayloadsTable.COLUMN_TYPE, p.getType());
    	values.put(PayloadsTable.COLUMN_ACK, p.isAck());
    	values.put(PayloadsTable.COLUMN_POW_DONE, powDone);   	
    	values.put(PayloadsTable.COLUMN_PAYLOAD, Base64.encodeToString(p.getPayload(), Base64.DEFAULT));
		
		long id = p.getId();
    	
		// Query the database via the ContentProvider and update the record with the matching ID
    	mContentResolver.update(DatabaseContentProvider.CONTENT_URI_PAYLOADS,
    			values, 
    			PayloadsTable.COLUMN_ID + " = ? ", 
				new String[]{String.valueOf(id)});
    	
    	Log.i(TAG, "Payload with ID " + id + " updated");
    }
    
    /**
     * Deletes an Payload object from the application's SQLite database<br><br>
     * 
     * <b>NOTE:</b> This method uses the given Payload's ID field to determine
     * which record in the database to delete
     * 
     * @param p - The Payload object to be deleted
     */
    public void deletePayload(Payload p) 
    {
		long id = p.getId();
		
		// Query the database via the ContentProvider and delete the record with the matching ID
		int recordsDeleted = mContentResolver.delete(
				DatabaseContentProvider.CONTENT_URI_PAYLOADS, 
				PayloadsTable.COLUMN_ID + " = ? ", 
				new String[]{String.valueOf(id)});
    	
    	Log.i(TAG, recordsDeleted + " Payload(s) deleted from database");
    }
    
    /**
     * Deletes all Payloads from the database
     */
    public void deleteAllPayloads()
    {
		// Query the database via the ContentProvider and delete all the records
		int recordsDeleted = mContentResolver.delete(
				DatabaseContentProvider.CONTENT_URI_PAYLOADS, 
				null, 
				null);
    	
    	Log.i(TAG, recordsDeleted + " Payload(s) deleted from database");
    }
    
    /**
     * Deletes any Payloads that have a time value earlier than the specified value.
     * 
     * @param deletionTime - The specified time
     * 
     * @return The number of payloads deleted from the database
     */
    public void deletePayloadsCreatedBefore(long deletionTime)
    {
		int recordsDeleted = mContentResolver.delete(
				DatabaseContentProvider.CONTENT_URI_PAYLOADS, 
				PayloadsTable.COLUMN_TIME + " < ? ", 
				new String[]{String.valueOf(deletionTime)});
    	
    	Log.i(TAG, recordsDeleted + " Payload(s) deleted from database");
     }
}