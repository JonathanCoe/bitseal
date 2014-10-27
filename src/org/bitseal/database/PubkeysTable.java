package org.bitseal.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class PubkeysTable 
{
  // Database table
  public static final String TABLE_PUBKEYS = "pubkeys";
  
  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_BELONGS_TO_ME = "belongs_to_me";
  public static final String COLUMN_POW_NONCE = "pow_nonce";
  public static final String COLUMN_EXPIRATION_TIME = "expiration_time";
  public static final String COLUMN_OBJECT_TYPE = "object_type";
  public static final String COLUMN_OBJECT_VERSION = "object_version";
  public static final String COLUMN_STREAM_NUMBER = "stream_number";
  public static final String COLUMN_CORRESPONDING_ADDRESS_ID = "corresponding_address_id";
  public static final String COLUMN_RIPE_HASH = "ripe_hash";
  public static final String COLUMN_LAST_DISSEMINATION_TIME = "last_dissemination_time";
  public static final String COLUMN_BEHAVIOUR_BITFIELD = "behaviour_bitfield";
  public static final String COLUMN_PUBLIC_SIGNING_KEY = "public_signing_key";
  public static final String COLUMN_PUBLIC_ENCRYPTION_KEY = "public_encryption_key";
  public static final String COLUMN_NONCE_TRIALS_PER_BYTE = "nonce_trials_per_byte";
  public static final String COLUMN_EXTRA_BYTES = "extra_bytes";
  public static final String COLUMN_SIGNATURE_LENGTH = "signature_length";
  public static final String COLUMN_SIGNATURE = "signature";

  // Database creation SQL statement
  private static final String DATABASE_CREATE = "create table " 
      + TABLE_PUBKEYS
      + "(" 
      + COLUMN_ID + " integer primary key autoincrement, "
      + COLUMN_BELONGS_TO_ME + " integer, "
      + COLUMN_POW_NONCE + " integer, "
      + COLUMN_EXPIRATION_TIME + " integer, "
      + COLUMN_OBJECT_TYPE + " integer, "
      + COLUMN_OBJECT_VERSION + " integer, "
      + COLUMN_STREAM_NUMBER + " integer, "
      + COLUMN_CORRESPONDING_ADDRESS_ID + " integer references addresses(_id), "      
      + COLUMN_RIPE_HASH + " text, "
      + COLUMN_LAST_DISSEMINATION_TIME + " integer, "
      + COLUMN_BEHAVIOUR_BITFIELD + " integer, "
      + COLUMN_PUBLIC_SIGNING_KEY + " text, "
      + COLUMN_PUBLIC_ENCRYPTION_KEY + " text, "
      + COLUMN_NONCE_TRIALS_PER_BYTE + " integer, "
      + COLUMN_EXTRA_BYTES + " integer, "
      + COLUMN_SIGNATURE_LENGTH + " integer, "
      + COLUMN_SIGNATURE + " text"
      + ");";

  public static void onCreate(SQLiteDatabase database) 
  {
    database.execSQL(DATABASE_CREATE);
  }

  public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) 
  {
    Log.w(MessagesTable.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion  + ", which will destroy all old data");
    database.execSQL("DROP TABLE IF EXISTS " + TABLE_PUBKEYS);
    onCreate(database);
  }
} 