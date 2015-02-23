package org.bitseal.database;

import java.util.ArrayList;

import org.bitseal.data.Pubkey;
import org.bitseal.util.ByteFormatter;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

/**
 * A singleton class which controls the creation, reading, updating, and
 * deletion of stored Pubkey objects. 
 * 
 * @author Jonathan Coe
 */

public class PubkeyProvider
{
    private static final String TAG = "PUBKEY_PROVIDER"; 

    private static PubkeyProvider sPubkeyProvider;
    
    private Context mAppContext;
    private static ContentResolver mContentResolver;

    private PubkeyProvider(Context appContext)
    {
        mAppContext = appContext;
        mContentResolver = mAppContext.getContentResolver();
    }
    
    /**
     * Returns an instance of this singleton class. 
     * 
     * @param c - A Context object for the currently running application
     */
    public static PubkeyProvider get(Context c)
    {
        if (sPubkeyProvider == null) 
        {
        	Context appContext = c.getApplicationContext();
        	sPubkeyProvider = new PubkeyProvider(appContext);
        }
        
        return sPubkeyProvider;
    }
    
    /**
     * Takes a Pubkey object and adds it to the app's 
     * SQLite database as a new record, returning the ID of the 
     * newly created record. 
     * 
     * @param p - The Pubkey object to be added
     * 
     * @return id - A long value representing the ID of the newly
     * created record
     */
    public long addPubkey(Pubkey p)
    {
    	int belongsToMe = 0;
    	if (p.belongsToMe())
    	{
    		belongsToMe = 1;
    	}
    	
    	ContentValues values = new ContentValues();
    	values.put(PubkeysTable.COLUMN_BELONGS_TO_ME, belongsToMe);
    	values.put(PubkeysTable.COLUMN_POW_NONCE, p.getPOWNonce());
    	values.put(PubkeysTable.COLUMN_EXPIRATION_TIME, p.getExpirationTime());
    	values.put(PubkeysTable.COLUMN_OBJECT_TYPE, p.getObjectType());
    	values.put(PubkeysTable.COLUMN_OBJECT_VERSION, p.getObjectVersion());
    	values.put(PubkeysTable.COLUMN_STREAM_NUMBER, p.getStreamNumber());
    	values.put(PubkeysTable.COLUMN_CORRESPONDING_ADDRESS_ID, p.getCorrespondingAddressId());
    	values.put(PubkeysTable.COLUMN_RIPE_HASH, Base64.encodeToString(p.getRipeHash(), Base64.DEFAULT));
    	values.put(PubkeysTable.COLUMN_BEHAVIOUR_BITFIELD, p.getBehaviourBitfield());
    	values.put(PubkeysTable.COLUMN_PUBLIC_SIGNING_KEY, Base64.encodeToString(p.getPublicSigningKey(), Base64.DEFAULT));
    	values.put(PubkeysTable.COLUMN_PUBLIC_ENCRYPTION_KEY, Base64.encodeToString(p.getPublicEncryptionKey(), Base64.DEFAULT));
    	values.put(PubkeysTable.COLUMN_NONCE_TRIALS_PER_BYTE, p.getNonceTrialsPerByte());
    	values.put(PubkeysTable.COLUMN_EXTRA_BYTES, p.getExtraBytes());
    	values.put(PubkeysTable.COLUMN_SIGNATURE_LENGTH, p.getSignatureLength());
    	values.put(PubkeysTable.COLUMN_SIGNATURE, Base64.encodeToString(p.getSignature(), Base64.DEFAULT));
			
		Uri insertionUri = mContentResolver.insert(DatabaseContentProvider.CONTENT_URI_PUBKEYS, values);
    	
		// Parse the ID of the newly created record from the insertion URI
		String uriString = insertionUri.toString();
		String idString = uriString.substring(uriString.indexOf("/") + 1);
		long id = Long.parseLong(idString);
		
		Log.i(TAG, "Pubkey with ripe hash " + ByteFormatter.byteArrayToHexString(p.getRipeHash()) + " and ID " + id + " saved to database");
		
		return id;
    }
    
    /**
     * Finds all Pubkeys in the application's database that match the given field
     * 
     * @param columnName - A String specifying the name of the column in the database that 
     * should be used to find matching records. See the PubkeysTable class to find
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
     * @return An ArrayList containing Pubkey objects populated with the data from
     *  the database search
     */
    public ArrayList<Pubkey> searchPubkeys(String columnName, String searchString)
    {
    	ArrayList<Pubkey> matchingRecords = new ArrayList<Pubkey>();

    	// Specify which columns from the table we are interested in
		String[] projection = {
				PubkeysTable.COLUMN_ID,
				PubkeysTable.COLUMN_BELONGS_TO_ME,
				PubkeysTable.COLUMN_POW_NONCE,
				PubkeysTable.COLUMN_EXPIRATION_TIME,
				PubkeysTable.COLUMN_OBJECT_TYPE,
				PubkeysTable.COLUMN_OBJECT_VERSION,
				PubkeysTable.COLUMN_STREAM_NUMBER,
				PubkeysTable.COLUMN_CORRESPONDING_ADDRESS_ID,
				PubkeysTable.COLUMN_RIPE_HASH,
				PubkeysTable.COLUMN_BEHAVIOUR_BITFIELD,
				PubkeysTable.COLUMN_PUBLIC_SIGNING_KEY,
				PubkeysTable.COLUMN_PUBLIC_ENCRYPTION_KEY,
				PubkeysTable.COLUMN_NONCE_TRIALS_PER_BYTE,
				PubkeysTable.COLUMN_EXTRA_BYTES,
				PubkeysTable.COLUMN_SIGNATURE_LENGTH,
				PubkeysTable.COLUMN_SIGNATURE};
		
		// Query the database via the ContentProvider
		Cursor cursor = mContentResolver.query(
				DatabaseContentProvider.CONTENT_URI_PUBKEYS, 
				projection, 
				PubkeysTable.TABLE_PUBKEYS + "." + columnName + " = ? ", 
				new String[]{searchString}, 
				null);
			
		if (cursor.moveToFirst())
    	{
    	    do 
    	    {
    	        long id = cursor.getLong(0);
    	        
    	        int belongsToMeValue = cursor.getInt(1);
    	        boolean belongsToMe = false;
    	        if (belongsToMeValue == 1)
    	        {
    	        	belongsToMe = true;
    	        }
    	        
    	        long powNonce = cursor.getLong(2);
    	        long time = cursor.getLong(3);
    	        int objectType = cursor.getInt(4);
    	        int objectVersion = cursor.getInt(5);
    	        int streamNumber = cursor.getInt(6);
    	        long correspondingAddressId = cursor.getLong(7);
    	        byte[] ripeHash = Base64.decode(cursor.getString(8), Base64.DEFAULT);
    	        int behaviourBitfield = cursor.getInt(9);
    	        byte[] publicSigningKey = Base64.decode(cursor.getString(10), Base64.DEFAULT);
    	        byte[] publicEncryptionKey = Base64.decode(cursor.getString(11), Base64.DEFAULT);
    	        int nonceTrialsPerByte = cursor.getInt(12);
    	        int extraBytes = cursor.getInt(13);
    	        int signatureLength = cursor.getInt(14);
    	        byte[] signature = Base64.decode(cursor.getString(15), Base64.DEFAULT);
    	      
    	        Pubkey p = new Pubkey();
    	        p.setId(id);
    	        p.setBelongsToMe(belongsToMe);
    	        p.setPOWNonce(powNonce);
    	        p.setExpirationTime(time);
    	        p.setObjectType(objectType);
    	        p.setObjectVersion(objectVersion);
    	        p.setStreamNumber(streamNumber);
    	        p.setCorrespondingAddressId(correspondingAddressId);
    	        p.setRipeHash(ripeHash);
    	        p.setBehaviourBitfield(behaviourBitfield);
    	        p.setPublicSigningKey(publicSigningKey);
    	        p.setPublicEncryptionKey(publicEncryptionKey);
    	        p.setNonceTrialsPerByte(nonceTrialsPerByte);
    	        p.setExtraBytes(extraBytes);
    	        p.setSignatureLength(signatureLength);
    	        p.setSignature(signature);
    	      
    	        matchingRecords.add(p);
    	    } 
    	    while (cursor.moveToNext());
    	}
		else
		{
			Log.i(TAG, "Unable to find any Pubkeys with the value " + searchString + " in the " + columnName + " column");
			cursor.close();
			return matchingRecords;
		}
		
		cursor.close();
    	return matchingRecords;
     }
    
    /**
     * Searches the database for the Pubkey with the given ID.
     * This method will return exactly one Pubkey object or throw
     * a RuntimeException.
     * 
     * @param id - A long value representing the Pubkey's ID.
     * 
     * @return The Pubkey object with the given ID. 
     */
    public Pubkey searchForSingleRecord(long id)
    {
    	ArrayList<Pubkey> retrievedRecords = searchPubkeys(PubkeysTable.COLUMN_ID, String.valueOf(id));
    	
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
     * Returns an ArrayList containing all the Pubkeys stored in the 
     * application's database
     * 
     * @return An ArrayList containing one Pubkey object for
     * each record in the Pubkeys table.
     */
    public ArrayList<Pubkey> getAllPubkeys()
    {
    	ArrayList<Pubkey> pubkeys = new ArrayList<Pubkey>();
    	
        // Specify which columns from the table we are interested in
		String[] projection = {
				PubkeysTable.COLUMN_ID, 
				PubkeysTable.COLUMN_BELONGS_TO_ME,
				PubkeysTable.COLUMN_POW_NONCE,
				PubkeysTable.COLUMN_EXPIRATION_TIME,
				PubkeysTable.COLUMN_OBJECT_TYPE,
				PubkeysTable.COLUMN_OBJECT_VERSION,
				PubkeysTable.COLUMN_STREAM_NUMBER,
				PubkeysTable.COLUMN_CORRESPONDING_ADDRESS_ID,
				PubkeysTable.COLUMN_RIPE_HASH,
				PubkeysTable.COLUMN_BEHAVIOUR_BITFIELD,
				PubkeysTable.COLUMN_PUBLIC_SIGNING_KEY,
				PubkeysTable.COLUMN_PUBLIC_ENCRYPTION_KEY,
				PubkeysTable.COLUMN_NONCE_TRIALS_PER_BYTE,
				PubkeysTable.COLUMN_EXTRA_BYTES,
				PubkeysTable.COLUMN_SIGNATURE_LENGTH,
				PubkeysTable.COLUMN_SIGNATURE};
		
		// Query the database via the ContentProvider
		Cursor cursor = mContentResolver.query(
				DatabaseContentProvider.CONTENT_URI_PUBKEYS, 
				projection,
				null,
				null,
				null);
    	
    	if (cursor.moveToFirst())
    	{
    	    do 
    	    {
    	        long id = cursor.getLong(0);
    	        
    	        
    	        int belongsToMeValue = cursor.getInt(1);
    	        boolean belongsToMe = false;
    	        if (belongsToMeValue == 1)
    	        {
    	        	belongsToMe = true;
    	        }
    	        
    	        long powNonce = cursor.getLong(2);
    	        long time = cursor.getLong(3);
    	        int objectType = cursor.getInt(4);
    	        int objectVersion = cursor.getInt(5);
    	        int streamNumber = cursor.getInt(6);
    	        long correspondingAddressId = cursor.getLong(7);
    	        byte[] ripeHash = Base64.decode(cursor.getString(8), Base64.DEFAULT);
    	        int behaviourBitfield = cursor.getInt(9);
    	        byte[] publicSigningKey = Base64.decode(cursor.getString(10), Base64.DEFAULT);
    	        byte[] publicEncryptionKey = Base64.decode(cursor.getString(11), Base64.DEFAULT);
    	        int nonceTrialsPerByte = cursor.getInt(12);
    	        int extraBytes = cursor.getInt(13);
    	        int signatureLength = cursor.getInt(14);
    	        byte[] signature = Base64.decode(cursor.getString(15), Base64.DEFAULT);
    	      
    	        Pubkey p = new Pubkey();
    	        p.setId(id);
    	        p.setBelongsToMe(belongsToMe);
    	        p.setPOWNonce(powNonce);
    	        p.setExpirationTime(time);
    	        p.setObjectType(objectType);
    	        p.setObjectVersion(objectVersion);
    	        p.setStreamNumber(streamNumber);
    	        p.setCorrespondingAddressId(correspondingAddressId);
    	        p.setRipeHash(ripeHash);
    	        p.setBehaviourBitfield(behaviourBitfield);
    	        p.setPublicSigningKey(publicSigningKey);
    	        p.setPublicEncryptionKey(publicEncryptionKey);
    	        p.setNonceTrialsPerByte(nonceTrialsPerByte);
    	        p.setExtraBytes(extraBytes);
    	        p.setSignatureLength(signatureLength);
    	        p.setSignature(signature);
    	      
    	        pubkeys.add(p);
    	    } 
    	    while (cursor.moveToNext());
    	}
    	
		cursor.close();
    	return pubkeys;
    }
    
    /**
     * Updates the database record for a given Pubkey object<br><br>
     * 
     * <b>NOTE:</b> This method uses the given Pubkey's ID field to determine
     * which record in the database to update
     * 
     * @param p - The Pubkey object to be updated
     */
    public void updatePubkey(Pubkey p)
    {
    	int belongsToMe = 0;
    	if (p.belongsToMe())
    	{
    		belongsToMe = 1;
    	}
    	
    	ContentValues values = new ContentValues();
    	values.put(PubkeysTable.COLUMN_BELONGS_TO_ME, belongsToMe);
    	values.put(PubkeysTable.COLUMN_POW_NONCE, p.getPOWNonce());
    	values.put(PubkeysTable.COLUMN_EXPIRATION_TIME, p.getExpirationTime());
    	values.put(PubkeysTable.COLUMN_OBJECT_TYPE, p.getObjectType());
    	values.put(PubkeysTable.COLUMN_OBJECT_VERSION, p.getObjectVersion());
    	values.put(PubkeysTable.COLUMN_STREAM_NUMBER, p.getStreamNumber());
    	values.put(PubkeysTable.COLUMN_CORRESPONDING_ADDRESS_ID, p.getCorrespondingAddressId());
    	values.put(PubkeysTable.COLUMN_RIPE_HASH, Base64.encodeToString(p.getRipeHash(), Base64.DEFAULT));
    	values.put(PubkeysTable.COLUMN_BEHAVIOUR_BITFIELD, p.getBehaviourBitfield());
    	values.put(PubkeysTable.COLUMN_PUBLIC_SIGNING_KEY, Base64.encodeToString(p.getPublicSigningKey(), Base64.DEFAULT));
    	values.put(PubkeysTable.COLUMN_PUBLIC_ENCRYPTION_KEY, Base64.encodeToString(p.getPublicEncryptionKey(), Base64.DEFAULT));
    	values.put(PubkeysTable.COLUMN_NONCE_TRIALS_PER_BYTE, p.getNonceTrialsPerByte());
    	values.put(PubkeysTable.COLUMN_EXTRA_BYTES, p.getExtraBytes());
    	values.put(PubkeysTable.COLUMN_SIGNATURE_LENGTH, p.getSignatureLength());
    	values.put(PubkeysTable.COLUMN_SIGNATURE, Base64.encodeToString(p.getSignature(), Base64.DEFAULT));
		
		long id = p.getId();
    	
		// Query the database via the ContentProvider and update the record with the matching ID
    	mContentResolver.update(DatabaseContentProvider.CONTENT_URI_PUBKEYS,
    			values, 
    			PubkeysTable.COLUMN_ID + " = ? ", 
				new String[]{String.valueOf(id)});
    	
    	Log.i(TAG, "Pubkey ID " + id + " updated");
    }
    
    /**
     * Deletes a Pubkey object from the application's SQLite database<br><br>
     * 
     * <b>NOTE:</b> This method uses the given Pubkey's ID field to determine
     * which record in the database to delete
     * 
     * @param p - The Pubkey object to be deleted
     */
    public void deletePubkey(Pubkey p) 
    {
		long id = p.getId();
		
		// Query the database via the ContentProvider and delete the record with the matching ID
		int recordsDeleted = mContentResolver.delete(
				DatabaseContentProvider.CONTENT_URI_PUBKEYS, 
				PubkeysTable.COLUMN_ID + " = ? ", 
				new String[]{String.valueOf(id)});
    	
    	Log.i(TAG, recordsDeleted + " Pubkey(s) deleted from database");
    }
    
    /**
     * Deletes all Pubkeys from the database
     */
    public void deleteAllPubkeys()
    {
		// Query the database via the ContentProvider and delete all the records
		int recordsDeleted = mContentResolver.delete(
				DatabaseContentProvider.CONTENT_URI_PUBKEYS, 
				null, 
				null);
    	
    	Log.i(TAG, recordsDeleted + " Pubkey(s) deleted from database");
    }
}