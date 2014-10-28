package org.bitseal.database;

import java.util.Arrays;
import java.util.HashSet;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class DatabaseContentProvider extends ContentProvider
{
	  // Database
	  private DatabaseHelper database;
	  
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

	  @Override
	  public boolean onCreate() 
	  {
	    database = new DatabaseHelper(getContext());
	    return false;
	  }
	  
	  @Override
	  public String getType(Uri uri) 
	  {
	    return null; // This method will not be called unless the application changes to specifically invoke it. Thus it can be safely left to return null.
	  }
	  
	  @Override
	  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) 
	  {
	    // Uisng SQLiteQueryBuilder instead of query() method
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

	    SQLiteDatabase db = database.getWritableDatabase();
	    Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
	    // make sure that potential listeners are getting notified
	    cursor.setNotificationUri(getContext().getContentResolver(), uri);

	    return cursor;
	  }

	  @Override
	  public Uri insert(Uri uri, ContentValues values)
	  {
	    int uriType = sURIMatcher.match(uri);
	    SQLiteDatabase sqlDB = database.getWritableDatabase();
	    long id = 0;
	    String path;
	    
	    switch (uriType) 
	    {
		    case ADDRESSES:
			      id = sqlDB.insert(AddressesTable.TABLE_ADDRESSES, null, values);
			      path = PATH_ADDRESSES;
			      break;
		      
		    case ADDRESS_BOOK_RECORDS:
			      id = sqlDB.insert(AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS, null, values);
			      path = PATH_ADDRESS_BOOK_RECORDS;
			      break;
			      
		    case MESSAGES:
			      id = sqlDB.insert(MessagesTable.TABLE_MESSAGES, null, values);
			      path = PATH_MESSAGES;
			      break;
			      
		    case QUEUE_RECORDS:
			      id = sqlDB.insert(QueueRecordsTable.TABLE_QUEUE_RECORDS, null, values);
			      path = PATH_QUEUE_RECORDS;
			      break;
			      
		    case PAYLOADS:
			      id = sqlDB.insert(PayloadsTable.TABLE_PAYLOADS, null, values);
			      path = PATH_PAYLOADS;
			      break;
			      
		    case PUBKEYS:
			      id = sqlDB.insert(PubkeysTable.TABLE_PUBKEYS, null, values);
			      path = PATH_PUBKEYS;
			      break;
			      
		    case SERVER_RECORDS:
			      id = sqlDB.insert(ServerRecordsTable.TABLE_SERVER_RECORDS, null, values);
			      path = PATH_SERVER_RECORDS;
			      break;
		      
		    default:
		    	  throw new IllegalArgumentException("Unknown URI: " + uri + " Exception occurred in DatabaseContentProvider.insert()");
	    }
	    
	    getContext().getContentResolver().notifyChange(uri, null);
	    return Uri.parse(path + "/" + id);
	  }

	  @Override
	  public int delete(Uri uri, String selection, String[] selectionArgs) 
	  {
	    int uriType = sURIMatcher.match(uri);
	    SQLiteDatabase sqlDB = database.getWritableDatabase();
	    int rowsDeleted = 0;
	    String id;
	    switch (uriType)
	    {
		    case ADDRESSES:
			      rowsDeleted = sqlDB.delete(AddressesTable.TABLE_ADDRESSES, selection, selectionArgs);
			      break;      
		    case ADDRESS_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsDeleted = sqlDB.delete(AddressesTable.TABLE_ADDRESSES, AddressesTable.COLUMN_ID + "=" + id, null);
			      }
			      else 
			      {
			    	  rowsDeleted = sqlDB.delete(AddressesTable.TABLE_ADDRESSES, AddressesTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case ADDRESS_BOOK_RECORDS:
			      rowsDeleted = sqlDB.delete(AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS, selection, selectionArgs);
			      break;      
		    case ADDRESS_BOOK_RECORD_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsDeleted = sqlDB.delete(AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS, AddressBookRecordsTable.COLUMN_ID + "=" + id, null);
			      }
			      else 
			      {
			    	  rowsDeleted = sqlDB.delete(AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS, AddressBookRecordsTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case MESSAGES:
			      rowsDeleted = sqlDB.delete(MessagesTable.TABLE_MESSAGES, selection, selectionArgs);
			      break;      
		    case MESSAGE_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsDeleted = sqlDB.delete(MessagesTable.TABLE_MESSAGES, MessagesTable.COLUMN_ID + "=" + id, null);
			      }
			      else 
			      {
			    	  rowsDeleted = sqlDB.delete(MessagesTable.TABLE_MESSAGES, MessagesTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case QUEUE_RECORDS:
			      rowsDeleted = sqlDB.delete(QueueRecordsTable.TABLE_QUEUE_RECORDS, selection, selectionArgs);
			      break;      
		    case QUEUE_RECORD_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsDeleted = sqlDB.delete(QueueRecordsTable.TABLE_QUEUE_RECORDS, QueueRecordsTable.COLUMN_ID + "=" + id, null);
			      }
			      else 
			      {
			    	  rowsDeleted = sqlDB.delete(QueueRecordsTable.TABLE_QUEUE_RECORDS, QueueRecordsTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case PAYLOADS:
			      rowsDeleted = sqlDB.delete(PayloadsTable.TABLE_PAYLOADS, selection, selectionArgs);
			      break;      
		    case PAYLOAD_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsDeleted = sqlDB.delete(PayloadsTable.TABLE_PAYLOADS, PayloadsTable.COLUMN_ID + "=" + id, null);
			      }
			      else 
			      {
			    	  rowsDeleted = sqlDB.delete(PayloadsTable.TABLE_PAYLOADS, PayloadsTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case PUBKEYS:
			      rowsDeleted = sqlDB.delete(PubkeysTable.TABLE_PUBKEYS, selection, selectionArgs);
			      break;      
		    case PUBKEY_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsDeleted = sqlDB.delete(PubkeysTable.TABLE_PUBKEYS, PubkeysTable.COLUMN_ID + "=" + id, null);
			      }
			      else 
			      {
			    	  rowsDeleted = sqlDB.delete(PubkeysTable.TABLE_PUBKEYS, PubkeysTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case SERVER_RECORDS:
			      rowsDeleted = sqlDB.delete(ServerRecordsTable.TABLE_SERVER_RECORDS, selection, selectionArgs);
			      break;      
		    case SERVER_RECORD_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsDeleted = sqlDB.delete(ServerRecordsTable.TABLE_SERVER_RECORDS, ServerRecordsTable.COLUMN_ID + "=" + id, null);
			      }
			      else 
			      {
			    	  rowsDeleted = sqlDB.delete(ServerRecordsTable.TABLE_SERVER_RECORDS, ServerRecordsTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    default:
		    	  throw new IllegalArgumentException("Unknown URI: " + uri + " Exception occurred in DatabaseContentProvider.delete()");
	    }
	    getContext().getContentResolver().notifyChange(uri, null);
	    return rowsDeleted;
	  }

	  @Override
	  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) 
	  {
	    int uriType = sURIMatcher.match(uri);
	    SQLiteDatabase sqlDB = database.getWritableDatabase();
	    int rowsUpdated = 0;
	    String id;
	    
	    switch (uriType)
	    {
		    case ADDRESSES:
			      rowsUpdated = sqlDB.update(AddressesTable.TABLE_ADDRESSES, values, selection, selectionArgs);
			      break;
		    case ADDRESS_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsUpdated = sqlDB.update(AddressesTable.TABLE_ADDRESSES, values, AddressesTable.COLUMN_ID + "=" + id, null);
			      } 
			      else 
			      {
			    	  rowsUpdated = sqlDB.update(AddressesTable.TABLE_ADDRESSES, values, AddressesTable.COLUMN_ID + "=" + id  + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case ADDRESS_BOOK_RECORDS:
			      rowsUpdated = sqlDB.update(AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS, values, selection, selectionArgs);
			      break;
		    case ADDRESS_BOOK_RECORD_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsUpdated = sqlDB.update(AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS, values, AddressBookRecordsTable.COLUMN_ID + "=" + id, null);
			      } 
			      else 
			      {
			    	  rowsUpdated = sqlDB.update(AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS, values, AddressBookRecordsTable.COLUMN_ID + "=" + id  + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case MESSAGES:
			      rowsUpdated = sqlDB.update(MessagesTable.TABLE_MESSAGES, values, selection, selectionArgs);
			      break;
		    case MESSAGE_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsUpdated = sqlDB.update(MessagesTable.TABLE_MESSAGES, values, MessagesTable.COLUMN_ID + "=" + id, null);
			      } 
			      else 
			      {
			    	  rowsUpdated = sqlDB.update(MessagesTable.TABLE_MESSAGES, values, MessagesTable.COLUMN_ID + "=" + id  + " and " + selection, selectionArgs);
			      }
			      break;

		    case QUEUE_RECORDS:
			      rowsUpdated = sqlDB.update(QueueRecordsTable.TABLE_QUEUE_RECORDS, values, selection, selectionArgs);
			      break;
		    case QUEUE_RECORD_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsUpdated = sqlDB.update(QueueRecordsTable.TABLE_QUEUE_RECORDS, values, QueueRecordsTable.COLUMN_ID + "=" + id, null);
			      } 
			      else 
			      {
			    	  rowsUpdated = sqlDB.update(QueueRecordsTable.TABLE_QUEUE_RECORDS, values, QueueRecordsTable.COLUMN_ID + "=" + id  + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case PAYLOADS:
			      rowsUpdated = sqlDB.update(PayloadsTable.TABLE_PAYLOADS, values, selection, selectionArgs);
			      break;
		    case PAYLOAD_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsUpdated = sqlDB.update(PayloadsTable.TABLE_PAYLOADS, values, PayloadsTable.COLUMN_ID + "=" + id, null);
			      }
			      else 
			      {
			    	  rowsUpdated = sqlDB.update(PayloadsTable.TABLE_PAYLOADS, values, PayloadsTable.COLUMN_ID + "=" + id  + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case PUBKEYS:
			      rowsUpdated = sqlDB.update(PubkeysTable.TABLE_PUBKEYS, values, selection, selectionArgs);
			      break;
		    case PUBKEY_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsUpdated = sqlDB.update(PubkeysTable.TABLE_PUBKEYS, values, PubkeysTable.COLUMN_ID + "=" + id, null);
			      } 
			      else 
			      {
			    	  rowsUpdated = sqlDB.update(PubkeysTable.TABLE_PUBKEYS, values, PubkeysTable.COLUMN_ID + "=" + id  + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    case SERVER_RECORDS:
			      rowsUpdated = sqlDB.update(ServerRecordsTable.TABLE_SERVER_RECORDS, values, selection, selectionArgs);
			      break;
		    case SERVER_RECORD_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) 
			      {
			    	  rowsUpdated = sqlDB.update(ServerRecordsTable.TABLE_SERVER_RECORDS, values, ServerRecordsTable.COLUMN_ID + "=" + id, null);
			      } 
			      else 
			      {
			    	  rowsUpdated = sqlDB.update(ServerRecordsTable.TABLE_SERVER_RECORDS, values, ServerRecordsTable.COLUMN_ID + "=" + id  + " and " + selection, selectionArgs);
			      }
			      break;
		      
		    default:
		    	  throw new IllegalArgumentException("Unknown URI: " + uri + " Exception occurred in DatabaseContentProvider.update()");
	    }
	    getContext().getContentResolver().notifyChange(uri, null);
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
		    			QueueRecordsTable.COLUMN_OBJECT_1_TYPE};
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
}