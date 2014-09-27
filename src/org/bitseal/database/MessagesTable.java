package org.bitseal.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class MessagesTable 
{
  // Database table
  public static final String TABLE_MESSAGES = "messages";
  
  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_MSG_PAYLOAD_ID = "msg_payload_id";
  public static final String COLUMN_ACK_PAYLOAD_ID = "ack_payload_id";
  public static final String COLUMN_BELONGS_TO_ME = "belongs_to_me";
  public static final String COLUMN_READ = "read";
  public static final String COLUMN_STATUS = "status";
  public static final String COLUMN_TIME = "time";
  public static final String COLUMN_TO_ADDRESS = "to_address";
  public static final String COLUMN_FROM_ADDRESS = "from_address";
  public static final String COLUMN_SUBJECT = "subject";
  public static final String COLUMN_BODY = "body";

  // Database creation SQL statement
  private static final String DATABASE_CREATE = "create table " 
      + TABLE_MESSAGES
      + "(" 
      + COLUMN_ID + " integer primary key autoincrement, " 
      + COLUMN_MSG_PAYLOAD_ID + " integer references payloads(_id), "
      + COLUMN_ACK_PAYLOAD_ID + " integer references payloads(_id), "
      + COLUMN_BELONGS_TO_ME + " integer, "   
      + COLUMN_READ + " integer, "
      + COLUMN_STATUS + " text, "
      + COLUMN_TIME + " integer, "     
      + COLUMN_TO_ADDRESS + " text, "
      + COLUMN_FROM_ADDRESS + " text, "
      + COLUMN_SUBJECT + " text, "
      + COLUMN_BODY + " text"
      + ");";

  public static void onCreate(SQLiteDatabase database) 
  {
    database.execSQL(DATABASE_CREATE);
  }

  public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) 
  {
    Log.w(MessagesTable.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion  + ", which will destroy all old data");
    database.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
    onCreate(database);
  }
} 