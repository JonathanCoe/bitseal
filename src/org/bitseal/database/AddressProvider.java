package org.bitseal.database;

import java.util.ArrayList;

import org.bitseal.data.Address;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

/**
 * A singleton class which controls the creation, reading, updating, and
 * deletion of stored Address objects. 
 * 
 * @author Jonathan Coe
 */

public class AddressProvider
{
    private static final String TAG = "ADDRESS_PROVIDER"; 

    private static AddressProvider sAddressProvider;
    
    private Context mAppContext;
    private static ContentResolver mContentResolver;

    private AddressProvider(Context appContext)
    {
        mAppContext = appContext;
        mContentResolver = mAppContext.getContentResolver();
    }
    
    /**
     * Returns an instance of this singleton class. 
     * 
     * @param c - A Context object for the currently running application
     */
    public static AddressProvider get(Context c)
    {
        if (sAddressProvider == null) 
        {
        	Context appContext = c.getApplicationContext();
        	sAddressProvider = new AddressProvider(appContext);
        }
        
        return sAddressProvider;
    }
    
    /**
     * Takes an Address object and adds it to the app's 
     * SQLite database as a new record, returning the ID of the 
     * newly created record. 
     * 
     * @param a - The Address object to be added
     * 
     * @return id - A long value representing the ID of the newly
     * created record
     */
    public long addAddress(Address a)
    {
    	ContentValues values = new ContentValues();
    	values.put(AddressesTable.COLUMN_CORRESPONDING_PUBKEY_ID, a.getCorrespondingPubkeyId());
    	values.put(AddressesTable.COLUMN_LABEL, a.getLabel());
    	values.put(AddressesTable.COLUMN_ADDRESS, a.getAddress());
    	values.put(AddressesTable.COLUMN_PRIVATE_SIGNING_KEY, a.getPrivateSigningKey());
    	values.put(AddressesTable.COLUMN_PRIVATE_ENCRYPTION_KEY, a.getPrivateEncryptionKey());
    	values.put(AddressesTable.COLUMN_RIPE_HASH, Base64.encodeToString(a.getRipeHash(), Base64.DEFAULT));
    	values.put(AddressesTable.COLUMN_TAG, Base64.encodeToString(a.getTag(), Base64.DEFAULT));  	
			
		Uri insertionUri = mContentResolver.insert(DatabaseContentProvider.CONTENT_URI_ADDRESSES, values);
    	Log.i(TAG, "Address with address " + a.getAddress() + " saved to database");
    	
		// Parse the ID of the newly created record from the insertion Uri
		String uriString = insertionUri.toString();
		String idString = uriString.substring(uriString.indexOf("/") + 1);
		long id = Long.parseLong(idString);
		return id;
    }
    
    /**
     * Finds all Addresses in the application's database that match the given field
     * 
     * @param columnName - A String specifying the name of the column in the database that 
     * should be used to find matching records. See the AddressesTable class to find
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
     * @return An ArrayList containing Address objects populated with the data from
     *  the database search
     */
    public ArrayList<Address> searchAddresses(String columnName, String searchString)
    {
    	ArrayList<Address> matchingRecords = new ArrayList<Address>();

    	// Specify which colums from the table we are interested in
		String[] projection = {
				AddressesTable.COLUMN_ID, 
				AddressesTable.COLUMN_CORRESPONDING_PUBKEY_ID,
				AddressesTable.COLUMN_LABEL,
				AddressesTable.COLUMN_ADDRESS,
				AddressesTable.COLUMN_PRIVATE_SIGNING_KEY,
				AddressesTable.COLUMN_PRIVATE_ENCRYPTION_KEY,
				AddressesTable.COLUMN_RIPE_HASH,
				AddressesTable.COLUMN_TAG};
		
		// Query the database via the ContentProvider
		Cursor cursor = mContentResolver.query(
				DatabaseContentProvider.CONTENT_URI_ADDRESSES, 
				projection, 
				AddressesTable.TABLE_ADDRESSES + "." + columnName + " = ? ", 
				new String[]{searchString}, 
				null);
			
		if (cursor.moveToFirst())
    	{
    	    do 
    	    {
    	        long id = cursor.getLong(0);
    	        long correspondingPubkeyId = cursor.getLong(1);
    	        String label = cursor.getString(2);
    	        String address = cursor.getString(3);
    	        String privateSigningKey = cursor.getString(4);
    	        String privateEncryptionKey = cursor.getString(5);
    	        byte[] ripeHash = Base64.decode(cursor.getString(6), Base64.DEFAULT);
    	        byte[] tag = Base64.decode(cursor.getString(7), Base64.DEFAULT);
    	      
    	        Address a = new Address();
    	        a.setId(id);
    	        a.setCorrespondingPubkeyId(correspondingPubkeyId);
    	        a.setLabel(label);
    	        a.setAddress(address);
    	        a.setPrivateSigningKey(privateSigningKey);
    	        a.setPrivateEncryptionKey(privateEncryptionKey);
    	        a.setRipeHash(ripeHash);
    	        a.setTag(tag);
    	      
    	        matchingRecords.add(a);
    	    } 
    	    while (cursor.moveToNext());
    	}
			
		else
		{
			Log.i(TAG, "Unable to find any Addresses with the value " + searchString + " in the " + columnName + " column");
			return matchingRecords;
		}
		
		cursor.close();
	
    	return matchingRecords;
     }
    
    /**
     * Searches the database for the Address with the given ID.
     * This method will return exactly one Address object or throw
     * a RuntimeException.
     * 
     * @param id - A long value representing the Address's ID.
     * 
     * @return The Address object with the given ID. 
     */
    public Address searchForSingleRecord(long id)
    {
    	ArrayList<Address> retrievedRecords = searchAddresses(AddressesTable.COLUMN_ID, String.valueOf(id));
    	
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
     * Returns an ArrayList containing all the Addresses stored in the 
     * application's database
     * 
     * @return An ArrayList containing one Address object for
     * each record in the Addresses table.
     */
    public ArrayList<Address> getAllAddresses()
    {
    	ArrayList<Address> addresses = new ArrayList<Address>();
    	
        // Specify which colums from the table we are interested in
		String[] projection = {
				AddressesTable.COLUMN_ID, 
				AddressesTable.COLUMN_CORRESPONDING_PUBKEY_ID,
				AddressesTable.COLUMN_LABEL,
				AddressesTable.COLUMN_ADDRESS,
				AddressesTable.COLUMN_PRIVATE_SIGNING_KEY,
				AddressesTable.COLUMN_PRIVATE_ENCRYPTION_KEY,
				AddressesTable.COLUMN_RIPE_HASH,
				AddressesTable.COLUMN_TAG};
		
		// Query the database via the ContentProvider
		Cursor cursor = mContentResolver.query(
				DatabaseContentProvider.CONTENT_URI_ADDRESSES, 
				projection, 
				null, 
				null, 
				null);
    	
    	if (cursor.moveToFirst())
    	{
    	   do 
    	   {
	   	        long id = cursor.getLong(0);
	   	        long correspondingPubkeyId = cursor.getLong(1);
	   	        String label = cursor.getString(2);
	   	        String address = cursor.getString(3);
	   	        String privateSigningKey = cursor.getString(4);
	   	        String privateEncryptionKey = cursor.getString(5);
	   	        byte[] ripeHash = Base64.decode(cursor.getString(6), Base64.DEFAULT);
	   	        byte[] tag = Base64.decode(cursor.getString(7), Base64.DEFAULT);
	   	      
	   	        Address a = new Address();
	   	        a.setId(id);
	   	        a.setCorrespondingPubkeyId(correspondingPubkeyId);
	   	        a.setLabel(label);
	   	        a.setAddress(address);
	   	        a.setPrivateSigningKey(privateSigningKey);
	   	        a.setPrivateEncryptionKey(privateEncryptionKey);
	   	        a.setRipeHash(ripeHash);
	   	        a.setTag(tag);
    	      
    	        addresses.add(a);
    	   } 
    	   while (cursor.moveToNext());
    	}
    	
    	return addresses;
    }
    
    /**
     * Updates the database record for a given Address object<br><br>
     * 
     * <b>NOTE:</b> This method uses the given Address's ID field to determine
     * which record in the database to update
     * 
     * @param a - The Address object to be updated
     */
    public void updateAddress(Address a)
    {
    	ContentValues values = new ContentValues();
    	values.put(AddressesTable.COLUMN_CORRESPONDING_PUBKEY_ID, a.getCorrespondingPubkeyId());
    	values.put(AddressesTable.COLUMN_LABEL, a.getLabel());
    	values.put(AddressesTable.COLUMN_ADDRESS, a.getAddress());
    	values.put(AddressesTable.COLUMN_PRIVATE_SIGNING_KEY, a.getPrivateSigningKey());
    	values.put(AddressesTable.COLUMN_PRIVATE_ENCRYPTION_KEY, a.getPrivateEncryptionKey());
    	values.put(AddressesTable.COLUMN_RIPE_HASH, Base64.encodeToString(a.getRipeHash(), Base64.DEFAULT));
    	values.put(AddressesTable.COLUMN_TAG, Base64.encodeToString(a.getTag(), Base64.DEFAULT));
		
		long id = a.getId();
    	
		// Query the database via the ContentProvider and update the record with the matching ID
    	mContentResolver.update(DatabaseContentProvider.CONTENT_URI_ADDRESSES,
    			values, 
    			AddressesTable.COLUMN_ID + " = ? ", 
				new String[]{String.valueOf(id)});
    	
    	Log.i(TAG, "Address ID " + id + " updated");
    }
    
    /**
     * Deletes an Address object from the application's SQLite database<br><br>
     * 
     * <b>NOTE:</b> This method uses the given Address's ID field to determine
     * which record in the database to delete
     * 
     * @param a - The Address object to be deleted
     */
    public void deleteAddress(Address a) 
    {
		long id = a.getId();
		
		// Query the database via the ContentProvider and delete the record with the matching ID
		int recordsDeleted = mContentResolver.delete(
				DatabaseContentProvider.CONTENT_URI_ADDRESSES, 
				AddressesTable.COLUMN_ID + " = ? ", 
				new String[]{String.valueOf(id)});
    	if (recordsDeleted > 0)
    	{
    		Log.i(TAG, "Address " + a.getAddress() + " deleted from database");
    	}
    	else
    	{
    		Log.e(TAG, "Unable to find the address specified for deletion. The address specified was " + a.getAddress());
    	}
    }
    
    /**
     * Deletes all Addresses from the database
     */
    public void deleteAllAddresses()
    {
		// Query the database via the ContentProvider and delete all the records
		int recordsDeleted = mContentResolver.delete(
				DatabaseContentProvider.CONTENT_URI_ADDRESSES, 
				null, 
				null);
    	
    	Log.i(TAG, recordsDeleted + " Address(es) deleted from database");
    }
}