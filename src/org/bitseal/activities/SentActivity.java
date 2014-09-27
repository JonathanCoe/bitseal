package org.bitseal.activities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.bitseal.R;
import org.bitseal.data.AddressBookRecord;
import org.bitseal.data.Message;
import org.bitseal.database.AddressBookRecordProvider;
import org.bitseal.database.AddressBookRecordsTable;
import org.bitseal.database.MessageProvider;
import org.bitseal.database.MessagesTable;
import org.bitseal.util.ColourCalculator;

import android.annotation.SuppressLint;
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
 * The Activity class for the app's 'Sent' screen
 * 
 * @author Jonathan Coe
 */

public class SentActivity extends ListActivity
{
	private ArrayList<Message> mMessages;
    
    private ListView mSentListView;
        
    private int mListPosition = 0;
    
	private static final String SENT_ACTIVITY_LIST_POSITION = "sentActivityListPosition";
    
	// Used when receiving Intents to the UI so that it can refresh the data it is displaying
	public static final String UI_NOTIFICATION = "uiNotification";
	
	private static final int SENT_COLOURS_ALPHA_VALUE = 70;
    
    private static final String TAG = "SENT_ACTIVITY";

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sent);
		
		// Get all 'sent' Messages from the database
		MessageProvider msgProv = MessageProvider.get(getApplicationContext());
		mMessages =msgProv.searchMessages(MessagesTable.COLUMN_BELONGS_TO_ME, String.valueOf(1)); // 1 stands for "true" in the database
		
        // Sort the messages so that the most recent are displayed first
        Collections.sort(mMessages);
		
        MessageAdapter adapter = new MessageAdapter(mMessages);
        mSentListView = new ListView(this);
        mSentListView = (ListView)findViewById(android.R.id.list);   
        setListAdapter(adapter);
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		
		// Register the broadcast receiver
		registerReceiver(receiver, new IntentFilter(UI_NOTIFICATION));
		
		// If we are returning to this activity after a sent message has been deleted, we need to do a
		// special adjustment to the list positon
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (prefs.getBoolean(SentMessageActivity.FLAG_SENT_MESSAGE_DELETED, false))
		{
			Log.i(TAG, "Sent - Running adjustment routine");
			
			mListPosition = prefs.getInt(SENT_ACTIVITY_LIST_POSITION, 0);
		    if (mListPosition > 0)
		    {
		    	Log.i(TAG, "We detected that a sent message has just been deleted - setting the list position to " + mListPosition);
		    	getListView().setSelection(mListPosition);
		    }
			SharedPreferences.Editor editor = prefs.edit();
		    editor.putBoolean(SentMessageActivity.FLAG_SENT_MESSAGE_DELETED, false);
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
	    editor.putInt(SENT_ACTIVITY_LIST_POSITION, mListPosition);
	    editor.commit();
	}
	
	private BroadcastReceiver receiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			Log.i(TAG, "SentActivity.BroadcastReceiver.onReceive() called");
			updateListView();
		}
	};
    
    private void updateListView()
    {   	
    	// Get all Messages that 'belong to me' (i.e. were sent by me) from the database
    	MessageProvider msgProv = MessageProvider.get(getApplicationContext());
    	mMessages =msgProv.searchMessages(MessagesTable.COLUMN_BELONGS_TO_ME, String.valueOf(1)); // 1 stands for "true" in the database
    	Collections.sort(mMessages);
		
		// Save ListView state so that we can resume at the same scroll position
		Parcelable state = mSentListView.onSaveInstanceState();
		
		// Re-instantiate the ListView and re-populate it
		mSentListView = new ListView(this);
		mSentListView = (ListView)findViewById(android.R.id.list);
		mSentListView.setAdapter(new MessageAdapter(mMessages));

		// Restore previous state (including selected item index and scroll position)
		mSentListView.onRestoreInstanceState(state);
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.options_menu, menu);
		return true;
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
	 * A ViewHolder used to speed up this activity's listview.
	 */
    static class ViewHolder 
    {
	    public TextView toAddressTextView;
	    public TextView dateTextView;
	    public TextView subjectTextView;
	    public TextView statusTextView;
    }
	
   public void onListItemClick(ListView l, View v, int position, long id)
   {
       Log.i(TAG, "Sent list item clicked");

	   // Get the Message selected from the adapter
	   Message m = ((MessageAdapter)mSentListView.getAdapter()).getItem(position);
	   Log.i(TAG, "Opening message with ID: " + m.getId());
	    
       // Start an instance of SentMessageActivity
       Intent i = new Intent(getBaseContext(), SentMessageActivity.class);
       i.putExtra(SentMessageActivity.EXTRA_MESSAGE_ID, m.getId());
       startActivityForResult(i, 0); 
    }
	    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        ((MessageAdapter)mSentListView.getAdapter()).notifyDataSetChanged();
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
        		convertView = getLayoutInflater().inflate(R.layout.list_item_sent, parent, false);
        		
        	    // Configure the view holder
        	    ViewHolder viewHolder = new ViewHolder();
        	    viewHolder.toAddressTextView = (TextView) convertView.findViewById(R.id.sent_messagelist_item_toaddress_textview);
        	    viewHolder.dateTextView = (TextView) convertView.findViewById(R.id.sent_messagelist_item_date_textview);
        	    viewHolder.subjectTextView = (TextView) convertView.findViewById(R.id.sent_messagelist_item_subject_textview);
        	    viewHolder.statusTextView = (TextView) convertView.findViewById(R.id.sent_messagelist_item_status_textview);
        	    convertView.setTag(viewHolder);
            }
        	
        	ViewHolder holder = (ViewHolder) convertView.getTag();
        	
            // Configure the view for this message
            Message m = getItem(position);
            
            // Set the value that will be displayed in the 'to address' field
            String toAddressString = m.getToAddress();
            
            // Declare the colour variables
            int color;
            int r;
            int g;
            int b;
            
            // Check if we have an entry for this address in our address book. If we do, substitute the label of that entry for the address.
            AddressBookRecordProvider addBookProv = AddressBookRecordProvider.get(getApplicationContext());
			ArrayList<AddressBookRecord> retrievedRecords = addBookProv.searchAddressBookRecords(AddressBookRecordsTable.COLUMN_ADDRESS, toAddressString);
			
			if (retrievedRecords.size() > 0)
			{
				holder.toAddressTextView.setText(retrievedRecords.get(0).getLabel());
				holder.toAddressTextView.setTextSize(14);
				
				r = retrievedRecords.get(0).getColourR();
				g = retrievedRecords.get(0).getColourG();
				b = retrievedRecords.get(0).getColourB();
			}
			else
			{
				holder.toAddressTextView.setText(toAddressString);
				holder.toAddressTextView.setTextSize(12);

	            int[] colourValues = ColourCalculator.calculateColoursFromAddress(toAddressString);
				r = colourValues[0];
				g = colourValues[1];
				b = colourValues[2];
			}
			
			// Set the colours for this view
			color = Color.argb(0, r, g, b);
			holder.subjectTextView.setBackgroundColor(color);
            holder.toAddressTextView.setBackgroundColor(color);
            holder.dateTextView.setBackgroundColor(color);
            convertView.setBackgroundColor(Color.argb(SENT_COLOURS_ALPHA_VALUE, r, g, b));
			
			// Set the value that will be displayed in the 'date' field
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
			
			// Set the value that will be displayed in the 'subject' field 
            if (m.getSubject() == null)
            {
            	holder.subjectTextView.setText("[No subject]");
            }
            
            else
            {
            	holder.subjectTextView.setText(m.getSubject());
            }
            
            // Set the value that will be displayed in the 'status' field
            holder.statusTextView.setText(m.getStatus());
            
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
                    Log.i(TAG, "Sent list item clicked");
             	    
                    // Start the SentMessageActivity
                    Intent i = new Intent(getBaseContext(), SentMessageActivity.class);
                    i.putExtra(SentMessageActivity.EXTRA_MESSAGE_ID, selectedMessage.getId());
                    i.putExtra(SentMessageActivity.EXTRA_COLOUR_R, selectedColorR);
                    i.putExtra(SentMessageActivity.EXTRA_COLOUR_G, selectedColorG);
                    i.putExtra(SentMessageActivity.EXTRA_COLOUR_B, selectedColorB);
                    startActivityForResult(i, 0);
                }
            });
			
			return convertView;
        }
    }
}