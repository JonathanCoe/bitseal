package org.bitseal.activities;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import org.bitseal.R;
import org.bitseal.core.AddressProcessor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
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
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

/**
 * The Activity class for the app's 'Import or Export' screen. 
 * 
 * @author Jonathan Coe
 */
public class ImportOrExportActivity extends Activity implements ICacheWordSubscriber
{
    private Button mImportAddressButton;
    private Button mExportAddressButton;
    
    private String privateSigningKey;
    private String privateEncryptionKey;
    
    private boolean privateSigningKeyScan;
    private boolean privateEncryptionKeyScan;
    
    /** The key for a boolean variable that records whether or not a user-defined database encryption passphrase has been saved */
    private static final String KEY_DATABASE_PASSPHRASE_SAVED = "databasePassphraseSaved"; 
    
    private CacheWordHandler mCacheWordHandler;
	
    private static final String TAG = "IMPORT_OR_EXPORT_ACTIVITY";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_import_or_export);
		
        // Check whether the user has set a database encryption passphrase
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false))
		{
			// Connect to the CacheWordService
			mCacheWordHandler = new CacheWordHandler(this);
			mCacheWordHandler.connectToService();
		}
		
		mImportAddressButton = (Button) findViewById(R.id.import_or_export_import_address_button);
		mImportAddressButton.setOnClickListener(new View.OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Import address button clicked");
				
				openImportDialog();
			}
		});
		mExportAddressButton = (Button) findViewById(R.id.import_or_export_export_address_button);
		mExportAddressButton.setOnClickListener(new View.OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Export address button clicked");
				
		        Intent i = new Intent(getBaseContext(), ExportAddressesActivity.class);
		        startActivityForResult(i, 0);
			}
		});
	}
	
	private void openImportDialog()
	{
		// Open the import dialog
		final Dialog importAddressDialog = new Dialog(ImportOrExportActivity.this);
		ScrollView dialogLayout = (ScrollView) View.inflate(ImportOrExportActivity.this, R.layout.dialog_import_address, null);
		importAddressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		importAddressDialog.setContentView(dialogLayout);
		
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
	    lp.copyFrom(importAddressDialog.getWindow().getAttributes());
	    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
	    lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
		
	    importAddressDialog.show();
	    importAddressDialog.getWindow().setAttributes(lp);
	    importAddressDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	    
	    final EditText privateSigningKeyEditText = (EditText) dialogLayout.findViewById(R.id.import_address_dialog_private_signing_key_edittext);
	    final EditText privateEncryptionKeyEditText = (EditText) dialogLayout.findViewById(R.id.import_address_dialog_private_encryption_key_edittext);
	    
	    if (privateSigningKey != null)
	    {
	    	privateSigningKeyEditText.setText(privateSigningKey);
	    }
	    if (privateEncryptionKey != null)
	    {
	    	privateEncryptionKeyEditText.setText(privateEncryptionKey);
	    }
	    
        // Show soft keyboard when the private signing key Edit Text gains focus
	    privateSigningKeyEditText.setOnFocusChangeListener(new OnFocusChangeListener()
	    {
            @Override
            public void onFocusChange(View v, boolean hasFocus) 
            {
            	privateSigningKeyEditText.post(new Runnable() 
            	{
                    @Override
                    public void run()
                    {
                        InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(privateSigningKeyEditText, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            }
        });
	    
	    // Show soft keyboard when the private encryption key Edit Text gains focus
	    privateEncryptionKeyEditText.setOnFocusChangeListener(new OnFocusChangeListener()
	    {
            @Override
            public void onFocusChange(View v, boolean hasFocus) 
            {
            	privateEncryptionKeyEditText.post(new Runnable() 
            	{
                    @Override
                    public void run()
                    {
                        InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(privateEncryptionKeyEditText, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            }
        });
	    
	    Button privateSigningKeyScanButton = (Button) dialogLayout.findViewById(R.id.import_address_dialog_private_signing_key_scan_qr_code_button);
		privateSigningKeyScanButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Private signing key scan button pressed");
				
				importAddressDialog.cancel();
				
				privateSigningKeyScan = true;
				
				IntentIntegrator integrator = new IntentIntegrator(ImportOrExportActivity.this);
                integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
			}
		});
		
	    Button privateEncryptionKeyScanButton = (Button) dialogLayout.findViewById(R.id.import_address_dialog_private_encryption_key_scan_qr_code_button);
		privateEncryptionKeyScanButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Private encryption key scan button pressed");
				
				importAddressDialog.cancel();
				
				privateEncryptionKeyScan = true;
				
				IntentIntegrator integrator = new IntentIntegrator(ImportOrExportActivity.this);
                integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
			}
		});
		
	    Button importButton = (Button) dialogLayout.findViewById(R.id.import_address_dialog_import_button);
		importButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Import button pressed");
				
				boolean importSuccessful = new AddressProcessor().importAddress(privateSigningKey, privateEncryptionKey);
				
				if (importSuccessful)
				{
					Toast.makeText(getApplicationContext(), R.string.import_or_export_toast_imported_successfully, Toast.LENGTH_LONG).show();
					importAddressDialog.cancel();
				}
				else
				{
					Toast.makeText(getApplicationContext(), R.string.import_or_export_toast_import_failed, Toast.LENGTH_LONG).show();
				}
			}
		});
	}
	
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) 
    {	
		// Get the result of the QR code scan
       IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
       if (result != null)
       {
	       String contents = result.getContents();
	       if (contents != null)
	       {
	    	   contents = contents.trim(); // Remove leading or trailing spaces
	       		
	    	   if (new AddressProcessor().validatePrivateKey(contents))
	    	   {
	    		   if (privateSigningKeyScan)
	    		   {
	    			   privateSigningKey = contents;
	    		   }
	    		   else if (privateEncryptionKeyScan)
	    		   {
	    			   privateEncryptionKey = contents;
	    		   }
	    		   
	    		   openImportDialog();
	    	   }
	    	   else
	    	   {
	    		   Toast.makeText(getApplicationContext(), R.string.import_or_export_toast_scanned_code_invalid, Toast.LENGTH_LONG).show();
	    		   openImportDialog();
	    	   }
	       }
	       else 
	       {
	       		Log.i(TAG, "No QRcode found");
	       		openImportDialog();
	       }
       }
       
       // Reset the boolean values that allow us to detect which key type we have been scanning for
		privateSigningKeyScan = false;
		privateEncryptionKeyScan = false;
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
 		    	// Lock the database
 		    	mCacheWordHandler.lock();
 		    	
 		    	// Open the lock screen activity
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