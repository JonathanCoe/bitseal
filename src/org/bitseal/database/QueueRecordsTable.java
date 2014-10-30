package org.bitseal.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class QueueRecordsTable
{
	  // Database table
	  public static final String TABLE_QUEUE_RECORDS = "queue_records";
	  
	  public static final String COLUMN_ID = "_id";
	  public static final String COLUMN_TASK = "task";
	  public static final String COLUMN_TRIGGER_TIME = "trigger_time";
	  public static final String COLUMN_RECORD_COUNT = "record_count";
	  public static final String COLUMN_LAST_ATTEMPT_TIME = "last_attempt_time";
	  public static final String COLUMN_ATTEMPTS = "attempts";
	  public static final String COLUMN_OBJECT_0_ID = "object_0_id";
	  public static final String COLUMN_OBJECT_0_TYPE = "object_0_type";
	  public static final String COLUMN_OBJECT_1_ID = "object_1_id";
	  public static final String COLUMN_OBJECT_1_TYPE = "object_1_type";
	  public static final String COLUMN_OBJECT_2_ID = "object_2_id";
	  public static final String COLUMN_OBJECT_2_TYPE = "object_2_type";

	  // Database creation SQL statement
	  private static final String DATABASE_CREATE = "create table " 
	      + TABLE_QUEUE_RECORDS
	      + "(" 
	      + COLUMN_ID + " integer primary key autoincrement, " 
	      + COLUMN_TASK + " text, "
	      + COLUMN_TRIGGER_TIME + " integer, "
	      + COLUMN_RECORD_COUNT + " integer, "
	      + COLUMN_LAST_ATTEMPT_TIME + " integer, "
	      + COLUMN_ATTEMPTS + " integer, "
	      + COLUMN_OBJECT_0_ID + " integer, "
	      + COLUMN_OBJECT_0_TYPE + " text, "
	      + COLUMN_OBJECT_1_ID + " integer, "
	      + COLUMN_OBJECT_1_TYPE + " text,"
	      + COLUMN_OBJECT_2_ID + " integer, "
	      + COLUMN_OBJECT_2_TYPE + " text"
	      + ");";

	  public static void onCreate(SQLiteDatabase database) 
	  {
	    database.execSQL(DATABASE_CREATE);
	  }

	  public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) 
	  {
	    Log.w(MessagesTable.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion  + ", which will destroy all old data");
	    database.execSQL("DROP TABLE IF EXISTS " + TABLE_QUEUE_RECORDS);
	    onCreate(database);
	  }
}