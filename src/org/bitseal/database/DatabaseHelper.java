package org.bitseal.database;

import info.guardianproject.cacheword.CacheWordHandler;
import net.sqlcipher.database.SQLiteDatabase;
import android.content.Context;

public class DatabaseHelper extends SQLCipherOpenHelper
{
	private static final String DATABASE_NAME = "bitseal_database.db";
	private static final int DATABASE_VERSION = 7;
	
	public DatabaseHelper(Context context, CacheWordHandler cacheWordHandler)
	{
		super(cacheWordHandler, context, DATABASE_NAME, null, DATABASE_VERSION);
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
		// Nothing to do here currently
	}
}