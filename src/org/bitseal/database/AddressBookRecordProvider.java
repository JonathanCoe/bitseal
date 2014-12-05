package org.bitseal.database;

import java.util.ArrayList;

import org.bitseal.data.AddressBookRecord;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * A singleton class which controls the creation, reading, updating, and
 * deletion of stored AddressBookRecord objects. 
 * 
 * @author Jonathan Coe
 */
public class AddressBookRecordProvider
{
    private static final String TAG = "ADDRESS_BOOK_RECORD_PROVIDER"; 

    private static AddressBookRecordProvider sAddressBookRecordProvider;
    
    private Context mAppContext;
    private static ContentResolver mContentResolver;

    private AddressBookRecordProvider(Context appContext)
    {
        mAppContext = appContext;
        mContentResolver = mAppContext.getContentResolver();
    }
    
    /**
     * Returns an instance of this singleton class. 
     * 
     * @param c - A Context object for the currently running application
     */
    public static AddressBookRecordProvider get(Context c)
    {
        if (sAddressBookRecordProvider == null) 
        {
        	Context appContext = c.getApplicationContext();
        	sAddressBookRecordProvider = new AddressBookRecordProvider(appContext);
        }
        
        return sAddressBookRecordProvider;
    }
    
    /**
     * Takes an AddressBookRecord object and adds it to the app's 
     * SQLite database as a new record, returning the ID of the 
     * newly created record. 
     * 
     * @param a - The AddressBookRecord object to be added
     * 
     * @return id - A long value representing the ID of the newly
     * created record
     */
    public long addAddressBookRecord(AddressBookRecord a) 
    {
    	ContentValues values = new ContentValues();
    	values.put(AddressBookRecordsTable.COLUMN_COLOUR_R, a.getColourR());
    	values.put(AddressBookRecordsTable.COLUMN_COLOUR_G, a.getColourG());
    	values.put(AddressBookRecordsTable.COLUMN_COLOUR_B, a.getColourB());
    	values.put(AddressBookRecordsTable.COLUMN_LABEL, a.getLabel());
    	values.put(AddressBookRecordsTable.COLUMN_ADDRESS, a.getAddress());
		
		Uri insertionUri = mContentResolver.insert(DatabaseContentProvider.CONTENT_URI_ADDRESS_BOOK_RECORDS, values);
		Log.i(TAG, "AddressBookRecord with address " + a.getAddress() + " saved to database");
		
		// Parse the ID of the newly created record from the insertion Uri
		String uriString = insertionUri.toString();
		String idString = uriString.substring(uriString.indexOf("/") + 1);
		long id = Long.parseLong(idString);
		return id;
    }
    
    /**
     * Finds all AddressBookRecords in the application's database that match the given field
     * 
     * @param columnName - A String specifying the name of the column in the database that 
     * should be used to find matching records. See the AddressBookRecordsTable class to find
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
     * @return An ArrayList containing AddressBookRecord objects populated with the data from
     *  the database search
     */
    public ArrayList<AddressBookRecord> searchAddressBookRecords(String columnName, String searchString)
    {
    	ArrayList<AddressBookRecord> matchingRecords = new ArrayList<AddressBookRecord>();

    	// Specify which columns from the table we are interested in
		String[] projection = {
				AddressBookRecordsTable.COLUMN_ID, 
				AddressBookRecordsTable.COLUMN_COLOUR_R,
				AddressBookRecordsTable.COLUMN_COLOUR_G,
				AddressBookRecordsTable.COLUMN_COLOUR_B,
				AddressBookRecordsTable.COLUMN_LABEL,
				AddressBookRecordsTable.COLUMN_ADDRESS};
		
		// Query the database via the ContentProvider
		Cursor cursor = mContentResolver.query(
				DatabaseContentProvider.CONTENT_URI_ADDRESS_BOOK_RECORDS, 
				projection, 
				AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS + "." + columnName + " = ? ", 
				new String[]{searchString}, 
				null);
			
		if (cursor.moveToFirst())
    	{
    	    do 
    	    {
    	        long id = cursor.getLong(0);
    	        int colourR = cursor.getInt(1);
    	        int colourG = cursor.getInt(2);
    	        int colourB = cursor.getInt(3);
    	        String label = cursor.getString(4);
    	        String address = cursor.getString(5);
    	      
    	        AddressBookRecord a = new AddressBookRecord();
    	        a.setId(id);
    	        a.setColourR(colourR);
    	        a.setColourG(colourG);
    	        a.setColourB(colourB);
    	        a.setLabel(label);
    	        a.setAddress(address);
    	      
    	        matchingRecords.add(a);
    	    } 
    	    while (cursor.moveToNext());
    	}	
		else
		{
			cursor.close();
			return matchingRecords;
		}
		
		cursor.close();
    	return matchingRecords;
     }
    
    /**
     * Searches the database for the AddressBookRecord with the given ID.
     * This method will return exactly one AddressBookRecord object or throw
     * a RuntimeException.
     * 
     * @param id - A long value representing the AddressBookRecord's ID.
     * 
     * @return The AddressBookRecord object with the given ID. 
     */
    public AddressBookRecord searchForSingleRecord(long id)
    {
    	ArrayList<AddressBookRecord> retrievedRecords = searchAddressBookRecords(AddressBookRecordsTable.COLUMN_ID, String.valueOf(id));
    	
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
     * Returns an ArrayList containing all the AddressBookRecords stored in the 
     * application's database
     * 
     * @return An ArrayList containing one AddressBookRecord object for
     * each record in the AddressBookRecords table.
     */
    public ArrayList<AddressBookRecord> getAllAddressBookRecords()
    {
    	ArrayList<AddressBookRecord> addressBookRecords = new ArrayList<AddressBookRecord>();
    	
        // Specify which colums from the table we are interested in
		String[] projection = {
				AddressBookRecordsTable.COLUMN_ID, 
				AddressBookRecordsTable.COLUMN_COLOUR_R,
				AddressBookRecordsTable.COLUMN_COLOUR_G,
				AddressBookRecordsTable.COLUMN_COLOUR_B,
				AddressBookRecordsTable.COLUMN_LABEL,
				AddressBookRecordsTable.COLUMN_ADDRESS};
		
		// Query the database via the ContentProvider
		Cursor cursor = mContentResolver.query(
				DatabaseContentProvider.CONTENT_URI_ADDRESS_BOOK_RECORDS, 
				projection, 
				null, 
				null, 
				null);
    	
    	if (cursor.moveToFirst())
    	{
    	   do 
    	   {
	   	        long id = cursor.getLong(0);
	   	        int colourR = cursor.getInt(1);
	   	        int colourG = cursor.getInt(2);
	   	        int colourB = cursor.getInt(3);
	   	        String label = cursor.getString(4);
	   	        String address = cursor.getString(5);
	   	      
	   	        AddressBookRecord a = new AddressBookRecord();
	   	        a.setId(id);
	   	        a.setColourR(colourR);
	   	        a.setColourG(colourG);
	   	        a.setColourB(colourB);
	   	        a.setLabel(label);
	   	        a.setAddress(address);
    	      
    	      addressBookRecords.add(a);
    	   } 
    	   while (cursor.moveToNext());
    	}
    	
		cursor.close();
    	return addressBookRecords;
    }
    
    /**
     * Updates the database record for a given AddressBookRecord object<br><br>
     * 
     * <b>NOTE:</b> This method uses the given AddressBookRecord's ID field to determine
     * which record in the database to update
     * 
     * @param a - The AddressBookRecord object to be updated
     */
    public void updateAddressBookRecord(AddressBookRecord a)
    {
    	ContentValues values = new ContentValues();
    	values.put(AddressBookRecordsTable.COLUMN_COLOUR_R, a.getColourR());
    	values.put(AddressBookRecordsTable.COLUMN_COLOUR_G, a.getColourG());
    	values.put(AddressBookRecordsTable.COLUMN_COLOUR_B, a.getColourB());
    	values.put(AddressBookRecordsTable.COLUMN_LABEL, a.getLabel());
    	values.put(AddressBookRecordsTable.COLUMN_ADDRESS, a.getAddress());
		
		long id = a.getId();
    	
		// Query the database via the ContentProvider and update the record with the matching ID
    	mContentResolver.update(DatabaseContentProvider.CONTENT_URI_ADDRESS_BOOK_RECORDS,
    			values, 
    			AddressBookRecordsTable.COLUMN_ID + " = ? ", 
				new String[]{String.valueOf(id)});
    	
    	Log.i(TAG, "AddressBookRecord ID " + id + " updated");
    }
    
    /**
     * Deletes an AddressBookRecord object from the application's SQLite database<br><br>
     * 
     * <b>NOTE:</b> This method uses the given AddressBookRecord's ID field to determine
     * which record in the database to delete
     * 
     * @param a - The AddressBookRecord object to be deleted
     */
    public void deleteAddressBookRecord(AddressBookRecord a) 
    {
		long id = a.getId();
		
		// Query the database via the ContentProvider and delete the record with the matching ID
		int recordsDeleted = mContentResolver.delete(
				DatabaseContentProvider.CONTENT_URI_ADDRESS_BOOK_RECORDS, 
				AddressBookRecordsTable.COLUMN_ID + " = ? ", 
				new String[]{String.valueOf(id)});
    	
    	Log.i(TAG, recordsDeleted + " AddressBookRecord(s) deleted from database");
    }
    
    /**
     * Deletes all AddressBookRecords from the database
     */
    public void deleteAllAddressBookRecords()
    {
		// Query the database via the ContentProvider and delete all the records
		int recordsDeleted = mContentResolver.delete(
				DatabaseContentProvider.CONTENT_URI_ADDRESS_BOOK_RECORDS, 
				null, 
				null);
    	
    	Log.i(TAG, recordsDeleted + " AddressBookRecord(s) deleted from database");
    }
}