package org.bitseal.activities;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import java.util.ArrayList;

import org.bitseal.R;
import org.bitseal.core.QueueRecordProcessor;
import org.bitseal.data.Address;
import org.bitseal.data.AddressBookRecord;
import org.bitseal.data.Message;
import org.bitseal.data.Payload;
import org.bitseal.data.QueueRecord;
import org.bitseal.database.AddressBookRecordProvider;
import org.bitseal.database.AddressBookRecordsTable;
import org.bitseal.database.AddressProvider;
import org.bitseal.database.AddressesTable;
import org.bitseal.database.MessageProvider;
import org.bitseal.database.MessagesTable;
import org.bitseal.database.PayloadProvider;
import org.bitseal.database.QueueRecordProvider;
import org.bitseal.database.QueueRecordsTable;
import org.bitseal.services.DatabaseLockHandler;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The Activity class for the display of a single sent message. 
 * 
 * @author Jonathan Coe
 */
public class SentMessageActivity extends Activity implements ICacheWordSubscriber
{	
	public static final String EXTRA_MESSAGE_ID = "BITSEAL_MESSAGE_ID";
	public static final String EXTRA_DESTINATION_ADDRESS = "sentMessageActivity.DESTINATION_ADDRESS";
	
	public static final String EXTRA_COLOUR_R = "sentMessageActivity.COLOUR_R";
	public static final String EXTRA_COLOUR_G = "sentMessageActivity.COLOUR_G";
	public static final String EXTRA_COLOUR_B = "sentMessageActivity.COLOUR_B";
	
	public static final String FLAG_SENT_MESSAGE_DELETED = "sentMessageDeleted";
	
	private ArrayList<Message> mMessages;
	
	private Message mMessage;
	private long mMessageId;
	
	private View mMainView;
	
	private TextView mToAddressTextView;
	private TextView mFromAddressTextView;
	private TextView mStatusTextView;
	private TextView mSubjectTextView;
	private TextView mBodyTextView;
	
	private Button mCopyButton;
	private Button mDeleteButton;
	
	private int mColourR;
	private int mColourG;
	private int mColourB;

	private boolean mDestinationAddressInAddressBook;
	
	private static final int SENT_MESSAGE_COLOURS_ALPHA_VALUE = 70;
	
    /** The key for a boolean variable that records whether or not a user-defined database encryption passphrase has been saved */
    private static final String KEY_DATABASE_PASSPHRASE_SAVED = "databasePassphraseSaved"; 
    
    private CacheWordHandler mCacheWordHandler;
	
    private static final String TAG = "SENT_MESSAGE_ACTIVITY";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sent_message);
		
        // Check whether the user has set a database encryption passphrase
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false))
		{
			// Connect to the CacheWordService
			mCacheWordHandler = new CacheWordHandler(this);
			mCacheWordHandler.connectToService();
		}
		
		Bundle b = getIntent().getExtras();
		
		mMessageId = b.getLong(EXTRA_MESSAGE_ID);
		mColourR = b.getInt(EXTRA_COLOUR_R);
		mColourG = b.getInt(EXTRA_COLOUR_G);
		mColourB = b.getInt(EXTRA_COLOUR_B);
		
		// Retrieve the selected Message from the application database using the ID from the intent
		MessageProvider msgProv = MessageProvider.get(getApplicationContext());
		ArrayList<Message> retrievedMessages = msgProv.searchMessages(MessagesTable.COLUMN_ID, String.valueOf(mMessageId));
		mMessage = retrievedMessages.get(0); 
		
		mToAddressTextView = (TextView) findViewById(R.id.sent_message_toAddress_textview);
		mFromAddressTextView = (TextView) findViewById(R.id.sent_message_fromAddress_textview);
		mStatusTextView = (TextView) findViewById(R.id.sent_message_status_textview);
		mSubjectTextView = (TextView) findViewById(R.id.sent_message_subject_textview);
		mBodyTextView = (TextView) findViewById(R.id.sent_message_body_textview);
		
        String toAddressString = mMessage.getToAddress();
        // Check if we have an entry for this address in our address book. If we do, substitute the label of that entry for the address.
        AddressBookRecordProvider addBookProv = AddressBookRecordProvider.get(getApplicationContext());
		ArrayList<AddressBookRecord> retrievedAddressBookRecords = addBookProv.searchAddressBookRecords(AddressBookRecordsTable.COLUMN_ADDRESS, toAddressString);
		if (retrievedAddressBookRecords.size() > 0)
		{
			mToAddressTextView.setText(retrievedAddressBookRecords.get(0).getLabel());
			mToAddressTextView.setTextSize(15);
			mDestinationAddressInAddressBook = true;
		}
		else
		{
			mToAddressTextView.setText(toAddressString);
		}

        String fromAddressString = mMessage.getFromAddress();
        // Check if we can find the proper label for this address. If we can, then display it rather than the raw address.
        AddressProvider addProv = AddressProvider.get(getApplicationContext());
        ArrayList<Address> retrievedAddresses = addProv.searchAddresses(AddressesTable.COLUMN_ADDRESS, fromAddressString);
		if (retrievedAddresses.size() > 0)
		{
			mFromAddressTextView.setText(retrievedAddresses.get(0).getLabel());
			mFromAddressTextView.setTextSize(15);
		}
		else
		{
			mFromAddressTextView.setText(fromAddressString);
		}
		
		mStatusTextView.setText(mMessage.getStatus());
		
        if (mMessage.getSubject() == null)
        {
        	mSubjectTextView.setText("[No subject]");
        }
        
        else
        {
        	mSubjectTextView.setText(mMessage.getSubject());
        }
		mBodyTextView.setText(mMessage.getBody());
		
		mCopyButton = (Button) findViewById(R.id.sent_message_copy_button);	
		mCopyButton.setOnClickListener(new View.OnClickListener()
		{
			@SuppressWarnings("deprecation")
			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
			@Override
			public void onClick(View v) 
			{
				Log.i(TAG, "Sent message copy button pressed");
				
				int sdk = android.os.Build.VERSION.SDK_INT;
				
				if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) 
				{
				    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				    clipboard.setText(mMessage.getBody());
				} 
				
				else 
				{
				    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
				    android.content.ClipData clip = android.content.ClipData.newPlainText("COPIED_MESSAGE_TEXT", mMessage.getBody());
				    clipboard.setPrimaryClip(clip);
				}
							
				Toast.makeText(getApplicationContext(), R.string.sent_message_toast_message_copied, Toast.LENGTH_LONG).show();
			}
		});	
		
		mDeleteButton = (Button) findViewById(R.id.sent_message_delete_button);
		mDeleteButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v) 
			{
				Log.i(TAG, "Sent message delete button pressed");
		        
		        // Open a dialog to confirm or cancel the deletion of the message
				final Dialog deleteDialog = new Dialog(SentMessageActivity.this);
				LinearLayout dialogLayout = (LinearLayout) View.inflate(SentMessageActivity.this, R.layout.dialog_sent_message_delete, null);
				deleteDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				deleteDialog.setContentView(dialogLayout);
				
				WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
			    lp.copyFrom(deleteDialog.getWindow().getAttributes());
			    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
				
			    deleteDialog.show();
			    deleteDialog.getWindow().setAttributes(lp);		  
			    
			    Button confirmButton = (Button) dialogLayout.findViewById(R.id.sent_message_delete_dialog_confirm_button);
			    confirmButton.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						Log.i(TAG, "Sent message delete dialog confirm button pressed");							
						
						// Delete this Message from the application's database
						MessageProvider msgProv = MessageProvider.get(getApplicationContext());	
						mMessages = msgProv.getAllMessages();
						mMessages.remove(mMessage);
						msgProv.deleteMessage(mMessage);
						
						// Find any QueueRecords and Payloads associated with this Message and delete them from the database
						try
						{
							QueueRecordProvider queueProv = QueueRecordProvider.get(getApplicationContext());
							ArrayList<QueueRecord> correspondingQueueRecords = queueProv.searchQueueRecords(QueueRecordsTable.COLUMN_OBJECT_0_ID, String.valueOf(mMessage.getId()));
							for (QueueRecord q : correspondingQueueRecords)
							{
	
								// If this is a 'disseminate msg' QueueRecord, delete the msg and ack payloads						
								if (q.getTask().equals(QueueRecordProcessor.TASK_DISSEMINATE_MESSAGE))
								{
									PayloadProvider payProv = PayloadProvider.get(getApplicationContext());
									Payload msgPayload = payProv.searchForSingleRecord(q.getObject1Id());
									payProv.deletePayload(msgPayload);
									
									// If there is an ack Payload stored for this msg, delete it as well
									Payload ackPayload = payProv.searchForSingleRecord(mMessage.getAckPayloadId());
									payProv.deletePayload(ackPayload);
								}
								
								// Delete this QueueRecord from the database
								queueProv.deleteQueueRecord(q);
							}
						}
						catch (Exception e)
						{
							Log.e(TAG, "Exception occurred in ComposeActivity.mDeleteButton.onClick(). The exception message was: \n" + e.getMessage());
						}
						
						// Set a flag so that the 'Sent' Activity can adjust its list view properly
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
						SharedPreferences.Editor editor = prefs.edit();
					    editor.putBoolean(FLAG_SENT_MESSAGE_DELETED, true);
					    editor.commit();
				        
				        deleteDialog.dismiss();
				        
				        Toast.makeText(getApplicationContext(), R.string.sent_message_toast_message_deleted, Toast.LENGTH_SHORT).show();
				        
				        // Return to the 'Sent' Activity
						Intent i = new Intent(getBaseContext(), SentActivity.class);
				        startActivityForResult(i, 0);
					}
				});
			    
			    Button cancelButton = (Button) dialogLayout.findViewById(R.id.sent_message_delete_dialog_cancel_button);
			    cancelButton.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						Log.i(TAG, "Sent message delete dialog cancel button pressed");							
						
						deleteDialog.dismiss();
					}
				});
			}
		});
		
		// Set the colors inherited from the sent list view
		int color = Color.argb(0, mColourR, mColourG, mColourB);
		mToAddressTextView.setBackgroundColor(color);
		mFromAddressTextView.setBackgroundColor(color);
		mStatusTextView.setBackgroundColor(color);
		mSubjectTextView.setBackgroundColor(color);
		mBodyTextView.setBackgroundColor(color);
		
		int backgroundColor = Color.argb(SENT_MESSAGE_COLOURS_ALPHA_VALUE, mColourR, mColourG, mColourB);
		mMainView = (View) findViewById(R.id.sent_message_scrollView);
		mMainView.setBackgroundColor(backgroundColor);
	}
	
	@Override
	protected void onSaveInstanceState (Bundle outState) 
	{
	    super.onSaveInstanceState(outState);
	}
	
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) 
 	{
	    // Inflate the menu items for use in the action bar
	    getMenuInflater().inflate(R.menu.sent_message_activity_actions, menu);
	    
	    MenuItem addToAddressBookAction = menu.findItem(R.id.action_add_to_address_book);
	    if (mDestinationAddressInAddressBook)
	    {
	    	addToAddressBookAction.setVisible(false);
	    }
	    
	    return super.onCreateOptionsMenu(menu);
 	}
     
     @Override
     public boolean onPrepareOptionsMenu(Menu menu)
     {
     	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
     	if (prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false) == false)
 		{
 			menu.removeItem(R.id.menu_item_lock);
 		}
         return super.onPrepareOptionsMenu(menu);
     }
 	
 	@SuppressLint("InlinedApi")
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) 
 	{
 	    switch(item.getItemId()) 
 	    {
	    	case R.id.action_add_to_address_book:
	    		Intent i = new Intent(getBaseContext(), AddressBookActivity.class);
		        i.putExtra(EXTRA_DESTINATION_ADDRESS, mMessage.getToAddress());
		        startActivityForResult(i, 0);
		        break;
 	    
 	    	case R.id.menu_item_inbox:
 		        Intent intent1 = new Intent(this, InboxActivity.class);
 		        startActivity(intent1);
 		        break;
 		        
 		    case R.id.menu_item_sent:
 		        Intent intent2 = new Intent(this, SentActivity.class);
 		        startActivity(intent2);
 		        break;  
 		        
 		    case R.id.menu_item_compose:
 		        Intent intent3 = new Intent(this, ComposeActivity.class);
 		        startActivity(intent3);
 		        break;
 		        
 		    case R.id.menu_item_identities:
 		        Intent intent4 = new Intent(this, IdentitiesActivity.class);
 		        startActivity(intent4);
 		        break;
 		        
 		    case R.id.menu_item_addressBook:
 		        Intent intent5 = new Intent(this, AddressBookActivity.class);
 		        startActivity(intent5);
 		        break;
 		        
 		    case R.id.menu_item_settings:
 		        Intent intent6 = new Intent(this, SettingsActivity.class);
 		        startActivity(intent6);
 		        break;
 		        
 		    case R.id.menu_item_lock:
		    	DatabaseLockHandler.runLockRoutine(mCacheWordHandler);
		        break;
 		        
 		    default:
 		        return super.onOptionsItemSelected(item);
 	    }

 	    return true;
 	}
     
     @Override
     protected void onStop()
     {
     	super.onStop();
     	if (mCacheWordHandler != null)
     	{
         	mCacheWordHandler.disconnectFromService();
     	}
      }
 	
 	@SuppressLint("InlinedApi")
 	@Override
 	public void onCacheWordLocked()
 	{
 		// Redirect to the lock screen activity
         Intent intent = new Intent(getBaseContext(), LockScreenActivity.class);
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) // FLAG_ACTIVITY_CLEAR_TASK only exists in API 11 and later 
         {
         	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);// Clear the stack of activities
         }
         else
         {
         	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         }
         startActivity(intent);
 	}

 	@Override
 	public void onCacheWordOpened()
 	{
 		// Nothing to do here currently
 	}
 	
 	@Override
 	public void onCacheWordUninitialized()
 	{
 		// Database encryption is currently not enabled by default, so there is nothing to do here
 	}
}