package org.bitseal.activities;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.bitseal.R;
import org.bitseal.core.App;
import org.bitseal.crypt.AddressGenerator;
import org.bitseal.data.Address;
import org.bitseal.data.AddressBookRecord;
import org.bitseal.data.Message;
import org.bitseal.database.AddressBookRecordProvider;
import org.bitseal.database.AddressBookRecordsTable;
import org.bitseal.database.AddressProvider;
import org.bitseal.database.MessageProvider;
import org.bitseal.database.MessagesTable;
import org.bitseal.services.AppLockHandler;
import org.bitseal.services.BackgroundService;
import org.bitseal.services.NotificationsService;
import org.bitseal.util.ColourCalculator;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
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
import android.widget.Toast;

/**
 * The Activity class for the app's inbox. 
 * 
 * @author Jonathan Coe
 */
public class InboxActivity extends ListActivity implements ICacheWordSubscriber
{
    private ArrayList<Message> mMessages;
    
    private ListView mInboxListView;
        
    private int mListPosition = 0;
        
    private static final String INBOX_FIRST_RUN = "inbox_first_run";
    
    private static final String FIRST_ADDRESS_LABEL = "Me";
    
	private static final String INBOX_ACTIVITY_LIST_POSITION = "inboxActivityListPosition";
	
	/** A key used to store the time of the last successful 'check for new msgs' server request */
	private static final String LAST_MSG_CHECK_TIME = "lastMsgCheckTime";
	
	/** Stores the Unix timestamp of the last msg payload we processed. This can be used to tell us how far behind the network we are. */
	private static final String LAST_PROCESSED_MSG_TIME = "lastProcessedMsgTime";
    
	// Used when receiving Intents to the UI so that it can refresh the data it is displaying
	public static final String UI_NOTIFICATION = "uiNotification";
	
	private static final int INBOX_COLOURS_ALPHA_VALUE = 70;
	
    /** The key for a boolean variable that records whether or not a user-defined database encryption passphrase has been saved */
    private static final String KEY_DATABASE_PASSPHRASE_SAVED = "databasePassphraseSaved"; 
    
    private CacheWordHandler mCacheWordHandler;
    	
    private static final String TAG = "INBOX_ACTIVITY";
    
	@SuppressLint("InlinedApi")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_inbox);
		
        // Check whether the user has set a database encryption passphrase
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false))
		{
			Log.i(TAG, "We detected that the user has a database encryption passphrase set");
			
			// If Bitseal is being launched (rather than re-opened)
			if (getIntent().hasCategory(Intent.CATEGORY_LAUNCHER))
			{
				Log.i(TAG, "InboxActivity's starting intent had Category_Launcher set");
				onCacheWordLocked();
				return;
			}
			
			// Connect to the CacheWordService
			mCacheWordHandler = new CacheWordHandler(this);
			mCacheWordHandler.connectToService();
			
			if (getIntent().hasExtra(LockScreenActivity.EXTRA_DATABASE_UNLOCKED))
			{
				// Start the BackgroundService
				Intent firstStartIntent = new Intent(this, BackgroundService.class);
				firstStartIntent.putExtra(BackgroundService.PERIODIC_BACKGROUND_PROCESSING_REQUEST, BackgroundService.BACKGROUND_PROCESSING_REQUEST);
				BackgroundService.sendWakefulWork(this, firstStartIntent);
			}
		}
		else
		{
			Log.i(TAG, "We detected that the user does NOT have a database encryption passphrase set");
		}
		
        // Check whether this is the first time the inbox activity has been opened - if so then run the 'first launch' routine
		if (prefs.getBoolean(INBOX_FIRST_RUN, true))
		{
			runFirstLaunchRoutine();
		}
        
        mInboxListView = new ListView(this);
        mInboxListView = (ListView)findViewById(android.R.id.list);
        
        setTitle(getResources().getString(R.string.inbox_activity_title));
        
        // Sometimes the CacheWordService will take too long to initialize, and as a result we will fail to detect 
        // that the app is locked. Therefore if our attempt to access the database fails and the user has a database
        // passphrase set, we will redirect to the lock screen.
        try
        {
    		MessageProvider msgProv = MessageProvider.get(this);
    		mMessages =msgProv.searchMessages(MessagesTable.COLUMN_BELONGS_TO_ME, String.valueOf(0)); // 0 stands for "false" in the database
    		
            // Sort the messages so that the most recent are displayed first
            Collections.sort(mMessages);
            
            MessageAdapter adapter = new MessageAdapter(mMessages);  
            setListAdapter(adapter);
        }
        catch (Exception e)
        {
        	Log.e(TAG, "While running InboxActivity.onCreate, our attempt to access the database failed.\n" +
        			"The exception message was: " + e.getMessage());
        	
        	if (prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false))
        	{
        		Log.e(TAG, "The user has a database passphrase set. Calling onCacheWordLocked().");
        		onCacheWordLocked();
        	}
        	else
        	{
        		Toast.makeText(getBaseContext(), R.string.inbox_toast_unknown_database_error, Toast.LENGTH_LONG).show();
        		Log.e(TAG, "Unknown exception occurred in InboxActivity.onCreate");
        	}
        }
        
        // If we have reached this point without crashing, then it should be safe to reset the uncaught exception handler flag
		SharedPreferences.Editor editor = prefs.edit();
	    editor.putBoolean(App.UNCAUGHT_EXCEPTION_HANDLED, true);
	    editor.commit();
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
	    Resources resources = getResources();
		AddressBookRecord addressBookEntry0 = new AddressBookRecord();
		addressBookEntry0.setLabel(resources.getString(R.string.inbox_default_address_book_entry_0_label));
		addressBookEntry0.setAddress(resources.getString(R.string.inbox_default_address_book_entry_0_address));
		
		AddressBookRecord addressBookEntry1 = new AddressBookRecord();
		addressBookEntry1.setLabel(resources.getString(R.string.inbox_default_address_book_entry_1_label));
		addressBookEntry1.setAddress(resources.getString(R.string.inbox_default_address_book_entry_1_address));
		
		AddressBookRecordProvider addBookProv = AddressBookRecordProvider.get(this);
		addBookProv.addAddressBookRecord(addressBookEntry0);
		addBookProv.addAddressBookRecord(addressBookEntry1);
	    
	    // Add the 'Welcome to Bitseal' message to the inbox
		Message welcomeMessage = new Message();
		welcomeMessage.setBelongsToMe(false);
		welcomeMessage.setToAddress(resources.getString(R.string.inbox_welcome_message_to_address));
		welcomeMessage.setFromAddress(resources.getString(R.string.inbox_welcome_message_from_address));
		welcomeMessage.setSubject(resources.getString(R.string.inbox_welcome_message_subject));
		welcomeMessage.setBody(resources.getString(R.string.inbox_welcome_message_body));
		welcomeMessage.setTime(System.currentTimeMillis() / 1000);
		
		MessageProvider msgProv = MessageProvider.get(getApplicationContext());
		long msg0Id = msgProv.addMessage(welcomeMessage);
		welcomeMessage.setId(msg0Id);
		mMessages = new ArrayList<Message>();
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
		    BackgroundService.sendWakefulWork(this, intent);
	    	
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
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.options_menu, menu);
		return true;
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
		    case R.id.menu_item_inbox:
		    	// We are already here, so there's nothing to do
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
		    	AppLockHandler.runLockRoutine(mCacheWordHandler);
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
		Log.i(TAG, "InboxActivity.onCacheWordLocked() called");
		
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