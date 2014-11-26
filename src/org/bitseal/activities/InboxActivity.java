package org.bitseal.activities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.bitseal.R;
import org.bitseal.crypt.AddressGenerator;
import org.bitseal.data.Address;
import org.bitseal.data.AddressBookRecord;
import org.bitseal.data.Message;
import org.bitseal.database.AddressBookRecordProvider;
import org.bitseal.database.AddressBookRecordsTable;
import org.bitseal.database.AddressProvider;
import org.bitseal.database.MessageProvider;
import org.bitseal.database.MessagesTable;
import org.bitseal.services.BackgroundService;
import org.bitseal.services.NotificationsService;
import org.bitseal.util.ColourCalculator;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * The Activity class for the app's inbox. 
 * 
 * @author Jonathan Coe
 */
public class InboxActivity extends ListActivity
{
    private ArrayList<Message> mMessages;
    
    private ListView mInboxListView;
        
    private int mListPosition = 0;
    
    // Some default entries for the address book
    private static final String INBOX_FIRST_RUN = "inbox_first_run";
    private static final String ADDRESS_BOOK_ENTRY_0_LABEL = "Bitseal Developers";
    private static final String ADDRESS_BOOK_ENTRY_0_ADDRESS = "BM-NC2oGii7w8UT4igUhsCBGBE7gngvoD83";
    private static final String ADDRESS_BOOK_ENTRY_1_LABEL = "Darklogs.com";
    private static final String ADDRESS_BOOK_ENTRY_1_ADDRESS = "BM-2cTUZmrFaypXnAR4DAXLbAb6KrFPRhGyEe";
    
    // A welcome message for new users
    protected static final String WELCOME_MESSAGE_TO_ADDRESS = "Me";
    private static final String WELCOME_MESSAGE_FROM_ADDRESS = "BM-NC2oGii7w8UT4igUhsCBGBE7gngvoD83";
    private static final String WELCOME_MESSAGE_SUBJECT = "Welcome to Bitseal!";
    private static final String WELCOME_MESSAGE_BODY = "Thanks for trying out Bitseal. We really hope you enjoy the app.\n\n" +
    												   "If you have any feedback, please feel free to send us a message.";
    
    private static final String FIRST_ADDRESS_LABEL = "Me";
    
	private static final String INBOX_ACTIVITY_LIST_POSITION = "inboxActivityListPosition";
	
	/** A key used to store the time of the last successful 'check for new msgs' server request */
	private static final String LAST_MSG_CHECK_TIME = "lastMsgCheckTime";
	
	/** Stores the Unix timestamp of the last msg payload we processed. This can be used to tell us how far behind the network we are. */
	private static final String LAST_PROCESSED_MSG_TIME = "lastProcessedMsgTime";
    
	// Used when receiving Intents to the UI so that it can refresh the data it is displaying
	public static final String UI_NOTIFICATION = "uiNotification";
	
	private static final int INBOX_COLOURS_ALPHA_VALUE = 70;
	
    private static final String TAG = "INBOX_ACTIVITY";
    
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_inbox);
        
        // Check whether this is the first time the inbox activity has been opened - if so then run the 'first launch' routine
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (prefs.getBoolean(INBOX_FIRST_RUN, true))
		{
			runFirstLaunchRoutine();
		}
		
		MessageProvider msgProv = MessageProvider.get(this);
		mMessages =msgProv.searchMessages(MessagesTable.COLUMN_BELONGS_TO_ME, String.valueOf(0)); // 0 stands for "false" in the database
					
        // Sort the messages so that the most recent are displayed first
        Collections.sort(mMessages);
        
        mInboxListView = new ListView(this);
        mInboxListView = (ListView)findViewById(android.R.id.list);
        
        setTitle(getResources().getString(R.string.inbox_activity_title));
        
        MessageAdapter adapter = new MessageAdapter(mMessages);  
        setListAdapter(adapter);
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		
		// Register the broadcast receiver
		registerReceiver(receiver, new IntentFilter(UI_NOTIFICATION));
		
		Intent intent = getIntent();
		if (intent.hasExtra(NotificationsService.EXTRA_NEW_MESSAGES_NOTIFICATION_CLEARED))
		{
			// Set the 'new messages notification currently displayed' shared preference to false
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			SharedPreferences.Editor editor = prefs.edit();
		    editor.putBoolean(NotificationsService.KEY_NEW_MESSAGES_NOTIFICATION_CURRENTLY_DISPLAYED, false);
		    editor.commit();
		}
		
		// If we are returning to this activity after an inbox message has been deleted, we need to do a
		// special adjustment to the list position
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (prefs.getBoolean(InboxMessageActivity.FLAG_INBOX_MESSAGE_DELETED, false))
		{
			mListPosition = prefs.getInt(INBOX_ACTIVITY_LIST_POSITION, 0);
		    if (mListPosition > 0)
		    {
		    	Log.i(TAG, "We detected that an inbox message has just been deleted - setting the list position to " + mListPosition);
		    	getListView().setSelection(mListPosition);
		    }
			SharedPreferences.Editor editor = prefs.edit();
		    editor.putBoolean(InboxMessageActivity.FLAG_INBOX_MESSAGE_DELETED, false);
		    editor.commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.options_menu, menu);
		return true;
	}
	
	@Override
	protected void onPause() 
	{
		super.onPause();
		
		unregisterReceiver(receiver);
		
		// Save the listView position so that we can resume in the same position even if a record is deleted
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = prefs.edit();
		mListPosition = getListView().getFirstVisiblePosition();
	    editor.putInt(INBOX_ACTIVITY_LIST_POSITION, mListPosition);
	    editor.commit();
	}
	
	@Override
	protected void onRestart()
	{
		super.onRestart();
		
		updateListView();
	}
	
	private BroadcastReceiver receiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			Log.i(TAG, "InboxActivity.BroadcastReceiver.onReceive() called");
			updateListView();
		}
	};
	
	/**
	 * Adds some default entries to the address book, adds a welcome message to
	 * the inbox, generates a new Bitmessage address for the user, and starts the
	 * BackgroundService for the first time.
	 */
	private void runFirstLaunchRoutine()
	{
	    // Set a flag in SharedPreferences so that this will not be called again
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = prefs.edit();
	    editor.putBoolean(INBOX_FIRST_RUN, false);
	    editor.commit();
	    
	    // Add some default entries to the address book
		AddressBookRecord addressBookEntry0 = new AddressBookRecord();
		addressBookEntry0.setLabel(ADDRESS_BOOK_ENTRY_0_LABEL);
		addressBookEntry0.setAddress(ADDRESS_BOOK_ENTRY_0_ADDRESS);
		
		AddressBookRecord addressBookEntry1 = new AddressBookRecord();
		addressBookEntry1.setLabel(ADDRESS_BOOK_ENTRY_1_LABEL);
		addressBookEntry1.setAddress(ADDRESS_BOOK_ENTRY_1_ADDRESS);
		
		AddressBookRecordProvider addBookProv = AddressBookRecordProvider.get(this);
		addBookProv.addAddressBookRecord(addressBookEntry0);
		addBookProv.addAddressBookRecord(addressBookEntry1);
	    
	    // Add the 'Welcome to Bitseal' message to the inbox
		Message welcomeMessage = new Message();
		welcomeMessage.setBelongsToMe(false);
		welcomeMessage.setToAddress(WELCOME_MESSAGE_TO_ADDRESS);
		welcomeMessage.setFromAddress(WELCOME_MESSAGE_FROM_ADDRESS);
		welcomeMessage.setSubject(WELCOME_MESSAGE_SUBJECT);
		welcomeMessage.setBody(WELCOME_MESSAGE_BODY);
		welcomeMessage.setTime(System.currentTimeMillis() / 1000);
		
		MessageProvider msgProv = MessageProvider.get(getApplicationContext());
		long msg0Id = msgProv.addMessage(welcomeMessage);
		welcomeMessage.setId(msg0Id);
		mMessages.add(welcomeMessage);
		
		// Generate a new Bitmessage address
	    try
	    {
	    	AddressGenerator addGen = new AddressGenerator();
	    	Address firstAddress = addGen.generateAndSaveNewAddress();
	    	firstAddress.setLabel(FIRST_ADDRESS_LABEL);
	    	AddressProvider addProv = AddressProvider.get(getApplicationContext());
	    	addProv.updateAddress(firstAddress);
	    	
	    	// Set the 'last msg check time' to the current time - otherwise the app will start checking for msgs sent
	    	// within the last 2.5 days, which makes no sense as our address has only just been generated.
	    	long currentTime = System.currentTimeMillis() / 1000;
		    editor.putLong(LAST_MSG_CHECK_TIME, currentTime);
		    editor.commit();
			Log.i(TAG, "Updated the 'last successful msg check time' value stored in SharedPreferences to " + currentTime);
			
	    	// Set the 'last msg processed time' to the current time. As above, we do not have any addresses yet, so we
			// cannot have been sent a message yet. 
		    editor.putLong(LAST_PROCESSED_MSG_TIME, currentTime);
		    editor.commit();
		    Log.i(TAG, "Updated the 'last processed msg time' value stored in SharedPreferences to " + currentTime);
	    	
	    	// Start the BackgroundService in order to complete the 'create new identity' task
		    Intent intent = new Intent(getBaseContext(), BackgroundService.class);
		    intent.putExtra(BackgroundService.UI_REQUEST, BackgroundService.UI_REQUEST_CREATE_IDENTITY);
		    intent.putExtra(BackgroundService.ADDRESS_ID, firstAddress.getId());
	    	startService(intent);
	    	
	    	Log.i(TAG, "Starting BackgroundService for the first time");
	    }
	    catch (Exception e)
	    {
	    	Log.e(TAG, "Exception occured in InboxActivity while running runFirstLaunchRoutine(). \n " +
	    			"Exception message: " + e.getMessage());
	    }
	}
	
	/** 
	 * Needed to update the ListView when a Message has been read, and so should not longer
	 * be highlighted as unread
	 **/
    private void updateListView()
    {
    	// Get all Messages that do not 'belong to me' (i.e. were sent by someone else) from the database
    	MessageProvider msgProv = MessageProvider.get(getApplicationContext());
    	mMessages =msgProv.searchMessages(MessagesTable.COLUMN_BELONGS_TO_ME, String.valueOf(0)); // 0 stands for "false" in the database
    	Collections.sort(mMessages);
		
		// Save ListView state so that we can resume at the same scroll position
		Parcelable state = mInboxListView.onSaveInstanceState();
		
		// Re-instantiate the ListView and re-populate it
        mInboxListView = new ListView(this);
        mInboxListView = (ListView)findViewById(android.R.id.list);
		mInboxListView.setAdapter(new MessageAdapter(mMessages));

		// Restore previous state (including selected item index and scroll position)
		mInboxListView.onRestoreInstanceState(state);
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
	    switch(item.getItemId()) 
	    {
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
	
	/**
	 * A ViewHolder used to speed up this activity's ListView.
	 */
    static class ViewHolder 
    {
	    public TextView fromAddressTextView;
	    public TextView dateTextView;
	    public TextView subjectTextView;
	    public TextView unreadTextView;
	}
	    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        ((MessageAdapter)mInboxListView.getAdapter()).notifyDataSetChanged();
    }
    
    private class MessageAdapter extends ArrayAdapter<Message> 
    {
        public MessageAdapter(ArrayList<Message> messages) 
        {
            super(getBaseContext(), android.R.layout.simple_list_item_1, messages);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) 
        {
        	// If we weren't given a view that can be recycled, inflate a new one
        	if (convertView == null)
            {
        		convertView = getLayoutInflater().inflate(R.layout.list_item_inbox, parent, false);
        		
        	    // Configure the view holder
        	    ViewHolder viewHolder = new ViewHolder();
        	    viewHolder.fromAddressTextView = (TextView) convertView.findViewById(R.id.inbox_messagelist_item_fromaddress_textview);
        	    viewHolder.dateTextView = (TextView) convertView.findViewById(R.id.inbox_messagelist_item_date_textview);
        	    viewHolder.subjectTextView = (TextView) convertView.findViewById(R.id.inbox_messagelist_item_subject_textview);
        	    viewHolder.unreadTextView = (TextView) convertView.findViewById(R.id.inbox_messagelist_item_unread_textview);
        	    convertView.setTag(viewHolder);
            }
        	
        	ViewHolder holder = (ViewHolder) convertView.getTag();
        	
            // Get the message
            Message m = getItem(position);
			
			// Set the value that will be displayed in the 'date' field
            //mDateTextView = (TextView) convertView.findViewById(R.id.inbox_messagelist_item_date_textview);
            long messageTime = m.getTime();
            long currentTime = System.currentTimeMillis() / 1000;        
            Date currentDate = new Date(currentTime * 1000);
            Date messageDate = new Date(messageTime * 1000);
            
            Calendar calendar = Calendar.getInstance();
            
            calendar.setTime(messageDate);
            int messageYear = calendar.get(Calendar.YEAR);
            int messageDay = calendar.get(Calendar.DAY_OF_YEAR);
            
            calendar.setTime(currentDate);
            int currentYear = calendar.get(Calendar.YEAR);
            int currentDay = calendar.get(Calendar.DAY_OF_YEAR);
            
			if (messageYear != currentYear)
			{
				SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
				sdf.setTimeZone(TimeZone.getDefault());
				String formattedDate = sdf.format(messageDate);
				holder.dateTextView.setText(formattedDate);
			}
			else if (messageDay == currentDay)
			{
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
				sdf.setTimeZone(TimeZone.getDefault());
				String formattedDate = sdf.format(messageDate);
				holder.dateTextView.setText(formattedDate);
			}
			else
			{
				SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
				sdf.setTimeZone(TimeZone.getDefault());
				String formattedDate = sdf.format(messageDate);
				holder.dateTextView.setText(formattedDate);
			}
			
            // Configure the view for the 'from address' field
            String fromAddressString = m.getFromAddress();
			
            // Set the value that will be displayed in the 'subject' field
            if (m.getSubject() == null)
            {
            	holder.subjectTextView.setText("[No subject]");
            }
            else
            {
            	holder.subjectTextView.setText(m.getSubject());
            }
            
            // Declare the colour variables
            int color;
            int r;
            int g;
            int b;
            
            // Check if we have an entry for this address in our address book. If we do, substitute the label of that entry for the address.
            AddressBookRecordProvider addBookProv = AddressBookRecordProvider.get(getApplicationContext());
			ArrayList<AddressBookRecord> retrievedRecords = addBookProv.searchAddressBookRecords(AddressBookRecordsTable.COLUMN_ADDRESS, fromAddressString);
			if (retrievedRecords.size() > 0)
			{
				holder.fromAddressTextView.setText(retrievedRecords.get(0).getLabel());
				holder.fromAddressTextView.setTextSize(14);
				
				r = retrievedRecords.get(0).getColourR();
				g = retrievedRecords.get(0).getColourG();
				b = retrievedRecords.get(0).getColourB();
			}
			else
			{
				holder.fromAddressTextView.setText(fromAddressString);
				holder.fromAddressTextView.setTextSize(12);
				
	            int[] colourValues = ColourCalculator.calculateColoursFromAddress(fromAddressString);
				r = colourValues[0];
				g = colourValues[1];
				b = colourValues[2];
			}
			
			// Set the colours for this view
			color = Color.argb(0, r, g, b);
			holder.subjectTextView.setBackgroundColor(color);
            holder.fromAddressTextView.setBackgroundColor(color);
            holder.dateTextView.setBackgroundColor(color);
            convertView.setBackgroundColor(Color.argb(INBOX_COLOURS_ALPHA_VALUE, r, g, b));
            
			// If this message is unread, show the 'unread' tag
			if (m.hasBeenRead() == false)
			{		
				holder.unreadTextView.setVisibility(View.VISIBLE);
			}
			else
			{
				holder.unreadTextView.setVisibility(View.GONE);
			}
			
			// Need to create some final variables that can be used inside the onClickListener
			final int selectedColorR = r;
			final int selectedColorG = g;
			final int selectedColorB = b;
			final Message selectedMessage = m;
			
			convertView.setOnClickListener(new View.OnClickListener()
			{
                @Override
                public void onClick(View v) 
                {
                    Log.i(TAG, "Inbox list item clicked");
             	    
                    // Start the InboxMessageActivity
                    Intent i = new Intent(getBaseContext(), InboxMessageActivity.class);
                    i.putExtra(InboxMessageActivity.EXTRA_MESSAGE_ID, selectedMessage.getId());
                    i.putExtra(InboxMessageActivity.EXTRA_COLOUR_R, selectedColorR);
                    i.putExtra(InboxMessageActivity.EXTRA_COLOUR_G, selectedColorG);
                    i.putExtra(InboxMessageActivity.EXTRA_COLOUR_B, selectedColorB);
                    startActivityForResult(i, 0);
                }
            });
    		
			return convertView;
        }
    }
}