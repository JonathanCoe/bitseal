package org.bitseal.database;

import java.util.ArrayList;

import org.bitseal.data.ServerRecord;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * A singleton class which controls the creation, reading, updating, and
 * deletion of stored ServerRecord objects. 
 * 
 * @author Jonathan Coe
 */

public class ServerRecordProvider
{
    private static final String TAG = "SERVER_RECORD_PROVIDER";

    private static ServerRecordProvider sServerRecordProvider;
    
    private Context mAppContext;
    private static ContentResolver mContentResolver;

    private ServerRecordProvider(Context appContext)
    {
        mAppContext = appContext;
        mContentResolver = mAppContext.getContentResolver();
    }
    
    /**
     * Returns an instance of this singleton class. 
     * 
     * @param c - A Context object for the currently running application
     */
    public static ServerRecordProvider get(Context c)
    {
        if (sServerRecordProvider == null) 
        {
        	Context appContext = c.getApplicationContext();
        	sServerRecordProvider = new ServerRecordProvider(appContext);
        }
        
        return sServerRecordProvider;
    }
    
    /**
     * Takes a ServerRecord object and adds it to the app's 
     * SQLite database as a new record, returning the ID of the 
     * newly created record. 
     * 
     * @param s - The ServerRecord object to be added
     * 
     * @return id - A long value representing the ID of the newly
     * created record
     */
    public long addServerRecord(ServerRecord s) 
    {
    	ContentValues values = new ContentValues();
    	values.put(ServerRecordsTable.COLUMN_URL, s.getURL());
    	values.put(ServerRecordsTable.COLUMN_USERNAME, s.getUsername());
    	values.put(ServerRecordsTable.COLUMN_PASSWORD, s.getPassword());
			
		Uri insertionUri = mContentResolver.insert(DatabaseContentProvider.CONTENT_URI_SERVER_RECORDS, values);
    	Log.i(TAG, "ServerRecord with url " + s.getURL() + " saved to database");
    	
		// Parse the ID of the newly created record from the insertion Uri
		String uriString = insertionUri.toString();
		String idString = uriString.substring(uriString.indexOf("/") + 1);
		long id = Long.parseLong(idString);
		return id;
    }
    
    /**
     * Finds all ServerRecords in the application's database that match the given field
     * 
     * @param columnName - A String specifying the name of the column in the database that 
     * should be used to find matching records. See the ServerRecordsTable class to find
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
     * @return An ArrayList containing ServerRecord objects populated with the data from
     *  the database search
     */
    public ArrayList<ServerRecord> searchServerRecords(String columnName, String searchString)
    {
    	ArrayList<ServerRecord> matchingRecords = new ArrayList<ServerRecord>();

    	// Specify which colums from the table we are interested in
		String[] projection = {
				ServerRecordsTable.COLUMN_ID,
				ServerRecordsTable.COLUMN_URL,
				ServerRecordsTable.COLUMN_USERNAME,
				ServerRecordsTable.COLUMN_PASSWORD};
		
		// Query the database via the ContentProvider
		Cursor cursor = mContentResolver.query(
				DatabaseContentProvider.CONTENT_URI_SERVER_RECORDS, 
				projection, 
				ServerRecordsTable.TABLE_SERVER_RECORDS + "." + columnName + " = ? ", 
				new String[]{searchString}, 
				null);
			
		if (cursor.moveToFirst())
    	{
    	    do 
    	    {
    	        long id = cursor.getLong(0);
    	        String url = cursor.getString(1);
    	        String username = cursor.getString(2);
    	        String password = cursor.getString(3);
    	      
    	        ServerRecord s = new ServerRecord();
    	        s.setId(id);
    	        s.setURL(url);
    	        s.setUsername(username);
    	        s.setPassword(password);
    	      
    	        matchingRecords.add(s);
    	    } 
    	    while (cursor.moveToNext());
    	}
			
		else
		{
			Log.i(TAG, "Unable to find any ServerRecords with the value " + searchString + " in the " + columnName + " column");
			return matchingRecords;
		}
		
		cursor.close();
	
    	return matchingRecords;
     }
    
    /**
     * Searches the database for the ServerRecord with the given ID.
     * This method will return exactly one ServerRecord object or throw
     * a RuntimeException.
     * 
     * @param id - A long value representing the ServerRecord's ID.
     * 
     * @return The ServerRecord object with the given ID. 
     */
    public ServerRecord searchForSingleRecord(long id)
    {
    	ArrayList<ServerRecord> retrievedRecords = searchServerRecords(ServerRecordsTable.COLUMN_ID, String.valueOf(id));
    	
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
     * Returns an ArrayList containing all the ServerRecords stored in the 
     * application's database
     * 
     * @return An ArrayList containing one ServerRecord object for
     * each record in the ServerRecords table.
     */
    public ArrayList<ServerRecord> getAllServerRecords()
    {
    	ArrayList<ServerRecord> serverRecords = new ArrayList<ServerRecord>();
    	
        // Specify which colums from the table we are interested in
		String[] projection = {
				ServerRecordsTable.COLUMN_ID,
				ServerRecordsTable.COLUMN_URL,
				ServerRecordsTable.COLUMN_USERNAME,
				ServerRecordsTable.COLUMN_PASSWORD};
		
		// Query the database via the ContentProvider
		Cursor cursor = mContentResolver.query(
				DatabaseContentProvider.CONTENT_URI_SERVER_RECORDS, 
				projection, 
				null, 
				null, 
				null);
    	
    	if (cursor.moveToFirst())
    	{
    	   do 
    	   {
	   	        long id = cursor.getLong(0);
	   	        String url = cursor.getString(1);
	   	        String username = cursor.getString(2);
	   	        String password = cursor.getString(3);
	   	      
	   	        ServerRecord s = new ServerRecord();
	   	        s.setId(id);
	   	        s.setURL(url);
	   	        s.setUsername(username);
	   	        s.setPassword(password);
    	      
	   	        serverRecords.add(s);
    	   } 
    	   while (cursor.moveToNext());
    	}
    	
    	return serverRecords;
    }
    
    /**
     * Updates the database record for a given ServerRecord object<br><br>
     * 
     * <b>NOTE:</b> This method uses the given ServerRecord's ID field to determine
     * which record in the database to update
     * 
     * @param s - The ServerRecord object to be updated
     */
    public void updateServerRecord(ServerRecord s)
    {
    	ContentValues values = new ContentValues();
    	values.put(ServerRecordsTable.COLUMN_URL, s.getURL());
    	values.put(ServerRecordsTable.COLUMN_USERNAME, s.getUsername());
    	values.put(ServerRecordsTable.COLUMN_PASSWORD, s.getPassword());
		
		long id = s.getId();
    	
		// Query the database via the ContentProvider and update the record with the matching ID
    	mContentResolver.update(DatabaseContentProvider.CONTENT_URI_SERVER_RECORDS,
    			values, 
    			ServerRecordsTable.COLUMN_ID + " = ? ", 
				new String[]{String.valueOf(id)});
    	
    	Log.i(TAG, "ServerRecord ID " + id + " updated");
    }
    
    /**
     * Deletes a ServerRecord object from the application's SQLite database<br><br>
     * 
     * <b>NOTE:</b> This method uses the given ServerRecord's ID field to determine
     * which record in the database to delete
     * 
     * @param s - The ServerRecord object to be deleted
     */
    public void deleteServerRecord(ServerRecord s)
    {
		long id = s.getId();
		
		// Query the database via the ContentProvider and delete the record with the matching ID
		int recordsDeleted = mContentResolver.delete(
				DatabaseContentProvider.CONTENT_URI_SERVER_RECORDS, 
				ServerRecordsTable.COLUMN_ID + " = ? ", 
				new String[]{String.valueOf(id)});
    	
    	Log.i(TAG, recordsDeleted + " ServerRecord(s) deleted from database");
    }
    
    /**
     * Deletes all ServerRecords from the database
     */
    public void deleteAllServerRecords()
    {
		// Query the database via the ContentProvider and delete all the records
		int recordsDeleted = mContentResolver.delete(
				DatabaseContentProvider.CONTENT_URI_SERVER_RECORDS, 
				null, 
				null);
    	
    	Log.i(TAG, recordsDeleted + " ServerRecord(s) deleted from database");
    }
}


