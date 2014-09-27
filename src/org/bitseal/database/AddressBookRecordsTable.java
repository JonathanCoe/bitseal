package org.bitseal.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class AddressBookRecordsTable 
{
  // Database table
  public static final String TABLE_ADDRESS_BOOK_RECORDS = "address_book_records";
  
  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_COLOUR_R = "_colourR";
  public static final String COLUMN_COLOUR_G = "_colourG";
  public static final String COLUMN_COLOUR_B = "_colourB";
  public static final String COLUMN_LABEL = "label";
  public static final String COLUMN_ADDRESS = "address";

  // Database creation SQL statement
  private static final String DATABASE_CREATE = "create table " 
      + TABLE_ADDRESS_BOOK_RECORDS
      + "(" 
      + COLUMN_ID + " integer primary key autoincrement, "
      + COLUMN_COLOUR_R + " integer, "
      + COLUMN_COLOUR_G + " integer, "
      + COLUMN_COLOUR_B + " integer, "
      + COLUMN_LABEL + " text, "
      + COLUMN_ADDRESS + " text"
      + ");";

  public static void onCreate(SQLiteDatabase database) 
  {
    database.execSQL(DATABASE_CREATE);
  }

  public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) 
  {
    Log.w(MessagesTable.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion  + ", which will destroy all old data");
    database.execSQL("DROP TABLE IF EXISTS " + TABLE_ADDRESS_BOOK_RECORDS);
    onCreate(database);
  }
} 