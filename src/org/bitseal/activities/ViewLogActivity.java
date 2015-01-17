package org.bitseal.activities;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.bitseal.R;
import org.bitseal.services.AppLockHandler;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
 * The Activity class for the app's View Log screen. 
 * 
 * @author Jonathan Coe
 */
public class ViewLogActivity extends ListActivity implements ICacheWordSubscriber
{   
    /** A set of log tags that we want filtered out of the log */
	private static final String[] UNWANTED_LOG_TAGS = {"art", "Choreographer", "dalvikvm", "qdmemalloc", "System", "InputMethodManager", "CacheWordService", "CacheWordBinder"};
    
    /** The frequency in milliseconds by which we will update the log view */
    private static final long UPDATE_FREQUENCY_MILLISECONDS = 1500;
    
    /** The maximum number of lines that we will read from logcat's output */
    private static final int LOGCAT_MAXIMUM_LINES = 2000;
    
    /** The maximum number of log items to be displayed in the log view */
    private static final int LOG_MAXIMUM_ITEMS = 50;
	
	/** The key for a boolean variable that records whether or not a user-defined database encryption passphrase has been saved */
    private static final String KEY_DATABASE_PASSPHRASE_SAVED = "databasePassphraseSaved"; 
    
    private static final String LOG_LEVEL_INFORMATION = "I";
    private static final String LOG_LEVEL_DEBUGGING = "D";
    private static final String LOG_LEVEL_ERROR = "E";
    
    private CacheWordHandler mCacheWordHandler;
        
    private ListView mLogListView;
    private LogAdapter mLogAdapter;    
    private ArrayList<String> mLogItems;
    
    private TimerTask refreshListTask;
    
    private String mLastLine;
    
    private int mProcessID;
    
    private static final String TAG = "VIEW_LOG_ACTIVITY";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_log);
		
        // Check whether the user has set a database encryption passphrase
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false))
		{
			// Connect to the CacheWordService
			mCacheWordHandler = new CacheWordHandler(this);
			mCacheWordHandler.connectToService();
		}
		
		// Get Bitseal's current process ID
		mProcessID = android.os.Process.myPid();
		
		// Populate a ListView with Bitseal's log output
        mLogListView = (ListView)findViewById(android.R.id.list);
        updateListView();
	}
	
	@Override
	protected void onPause() 
	{
		super.onPause();
		
		refreshListTask.cancel();
	}
	
	protected void onResume() 
	{
		super.onResume();
		
		// Check for new log lines regularly and update the ListView if any are found
        refreshListTask = new TimerTask()
	    {
	        @Override
	        public void run() 
	        {
	            runOnUiThread(new Runnable()
	            {
	                public void run() 
	                {
	                	if (checkForNewLines())
	                	{
	                		updateListView();
	                	}
	                }
	            });
	        }
	    };
	    new Timer().schedule(refreshListTask, 0, UPDATE_FREQUENCY_MILLISECONDS);
	}
	
	/**
	 * Checks whether there are new log lines to be displayed
	 */
	private boolean checkForNewLines()
	{
		try 
		{
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
                        
            // Check whether the last line of the log output is new
            String newLastLine = "";
            while ((line = bufferedReader.readLine()) != null)
            {
            	// Get the most recent line of the output we want to display
            	if (filterLogLine(line))
            	{
            		newLastLine = line;
            	}
            }
            
            // If the last line is new (i.e. there is new log output), refresh the displayed text
        	if (newLastLine.equals(mLastLine) == false)
        	{
        		return true;
        	}
        	else
        	{
        		return false;
        	}
		}
		catch (Exception e)
		{
			Log.e(TAG, "Exception ocurred in ViewLogActivity.checkForNewLines. The exception message was:\n"
					+ e.getMessage());
			return false;
        }
	}
	
	/**
	 * Returns the an ArrayList<String> containing the log items
	 * which should be displayed
	 */
	private ArrayList<String> getLogLines()
	{
		try
		{
			// Get the logcat output and prepare to read it
			Process process = Runtime.getRuntime().exec("logcat -d");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			ArrayList<String> logLines = new ArrayList<String>();
			String line = "";
			
			// Count the number of lines from the logcat output
            int lines = 0;
            while ((bufferedReader.readLine()) != null)
            {
            	lines ++;
            }
            
            // Create a new BufferedReader so we can read from the start
            process = Runtime.getRuntime().exec("logcat -d");
    		bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            // If there are more lines than we are prepared to process, only process the most recent ones
            int startPoint = lines - LOGCAT_MAXIMUM_LINES;
            if (lines > LOGCAT_MAXIMUM_LINES)
            {
            	// Skip through lines until we are at the correct start point
            	for (int i = 0; i < startPoint && bufferedReader.ready(); bufferedReader.readLine()) { }
            }
            
            // Read the selected lines
        	while ((line = bufferedReader.readLine()) != null)
	        {
	            // Filter log output by Bitseal's current process number and by removing unwanted lines
	        	if (filterLogLine(line))
	        	{
	        		logLines.add(line);
	            }
	        }
            	
            // If there are no lines to read, return a placeholder message
            if (logLines.size() == 0)
            {
            	logLines.add(getResources().getString(R.string.activity_view_log_placeholder_message));
            }
            else
            {       
    	        // Record the last read line
    	        mLastLine = logLines.get(logLines.size() - 1);
    	        
    	        // If the log text is over the maximum number of items, shorten it
    	        if (logLines.size() > LOG_MAXIMUM_ITEMS)
    	        {
    	        	logLines = new ArrayList<String>(logLines.subList(logLines.size() - LOG_MAXIMUM_ITEMS, logLines.size()));
    	        }
            }
	        
	        return logLines;
		}
		catch (Exception e)
		{
			Log.e(TAG, "Exception ocurred in ViewLogActivity.getLogLines(). The exception message was:\n"
					+ e.getMessage());
			ArrayList<String> placeholderList = new ArrayList<String>();
			placeholderList.add(getResources().getString(R.string.activity_view_log_placeholder_message));
			return placeholderList;
	    }
	}
	
	/**
	 * Filters a log line, returning whether or not is should be included
	 * in the displayed output
	 */
	private boolean filterLogLine(String line)
	{
		// Filter log output by Bitseal's current process number
    	if (line.contains(String.valueOf(mProcessID)))
        {
            // Filter out unwanted log messages by their log tag
    		for (String s : UNWANTED_LOG_TAGS)
            {
            	if (line.contains(s))
            	{
            		return false;
            	}
            }
    		return true;
        }
    	else
    	{
    		return false;
    	}
	}
	
	/** 
	 * Updates the log ListView
	 **/
    private void updateListView()
    {
    	// Get the log lines to display
    	mLogItems = getLogLines();
		
		// Save ListView state so that we can resume at the same scroll position
		Parcelable state = mLogListView.onSaveInstanceState();
		
		// Re-instantiate the ListView and re-populate it
        mLogListView = new ListView(this);
        mLogListView = (ListView)findViewById(android.R.id.list);
        mLogAdapter = new LogAdapter(mLogItems);
		mLogListView.setAdapter(mLogAdapter);

		// Restore previous state (including selected item index and scroll position)
		mLogListView.onRestoreInstanceState(state);
		
		try
		{
			// If the user has scrolled to the bottom of the ListView, keep scrolling to the
			// bottom as new items are added
			if (mLogListView.getLastVisiblePosition() == mLogListView.getAdapter().getCount() -1 &&
					mLogListView.getChildAt(mLogListView.getChildCount() - 1).getBottom() <= mLogListView.getHeight())
			{
				scrollMyListViewToBottom();
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "Exception ocurred in ViewLogActivity.updateListView(). The exception message was:\n"
					+ e.getMessage());
		}
    }
    
    private void scrollMyListViewToBottom()
    {
    	mLogListView.post(new Runnable()
        {
            @Override
            public void run()
            {
                // Select the last row so it will scroll into view
            	mLogListView.setSelection(mLogAdapter.getCount() - 1);
            }
        });
    }
	
	/**
	 * A ViewHolder used to speed up this activity's ListView.
	 */
    static class ViewHolder 
    {
	    public TextView logLineTextView;
	}
	    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        ((LogAdapter)mLogListView.getAdapter()).notifyDataSetChanged();
    }
    
    private class LogAdapter extends ArrayAdapter<String> 
    {
        public LogAdapter(ArrayList<String> logLines) 
        {
            super(getBaseContext(), android.R.layout.simple_list_item_1, logLines);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) 
        {
        	// If we weren't given a view that can be recycled, inflate a new one
        	if (convertView == null)
            {
        		convertView = getLayoutInflater().inflate(R.layout.list_item_log, parent, false);
        		
        	    // Configure the view holder
        	    ViewHolder viewHolder = new ViewHolder();
        	    viewHolder.logLineTextView = (TextView) convertView.findViewById(R.id.view_log_line_textview);
        	    convertView.setTag(viewHolder);
            }
        	
        	ViewHolder holder = (ViewHolder) convertView.getTag();
        	
            // Get the log line
            final String logLine = getItem(position);
            holder.logLineTextView.setText(logLine);
           
        	// Set the text colour for this view
			if (logLine.startsWith(LOG_LEVEL_INFORMATION))
			{
				holder.logLineTextView.setTextColor(getResources().getColor(R.color.dark_green));
			}
			else if (logLine.startsWith(LOG_LEVEL_DEBUGGING))
			{
				holder.logLineTextView.setTextColor(Color.BLUE);
			}
			else if (logLine.startsWith(LOG_LEVEL_ERROR))
			{
				holder.logLineTextView.setTextColor(Color.RED);
			}
			
			convertView.setOnLongClickListener(new View.OnLongClickListener()
			{
                @SuppressWarnings("deprecation")
				@SuppressLint("NewApi")
				@Override
                public boolean onLongClick(View v) 
                {
                    Log.i(TAG, "Log item long clicked");
             	    
                    // Copy the error to the clipboard
    				int sdk = android.os.Build.VERSION.SDK_INT;
    				
    				if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) 
    				{
    				    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    				    clipboard.setText(logLine);
    				} 
    				
    				else 
    				{
    				    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
    				    android.content.ClipData clip = android.content.ClipData.newPlainText("COPIED_MESSAGE_TEXT", logLine);
    				    clipboard.setPrimaryClip(clip);
    				}
    				
    				Toast.makeText(getApplicationContext(), R.string.activity_view_log_item_copied_toast, Toast.LENGTH_LONG).show();
    				
    				// Indicate that we don't want any further processing
    				return true;
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