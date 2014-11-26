package org.bitseal.activities;

import java.util.ArrayList;

import org.bitseal.R;
import org.bitseal.core.App;
import org.bitseal.crypt.AddressGenerator;
import org.bitseal.data.Address;
import org.bitseal.data.Pubkey;
import org.bitseal.database.AddressProvider;
import org.bitseal.database.PubkeyProvider;
import org.bitseal.services.BackgroundService;
import org.bitseal.util.ColourCalculator;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

/**
 * The Activity class for the app's "Identities" screen. 
 * 
 * @author Jonathan Coe
 */
public class IdentitiesActivity extends ListActivity
{
	private ArrayList<Address> mAddresses;
	
    private ListView mAddressListView;
	
	private TextView mListItemLabelTextView;
	private TextView mListItemAddressTextView;
	
	private static final String IDENTITIES_FIRST_OPEN = "identities_first_open";
	
    private static final String IDENTITIES_WELCOME_MESSAGE_TOAST = "Here is your new Bitmessage address!\n\nGive it to your friends so they can send you a message.";
	
	// Used when receiving Intents to the UI so that it can refresh the data it is displaying
	public static final String UI_NOTIFICATION = "uiNotification";
	public static final String EXTRA_UPDATE_MY_ADDRESSES_LIST = "updateMyAddressesList";
	
	private static final int IDENTITIES_COLOURS_ALPHA_VALUE = 70;
	
    private static final String TAG = "IDENTITIES_ACTIVITY";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_identities);
		
		// Get all Addresses from the application's database
		AddressProvider addProv = AddressProvider.get(getApplicationContext());
		addProv = AddressProvider.get(getApplicationContext());
		mAddresses = addProv.getAllAddresses();
		
		// Set up the ListView with data from the AddressAdapter
		AddressAdapter adapter = new AddressAdapter(mAddresses);
		mAddressListView = new ListView(this);
		mAddressListView = (ListView)findViewById(android.R.id.list);          
        setListAdapter(adapter);
		
        // Check whether this is the first time the identities activity has been opened - if so then let's create a new address for them
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (prefs.getBoolean(IDENTITIES_FIRST_OPEN, true))
		{
			runFirstOpenRoutine();
		}
	}
	
	/**
	 * Generates a new Bitmessage address, saves it to the database, and starts
	 * the background service to run the rest of the 'create identity' task.
	 */
	private void createNewAddress()
	{
		Log.i(TAG, "Generate New Address Button pressed");
		Toast.makeText(getApplicationContext(), "Generating a new Bitmessage address...", Toast.LENGTH_LONG).show();
		
		// ---------------------------------- Create a new identity! ----------------------------
	    try
	    {
	    	// Generate a new Bitmessage address
	    	AddressGenerator addGen = new AddressGenerator();
	    	Address address = addGen.generateAndSaveNewAddress();
	    	
	    	// Update the displayed list of addresses
	    	updateListView();
	    	
	    	// Start the BackgroundService in order to complete the 'create new identity' task
		    Intent intent = new Intent(getBaseContext(), BackgroundService.class);
		    intent.putExtra(BackgroundService.UI_REQUEST, BackgroundService.UI_REQUEST_CREATE_IDENTITY);
		    intent.putExtra(BackgroundService.ADDRESS_ID, address.getId());
	    	startService(intent);
	    }
	    catch (Exception e)
	    {
	    	Log.e(TAG, "Exception occured in IdentitiesActivity while running mAddressGenerationButton.onClick().\n" +
	    			"The Exception message was: " + e.getMessage());
	    }
		// ---------------------------------------------------------------------------------------
	}
	
	/**
	 * Creates a new first address for the user and sets a flag so
	 * that this routine won't be run again.
	 */
	private void runFirstOpenRoutine()
	{
	    // Set a flag in SharedPreferences so that this will not be called again
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = prefs.edit();
	    editor.putBoolean(IDENTITIES_FIRST_OPEN, false);
	    editor.commit();
		
	    for (int i = 0; i < 2; i++) // Yes, it's a hack. Come at me bro. 
	    {
	    	Toast.makeText(getApplicationContext(), IDENTITIES_WELCOME_MESSAGE_TOAST, Toast.LENGTH_LONG).show();
	    }
	}
	
	/**
	 * Opens a dialog box to confirm or cancel the creation of a new address.
	 */
    private void openNewAddressDialog()
    {
		// Open a dialog to confirm or cancel the creation of a new address
		final Dialog confirmAddressCreationDialog = new Dialog(IdentitiesActivity.this);
		LinearLayout dialogLayout = (LinearLayout) View.inflate(IdentitiesActivity.this, R.layout.dialog_identities_confirm_address_creation, null);
		confirmAddressCreationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		confirmAddressCreationDialog.setContentView(dialogLayout);
		
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
	    lp.copyFrom(confirmAddressCreationDialog.getWindow().getAttributes());
	    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
		
	    confirmAddressCreationDialog.show();
	    confirmAddressCreationDialog.getWindow().setAttributes(lp);		  
	    
	    Button confirmButton = (Button) dialogLayout.findViewById(R.id.identities_confirm_address_creation_confirm_button);
	    confirmButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Identities confirm address creation confirm button pressed");
				
				confirmAddressCreationDialog.dismiss();
				
				createNewAddress();
			}
		});
	    Button cancelButton = (Button) dialogLayout.findViewById(R.id.identities_confirm_address_creation_cancel_button);
	    cancelButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Identities confirm address creation cancel button pressed");							
				
				confirmAddressCreationDialog.dismiss();
			}
		});
	}
	
	private BroadcastReceiver receiver = new BroadcastReceiver() 
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			Log.i(TAG, "IdentitiesActivity.BroadcastReceiver.onReceive() called");
			
			if (intent.hasExtra(EXTRA_UPDATE_MY_ADDRESSES_LIST))
			{
				updateListView();
			}
		}
	};
    
    private void updateListView()
    {
    	// Get all Addresses from the database
    	AddressProvider addProv = AddressProvider.get(getApplicationContext());
    	mAddresses = addProv.getAllAddresses();
    	// Clear out the old list of Addresses
    	((AddressAdapter)mAddressListView.getAdapter()).clear();
    	// Add each Address to the adapter
    	for (Address a : mAddresses)
    	{
    		((AddressAdapter)mAddressListView.getAdapter()).add(a);
    	}
    }
	
	@Override
	protected void onResume() 
	{
		super.onResume();

		// Register the broadcast receiver
		registerReceiver(receiver, new IntentFilter(UI_NOTIFICATION));
	}
	  
	@Override
	protected void onPause() 
	{
		super.onPause();
		
		unregisterReceiver(receiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    // Inflate the menu items for use in the action bar
	    getMenuInflater().inflate(R.menu.identities_activity_actions, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
	    switch(item.getItemId()) 
	    {
	    	case R.id.action_create_new_address:
				Log.i(TAG, "Create new address button pressed");
				AddressProvider addProv = AddressProvider.get(getApplicationContext());
				ArrayList<Address> myAddresses = addProv.getAllAddresses();
				if (myAddresses.size() > 0)
				{
					// If we already have one or more addresses, open a dialog to confirm the creation
					// of a new address
					openNewAddressDialog();
				}
				else
				{
					createNewAddress();
				}
		        break;
		        
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
	
     @Override
     public void onActivityResult(int requestCode, int resultCode, Intent data) 
     {
        ((AddressAdapter)mAddressListView.getAdapter()).notifyDataSetChanged();
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
            convertView.setBackgroundColor(Color.argb(IDENTITIES_COLOURS_ALPHA_VALUE, colourValues[0], colourValues[1], colourValues[2]));

            int color = Color.argb(0, colourValues[0], colourValues[1], colourValues[2]);
            mListItemLabelTextView.setBackgroundColor(color);
            mListItemAddressTextView.setBackgroundColor(color);
            
            // Need to create some final variables that can be used inside the onClickListener
            final int selectedPosition = position;
			final int selectedColorR = colourValues[0];
			final int selectedColorG = colourValues[1];
			final int selectedColorB = colourValues[2];
            
			convertView.setOnClickListener(new View.OnClickListener()
			{
                @Override
                public void onClick(View v) 
                {
                    Log.i(TAG, "Identities list item clicked");
             	    
                    // Get the Address selected from the adapter
            		final Address listAddress = ((AddressAdapter)mAddressListView.getAdapter()).getItem(selectedPosition);
            	    
                    // Open a dialog to enter the data for the selected address
            		final Dialog listItemDialog = new Dialog(IdentitiesActivity.this);
            		LinearLayout dialogLayout = (LinearLayout) View.inflate(IdentitiesActivity.this, R.layout.dialog_identities_list_item_options, null);
            		listItemDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            		listItemDialog.setContentView(dialogLayout);
            		
            		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            	    lp.copyFrom(listItemDialog.getWindow().getAttributes());
            	    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            		
            	    listItemDialog.show();
            	    listItemDialog.getWindow().setAttributes(lp);
            	    
            	    final EditText listItemDialogLabelEditText = (EditText) dialogLayout.findViewById(R.id.identities_dialog_list_item_label_edittext);
            	    final EditText listItemDialogAddressEditText = (EditText) dialogLayout.findViewById(R.id.identities_dialog_list_item_address_edittext);
            	    
            	    // Set the colours to use in the dialog
            		int color = Color.argb(0, selectedColorR, selectedColorG, selectedColorB);
            		listItemDialogLabelEditText.setBackgroundColor(color);
            		listItemDialogAddressEditText.setBackgroundColor(color);
            		int backgroundColor = Color.argb(IDENTITIES_COLOURS_ALPHA_VALUE, selectedColorR, selectedColorG, selectedColorB);
            		dialogLayout.setBackgroundColor(backgroundColor);
            	    
            	    // Set the text of the two EditTexts in the dialog
            	    listItemDialogLabelEditText.setText(listAddress.getLabel());
            	    listItemDialogAddressEditText.setText(listAddress.getAddress());
            	    
            	    // Set the position of the cursor in each EditText to the end of the text
            	    listItemDialogLabelEditText.setSelection(listItemDialogLabelEditText.getText().length());
            	    listItemDialogAddressEditText.setSelection(listItemDialogAddressEditText.getText().length());
            	    
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
            	    
            		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            		{
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
            		}
            		
            	    Button sendFromButton = (Button) dialogLayout.findViewById(R.id.identities_dialog_list_item_send_from_button);
            	    sendFromButton.setOnClickListener(new View.OnClickListener()
            		{
            			@Override
            			public void onClick(View v)
            			{
            				Log.i(TAG, "List item dialog send from button pressed");							
            				
            				Intent i = new Intent(getBaseContext(), ComposeActivity.class);
            		        i.putExtra(ComposeActivity.EXTRA_FROM_ADDRESS, listAddress.getAddress());
                            i.putExtra(ComposeActivity.EXTRA_COLOUR_R, selectedColorR);
                            i.putExtra(ComposeActivity.EXTRA_COLOUR_G, selectedColorG);
                            i.putExtra(ComposeActivity.EXTRA_COLOUR_B, selectedColorB);
            		        startActivityForResult(i, 0); 
            			}
            		});
            		
            	    Button showQRCodeButton = (Button) dialogLayout.findViewById(R.id.identities_dialog_list_item_show_qr_code_button);
            	    showQRCodeButton.setOnClickListener(new View.OnClickListener()
            		{
            			@Override
            			public void onClick(View v)
            			{
            				Log.i(TAG, "List item dialog show qr code button pressed");							
            				
            				IntentIntegrator integrator = new IntentIntegrator(IdentitiesActivity.this);
                            integrator.shareText(listAddress.getAddress());
            			}
            		});
            	    
            	    Button saveButton = (Button) dialogLayout.findViewById(R.id.identities_dialog_list_item_save_button);
            	    saveButton.setOnClickListener(new View.OnClickListener()
            		{
            			@Override
            			public void onClick(View v)
            			{
            				Log.i(TAG, "List item dialog save button pressed");							
            				
            				String label = listItemDialogLabelEditText.getText().toString();
            				if (label.equals(""))
            				{
            					Toast.makeText(getApplicationContext(), "The label must not be blank", Toast.LENGTH_LONG).show();
            					return;
            				}
            				else
            				{
            					listAddress.setLabel(label);
            				}
            				
            				AddressProvider addProv = AddressProvider.get(getApplicationContext());
            				addProv.updateAddress(listAddress);
            				
            				((AddressAdapter)mAddressListView.getAdapter()).notifyDataSetChanged();
            				
            				listItemDialog.dismiss();
            				
            				closeKeyboardIfOpen();
            			}
            		});
            	    
            	    Button copyButton = (Button) dialogLayout.findViewById(R.id.identities_dialog_list_item_copy_button);
            	    copyButton.setOnClickListener(new View.OnClickListener()
            		{
            			@SuppressWarnings("deprecation")
            			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
            			@Override
            			public void onClick(View v)
            			{
            				Log.i(TAG, "List item dialog copy button pressed");							
            				
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
            				
            				listItemDialog.dismiss();
            				
            				Toast.makeText(getApplicationContext(), "Address copied to the clipboard", Toast.LENGTH_LONG).show();
            			}
            		});
            	    
            	    Button deleteButton = (Button) dialogLayout.findViewById(R.id.identities_dialog_list_item_delete_button);
            	    deleteButton.setOnClickListener(new View.OnClickListener()
            		{
            			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
            			@Override
            			public void onClick(View v)
            			{
            				Log.i(TAG, "List item dialog delete button pressed");							
            				
            				// Open a dialog to confirm or cancel the deletion of the message
            				final Dialog firstDeleteDialog = new Dialog(IdentitiesActivity.this);
            				LinearLayout dialogLayout = (LinearLayout) View.inflate(IdentitiesActivity.this, R.layout.dialog_identities_entry_delete_first, null);
            				firstDeleteDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            				firstDeleteDialog.setContentView(dialogLayout);
            				
            				WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            			    lp.copyFrom(firstDeleteDialog.getWindow().getAttributes());
            			    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            				
            			    firstDeleteDialog.show();
            			    firstDeleteDialog.getWindow().setAttributes(lp);		  
            			    
            			    Button firstConfirmButton = (Button) dialogLayout.findViewById(R.id.identities_first_delete_dialog_confirm_button);
            			    firstConfirmButton.setOnClickListener(new View.OnClickListener()
            				{
            					@Override
            					public void onClick(View v)
            					{
            						Log.i(TAG, "Identities first delete dialog confirm button pressed");
            						
            						firstDeleteDialog.dismiss();
            						
            						//Open the second delete address dialog for further confirmation
            						final Dialog secondDeleteDialog = new Dialog(IdentitiesActivity.this);
            						LinearLayout dialogLayout = (LinearLayout) View.inflate(IdentitiesActivity.this, R.layout.dialog_identities_entry_delete_second, null);
            						secondDeleteDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            						secondDeleteDialog.setContentView(dialogLayout);
            						
            						WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            					    lp.copyFrom(secondDeleteDialog.getWindow().getAttributes());
            					    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            						
            					    secondDeleteDialog.show();
            					    secondDeleteDialog.getWindow().setAttributes(lp);		  
            					    
            					    Button secondConfirmButton = (Button) dialogLayout.findViewById(R.id.identities_second_delete_dialog_confirm_button);
            					    secondConfirmButton.setOnClickListener(new View.OnClickListener()
            						{
            							@Override
            							public void onClick(View v)
            							{
            								Log.i(TAG, "Identities second delete dialog confirm button pressed");
            								
            								secondDeleteDialog.dismiss();
            								
            								mAddresses.remove(listAddress);
            								
            								try
            								{
                								// Delete the selected Address from the application database
                								AddressProvider addProv = AddressProvider.get(getApplicationContext());
                								addProv.deleteAddress(listAddress);
                								
                								// Delete the pubkey of this address from the application database
                								PubkeyProvider pubProv = PubkeyProvider.get(App.getContext());
                								Pubkey pubkey = pubProv.searchForSingleRecord(listAddress.getCorrespondingPubkeyId());
                								pubProv.deletePubkey(pubkey);
            								}
            								catch (Exception e)
            								{
            									Log.e(TAG, "Exception occurred in IdentitesActivity while attempting to delete an address and its corresponding pubkey.\n" +
            											"The exception message was: " + e.getMessage());
            								}
            								
            						        secondDeleteDialog.dismiss();
            								listItemDialog.dismiss();
            								
            								Toast.makeText(getApplicationContext(), "Address deleted", Toast.LENGTH_SHORT).show();
            								
            								updateListView();
            							}
            						});
            					    
            					    Button secondCancelButton = (Button) dialogLayout.findViewById(R.id.identities_second_delete_dialog_cancel_button);
            					    secondCancelButton.setOnClickListener(new View.OnClickListener()
            						{
            							@Override
            							public void onClick(View v)
            							{
            								Log.i(TAG, "Identities second delete dialog cancel button pressed");							
            								
            								secondDeleteDialog.dismiss();
            							}
            						});
            					}
            				});
            			    
            			    Button firstCancelButton = (Button) dialogLayout.findViewById(R.id.identities_first_delete_dialog_cancel_button);
            			    firstCancelButton.setOnClickListener(new View.OnClickListener()
            				{
            					@Override
            					public void onClick(View v)
            					{
            						Log.i(TAG, "Identities first delete dialog cancel button pressed");							
            						
            						firstDeleteDialog.dismiss();
            					}
            				});
            			}
            		});
                }
            });

            return convertView;
        }
    }
}