package org.bitseal.activities;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import java.util.ArrayList;

import org.bitseal.R;
import org.bitseal.data.Address;
import org.bitseal.database.AddressProvider;
import org.bitseal.services.AppLockHandler;
import org.bitseal.util.ColourCalculator;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.ListActivity;
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
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;

/**
 * The Activity class for the app's "Export Addresses" screen. 
 * 
 * @author Jonathan Coe
 */
public class ExportAddressesActivity extends ListActivity implements ICacheWordSubscriber
{
	private ArrayList<Address> mAddresses;
	
    private ListView mAddressListView;
	
	private TextView mListItemLabelTextView;
	private TextView mListItemAddressTextView;
	
	private static final int EXPORT_ADDRESSES_COLOURS_ALPHA_VALUE = 70;
	
    /** The key for a boolean variable that records whether or not a user-defined database encryption passphrase has been saved */
    private static final String KEY_DATABASE_PASSPHRASE_SAVED = "databasePassphraseSaved"; 
    
    private CacheWordHandler mCacheWordHandler;
	
    private static final String TAG = "EXPORT_ADDRESSES_ACTIVITY";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_export_addresses);
		
        // Check whether the user has set a database encryption passphrase
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false))
		{
			// Connect to the CacheWordService
			mCacheWordHandler = new CacheWordHandler(this);
			mCacheWordHandler.connectToService();
		}
		
		// Get all Addresses from the application's database
		AddressProvider addProv = AddressProvider.get(getApplicationContext());
		addProv = AddressProvider.get(getApplicationContext());
		mAddresses = addProv.getAllAddresses();
		
		// Set up the ListView with data from the AddressAdapter
		AddressAdapter adapter = new AddressAdapter(mAddresses);
		mAddressListView = new ListView(this);
		mAddressListView = (ListView)findViewById(android.R.id.list);          
        setListAdapter(adapter);
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();
	}
	  
	@Override
	protected void onPause() 
	{
		super.onPause();
	}
	
	public void onListItemClick(ListView l, View v, int position, long id)
    {
		Log.i(TAG, "Identities first delete dialog confirm button pressed");
		
		// Get the Address selected from the adapter
		final Address listAddress = ((AddressAdapter)mAddressListView.getAdapter()).getItem(position);
		
		// Open the warning dialog for confirmation
		final Dialog warningDialog = new Dialog(ExportAddressesActivity.this);
		LinearLayout dialogLayout = (LinearLayout) View.inflate(ExportAddressesActivity.this, R.layout.dialog_export_addresses_warning, null);
		warningDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		warningDialog.setContentView(dialogLayout);
		
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
	    lp.copyFrom(warningDialog.getWindow().getAttributes());
	    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
		
	    warningDialog.show();
	    warningDialog.getWindow().setAttributes(lp);		  
	    
	    Button warningConfirmButton = (Button) dialogLayout.findViewById(R.id.export_addresses_warning_dialog_confirm_button);
	    warningConfirmButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Export addresses warning confirm button pressed");
				
				warningDialog.dismiss();
				
		        // Open a dialog to enter the data for the selected address
				final Dialog listItemDialog = new Dialog(ExportAddressesActivity.this);
				ScrollView dialogLayout = (ScrollView) View.inflate(ExportAddressesActivity.this, R.layout.dialog_export_addresses_list_item_options, null);
				listItemDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				listItemDialog.setContentView(dialogLayout);
				
				WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
			    lp.copyFrom(listItemDialog.getWindow().getAttributes());
			    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
			    lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
				
			    listItemDialog.show();
			    listItemDialog.getWindow().setAttributes(lp);
			    
			    final EditText listItemDialogLabelEditText = (EditText) dialogLayout.findViewById(R.id.export_addresses_dialog_label_edittext);
			    final EditText listItemDialogAddressEditText = (EditText) dialogLayout.findViewById(R.id.export_addresses_dialog_address_edittext);
			    final EditText listItemDialogPrivateSigningKeyEditText = (EditText) dialogLayout.findViewById(R.id.export_addresses_dialog_private_signing_key_edittext);
			    final EditText listItemDialogPrivateEncryptionKeyEditText = (EditText) dialogLayout.findViewById(R.id.export_addresses_dialog_private_encryption_key_edittext);
			    
			    // Set the text of the two EditTexts in the dialog
			    listItemDialogLabelEditText.setText(listAddress.getLabel());
			    listItemDialogAddressEditText.setText(listAddress.getAddress());
			    listItemDialogPrivateSigningKeyEditText.setText(listAddress.getPrivateSigningKey());
			    listItemDialogPrivateEncryptionKeyEditText.setText(listAddress.getPrivateEncryptionKey());
			    
			    // Set the position of the cursor in each EditText to the end of the text
			    listItemDialogLabelEditText.setSelection(listItemDialogLabelEditText.getText().length());
			    listItemDialogAddressEditText.setSelection(listItemDialogAddressEditText.getText().length());
			    listItemDialogPrivateSigningKeyEditText.setSelection(listItemDialogPrivateSigningKeyEditText.getText().length());
			    listItemDialogPrivateEncryptionKeyEditText.setSelection(listItemDialogPrivateEncryptionKeyEditText.getText().length());
			    
		        // Show soft keyboard when the Label Edit Text gains focus
			    listItemDialogLabelEditText.setOnFocusChangeListener(new OnFocusChangeListener() 
			    {
		            @Override
		            public void onFocusChange(View v, boolean hasFocus) 
		            {
		            	listItemDialogLabelEditText.post(new Runnable() 
		            	{
		                    @Override
		                    public void run() {
		                        InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		                        imm.showSoftInput(listItemDialogLabelEditText, InputMethodManager.SHOW_IMPLICIT);
		                    }
		                });
		            }
		        });
			    
			    // Show soft keyboard when the Address Edit Text gains focus
			    listItemDialogAddressEditText.setOnFocusChangeListener(new OnFocusChangeListener() 
			    {
		            @Override
		            public void onFocusChange(View v, boolean hasFocus) 
		            {
		            	listItemDialogAddressEditText.post(new Runnable() 
		            	{
		                    @Override
		                    public void run() {
		                        InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		                        imm.showSoftInput(listItemDialogAddressEditText, InputMethodManager.SHOW_IMPLICIT);
		                    }
		                });
		            }
		        });
			    
			    // Show soft keyboard when the Private Signing Key Edit Text gains focus
			    listItemDialogPrivateSigningKeyEditText.setOnFocusChangeListener(new OnFocusChangeListener() 
			    {
		            @Override
		            public void onFocusChange(View v, boolean hasFocus) 
		            {
		            	listItemDialogPrivateSigningKeyEditText.post(new Runnable() 
		            	{
		                    @Override
		                    public void run() {
		                        InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		                        imm.showSoftInput(listItemDialogAddressEditText, InputMethodManager.SHOW_IMPLICIT);
		                    }
		                });
		            }
		        });
			    
			    // Show soft keyboard when the Private Encryption Key Edit Text gains focus
			    listItemDialogPrivateEncryptionKeyEditText.setOnFocusChangeListener(new OnFocusChangeListener() 
			    {
		            @Override
		            public void onFocusChange(View v, boolean hasFocus) 
		            {
		            	listItemDialogPrivateEncryptionKeyEditText.post(new Runnable() 
		            	{
		                    @Override
		                    public void run() {
		                        InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		                        imm.showSoftInput(listItemDialogPrivateEncryptionKeyEditText, InputMethodManager.SHOW_IMPLICIT);
		                    }
		                });
		            }
		        });
				
			    // Buttons for the Address
			    Button showQRCodeButton = (Button) dialogLayout.findViewById(R.id.export_addresses_dialog_address_show_qr_code_button);
			    showQRCodeButton.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						Log.i(TAG, "List item dialog address show qr code button pressed");							
						
						IntentIntegrator integrator = new IntentIntegrator(ExportAddressesActivity.this);
		                integrator.shareText(listAddress.getAddress());
					}
				});
			    Button copyButton = (Button) dialogLayout.findViewById(R.id.export_addresses_address_copy_button);
			    copyButton.setOnClickListener(new View.OnClickListener()
				{
					@SuppressWarnings("deprecation")
					@TargetApi(Build.VERSION_CODES.HONEYCOMB)
					@Override
					public void onClick(View v)
					{
						Log.i(TAG, "List item dialog address copy button pressed");							
						
						listAddress.getAddress();
						
						int sdk = android.os.Build.VERSION.SDK_INT;
						
						if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) 
						{
						    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						    clipboard.setText(listAddress.getAddress());
						} 
						
						else 
						{
						    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
						    android.content.ClipData clip = android.content.ClipData.newPlainText("COPIED_ADDRESS", listAddress.getAddress());
						    clipboard.setPrimaryClip(clip);
						}
						
						Toast.makeText(getApplicationContext(), R.string.export_addresses_toast_address_copied, Toast.LENGTH_LONG).show();
					}
				});
			    
			    // Buttons for the Private Signing Key
			    Button privateSigningKeyShowQRCodeButton = (Button) dialogLayout.findViewById(R.id.export_addresses_dialog_private_signing_key_show_qr_code_button);
			    privateSigningKeyShowQRCodeButton.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						Log.i(TAG, "List item dialog private signing key show qr code button pressed");							
						
						IntentIntegrator integrator = new IntentIntegrator(ExportAddressesActivity.this);
		                integrator.shareText(listAddress.getPrivateSigningKey());
					}
				});
			    Button privateSigningKeyCopyButton = (Button) dialogLayout.findViewById(R.id.export_addresses_private_signing_key_copy_button);
			    privateSigningKeyCopyButton.setOnClickListener(new View.OnClickListener()
				{
					@SuppressWarnings("deprecation")
					@TargetApi(Build.VERSION_CODES.HONEYCOMB)
					@Override
					public void onClick(View v)
					{
						Log.i(TAG, "List item dialog private signing key copy button pressed");							
						
						listAddress.getAddress();
						
						int sdk = android.os.Build.VERSION.SDK_INT;
						
						if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) 
						{
						    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						    clipboard.setText(listAddress.getPrivateSigningKey());
						} 
						
						else 
						{
						    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
						    android.content.ClipData clip = android.content.ClipData.newPlainText("COPIED_ADDRESS", listAddress.getPrivateSigningKey());
						    clipboard.setPrimaryClip(clip);
						}
						
						Toast.makeText(getApplicationContext(), R.string.export_addresses_toast_private_signing_key_copied, Toast.LENGTH_LONG).show();
					}
				});
			    
			    // Buttons for the Private Encryption Key
			    Button privateEncryptionKeyShowQRCodeButton = (Button) dialogLayout.findViewById(R.id.export_addresses_dialog_private_encryption_key_show_qr_code_button);
			    privateEncryptionKeyShowQRCodeButton.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						Log.i(TAG, "List item dialog private encryption key show qr code button pressed");							
						
						IntentIntegrator integrator = new IntentIntegrator(ExportAddressesActivity.this);
		                integrator.shareText(listAddress.getPrivateEncryptionKey());
					}
				});
			    Button privateEncryptionKeyCpyButton = (Button) dialogLayout.findViewById(R.id.export_addresses_dialog_private_encryption_key_copy_button);
			    privateEncryptionKeyCpyButton.setOnClickListener(new View.OnClickListener()
				{
					@SuppressWarnings("deprecation")
					@TargetApi(Build.VERSION_CODES.HONEYCOMB)
					@Override
					public void onClick(View v)
					{
						Log.i(TAG, "List item dialog private encryption key copy button pressed");							
						
						listAddress.getAddress();
						
						int sdk = android.os.Build.VERSION.SDK_INT;
						
						if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) 
						{
						    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						    clipboard.setText(listAddress.getPrivateEncryptionKey());
						} 
						
						else 
						{
						    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
						    android.content.ClipData clip = android.content.ClipData.newPlainText("COPIED_ADDRESS", listAddress.getPrivateEncryptionKey());
						    clipboard.setPrimaryClip(clip);
						}
						
						Toast.makeText(getApplicationContext(), R.string.export_addresses_toast_private_encryption_key_copied, Toast.LENGTH_LONG).show();
					}
				});
			}
		});
	    
	    Button warningCancelButton = (Button) dialogLayout.findViewById(R.id.export_addresses_warning_dialog_cancel_button);
	    warningCancelButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Export addresses warning dialog cancel button pressed");							
				
				warningDialog.dismiss();
			}
		});
     }
	 
     @Override
     public void onActivityResult(int requestCode, int resultCode, Intent data) 
     {
        ((AddressAdapter)mAddressListView.getAdapter()).notifyDataSetChanged();
     }
     
     // Controls the main ListView of the Identities Activity
     private class AddressAdapter extends ArrayAdapter<Address> 
     {
        public AddressAdapter(ArrayList<Address> addresses) 
        {
            super(getBaseContext(), android.R.layout.simple_list_item_1, addresses);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) 
        {
            // If we weren't given a view, inflate one
            if (convertView == null) 
            {
            	convertView = getLayoutInflater().inflate(R.layout.list_item_identities, parent, false);
            }

            // Configure the view for this Address
            Address a = getItem(position);

            mListItemLabelTextView = (TextView)convertView.findViewById(R.id.identities_list_item_options_label_textview);
            
            if (a.getLabel() == null)
            {
            	mListItemLabelTextView.setText("[No label]");
            }
            
            else
            {
            	mListItemLabelTextView.setText(a.getLabel());
            }
            
            mListItemAddressTextView = (TextView)convertView.findViewById(R.id.identities_list_item_options_address_textview);
            mListItemAddressTextView.setText(a.getAddress());
            
            // Set the colours for this view
            int[] colourValues = ColourCalculator.calculateColoursFromAddress(a.getAddress());  
            convertView.setBackgroundColor(Color.argb(EXPORT_ADDRESSES_COLOURS_ALPHA_VALUE, colourValues[0], colourValues[1], colourValues[2]));

            int color = Color.argb(0, colourValues[0], colourValues[1], colourValues[2]);
            mListItemLabelTextView.setBackgroundColor(color);
            mListItemAddressTextView.setBackgroundColor(color);

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