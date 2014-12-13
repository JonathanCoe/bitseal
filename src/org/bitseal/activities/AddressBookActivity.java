package org.bitseal.activities;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import java.util.ArrayList;
import java.util.Collections;

import org.bitseal.R;
import org.bitseal.core.AddressProcessor;
import org.bitseal.data.AddressBookRecord;
import org.bitseal.database.AddressBookRecordProvider;
import org.bitseal.database.AddressBookRecordsTable;
import org.bitseal.util.ColourCalculator;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

/**
 * The Activity class for the app's Address Book. 
 * 
 * @author Jonathan Coe
 */
public class AddressBookActivity extends ListActivity implements ICacheWordSubscriber
{	
	private ArrayList<AddressBookRecord> mAddressBookRecords;
    
    private ListView mAddressBookListView;
    
    private TextView mListItemLabelTextView;
    private TextView mListItemAddressTextView;
    
    private String mNewEntryDialogLabelText = ""; // Used to save a label entered by the user before scanning an address QR code
    
    private static final String BITMESSAGE_ADDRESS_PREFIX = "BM-";
    
    private static final String ADDRESS_BOOK_FIRST_OPEN = "address_book_first_open";
    
    private static final int ADDRESS_BOOK_COLOURS_ALPHA_VALUE = 70;
    
    /** The key for a boolean variable that records whether or not a user-defined database encryption passphrase has been saved */
    private static final String KEY_DATABASE_PASSPHRASE_SAVED = "databasePassphraseSaved"; 
    
    private CacheWordHandler mCacheWordHandler;
    
    private static final String TAG = "ADDRESS_BOOK_ACTIVITY";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_address_book);
		
        // Check whether the user has set a database encryption passphrase
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false))
		{
			// Connect to the CacheWordService
			mCacheWordHandler = new CacheWordHandler(this);
			mCacheWordHandler.connectToService();
		}
		
		// Set up the data for this activity
		AddressBookRecordProvider addBookProv = AddressBookRecordProvider.get(getApplicationContext());
		mAddressBookRecords = addBookProv.getAllAddressBookRecords();
		
		// Sort the AddressBookRecords by label, in alphabetical order
		Collections.sort(mAddressBookRecords);

		// Set up this activity's view
		AddressBookRecordAdapter adapter = new AddressBookRecordAdapter(mAddressBookRecords);
        
        mAddressBookListView = new ListView(this);
        mAddressBookListView = (ListView)findViewById(android.R.id.list);
                
        setListAdapter(adapter);
		
		// If this Activity was started using one of the 'Add sender / recipient to address book' options from a message:
		Bundle extras = getIntent().getExtras();
		if (extras != null)
		{
			if (extras.containsKey(InboxMessageActivity.EXTRA_SENDER_ADDRESS))
			{
				String senderAddress = extras.getString(InboxMessageActivity.EXTRA_SENDER_ADDRESS);
				openNewEntryDialog(senderAddress);
			}
			
			else if (extras.containsKey(SentMessageActivity.EXTRA_DESTINATION_ADDRESS))
			{
				String senderAddress = extras.getString(SentMessageActivity.EXTRA_DESTINATION_ADDRESS);
				openNewEntryDialog(senderAddress);
			}
		}
		
        // Check whether this is the first time the identities activity has been opened - if so then let's create a new address for them
		if (prefs.getBoolean(ADDRESS_BOOK_FIRST_OPEN, true))
		{
			runFirstOpenRoutine();
		}
	}
	
	/**
	 * Displays a welcome message to the user
	 */
	private void runFirstOpenRoutine()
	{
	    // Set a flag in SharedPreferences so that this will not be called again
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = prefs.edit();
	    editor.putBoolean(ADDRESS_BOOK_FIRST_OPEN, false);
	    editor.commit();
		
	    for (int i = 0; i < 2; i++) // Yes, it's a hack. Come at me bro. 
	    {
	    	Toast.makeText(getApplicationContext(), R.string.addressBook_toast_welcome_message, Toast.LENGTH_LONG).show();
	    }
	}
	
	private void openNewEntryDialog(String address)
	{
		// Open a dialog to enter the data for a new address book entry
		final Dialog newEntryDialog = new Dialog(AddressBookActivity.this);
		LinearLayout dialogLayout = (LinearLayout) View.inflate(AddressBookActivity.this, R.layout.dialog_address_book_new_entry, null);
		newEntryDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		newEntryDialog.setContentView(dialogLayout);
		
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
	    lp.copyFrom(newEntryDialog.getWindow().getAttributes());
	    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
		
	    newEntryDialog.show();
	    newEntryDialog.getWindow().setAttributes(lp);
	    
	    final EditText newEntryDialogLabelEditText = (EditText) dialogLayout.findViewById(R.id.addressBook_dialog_new_entry_label_edittext);
	    final EditText newEntryDialogAddressEditText = (EditText) dialogLayout.findViewById(R.id.addressBook_dialog_new_entry_address_edittext);
	    newEntryDialogAddressEditText.setText(address);
	    
	    if (mNewEntryDialogLabelText.equals("") == false)
	    {
	    	// If we are returning to this dialog after scanning a QR code
	    	newEntryDialogLabelEditText.setText(mNewEntryDialogLabelText);
	    }
	    
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{ 
	        // Show soft keyboard when the Label Edit Text gains focus
		    newEntryDialogLabelEditText.setOnFocusChangeListener(new OnFocusChangeListener() 
		    {
	            @Override
	            public void onFocusChange(View v, boolean hasFocus) 
	            {
	            	newEntryDialogLabelEditText.post(new Runnable() 
	            	{
	                    @Override
	                    public void run() {
	                        InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	                        imm.showSoftInput(newEntryDialogLabelEditText, InputMethodManager.SHOW_IMPLICIT);
	                    }
	                });
	            }
	        });
		    
		    // Show soft keyboard when the Address Edit Text gains focus
		    newEntryDialogAddressEditText.setOnFocusChangeListener(new OnFocusChangeListener() 
		    {
	            @Override
	            public void onFocusChange(View v, boolean hasFocus) 
	            {
	            	newEntryDialogAddressEditText.post(new Runnable() 
	            	{
	                    @Override
	                    public void run() {
	                        InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	                        imm.showSoftInput(newEntryDialogAddressEditText, InputMethodManager.SHOW_IMPLICIT);
	                    }
	                });
	            }
	        });
		}
		
		Button scanButton = (Button) dialogLayout.findViewById(R.id.addressBook_dialog_new_entry_scan_qr_code_button);
		scanButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "New entry dialog scan button pressed");
				
				if (newEntryDialogLabelEditText.getText().toString().equals("") == false)
				{
					mNewEntryDialogLabelText = newEntryDialogLabelEditText.getText().toString();
				}
				 
				newEntryDialog.cancel();
				
				IntentIntegrator integrator = new IntentIntegrator(AddressBookActivity.this);
                integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
			}
		});
	    
	    Button saveButton = (Button) dialogLayout.findViewById(R.id.addressBook_dialog_new_entry_save_button);
	    saveButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "New entry dialog save button pressed");
				
				AddressBookRecord newEntry = new AddressBookRecord();
				AddressBookRecordProvider addBookProv = AddressBookRecordProvider.get(getApplicationContext());
				
				String label = newEntryDialogLabelEditText.getText().toString();
				if (label.equals(""))
				{
					Toast.makeText(getApplicationContext(), R.string.addressBook_toast_must_enter_label, Toast.LENGTH_LONG).show();
					return;
				}
				else
				{
					newEntry.setLabel(label);
				}
				
				String address = newEntryDialogAddressEditText.getText().toString();
				if (address.equals(""))
				{
					Toast.makeText(getApplicationContext(), R.string.addressBook_toast_must_enter_address, Toast.LENGTH_LONG).show();
					return;
				}
				else
				{
					// Check whether the address is valid
					AddressProcessor addProc = new AddressProcessor();
					boolean addressValid = addProc.validateAddress(address);
					if (addressValid == false)
					{
						Toast.makeText(getApplicationContext(), R.string.addressBook_toast_not_valid_address, Toast.LENGTH_LONG).show();
						return;
					}
					
					// Check whether there is already an address book entry for this address
					ArrayList<AddressBookRecord> retrievedRecords = addBookProv.searchAddressBookRecords(AddressBookRecordsTable.COLUMN_ADDRESS, address);
					if (retrievedRecords.size() > 0)
					{
						Toast.makeText(getApplicationContext(), R.string.addressBook_toast_entry_already_exists, Toast.LENGTH_LONG).show();
						return;
					}
					else
					{
						newEntry.setAddress(address);
					}
				}
				
				// Set the colour values for this address book record
	            int[] colourValues = ColourCalculator.calculateColoursFromAddress(address);
				newEntry.setColourR(colourValues[0]);
				newEntry.setColourG(colourValues[1]);
				newEntry.setColourB(colourValues[2]);
				
				long id = addBookProv.addAddressBookRecord(newEntry);
				newEntry.setId(id);
				
				mAddressBookRecords.add(newEntry);
				Collections.sort(mAddressBookRecords);
				((AddressBookRecordAdapter)mAddressBookListView.getAdapter()).notifyDataSetChanged();
				
				// Clear this variable so that it isn't reused if the user tries to add another new address book entry
				mNewEntryDialogLabelText = "";
				
				newEntryDialog.dismiss();
				
				closeKeyboardIfOpen();
			}
		});
	    
	    Button cancelButton = (Button) dialogLayout.findViewById(R.id.addressBook_dialog_new_entry_cancel_button);
	    cancelButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "New entry dialog cancel button pressed");
				
				newEntryDialog.dismiss();
			}
		});
	}
	
	/**
	 * If the soft keyboard is open, this method will close it. Currently only
	 * works for API 16 and above. 
	 */
	private void closeKeyboardIfOpen()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
		{
			final View activityRootView = getWindow().getDecorView().getRootView();	
			final OnGlobalLayoutListener globalListener = new OnGlobalLayoutListener()
			{
				@Override
				@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
				public void onGlobalLayout() 
				{
				    Rect rect = new Rect();
				    // rect will be populated with the coordinates of your view that area still visible.
				    activityRootView.getWindowVisibleDisplayFrame(rect);
	
				    int heightDiff = activityRootView.getRootView().getHeight() - (rect.bottom - rect.top);
				    if (heightDiff > 100)
				    {
				    	// If the difference is more than 100 pixels, it's probably caused by the soft keyboard being open. Now we want to close it.
						InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0); // Toggle the soft keyboard. 
				    }
				    
				    // Now we have to remove the OnGlobalLayoutListener, otherwise we will experience errors
				    activityRootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				}
			};
			activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(globalListener);
		}
	}
	    
     @Override
     public void onActivityResult(int requestCode, int resultCode, Intent intent) 
     {
        ((AddressBookRecordAdapter)mAddressBookListView.getAdapter()).notifyDataSetChanged();
        
        // Get the result of the QR code scan
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (result != null)
        {
        	String contents = result.getContents();
        	if (contents != null)
        	{
        		contents = contents.trim(); // Remove leading or trailing spaces
        		
        		Log.i(TAG, "Found QRcode with the following contents: " + contents);
        		
        		if (contents.substring(0, 3).equals(BITMESSAGE_ADDRESS_PREFIX))
        		{
        			openNewEntryDialog(contents);
        		}
        		else
        		{
        			Toast.makeText(getApplicationContext(), R.string.addressBook_toast_qr_address_invalid, Toast.LENGTH_LONG).show();
        			openNewEntryDialog("");
        		}
        	} 
        	else 
        	{
        		Log.i(TAG, "No QRcode found");
        		openNewEntryDialog("");
        	}
        }
     }
    
     private class AddressBookRecordAdapter extends ArrayAdapter<AddressBookRecord> 
     {
        public AddressBookRecordAdapter(ArrayList<AddressBookRecord> addressBookRecords) 
        {
            super(getBaseContext(), android.R.layout.simple_list_item_1, addressBookRecords);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) 
        {
            // If we weren't given a view, inflate one
            if (null == convertView) 
            {
            	convertView = getLayoutInflater().inflate(R.layout.list_item_address_book, parent, false);
            }

            // Configure the view for this Address Book Record
            AddressBookRecord a = getItem(position);

            mListItemLabelTextView = (TextView)convertView.findViewById(R.id.addressBook_list_item_label_textview);
            
            if (a.getLabel() == null)
            {
            	mListItemLabelTextView.setText("[No label]");
            }
            
            else
            {
            	mListItemLabelTextView.setText(a.getLabel());
            }
            
            mListItemAddressTextView = (TextView)convertView.findViewById(R.id.addressBook_list_item_address_textview);
            mListItemAddressTextView.setText(a.getAddress());
            
            // Set the colour for this view
			int r = a.getColourR();
			int g = a.getColourG();
			int b = a.getColourB();
			 
            convertView.setBackgroundColor(Color.argb(ADDRESS_BOOK_COLOURS_ALPHA_VALUE, r, g, b));

            int color = Color.argb(0, r, g, b);
            mListItemLabelTextView.setBackgroundColor(color);
            mListItemAddressTextView.setBackgroundColor(color);
            
            // Need to create some final variables that can be used inside the onClickListener
            final int selectedPosition = position;
			final int selectedColorR = r;
			final int selectedColorG = g;
			final int selectedColorB = b;
            
			convertView.setOnClickListener(new View.OnClickListener()
			{
                @Override
                public void onClick(View v) 
                {
                    Log.i(TAG, "Address book list item clicked");
                    
                    // Get the AddressBookRecord selected from the adapter
            		final AddressBookRecord selectedRecord = ((AddressBookRecordAdapter)mAddressBookListView.getAdapter()).getItem(selectedPosition);
            	    
                    // Open a dialog to enter the data for the selected address book entry
            		final Dialog listItemDialog = new Dialog(AddressBookActivity.this);
            		LinearLayout dialogLayout = (LinearLayout) View.inflate(AddressBookActivity.this, R.layout.dialog_address_book_list_item_options, null);
            		listItemDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            		listItemDialog.setContentView(dialogLayout);
            		
            		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            	    lp.copyFrom(listItemDialog.getWindow().getAttributes());
            	    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            		
            	    listItemDialog.show();
            	    listItemDialog.getWindow().setAttributes(lp);
            	    
            	    final EditText listItemDialogLabelEditText= (EditText) dialogLayout.findViewById(R.id.addressBook_dialog_list_item_label_edittext);
            	    final EditText listItemDialogAddressEditText = (EditText) dialogLayout.findViewById(R.id.addressBook_dialog_list_item_address_edittext);
            	    
            	    // Set the colours to use in the dialog
            		int color = Color.argb(0, selectedColorR, selectedColorG, selectedColorB);
            		listItemDialogLabelEditText.setBackgroundColor(color);
            		listItemDialogAddressEditText.setBackgroundColor(color);
            		int backgroundColor = Color.argb(ADDRESS_BOOK_COLOURS_ALPHA_VALUE, selectedColorR, selectedColorG, selectedColorB);
            		dialogLayout.setBackgroundColor(backgroundColor);
            	    
            	    // Set the text of the two EditTexts in the dialog
            	    listItemDialogLabelEditText.setText(selectedRecord.getLabel());
            	    listItemDialogAddressEditText.setText(selectedRecord.getAddress());
            	    
            	    // Set the position of the cursor in each EditText to the end of the text
            	    listItemDialogLabelEditText.setSelection(listItemDialogLabelEditText.getText().length());
            	    listItemDialogAddressEditText.setSelection(listItemDialogAddressEditText.getText().length());
            	    
            		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            		{
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
            	                    public void run() 
            	                    {
            	                        InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            	                        imm.showSoftInput(listItemDialogAddressEditText, InputMethodManager.SHOW_IMPLICIT);
            	                    }
            	                });
            	            }
            	        });
            		}
            	    
            	    Button sendToButton = (Button) dialogLayout.findViewById(R.id.addressBook_dialog_list_item_send_to_button);
            	    sendToButton.setOnClickListener(new View.OnClickListener()
            		{
            			@Override
            			public void onClick(View v)
            			{
            				Log.i(TAG, "List item dialog send to button pressed");		
            				
            				Intent i = new Intent(getBaseContext(), ComposeActivity.class);
            		        i.putExtra(ComposeActivity.EXTRA_TO_ADDRESS, selectedRecord.getAddress());
                            i.putExtra(ComposeActivity.EXTRA_COLOUR_R, selectedColorR);
                            i.putExtra(ComposeActivity.EXTRA_COLOUR_G, selectedColorG);
                            i.putExtra(ComposeActivity.EXTRA_COLOUR_B, selectedColorB);
            		        startActivityForResult(i, 0);
            			}
            		});
            	    
            	    Button showQRCodeButton = (Button) dialogLayout.findViewById(R.id.addressBook_dialog_list_item_show_qr_code_button);
            	    showQRCodeButton.setOnClickListener(new View.OnClickListener()
            		{
            			@Override
            			public void onClick(View v)
            			{
            				Log.i(TAG, "List item dialog show qr code button pressed");							
            				
            				IntentIntegrator integrator = new IntentIntegrator(AddressBookActivity.this);
                            integrator.shareText(selectedRecord.getAddress());
            			}
            		});
            	    
            	    Button saveButton = (Button) dialogLayout.findViewById(R.id.addressBook_dialog_list_item_save_button);
            	    saveButton.setOnClickListener(new View.OnClickListener()
            		{
            			@Override
            			public void onClick(View v)
            			{
            				Log.i(TAG, "List item dialog save button pressed");
            				
            				AddressBookRecordProvider addBookProv = AddressBookRecordProvider.get(getApplicationContext());
            				
            				String label = listItemDialogLabelEditText.getText().toString();
            				if (label.equals(""))
            				{
            					Toast.makeText(getApplicationContext(), R.string.addressBook_toast_blank_label, Toast.LENGTH_LONG).show();
            					return;
            				}
            				
            				String address = listItemDialogAddressEditText.getText().toString();
            				if (address.equals(""))
            				{
            					Toast.makeText(getApplicationContext(), R.string.addressBook_toast_blank_address, Toast.LENGTH_LONG).show();
            					return;
            				}
            				
            				AddressProcessor addProc = new AddressProcessor();
            				boolean addressValid = addProc.validateAddress(address);
            				if (addressValid == false)
            				{
            					Toast.makeText(getApplicationContext(), R.string.addressBook_toast_not_valid_address, Toast.LENGTH_LONG).show();
            					return;
            				}
            				
            				// If we reach this point, the label and address should be valid
            				selectedRecord.setLabel(listItemDialogLabelEditText.getText().toString());
            				selectedRecord.setAddress(listItemDialogAddressEditText.getText().toString());
            				
            				addBookProv.updateAddressBookRecord(selectedRecord);
            				
            				Collections.sort(mAddressBookRecords);
            				((AddressBookRecordAdapter)mAddressBookListView.getAdapter()).notifyDataSetChanged();	
            				
            				listItemDialog.dismiss();
            				
            				closeKeyboardIfOpen();
            			}
            		});
            	    
            	    Button copyButton = (Button) dialogLayout.findViewById(R.id.addressBook_dialog_list_item_copy_button);
            	    copyButton.setOnClickListener(new View.OnClickListener()
            		{
            			@SuppressWarnings("deprecation")
            			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
            			@Override
            			public void onClick(View v)
            			{
            				Log.i(TAG, "List item dialog copy button pressed");							
            				
            				selectedRecord.getAddress();
            				
            				int sdk = android.os.Build.VERSION.SDK_INT;
            				
            				if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) 
            				{
            				    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            				    clipboard.setText(selectedRecord.getAddress());
            				} 
            				
            				else 
            				{
            				    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
            				    android.content.ClipData clip = android.content.ClipData.newPlainText("COPIED_ADDRESS", selectedRecord.getAddress());
            				    clipboard.setPrimaryClip(clip);
            				}
            				
            				listItemDialog.dismiss();
            				
            				Toast.makeText(getApplicationContext(), R.string.addressBook_toast_address_copied, Toast.LENGTH_LONG).show();
            			}
            		});
            	    
            	    Button deleteButton = (Button) dialogLayout.findViewById(R.id.addressBook_dialog_list_item_delete_button);
            	    deleteButton.setOnClickListener(new View.OnClickListener()
            		{
            			@Override
            			public void onClick(View v)
            			{ 
            				Log.i(TAG, "List item dialog delete button pressed");
            				
            		        // Open a dialog to confirm or cancel the deletion of the message
            				final Dialog deleteDialog = new Dialog(AddressBookActivity.this);
            				LinearLayout dialogLayout = (LinearLayout) View.inflate(AddressBookActivity.this, R.layout.dialog_address_book_entry_delete, null);
            				deleteDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            				deleteDialog.setContentView(dialogLayout);
            				
            				WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            			    lp.copyFrom(deleteDialog.getWindow().getAttributes());
            			    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            				
            			    deleteDialog.show();
            			    deleteDialog.getWindow().setAttributes(lp);		  
            			    
            			    Button confirmButton = (Button) dialogLayout.findViewById(R.id.addressBook_delete_dialog_confirm_button);
            			    confirmButton.setOnClickListener(new View.OnClickListener()
            				{
            					@Override
            					public void onClick(View v)
            					{
            						Log.i(TAG, "Address book delete dialog confirm button pressed");
            						
            						mAddressBookRecords.remove(selectedRecord);
            						
            						// Delete the selected AddressBookRecord from the application database
            						AddressBookRecordProvider addBookProv = AddressBookRecordProvider.get(getApplicationContext());
            						addBookProv.deleteAddressBookRecord(selectedRecord);
            						
            						Collections.sort(mAddressBookRecords);
            						((AddressBookRecordAdapter)mAddressBookListView.getAdapter()).notifyDataSetChanged();
            			    		
            				        deleteDialog.dismiss();
            						listItemDialog.dismiss();
            						
            						Toast.makeText(getApplicationContext(), R.string.addressBook_toast_entry_deleted, Toast.LENGTH_SHORT).show();
            					}
            				});
            			    
            			    Button cancelButton = (Button) dialogLayout.findViewById(R.id.addressBook_delete_dialog_cancel_button);
            			    cancelButton.setOnClickListener(new View.OnClickListener()
            				{
            					@Override
            					public void onClick(View v)
            					{
            						Log.i(TAG, "Address Book delete dialog cancel button pressed");							
            						
            						deleteDialog.dismiss();
            					}
            				});
            			}
            		});
                }
	        });
			return convertView;
	    }
	}
     
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) 
 	{
 		// Inflate the menu; this adds items to the action bar if it is present.
 		getMenuInflater().inflate(R.menu.address_book_activity_actions, menu);
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
	    	case R.id.action_add_to_address_book:
		        openNewEntryDialog(""); // We can pass in an empty String safely
		        break;
 	    
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