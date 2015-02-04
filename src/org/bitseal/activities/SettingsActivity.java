package org.bitseal.activities;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import java.util.Timer;
import java.util.TimerTask;

import org.bitseal.R;
import org.bitseal.services.AppLockHandler;
import org.bitseal.util.TimeUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * The Activity class for the app's Settings screen. 
 * 
 * @author Jonathan Coe
 */
public class SettingsActivity extends Activity implements ICacheWordSubscriber
{	   
	private Button mSecuritySettingsButton;
	private Button mNetworkSettingsButton;
    private Button mImportOrExportButton;
    private Button mSystemToolsButton;
    
    private TextView mTimeBehindNetworkTextView;
    
    private CheckBox mShowSettingsCheckbox;
    
    private static final String KEY_SHOW_SETTINGS = "showSettings";
    
    private static final long UPDATE_FREQUENCY_MILLISECONDS = 1000;
    
    /** The key for a boolean variable that records whether or not a user-defined database encryption passphrase has been saved */
    private static final String KEY_DATABASE_PASSPHRASE_SAVED = "databasePassphraseSaved"; 
    
    private CacheWordHandler mCacheWordHandler;
    
    private static final String TAG = "SETTINGS_ACTIVITY";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		
        // Check whether the user has set a database encryption passphrase
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false))
		{
			// Connect to the CacheWordService
			mCacheWordHandler = new CacheWordHandler(this);
			mCacheWordHandler.connectToService();
		}
		
		mTimeBehindNetworkTextView = (TextView) findViewById(R.id.settings_time_behind_network_textview);
		mTimeBehindNetworkTextView.setText(TimeUtils.getTimeBehindNetworkMessage());
		
		// Update the 'time behind network' text view regularly
	    new Timer().schedule(new TimerTask() 
	    {
	        @Override
	        public void run() 
	        {
	            runOnUiThread(new Runnable()
	            {
	                public void run() 
	                {
	                	mTimeBehindNetworkTextView.setText(TimeUtils.getTimeBehindNetworkMessage());
	                }
	            });
	        }
	    }, 0, UPDATE_FREQUENCY_MILLISECONDS);
		
	    mSecuritySettingsButton = (Button) findViewById(R.id.settings_security_settings_button);
	    mSecuritySettingsButton.setOnClickListener(new View.OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{	
				Log.i(TAG, "Security settings button clicked");
				
		        Intent i = new Intent(getBaseContext(), SecurityActivity.class);
		        startActivityForResult(i, 0);
			}
		});
	    
		mNetworkSettingsButton = (Button) findViewById(R.id.settings_network_settings_button);
		mNetworkSettingsButton.setOnClickListener(new View.OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{				
				Log.i(TAG, "Network settings button clicked");
				
		        Intent i = new Intent(getBaseContext(), NetworkSettingsActivity.class);
		        startActivityForResult(i, 0);
			}
		});
		
		mImportOrExportButton = (Button) findViewById(R.id.settings_import_or_export_button);
		mImportOrExportButton.setOnClickListener(new View.OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Import or export button clicked");
				
		        Intent i = new Intent(getBaseContext(), ImportOrExportActivity.class);
		        startActivityForResult(i, 0);
			}
		});
		
		mShowSettingsCheckbox = (CheckBox) findViewById(R.id.settings_show_settings_checkbox);
		mShowSettingsCheckbox.setOnClickListener(new View.OnClickListener() 
        {
        	@Override
			public void onClick(View v) 
        	{
	            if (mShowSettingsCheckbox.isChecked()) 
	            {
	            	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	            	SharedPreferences.Editor editor = prefs.edit();
	    		    editor.putBoolean(KEY_SHOW_SETTINGS, true);
	    		    editor.commit();
	    		    
	    		    showSettings();
	            } 
	            else 
	            {
	            	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	            	SharedPreferences.Editor editor = prefs.edit();
	    		    editor.putBoolean(KEY_SHOW_SETTINGS, false);
	    		    editor.commit();
	            	
	    		    hideSettings();
	            }
        	}
        });
		
		mSystemToolsButton = (Button) findViewById(R.id.settings_system_tools_button);
		mSystemToolsButton.setOnClickListener(new View.OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Settings system tools button clicked");
				
		        Intent i = new Intent(getBaseContext(), SystemToolsActivity.class);
		        startActivityForResult(i, 0);
			}
		});
		
		// Read the Shared Preferences to determine whether or not the settings should be visible
		boolean showSettings = prefs.getBoolean(KEY_SHOW_SETTINGS, false);
		Log.i(TAG, "Show settings is set to " + showSettings);
		if (showSettings == true)
		{
			showSettings();
			
			mShowSettingsCheckbox.setChecked(true);
		}
		else
		{		
			hideSettings();
			
			mShowSettingsCheckbox.setChecked(false);
		}
	}
	
	private void showSettings()
	{
	    mSecuritySettingsButton.setVisibility(View.VISIBLE);
		mNetworkSettingsButton.setVisibility(View.VISIBLE);
		mImportOrExportButton.setVisibility(View.VISIBLE);
		mSystemToolsButton.setVisibility(View.VISIBLE);
		mTimeBehindNetworkTextView.setVisibility(View.VISIBLE);
	}
	
	private void hideSettings()
	{
	    mSecuritySettingsButton.setVisibility(View.GONE);
		mNetworkSettingsButton.setVisibility(View.GONE);
		mImportOrExportButton.setVisibility(View.GONE);
		mSystemToolsButton.setVisibility(View.GONE);
		mTimeBehindNetworkTextView.setVisibility(View.GONE);
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
 		    	// We are already here, so there's nothing to do
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