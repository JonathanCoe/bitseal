package org.bitseal.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class AddressesTable 
{
	// Database table
	public static final String TABLE_ADDRESSES = "addresses";
	  
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_CORRESPONDING_PUBKEY_ID = "corresponding_pubkey_id";
	public static final String COLUMN_LABEL = "label";
	public static final String COLUMN_ADDRESS = "address";
	public static final String COLUMN_PRIVATE_SIGNING_KEY = "private_signing_key";
	public static final String COLUMN_PRIVATE_ENCRYPTION_KEY = "private_encryption_key";
	public static final String COLUMN_RIPE_HASH = "ripe_hash";
	public static final String COLUMN_TAG = "tag";

	// Database creation SQL statement
	private static final String DATABASE_CREATE = "create table " 
      + TABLE_ADDRESSES
      + "(" 
      + COLUMN_ID + " integer primary key autoincrement, " 
      + COLUMN_CORRESPONDING_PUBKEY_ID + " integer references pubkeys(_id), "
      + COLUMN_LABEL + " text, "
      + COLUMN_ADDRESS + " text, "
      + COLUMN_PRIVATE_SIGNING_KEY + " text, "
      + COLUMN_PRIVATE_ENCRYPTION_KEY + " text, "
      + COLUMN_RIPE_HASH + " text, "
      + COLUMN_TAG + " text"
      + ");";

	public static void onCreate(SQLiteDatabase database) 
	{
		database.execSQL(DATABASE_CREATE);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) 
	{
		Log.w(MessagesTable.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion  + ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_ADDRESSES);
		onCreate(database);
	}
} 