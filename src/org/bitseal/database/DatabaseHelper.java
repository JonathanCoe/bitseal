package org.bitseal.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper
{
	private static final String DATABASE_NAME = "bitseal_database.db";
	private static final int DATABASE_VERSION = 5;
	
	private static final String TAG = "DATABASE_HELPER";
	
	public DatabaseHelper(Context context) 
	{
	   super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	  
	// Method is called during creation of the database
	@Override
	public void onCreate(SQLiteDatabase database) 
	{
	   AddressBookRecordsTable.onCreate(database);
	   AddressesTable.onCreate(database);
	   MessagesTable.onCreate(database);
	   PayloadsTable.onCreate(database);
	   PubkeysTable.onCreate(database);
	   QueueRecordsTable.onCreate(database);
	   ServerRecordsTable.onCreate(database);
	}
	
	// Method is called during an upgrade of the database, e.g. if you increase the database version
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) 
	{
		if(newVersion == 2)
		{
			database.execSQL("ALTER TABLE " + AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS + " ADD COLUMN "
					+ AddressBookRecordsTable.COLUMN_COLOUR_R + " integer INT AFTER " + AddressBookRecordsTable.COLUMN_ID);
			
			database.execSQL("ALTER TABLE " + AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS + " ADD COLUMN "
					+ AddressBookRecordsTable.COLUMN_COLOUR_G + " integer INT AFTER " + AddressBookRecordsTable.COLUMN_COLOUR_R);
			
			database.execSQL("ALTER TABLE " + AddressBookRecordsTable.TABLE_ADDRESS_BOOK_RECORDS + " ADD COLUMN "
					+ AddressBookRecordsTable.COLUMN_COLOUR_B + " integer INT AFTER " + AddressBookRecordsTable.COLUMN_COLOUR_G);
		}
		
		if (newVersion == 3)
		{
			// Add 'is ack' column to Payloads table
			Log.w(TAG, "Adding 'is ack' column to Payloads table");
			database.execSQL("ALTER TABLE " + PayloadsTable.TABLE_PAYLOADS + " ADD COLUMN "
					+ PayloadsTable.COLUMN_ACK + " integer INT AFTER " + PayloadsTable.COLUMN_TYPE);
			
			// The Pubkeys table has changed a lot from version 2 to version 3, so we will delete the table and re-create it. All pubkeys
			// can be generated or retrieved again as necessary, so this is not a significant loss of data
			Log.w(TAG, "Dropping Pubkeys table");
			database.execSQL("DROP TABLE IF EXISTS " + PubkeysTable.TABLE_PUBKEYS);
			Log.w(TAG, "Re-creating Pubkeys table");
			PubkeysTable.onCreate(database);
		}
		
		if(newVersion == 4)
		{
			// Add 'trigger time' column to QueueRecords table
			Log.w(TAG, "Adding 'trigger time' column to QueueRecords table");
			database.execSQL("ALTER TABLE " + QueueRecordsTable.TABLE_QUEUE_RECORDS + " ADD COLUMN "
					+ QueueRecordsTable.COLUMN_TRIGGER_TIME + " integer INT AFTER " + QueueRecordsTable.COLUMN_TASK);
			
			// Add 'record count' column to QueueRecords table
			Log.w(TAG, "Adding 'record count' column to QueueRecords table");
			database.execSQL("ALTER TABLE " + QueueRecordsTable.TABLE_QUEUE_RECORDS + " ADD COLUMN "
					+ QueueRecordsTable.COLUMN_RECORD_COUNT + " integer INT AFTER " + QueueRecordsTable.COLUMN_TRIGGER_TIME);
		}
		
		if (newVersion == 5)
		{
			// Remove 'last dissemination time' column from Pubkeys table
			Log.w(TAG, "Dropping Pubkeys table");
			database.execSQL("DROP TABLE IF EXISTS " + PubkeysTable.TABLE_PUBKEYS);
			Log.w(TAG, "Re-creating Pubkeys table");
			PubkeysTable.onCreate(database);
		}
	}
}