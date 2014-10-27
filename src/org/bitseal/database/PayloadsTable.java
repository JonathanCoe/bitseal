package org.bitseal.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class PayloadsTable
{
	// Database table
	public static final String TABLE_PAYLOADS = "payloads";
	  
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_RELATED_ADDRESS_ID = "related_address_id";
	public static final String COLUMN_BELONGS_TO_ME = "belongs_to_me";
	public static final String COLUMN_PROCESSING_COMPLETE = "processing_complete";
	public static final String COLUMN_TIME = "time";
	public static final String COLUMN_TYPE = "type";
	public static final String COLUMN_ACK = "ack";
	public static final String COLUMN_POW_DONE = "pow_done";
	public static final String COLUMN_PAYLOAD = "payload";

	// Database creation SQL statement
	private static final String DATABASE_CREATE = "create table " 
	    + TABLE_PAYLOADS
	    + "(" 
	    + COLUMN_ID + " integer primary key autoincrement, " 
	    + COLUMN_RELATED_ADDRESS_ID + " integer references addresses(_id), "
	    + COLUMN_BELONGS_TO_ME + " integer, "
	    + COLUMN_PROCESSING_COMPLETE + " integer, "
	    + COLUMN_TIME + " integer, "
	    + COLUMN_TYPE + " text, "
	    + COLUMN_ACK + " integer, "
	    + COLUMN_POW_DONE + " integer, "
	    + COLUMN_PAYLOAD + " text"
	    + ");";

	public static void onCreate(SQLiteDatabase database)
	{
	    database.execSQL(DATABASE_CREATE);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) 
	{
	  Log.w(MessagesTable.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion  + ", which will destroy all old data");
	  database.execSQL("DROP TABLE IF EXISTS " + TABLE_PAYLOADS);
	  onCreate(database);
	}
} 