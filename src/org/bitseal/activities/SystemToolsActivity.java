package org.bitseal.activities;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import org.bitseal.R;
import org.bitseal.services.AppLockHandler;
import org.bitseal.services.BackgroundService;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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
import android.widget.TextView;
import android.widget.Toast;

/**
 * The Activity class for the app's System Tools screen. 
 * 
 * @author Jonathan Coe
 */
public class SystemToolsActivity extends Activity implements ICacheWordSubscriber
{	   
	private Button mViewLogButton;
	private Button mViewExceptionsButton;
    private Button mRestartBackgroundServiceButton;
    
    private TextView mAppVersionTextView;
    
    /** The key for a boolean variable that records whether or not a user-defined database encryption passphrase has been saved */
    private static final String KEY_DATABASE_PASSPHRASE_SAVED = "databasePassphraseSaved"; 
    
    private CacheWordHandler mCacheWordHandler;
    
    private static final String TAG = "SYSTEM_TOOLS_ACTIVITY";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_system_tools);
		
        // Check whether the user has set a database encryption passphrase
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false))
		{
			// Connect to the CacheWordService
			mCacheWordHandler = new CacheWordHandler(this);
			mCacheWordHandler.connectToService();
		}
		
	    mViewLogButton = (Button) findViewById(R.id.system_tools_view_log_button);
	    mViewLogButton.setOnClickListener(new View.OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "System tools view log button clicked");
				
		        Intent i = new Intent(getBaseContext(), ViewLogActivity.class);
		        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		        startActivityForResult(i, 0);
			}
		});
	    
		mViewExceptionsButton = (Button) findViewById(R.id.system_tools_view_exceptions_button);
		mViewExceptionsButton.setOnClickListener(new View.OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "System tools view exceptions button clicked");
				
		        Intent i = new Intent(getBaseContext(), ViewErrorsActivity.class);
		        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		        startActivityForResult(i, 0);
			}
		});
		
		mRestartBackgroundServiceButton = (Button) findViewById(R.id.system_tools_restart_background_service_button);
		mRestartBackgroundServiceButton.setOnClickListener(new View.OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{
				Toast.makeText(getApplicationContext(), R.string.system_tools_toast_background_service_restart, Toast.LENGTH_LONG).show();
				
				// Start the BackgroundService
				Context context = getApplicationContext();
				Intent firstStartIntent = new Intent(context, BackgroundService.class);
				firstStartIntent.putExtra(BackgroundService.PERIODIC_BACKGROUND_PROCESSING_REQUEST, BackgroundService.BACKGROUND_PROCESSING_REQUEST);
				BackgroundService.sendWakefulWork(context, firstStartIntent);
			}
		});
		
		try
		{
			mAppVersionTextView = (TextView) findViewById(R.id.system_tools_app_version_textview);
			String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			String appName = getResources().getString(R.string.app_name);
			mAppVersionTextView.setText(appName + " " + versionName);
		}
		catch (Exception e)
		{
			Log.e(TAG, "Execption occurred while trying to set up mAppVersionTextView in SystemToolsActivity.onCreate()");
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
 		        intent1.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
 		        startActivity(intent1);
 		        break;
 		        
 		    case R.id.menu_item_sent:
 		        Intent intent2 = new Intent(this, SentActivity.class);
 		        intent2.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
 		        startActivity(intent2);
 		        break;  
 		        
 		    case R.id.menu_item_compose:
 		        Intent intent3 = new Intent(this, ComposeActivity.class);
 		        intent3.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
 		        startActivity(intent3);
 		        break;
 		        
 		    case R.id.menu_item_identities:
 		        Intent intent4 = new Intent(this, IdentitiesActivity.class);
 		        intent4.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
 		        startActivity(intent4);
 		        break;
 		        
 		    case R.id.menu_item_addressBook:
 		        Intent intent5 = new Intent(this, AddressBookActivity.class);
 		        intent5.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
 		        startActivity(intent5);
 		        break;
 		        
 		    case R.id.menu_item_settings:
 		        Intent intent6 = new Intent(this, SettingsActivity.class);
 		        intent6.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
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