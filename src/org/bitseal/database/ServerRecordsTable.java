package org.bitseal.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class ServerRecordsTable 
{
  // Database table
  public static final String TABLE_SERVER_RECORDS = "server_records";
  
  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_URL = "url";
  public static final String COLUMN_USERNAME = "username";
  public static final String COLUMN_PASSWORD = "password";

  // Database creation SQL statement
  private static final String DATABASE_CREATE = "create table " 
      + TABLE_SERVER_RECORDS
      + "(" 
      + COLUMN_ID + " integer primary key autoincrement, " 
      + COLUMN_URL + " text, "
      + COLUMN_USERNAME + " text, "
      + COLUMN_PASSWORD + " text"
      + ");";

  public static void onCreate(SQLiteDatabase database) 
  {
    database.execSQL(DATABASE_CREATE);
  }

  public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) 
  {
    Log.w(MessagesTable.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion  + ", which will destroy all old data");
    database.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVER_RECORDS);
    onCreate(database);
  }
} 