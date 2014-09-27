package org.bitseal.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper
{
	private static final String DATABASE_NAME = "bitseal_database.db";
	private static final int DATABASE_VERSION = 2;
	
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
	}
}