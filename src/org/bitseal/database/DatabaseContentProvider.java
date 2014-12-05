package org.bitseal.database;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.cacheword.PassphraseSecrets;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashSet;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteQueryBuilder;
import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class DatabaseContentProvider extends ContentProvider implements ICacheWordSubscriber
{
	private static DatabaseHelper sDatabaseHelper;
	private static Context sContext;
	private static CacheWordHandler sCacheWordHandler;
	private static SQLiteDatabase sDatabase;
	
    /** The key for a boolean variable that records whether or not a user-defined database encryption passphrase has been saved */
    private static final String KEY_DATABASE_PASSPHRASE_SAVED = "databasePassphraseSaved";
    
    /** The default passphrase for the database. This is NOT intended to have any security value, but rather to make the
     * code simpler by always having the database encrypted and therefore not forcing us to switch between encrypted and unencrypted
     * databases. */
    public static final String DEFAULT_DATABASE_PASSPHRASE = "myDefaultDatabasePassphrase";
    
	// Used by the URI Matcher
	private static final int ADDRESSES = 10;
	private static final int ADDRESS_ID = 20;
	private static final int ADDRESS_BOOK_RECORDS = 30;
	private static final int ADDRESS_BOOK_RECORD_ID = 40;
	private static final int MESSAGES = 50;
	private static final int MESSAGE_ID = 60;
	private static final int QUEUE_RECORDS = 70;
	private static final int QUEUE_RECORD_ID = 80;
    private static final int PAYLOADS = 90;
    private static final int PAYLOAD_ID = 100; 
    private static final int PUBKEYS = 110;
    private static final int PUBKEY_ID = 120;
    private static final int SERVER_RECORDS = 130;
    private static final int SERVER_RECORD_ID = 140;
	  
    private static final String AUTHORITY = "org.bitseal.database";
	  
    // The path strings for each table in the database
    private static final String PATH_ADDRESSES = "addresses";
    private static final String PATH_ADDRESS_BOOK_RECORDS = "address_book_records";
    private static final String PATH_MESSAGES = "messages";
    private static final String PATH_QUEUE_RECORDS = "queue_records";
    private static final String PATH_PAYLOADS = "payloads";
    private static final String PATH_PUBKEYS = "pubkeys";
    private static final String PATH_SERVER_RECORDS = "server_records";
	  
    // The URIs for each table in the database
    public static final Uri CONTENT_URI_ADDRESSES = Uri.parse("content://" + AUTHORITY + "/" + PATH_ADDRESSES);
    public static final Uri CONTENT_URI_ADDRESS_BOOK_RECORDS = Uri.parse("content://" + AUTHORITY + "/" + PATH_ADDRESS_BOOK_RECORDS);
    public static final Uri CONTENT_URI_MESSAGES = Uri.parse("content://" + AUTHORITY + "/" + PATH_MESSAGES);
    public static final Uri CONTENT_URI_QUEUE_RECORDS = Uri.parse("content://" + AUTHORITY + "/" + PATH_QUEUE_RECORDS);
    public static final Uri CONTENT_URI_PAYLOADS = Uri.parse("content://" + AUTHORITY + "/" + PATH_PAYLOADS);
    public static final Uri CONTENT_URI_PUBKEYS = Uri.parse("content://" + AUTHORITY + "/" + PATH_PUBKEYS);
    public static final Uri CONTENT_URI_SERVER_RECORDS = Uri.parse("content://" + AUTHORITY + "/" + PATH_SERVER_RECORDS);
	  
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    
    private static final String TAG = "DATABASE_CONTENT_PROVIDER";
            	  
    static 
    {
    	sURIMatcher.addURI(AUTHORITY, PATH_ADDRESSES, ADDRESSES);
    	sURIMatcher.addURI(AUTHORITY, PATH_ADDRESSES + "/#", ADDRESS_ID);
    	
    	sURIMatcher.addURI(AUTHORITY, PATH_ADDRESS_BOOK_RECORDS, ADDRESS_BOOK_RECORDS);
    	sURIMatcher.addURI(AUTHORITY, PATH_ADDRESS_BOOK_RECORDS + "/#", ADDRESS_BOOK_RECORD_ID);
    	
    	sURIMatcher.addURI(AUTHORITY, PATH_MESSAGES, MESSAGES);
    	sURIMatcher.addURI(AUTHORITY, PATH_MESSAGES + "/#", MESSAGE_ID);
    	
    	sURIMatcher.addURI(AUTHORITY, PATH_QUEUE_RECORDS, QUEUE_RECORDS);
    	sURIMatcher.addURI(AUTHORITY, PATH_QUEUE_RECORDS + "/#", QUEUE_RECORD_ID);
    	
    	sURIMatcher.addURI(AUTHORITY, PATH_PAYLOADS, PAYLOADS);
    	sURIMatcher.addURI(AUTHORITY, PATH_PAYLOADS + "/#", PAYLOAD_ID);
    	
    	sURIMatcher.addURI(AUTHORITY, PATH_PUBKEYS, PUBKEYS);
    	sURIMatcher.addURI(AUTHORITY, PATH_PUBKEYS + "/#", PUBKEY_ID);
    	
    	sURIMatcher.addURI(AUTHORITY, PATH_SERVER_RECORDS, SERVER_RECORDS);
    	sURIMatcher.addURI(AUTHORITY, PATH_SERVER_RECORDS + "/#", SERVER_RECORD_ID);
    }

    @SuppressLint("InlinedApi")
	@Override
    public boolean onCreate() 
    {    	
    	Log.i(TAG, "Database content provider onCreate() called");
    	
    	sContext = getContext();
    	
    	sCacheWordHandler = new CacheWordHandler(sContext, this);
    	sCacheWordHandler.connectToService();
    	
    	sDatabaseHelper = new DatabaseHelper(sContext, sCacheWordHandler);
		
		return false;
    }
    
    /**
     * Gets a writable SQLiteDatabase object
     * 
     * @return The SQLiteDatabase object
     */
    public static SQLiteDatabase openDatabase()
    {
    	Log.i(TAG, "DatabaseContentProvider.openDatabase() called");
    	
    	try
    	{
    		SQLiteDatabase.loadLibs(sContext);
    		sDatabase = sDatabaseHelper.getWritableDatabase();
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception occurred while running DatabaseContentProvider.openDatabase(). The exception message was:\n" 
    				+ e.getMessage());
    	}
    	
		return sDatabase;
    }
    
    /**
     * Changes the database passphrase
     * 
     * @param newPassphrase - The new passphrase
     * 
     * @return A boolean indicating whether or not the database passphrase was
     * changed successfully
     */
    public static boolean changeDatabasePassphrase(String newPassphrase)
    {
    	Log.i(TAG, "DatabaseContentProvider.changeDatabasePassphrase() called");
    	
    	try
    	{
	    	// Get the old encryption key
    		String oldEncryptionKey = DatabaseHelper.encodeRawKeyToStr(sCacheWordHandler.getEncryptionKey());
    		
    		// Set CacheWord to use the new passphrase
			sCacheWordHandler.changePassphrase((PassphraseSecrets) sCacheWordHandler.getCachedSecrets(), newPassphrase.toCharArray());
			
			// Get the new encryption key
			String newEncryptionKey = DatabaseHelper.encodeRawKeyToStr(sCacheWordHandler.getEncryptionKey());
	    	
	    	sDatabase.execSQL("PRAGMA key = \"" + oldEncryptionKey + "\";");
	    	sDatabase.execSQL("PRAGMA rekey = \"" + newEncryptionKey + "\";");
	    	
	    	openDatabase();
	    	return true;
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception occurred while running DatabaseContentProvider.changeDatabasePassphrase(). The exception message was:\n" + 
    				e.getMessage());
    		return false;
    	}
    }
    
	/**
	 * Closes the SQLiteDatabase object that we use to interact with the
	 * app's database. This method is intended to be used when the user locks
	 * the app. 
	 */
	public static void closeDatabase()
	{
		Log.i(TAG, "DatabaseContentProvider.closeDatabase() called.");
		if (sDatabase != null)
		{
			Log.d(TAG, "About to close database");
			sDatabase.close();
			sDatabase = null;
			System.gc();
		}
	}
    
    @Override
    public String getType(Uri uri) 
    {
    	return null; // This method will not be called unless the application changes to specifically invoke it. Thus it can be safely left to return null.
    }
    
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) 
    {
    	// Using SQLiteQueryBuilder instead of query() method
	    SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
	    
	    int uriType = sURIMatcher.match(uri);

	    // Check if the caller has requested a column which does not exists
	    checkColumns(projection, uriType);
    
	    switch (uriType)
	    {
	        case ADDRESS_ID:
	            // Adding the ID to the original query
	            queryBuilder.appendWhere(AddressesTable.COLUMN_ID + "=" + uri.getLastPathSegment());
	        case ADDRESSES:
	            queryBuilder.setTables(AddressesTable.TABLE_ADDRESSES);
	            break;

	        case ADDRESS_BOOK_RECORD_ID:
	            // Adding the ID to the original query
	            queryBuilder.appendWhere(AddressBookRecordsTable.COLUMN_ID + "=" + uri.getLastPathSegment());
	        case ADDRESS_BOOK_RECORDS:
	            queryBuilder.setTables(AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS);
	            break;
	            
	        case MESSAGE_ID:
	            // Adding the ID to the original query
	            queryBuilder.appendWhere(MessagesTable.COLUMN_ID + "=" + uri.getLastPathSegment());
	        case MESSAGES:
	            queryBuilder.setTables(MessagesTable.TABLE_MESSAGES);
	            break;
	            
	        case QUEUE_RECORD_ID:
	            // Adding the ID to the original query
	            queryBuilder.appendWhere(QueueRecordsTable.COLUMN_ID + "=" + uri.getLastPathSegment());
	        case QUEUE_RECORDS:
	            queryBuilder.setTables(QueueRecordsTable.TABLE_QUEUE_RECORDS);
	            break;
	            
	        case PAYLOAD_ID:
	            // Adding the ID to the original query
	            queryBuilder.appendWhere(PayloadsTable.COLUMN_ID + "=" + uri.getLastPathSegment());
	        case PAYLOADS:
	            queryBuilder.setTables(PayloadsTable.TABLE_PAYLOADS);
	            break;
	            
	        case PUBKEY_ID:
	            // Adding the ID to the original query
	            queryBuilder.appendWhere(PubkeysTable.COLUMN_ID + "=" + uri.getLastPathSegment());
	        case PUBKEYS:
	            queryBuilder.setTables(PubkeysTable.TABLE_PUBKEYS);
	            break;
	            
	        case SERVER_RECORD_ID:
	            // Adding the ID to the original query
	            queryBuilder.appendWhere(ServerRecordsTable.COLUMN_ID + "=" + uri.getLastPathSegment());
	        case SERVER_RECORDS:
	            queryBuilder.setTables(ServerRecordsTable.TABLE_SERVER_RECORDS);
	            break;
	      
		    default:
		    	throw new IllegalArgumentException("Unknown URI: " + uri + " Exception occurred in DatabaseContentProvider.query()");
	    }
	    
	    Cursor cursor = queryBuilder.query(sDatabase, projection, selection, selectionArgs, null, null, sortOrder);
	    // make sure that potential listeners are getting notified
	    cursor.setNotificationUri(sContext.getContentResolver(), uri);
	    return cursor;
	  }

	  @Override
	  public Uri insert(Uri uri, ContentValues values)
	  {
		int uriType = sURIMatcher.match(uri);
	    long id = 0;
	    String path;
	    
	    switch (uriType) 
	    {
		    case ADDRESSES:
			      id = sDatabase.insert(AddressesTable.TABLE_ADDRESSES, null, values);
			      path = PATH_ADDRESSES;
			      break;
		      
		    case ADDRESS_BOOK_RECORDS:
			      id = sDatabase.insert(AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS, null, values);
			      path = PATH_ADDRESS_BOOK_RECORDS;
			      break;
			      
		    case MESSAGES:
			      id = sDatabase.insert(MessagesTable.TABLE_MESSAGES, null, values);
			      path = PATH_MESSAGES;
			      break;
			      
		    case QUEUE_RECORDS:
			      id = sDatabase.insert(QueueRecordsTable.TABLE_QUEUE_RECORDS, null, values);
			      path = PATH_QUEUE_RECORDS;
			      break;
			      
		    case PAYLOADS:
			      id = sDatabase.insert(PayloadsTable.TABLE_PAYLOADS, null, values);
			      path = PATH_PAYLOADS;
			      break;
			      
		    case PUBKEYS:
			      id = sDatabase.insert(PubkeysTable.TABLE_PUBKEYS, null, values);
			      path = PATH_PUBKEYS;
			      break;
			      
		    case SERVER_RECORDS:
			      id = sDatabase.insert(ServerRecordsTable.TABLE_SERVER_RECORDS, null, values);
			      path = PATH_SERVER_RECORDS;
			      break;
		      
		    default:
		    	  throw new IllegalArgumentException("Unknown URI: " + uri + " Exception occurred in DatabaseContentProvider.insert()");
	    }
	    
	    sContext.getContentResolver().notifyChange(uri, null);
	    return Uri.parse(path + "/" + id);
	  }

	  @Override
	  public int delete(Uri uri, String selection, String[] selectionArgs) 
	  {
		int uriType = sURIMatcher.match(uri);
	    int rowsDeleted = 0;
	    String id;
	    
	    switch (uriType)
	    {
		    case ADDRESSES:
			      rowsDeleted = sDatabase.delete(AddressesTable.TABLE_ADDRESSES, selection, selectionArgs);
			      break;      
		    case ADDRESS_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsDeleted = sDatabase.delete(AddressesTable.TABLE_ADDRESSES, AddressesTable.COLUMN_ID + "=" + id, null);
			      }
			      else 
			      {
			    	  rowsDeleted = sDatabase.delete(AddressesTable.TABLE_ADDRESSES, AddressesTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case ADDRESS_BOOK_RECORDS:
			      rowsDeleted = sDatabase.delete(AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS, selection, selectionArgs);
			      break;      
		    case ADDRESS_BOOK_RECORD_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsDeleted = sDatabase.delete(AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS, AddressBookRecordsTable.COLUMN_ID + "=" + id, null);
			      }
			      else 
			      {
			    	  rowsDeleted = sDatabase.delete(AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS, AddressBookRecordsTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case MESSAGES:
			      rowsDeleted = sDatabase.delete(MessagesTable.TABLE_MESSAGES, selection, selectionArgs);
			      break;      
		    case MESSAGE_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsDeleted = sDatabase.delete(MessagesTable.TABLE_MESSAGES, MessagesTable.COLUMN_ID + "=" + id, null);
			      }
			      else 
			      {
			    	  rowsDeleted = sDatabase.delete(MessagesTable.TABLE_MESSAGES, MessagesTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case QUEUE_RECORDS:
			      rowsDeleted = sDatabase.delete(QueueRecordsTable.TABLE_QUEUE_RECORDS, selection, selectionArgs);
			      break;      
		    case QUEUE_RECORD_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsDeleted = sDatabase.delete(QueueRecordsTable.TABLE_QUEUE_RECORDS, QueueRecordsTable.COLUMN_ID + "=" + id, null);
			      }
			      else 
			      {
			    	  rowsDeleted = sDatabase.delete(QueueRecordsTable.TABLE_QUEUE_RECORDS, QueueRecordsTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case PAYLOADS:
			      rowsDeleted = sDatabase.delete(PayloadsTable.TABLE_PAYLOADS, selection, selectionArgs);
			      break;      
		    case PAYLOAD_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsDeleted = sDatabase.delete(PayloadsTable.TABLE_PAYLOADS, PayloadsTable.COLUMN_ID + "=" + id, null);
			      }
			      else 
			      {
			    	  rowsDeleted = sDatabase.delete(PayloadsTable.TABLE_PAYLOADS, PayloadsTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case PUBKEYS:
			      rowsDeleted = sDatabase.delete(PubkeysTable.TABLE_PUBKEYS, selection, selectionArgs);
			      break;      
		    case PUBKEY_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsDeleted = sDatabase.delete(PubkeysTable.TABLE_PUBKEYS, PubkeysTable.COLUMN_ID + "=" + id, null);
			      }
			      else 
			      {
			    	  rowsDeleted = sDatabase.delete(PubkeysTable.TABLE_PUBKEYS, PubkeysTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case SERVER_RECORDS:
			      rowsDeleted = sDatabase.delete(ServerRecordsTable.TABLE_SERVER_RECORDS, selection, selectionArgs);
			      break;      
		    case SERVER_RECORD_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsDeleted = sDatabase.delete(ServerRecordsTable.TABLE_SERVER_RECORDS, ServerRecordsTable.COLUMN_ID + "=" + id, null);
			      }
			      else 
			      {
			    	  rowsDeleted = sDatabase.delete(ServerRecordsTable.TABLE_SERVER_RECORDS, ServerRecordsTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    default:
		    	  throw new IllegalArgumentException("Unknown URI: " + uri + " Exception occurred in DatabaseContentProvider.delete()");
	    }
	    sContext.getContentResolver().notifyChange(uri, null);
	    return rowsDeleted;
	  }

	  @Override
	  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) 
	  {
		int uriType = sURIMatcher.match(uri);
	    int rowsUpdated = 0;
	    String id;
	    
	    switch (uriType)
	    {
		    case ADDRESSES:
			      rowsUpdated = sDatabase.update(AddressesTable.TABLE_ADDRESSES, values, selection, selectionArgs);
			      break;
		    case ADDRESS_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsUpdated = sDatabase.update(AddressesTable.TABLE_ADDRESSES, values, AddressesTable.COLUMN_ID + "=" + id, null);
			      } 
			      else 
			      {
			    	  rowsUpdated = sDatabase.update(AddressesTable.TABLE_ADDRESSES, values, AddressesTable.COLUMN_ID + "=" + id  + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case ADDRESS_BOOK_RECORDS:
			      rowsUpdated = sDatabase.update(AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS, values, selection, selectionArgs);
			      break;
		    case ADDRESS_BOOK_RECORD_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsUpdated = sDatabase.update(AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS, values, AddressBookRecordsTable.COLUMN_ID + "=" + id, null);
			      } 
			      else 
			      {
			    	  rowsUpdated = sDatabase.update(AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS, values, AddressBookRecordsTable.COLUMN_ID + "=" + id  + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case MESSAGES:
			      rowsUpdated = sDatabase.update(MessagesTable.TABLE_MESSAGES, values, selection, selectionArgs);
			      break;
		    case MESSAGE_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsUpdated = sDatabase.update(MessagesTable.TABLE_MESSAGES, values, MessagesTable.COLUMN_ID + "=" + id, null);
			      } 
			      else 
			      {
			    	  rowsUpdated = sDatabase.update(MessagesTable.TABLE_MESSAGES, values, MessagesTable.COLUMN_ID + "=" + id  + " and " + selection, selectionArgs);
			      }
			      break;

		    case QUEUE_RECORDS:
			      rowsUpdated = sDatabase.update(QueueRecordsTable.TABLE_QUEUE_RECORDS, values, selection, selectionArgs);
			      break;
		    case QUEUE_RECORD_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsUpdated = sDatabase.update(QueueRecordsTable.TABLE_QUEUE_RECORDS, values, QueueRecordsTable.COLUMN_ID + "=" + id, null);
			      } 
			      else 
			      {
			    	  rowsUpdated = sDatabase.update(QueueRecordsTable.TABLE_QUEUE_RECORDS, values, QueueRecordsTable.COLUMN_ID + "=" + id  + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case PAYLOADS:
			      rowsUpdated = sDatabase.update(PayloadsTable.TABLE_PAYLOADS, values, selection, selectionArgs);
			      break;
		    case PAYLOAD_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsUpdated = sDatabase.update(PayloadsTable.TABLE_PAYLOADS, values, PayloadsTable.COLUMN_ID + "=" + id, null);
			      }
			      else 
			      {
			    	  rowsUpdated = sDatabase.update(PayloadsTable.TABLE_PAYLOADS, values, PayloadsTable.COLUMN_ID + "=" + id  + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case PUBKEYS:
			      rowsUpdated = sDatabase.update(PubkeysTable.TABLE_PUBKEYS, values, selection, selectionArgs);
			      break;
		    case PUBKEY_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsUpdated = sDatabase.update(PubkeysTable.TABLE_PUBKEYS, values, PubkeysTable.COLUMN_ID + "=" + id, null);
			      } 
			      else 
			      {
			    	  rowsUpdated = sDatabase.update(PubkeysTable.TABLE_PUBKEYS, values, PubkeysTable.COLUMN_ID + "=" + id  + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case SERVER_RECORDS:
			      rowsUpdated = sDatabase.update(ServerRecordsTable.TABLE_SERVER_RECORDS, values, selection, selectionArgs);
			      break;
		    case SERVER_RECORD_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsUpdated = sDatabase.update(ServerRecordsTable.TABLE_SERVER_RECORDS, values, ServerRecordsTable.COLUMN_ID + "=" + id, null);
			      } 
			      else 
			      {
			    	  rowsUpdated = sDatabase.update(ServerRecordsTable.TABLE_SERVER_RECORDS, values, ServerRecordsTable.COLUMN_ID + "=" + id  + " and " + selection, selectionArgs);
			      }
			      break;
		      
		    default:
		    	  throw new IllegalArgumentException("Unknown URI: " + uri + " Exception occurred in DatabaseContentProvider.update()");
	    }
	    sContext.getContentResolver().notifyChange(uri, null);
	    return rowsUpdated;
	  }
	  
	  private void checkColumns(String[] projection, int uriType) 
	  {
		    String[] available = getAvailable(uriType);

		    if (projection != null)
		    {
			    HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
			    HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
			    // check if all columns which are requested are available
			    if (!availableColumns.containsAll(requestedColumns)) 
			    {
				    throw new IllegalArgumentException("Unknown columns in projection. Exception occurred in DatabaseContentProvider.checkColumns()");
			    }
		   }
	  }
	  
	  private String[] getAvailable(int uriType)
	  {
		    if (uriType == ADDRESSES || uriType == ADDRESS_ID)
		    {
		    	String[] available = {AddressesTable.COLUMN_ID, AddressesTable.COLUMN_CORRESPONDING_PUBKEY_ID, AddressesTable.COLUMN_LABEL, 
		    			AddressesTable.COLUMN_ADDRESS, AddressesTable.COLUMN_PRIVATE_SIGNING_KEY, AddressesTable.COLUMN_PRIVATE_ENCRYPTION_KEY,
		    			AddressesTable.COLUMN_RIPE_HASH, AddressesTable.COLUMN_TAG};
		    	return available;
		    }
			     
		    else if (uriType == ADDRESS_BOOK_RECORDS || uriType == ADDRESS_BOOK_RECORD_ID)
	    	{
		    	String[] available = {AddressBookRecordsTable.COLUMN_ID, AddressBookRecordsTable.COLUMN_COLOUR_R, AddressBookRecordsTable.COLUMN_COLOUR_G, 
		    			AddressBookRecordsTable.COLUMN_COLOUR_B, AddressBookRecordsTable.COLUMN_LABEL, AddressBookRecordsTable.COLUMN_ADDRESS};
		    	return available;
	    	}
		    
		    else if (uriType == MESSAGES || uriType == MESSAGE_ID)
	    	{
		    	String[] available = {MessagesTable.COLUMN_ID, MessagesTable.COLUMN_MSG_PAYLOAD_ID, MessagesTable.COLUMN_ACK_PAYLOAD_ID, MessagesTable.COLUMN_BELONGS_TO_ME, 
		    			MessagesTable.COLUMN_READ, MessagesTable.COLUMN_STATUS, MessagesTable.COLUMN_TIME, MessagesTable.COLUMN_TO_ADDRESS, 
		    			MessagesTable.COLUMN_FROM_ADDRESS, MessagesTable.COLUMN_SUBJECT, MessagesTable.COLUMN_BODY};
		    	return available;
	    	}

		    else if (uriType == QUEUE_RECORDS || uriType == QUEUE_RECORD_ID)
	    	{
		    	String[] available = {QueueRecordsTable.COLUMN_ID, QueueRecordsTable.COLUMN_TASK, QueueRecordsTable.COLUMN_TRIGGER_TIME, 
		    			QueueRecordsTable.COLUMN_RECORD_COUNT, QueueRecordsTable.COLUMN_LAST_ATTEMPT_TIME, QueueRecordsTable.COLUMN_ATTEMPTS, 
		    			QueueRecordsTable.COLUMN_OBJECT_0_ID, QueueRecordsTable.COLUMN_OBJECT_0_TYPE, QueueRecordsTable.COLUMN_OBJECT_1_ID,
		    			QueueRecordsTable.COLUMN_OBJECT_1_TYPE, QueueRecordsTable.COLUMN_OBJECT_2_ID, QueueRecordsTable.COLUMN_OBJECT_2_TYPE};
		    	return available;
	    	}
		    
		    else if (uriType == PAYLOADS || uriType == PAYLOAD_ID)
	    	{
		    	String[] available = {PayloadsTable.COLUMN_ID, PayloadsTable.COLUMN_RELATED_ADDRESS_ID, PayloadsTable.COLUMN_BELONGS_TO_ME,
		    			PayloadsTable.COLUMN_PROCESSING_COMPLETE, PayloadsTable.COLUMN_TIME, PayloadsTable.COLUMN_TYPE, PayloadsTable.COLUMN_ACK, 
		    			PayloadsTable.COLUMN_POW_DONE, PayloadsTable.COLUMN_PAYLOAD};
		    	return available;
	    	}
		    
		    else if (uriType == PUBKEYS || uriType == PUBKEY_ID)
	    	{
		    	String[] available = {PubkeysTable.COLUMN_ID, PubkeysTable.COLUMN_BELONGS_TO_ME, PubkeysTable.COLUMN_POW_NONCE, PubkeysTable.COLUMN_EXPIRATION_TIME, 
		    			PubkeysTable.COLUMN_OBJECT_TYPE, PubkeysTable.COLUMN_OBJECT_VERSION, PubkeysTable.COLUMN_STREAM_NUMBER, PubkeysTable.COLUMN_CORRESPONDING_ADDRESS_ID,
		    			PubkeysTable.COLUMN_RIPE_HASH, PubkeysTable.COLUMN_BEHAVIOUR_BITFIELD, PubkeysTable.COLUMN_PUBLIC_SIGNING_KEY,	PubkeysTable.COLUMN_PUBLIC_ENCRYPTION_KEY,
		    			PubkeysTable.COLUMN_NONCE_TRIALS_PER_BYTE, PubkeysTable.COLUMN_EXTRA_BYTES, PubkeysTable.COLUMN_SIGNATURE_LENGTH, PubkeysTable.COLUMN_SIGNATURE};
		    	return available;
	    	}
		    
		    else if (uriType == SERVER_RECORDS || uriType == SERVER_RECORD_ID)
	    	{
		    	String[] available = {ServerRecordsTable.COLUMN_ID, ServerRecordsTable.COLUMN_URL, ServerRecordsTable.COLUMN_USERNAME, ServerRecordsTable.COLUMN_PASSWORD};
		    	return available;
	    	}
	
		    else
		    {
		    	 throw new IllegalArgumentException("Unknown URI Type: " + uriType + " Exception occurred in DatabaseContentProvider.getAvailable()");
		    }
	  }
	
	/**
	 * If the database encryption passphrase is currently set to its default value, 
	 * this method retrieves the corresponding encryption key and stores it using
	 * CacheWord. 
	 */
	public static void attemptGetDefaultEncryptionKey()
	{
      	// Check whether the user has set a database encryption passphrase
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(sContext);
		if (prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false) == false)
		{
			try
			{
				sCacheWordHandler.setPassphrase(DEFAULT_DATABASE_PASSPHRASE.toCharArray());
			}
			catch (GeneralSecurityException e)
			{
				Log.e(TAG, "GeneralSecurityException occurred in DatabaseContentProvider.onCreate(). The exception message was:\n" 
					+ e.getMessage());
			}
		}
	}
	
	@SuppressLint("InlinedApi")
	@Override
	public void onCacheWordLocked()
	{
		Log.d(TAG, "DatabaseContenProvider.onCacheWordLocked() called.");
		
		attemptGetDefaultEncryptionKey();
	}

	@Override
	public void onCacheWordOpened()
	{
		Log.d(TAG, "DatabaseContenProvider.onCacheWordOpened() called.");
		
		openDatabase();
	}

	@Override
	public void onCacheWordUninitialized()
	{
		Log.d(TAG, "DatabaseContenProvider.onCacheWordUninitialized() called.");
	   // Nothing to do here
	}
}