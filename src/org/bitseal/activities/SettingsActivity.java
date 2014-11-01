package org.bitseal.activities;

import java.util.Timer;
import java.util.TimerTask;

import org.bitseal.R;
import org.bitseal.services.BackgroundService;
import org.bitseal.util.TimeUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The Activity class for the app's Settings screen. 
 * 
 * @author Jonathan Coe
 */
public class SettingsActivity extends Activity
{	   
    private Button mServerSettingsButton;
    private Button mImportOrExportButton;
    private Button mRestartBackgroundServiceButton;
    
    private TextView mSectionSeparatorServer;
    private TextView mSectionSeparatorData;
    private TextView mSectionSeparatorOther;
    
    private TextView mTimeBehindNetworkTextView;
    
    private CheckBox mShowSettingsCheckbox;
    
    private static final String KEY_SHOW_SETTINGS = "showSettings";
    
    private static final long UPDATE_FREQUENCY_MILLISECONDS = 1000;
    
    private static final String TAG = "SETTINGS_ACTIVITY";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		
		mSectionSeparatorServer = (TextView) findViewById(R.id.settings_server_section_separator);
		mSectionSeparatorData = (TextView) findViewById(R.id.settings_data_section_separator);
		mSectionSeparatorOther = (TextView) findViewById(R.id.settings_other_section_separator);
		
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
		
		mServerSettingsButton = (Button) findViewById(R.id.settings_server_settings_button);
		mServerSettingsButton.setOnClickListener(new View.OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Server settings button clicked");
				
		        Intent i = new Intent(getBaseContext(), ServersActivity.class);
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
	    		    
	    			mServerSettingsButton.setVisibility(View.VISIBLE);
	    			mImportOrExportButton.setVisibility(View.VISIBLE);
	    			mRestartBackgroundServiceButton.setVisibility(View.VISIBLE);
	    			mSectionSeparatorServer.setVisibility(View.VISIBLE);
	    			mSectionSeparatorData.setVisibility(View.VISIBLE);
	    			mSectionSeparatorOther.setVisibility(View.VISIBLE);
	    			mTimeBehindNetworkTextView.setVisibility(View.VISIBLE);
	            } 
	            else 
	            {
	            	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	            	SharedPreferences.Editor editor = prefs.edit();
	    		    editor.putBoolean(KEY_SHOW_SETTINGS, false);
	    		    editor.commit();
	            	
	    			mServerSettingsButton.setVisibility(View.GONE);
	    			mImportOrExportButton.setVisibility(View.GONE);
	    			mRestartBackgroundServiceButton.setVisibility(View.GONE);
	    			mSectionSeparatorServer.setVisibility(View.GONE);
	    			mSectionSeparatorData.setVisibility(View.GONE);
	    			mSectionSeparatorOther.setVisibility(View.GONE);
	    			mTimeBehindNetworkTextView.setVisibility(View.GONE);
	            }
        	}
        });
		
		mRestartBackgroundServiceButton = (Button) findViewById(R.id.settings_restart_background_service_button);
		mRestartBackgroundServiceButton.setOnClickListener(new View.OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{
				Toast.makeText(getApplicationContext(), "Background Service queued for restart", Toast.LENGTH_LONG).show();
				
				// Start the BackgroundService for the first time
				Context context = getApplicationContext();
				Intent firstStartIntent = new Intent(context, BackgroundService.class);
				firstStartIntent.putExtra(BackgroundService.PERIODIC_BACKGROUND_PROCESSING_REQUEST, BackgroundService.BACKGROUND_PROCESSING_REQUEST);
				context.startService(firstStartIntent);
			}
		});
		
		// Read the Shared Preferences to determine whether or not the settings should be visible
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		boolean showSettings = prefs.getBoolean(KEY_SHOW_SETTINGS, false);
		Log.i(TAG, "Show settings is set to " + showSettings);
		if (showSettings == true)
		{
			mServerSettingsButton.setVisibility(View.VISIBLE);
			mImportOrExportButton.setVisibility(View.VISIBLE);
			mRestartBackgroundServiceButton.setVisibility(View.VISIBLE);
			mSectionSeparatorServer.setVisibility(View.VISIBLE);
			mSectionSeparatorData.setVisibility(View.VISIBLE);
			mSectionSeparatorOther.setVisibility(View.VISIBLE);
			mTimeBehindNetworkTextView.setVisibility(View.VISIBLE);
			
			mShowSettingsCheckbox.setChecked(true);
		}
		else
		{		
			mServerSettingsButton.setVisibility(View.GONE);
			mImportOrExportButton.setVisibility(View.GONE);
			mRestartBackgroundServiceButton.setVisibility(View.GONE);
			mSectionSeparatorServer.setVisibility(View.GONE);
			mSectionSeparatorData.setVisibility(View.GONE);
			mSectionSeparatorOther.setVisibility(View.GONE);
			mTimeBehindNetworkTextView.setVisibility(View.GONE);
			
			mShowSettingsCheckbox.setChecked(false);
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
}