package org.bitseal.activities;

import java.util.ArrayList;

import org.bitseal.R;
import org.bitseal.data.Address;
import org.bitseal.data.AddressBookRecord;
import org.bitseal.data.Message;
import org.bitseal.database.AddressBookRecordProvider;
import org.bitseal.database.AddressBookRecordsTable;
import org.bitseal.database.AddressProvider;
import org.bitseal.database.AddressesTable;
import org.bitseal.database.MessageProvider;
import org.bitseal.database.MessagesTable;

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
public class SentMessageActivity extends Activity 
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
	
	private static final String TAG = "SENT_MESSAGE_ACTIVITY";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sent_message);
		
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
							
				Toast.makeText(getApplicationContext(), "Message text copied to the clipboard", Toast.LENGTH_LONG).show();
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
						
						// Set a flag so that the 'Sent' Activity can adjust its list view properly
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
						SharedPreferences.Editor editor = prefs.edit();
					    editor.putBoolean(FLAG_SENT_MESSAGE_DELETED, true);
					    editor.commit();
				        
				        deleteDialog.dismiss();
				        
				        Toast.makeText(getApplicationContext(), "Message deleted", Toast.LENGTH_SHORT).show();
				        
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
		        this.startActivity(intent1);
		        break;
		        
		    case R.id.menu_item_sent:
		        Intent intent2 = new Intent(this, SentActivity.class);
		        this.startActivity(intent2);
		        break;  
		        
		    case R.id.menu_item_compose:
		        Intent intent3 = new Intent(this, ComposeActivity.class);
		        this.startActivity(intent3);
		        break;
		        
		    case R.id.menu_item_identities:
		        Intent intent4 = new Intent(this, IdentitiesActivity.class);
		        this.startActivity(intent4);
		        break;
		        
		    case R.id.menu_item_addressBook:
		        Intent intent5 = new Intent(this, AddressBookActivity.class);
		        this.startActivity(intent5);
		        break;
		        
		    case R.id.menu_item_settings:
		        Intent intent6 = new Intent(this, SettingsActivity.class);
		        this.startActivity(intent6);
		        break;
		        
		    default:
		        return super.onOptionsItemSelected(item);
	    }

	    return true;
	}
}