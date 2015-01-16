package org.bitseal.activities;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import java.util.ArrayList;
import java.util.Collections;

import org.bitseal.R;
import org.bitseal.core.AddressProcessor;
import org.bitseal.data.Address;
import org.bitseal.data.AddressBookRecord;
import org.bitseal.data.Message;
import org.bitseal.database.AddressBookRecordProvider;
import org.bitseal.database.AddressBookRecordsTable;
import org.bitseal.database.AddressProvider;
import org.bitseal.database.AddressesTable;
import org.bitseal.database.MessageProvider;
import org.bitseal.services.BackgroundService;
import org.bitseal.services.AppLockHandler;
import org.bitseal.util.ColourCalculator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
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
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
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
 * The Activity class for the app's "Compose" screen. 
 * 
 * @author Jonathan Coe
 */
public class ComposeActivity extends Activity implements ICacheWordSubscriber
{	
	public static final String EXTRA_TO_ADDRESS = "composeActivity.TO_ADDRESS";
	public static final String EXTRA_FROM_ADDRESS = "composeActivity.FROM_ADDRESS";
	public static final String EXTRA_SUBJECT = "composeActivity.SUBJECT";
	public static final String EXTRA_BODY = "composeActivity.BODY";
	
	public static final String EXTRA_COLOUR_R = "composeActivity.COLOUR_R";
	public static final String EXTRA_COLOUR_G = "composeActivity.COLOUR_G";
	public static final String EXTRA_COLOUR_B = "composeActivity.COLOUR_B";
	
	public static final String KEY_TO_ADDRESS = "toAddress";
	public static final String KEY_FROM_ADDRESS = "fromAddress";
	public static final String KEY_SUBJECT = "subject";
	public static final String KEY_BODY = "body";
	
	private ArrayList<AddressBookRecord> mAddressBookRecords;
	private ArrayList<Address> mAddresses;
	
	private AddressBookRecordAdapter mToAddressesAdapter;
	private AddressAdapter mFromAddressesAdapter;
	
	private String mToAddress = "";
	private String mFromAddress = "";
	private String mSubject = "";
	private String mBody = "";
	
	private View mMainView;
		
	private ListView mToAddressSelectionListView;
	private ListView mFromAddressSelectionListView;
	
	private EditText mToAddressEditText;
	private EditText mFromAddressEditText;
	private EditText mSubjectEditText;
	private EditText mBodyEditText;
		
	private TextView mToAddressSelectionListItemLabelTextView;
	private TextView mToAddressSelectionListItemAddressTextView;
	private TextView mFromAddressSelectionListItemLabelTextView;
	private TextView mFromAddressSelectionListItemAddressTextView;
	
	private int mColourR;
	private int mColourG;
	private int mColourB;
	
	private static final String KEY_TO_ADDRESS_DIALOG_SELECTION = "toAddressDialogSelection";
	private static final String KEY_FROM_ADDRESS_DIALOG_SELECTION = "fromAddressDialogSelection";
	private static final String KEY_ON_PAUSE_CALLED = "onPauseCalled";
		
	private static final int COMPOSE_COLOURS_ALPHA_VALUE = 70;
	
	/**
	 * The maximum permissible size for a message's text (subject + body), in bytes. The maximum size
	 * of an object in Bitmessage protocol version 3 is 256kB. We allow some extra room for the rest of 
	 * the msg object which this message which eventually be transformed into. 
	 */
	private static final int MAXIMUM_MESSAGE_TEXT_SIZE = 250000;
	
    /** The key for a boolean variable that records whether or not a user-defined database encryption passphrase has been saved */
    private static final String KEY_DATABASE_PASSPHRASE_SAVED = "databasePassphraseSaved"; 
    
    private CacheWordHandler mCacheWordHandler;
	
    private static final String TAG = "COMPOSE_ACTIVITY";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
        // Check whether the user has set a database encryption passphrase
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false))
		{
			// Connect to the CacheWordService
			mCacheWordHandler = new CacheWordHandler(this);
			mCacheWordHandler.connectToService();
		}
		
		setLayout();
		createDialogs();
		
		useExtras(getIntent()); // Use any extras bundled with the Intent that started this activity
		autoSetFromAddress(); // If we have only one address, auto-set mFromAddress
		setColours();
		
		populateEditTexts();
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		
		useAddressLabels();
		formatEditTexts();
		positionCursor();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		// We need to clear the saved dialog selections so that they aren't used in future openings of this activity
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = prefs.edit();
	    editor.putString(KEY_TO_ADDRESS_DIALOG_SELECTION, "");
	    editor.putString(KEY_FROM_ADDRESS_DIALOG_SELECTION, "");
	    
	    // Used to detect screen rotation while the to or from address selection dialogs are open
	    editor.putBoolean(KEY_ON_PAUSE_CALLED, true);
	    
	    editor.commit();
	}
	
	@Override
	protected void onSaveInstanceState (Bundle savedInstanceState) 
	{
	    super.onSaveInstanceState(savedInstanceState);
	        
	    savedInstanceState.putString(KEY_TO_ADDRESS, mToAddress);
	    savedInstanceState.putString(KEY_FROM_ADDRESS, mFromAddress);
	    savedInstanceState.putString(KEY_SUBJECT, mSubject);
	    savedInstanceState.putString(KEY_BODY, mBody);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) 
	{
	    super.onRestoreInstanceState(savedInstanceState);
	    
    	mToAddress = savedInstanceState.getString(KEY_TO_ADDRESS);
	    mFromAddress = savedInstanceState.getString(KEY_FROM_ADDRESS);
	    mSubject = savedInstanceState.getString(KEY_SUBJECT);
	    mBody = savedInstanceState.getString(KEY_BODY);
	}
	
    @Override
    /**
     * Deals with the result of the QR code scan
     */
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
	       		
	    	   Log.i(TAG, "Found QRcode with the following contents: " + contents);
	       		
	    	   if (new AddressProcessor().validateAddress(contents))
	    	   {
	    		   mToAddress = contents;
	    		   mToAddressEditText.setTextSize((float) 12.5);
	    		   
		           int[] colourValues = ColourCalculator.calculateColoursFromAddress(contents);
		           mColourR = colourValues[0];
		           mColourG = colourValues[1];
		           mColourB = colourValues[2];
		           
		  		   getIntent().putExtra(EXTRA_COLOUR_R, colourValues[0]);
		  		   getIntent().putExtra(EXTRA_COLOUR_G, colourValues[1]);
		  		   getIntent().putExtra(EXTRA_COLOUR_B, colourValues[2]);
	    		   
		  		   setColours();
	    		   populateEditTexts();
	    		   positionCursor();
	       		}
	       		else
	       		{
	       			Toast.makeText(getApplicationContext(), R.string.compose_toast_qr_address_invalid, Toast.LENGTH_LONG).show();
	       		}
	       	}
	       else
	       {
	    	   Log.i(TAG, "No QRcode found");
	       }
	    }
    }
	
	/**
	 * Sets up the basic layout for this activity
	 */
	private void setLayout()
	{
		setContentView(R.layout.activity_compose);
		
		mToAddressEditText = (EditText) findViewById(R.id.compose_toAddress_EditText);
		mFromAddressEditText = (EditText) findViewById(R.id.compose_fromAddress_EditText);
		mSubjectEditText = (EditText) findViewById(R.id.compose_subject_EditText);
		mBodyEditText = (EditText) findViewById(R.id.compose_body_EditText);
	}
	
	/**
	 * Executed when the user presses the 'Send' button
	 */
	private void sendMessage()
	{
		Log.i(TAG, "Send Button pressed");
		
		String toAddress = mToAddressEditText.getText().toString();
		String fromAddress = mFromAddressEditText.getText().toString();
		
		// Check that the 'to address' entered by the user is either a valid Bitmessage address or a valid label from the address book
		try
		{
			if (toAddress.equals(""))
			{
				Toast.makeText(getApplicationContext(), R.string.compose_toast_enter_to_address, Toast.LENGTH_LONG).show();
				return;
			}
			AddressProcessor addProc = new AddressProcessor();
			boolean toAddressValid = addProc.validateAddress(toAddress);
			if (toAddressValid == false)
			{
				AddressBookRecordProvider addBookProv = AddressBookRecordProvider.get(getApplicationContext());
				ArrayList<AddressBookRecord> retrievedRecords = addBookProv.searchAddressBookRecords(AddressBookRecordsTable.COLUMN_LABEL, toAddress);
				if (retrievedRecords.size() > 0)
				{
					toAddress = retrievedRecords.get(0).getAddress();
				}
				else
				{
					Toast.makeText(getApplicationContext(), R.string.compose_toast_to_address_invalid, Toast.LENGTH_LONG).show();
					return;
				}
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "Exception occurred in ComposeActivity.sendMessage(). \n" +
					"The exception messsage was: " + e.getMessage());
			Toast.makeText(getApplicationContext(), R.string.compose_toast_to_address_error, Toast.LENGTH_LONG).show();
			return;
		}	
		
		// Check that the 'from address' entered by the user is either a valid Bitmessage address owned by the user or the label of one
		// of those addresses
		try
		{
			if (fromAddress.equals(""))
			{
				Toast.makeText(getApplicationContext(), R.string.compose_toast_enter_from_address, Toast.LENGTH_LONG).show();
				return;
			}
	
	       	AddressProvider addProv = AddressProvider.get(getApplicationContext());
	        ArrayList<Address> retrievedAddresses = addProv.searchAddresses(AddressesTable.COLUMN_ADDRESS, fromAddress);
			if (retrievedAddresses.size() == 0)
			{
				retrievedAddresses = addProv.searchAddresses(AddressesTable.COLUMN_LABEL, fromAddress);
				if (retrievedAddresses.size() > 0)
				{
					fromAddress = retrievedAddresses.get(0).getAddress();
				}
				else
				{
					Toast.makeText(getApplicationContext(), R.string.compose_toast_from_address_invalid, Toast.LENGTH_LONG).show();
					return;
				}
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "Exception occurred in ComposeActivity.sendMessage(). \n" +
					"The exception messsage was: " + e.getMessage());
			Toast.makeText(getApplicationContext(), R.string.compose_toast_from_address_error, Toast.LENGTH_LONG).show();
			return;
		}
		
		// Check that the size of the message text is below the maximum permissible value
		String subject = mSubjectEditText.getText().toString();
		String body = mBodyEditText.getText().toString();
		String combinedMessageText = subject.concat(body);
		int messageTextSize = combinedMessageText.getBytes().length;
		if (messageTextSize > MAXIMUM_MESSAGE_TEXT_SIZE)
		{
			Log.e(TAG, "The user attempted to send a message with a combined text (subject + body) size greater than the maximum value allowed. \n" +
					"The size of the combined message text was " + messageTextSize + " bytes.");
			Toast.makeText(getApplicationContext(), R.string.compose_toast_message_too_long, Toast.LENGTH_LONG).show();
			return;
		}
		
		// --------------------------------- Send the message! -------------------------------------------
	    try
	    {
    		// Create a new Message object and populate its fields
    		Message messageToSend = new Message();
			messageToSend.setBelongsToMe(true); // If I create and send a message then it 'belongs to me'. If I receive a message from someone else then it does not.
			messageToSend.setTime(System.currentTimeMillis() / 1000);
			messageToSend.setToAddress(toAddress);
			messageToSend.setFromAddress(fromAddress);
			messageToSend.setSubject(subject);
			messageToSend.setBody(body);
			messageToSend.setStatus(getBaseContext().getString(R.string.message_status_preparing_to_send));
			
			// Save the Message to the database
			MessageProvider msgProv = MessageProvider.get(getApplicationContext());
			long messageId = msgProv.addMessage(messageToSend);
			messageToSend.setId(messageId);

			// Start the BackgroundService to complete the 'send message' task
    		Intent intent = new Intent(getBaseContext(), BackgroundService.class);		    
		    intent.putExtra(BackgroundService.UI_REQUEST, BackgroundService.UI_REQUEST_SEND_MESSAGE);	    
		    intent.putExtra(BackgroundService.MESSAGE_ID, messageId);	    
		    BackgroundService.sendWakefulWork(getBaseContext(), intent);
			
			Toast.makeText(getApplicationContext(), R.string.compose_toast_sending_message, Toast.LENGTH_LONG).show();
			
			mToAddressEditText.setText("");
			mFromAddressEditText.setText("");
			mSubjectEditText.setText("");
			mBodyEditText.setText("");
			
			// Open the Sent Activity
	        Intent i = new Intent(getBaseContext(), SentActivity.class);
	        startActivityForResult(i, 0);
	    }		    
	    catch (Exception e)
	    {
	    	Log.e(TAG, "Exception occured in ComposeActivity while running mSendButton.onClick()");
	    	e.printStackTrace();
			Toast.makeText(getApplicationContext(), R.string.compose_toast_error, Toast.LENGTH_LONG).show();
			return;
	    }
	    // ----------------------------------------------------------------------------------------------
	}
	
	/**
	 * Creates 'to address selection' and 'from address selection' dialogs
	 */
	private void createDialogs()
	{
		final Dialog toAddressSelectionDialog = new Dialog(ComposeActivity.this);
		toAddressSelectionDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);	// This line has to be here so that it is not called repeatedly, which causes a crash
		mToAddressEditText.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "To address edit text clicked");
				
				// Get all AddressBookRecords from the application's database
				AddressBookRecordProvider addBookProv = AddressBookRecordProvider.get(getApplicationContext());
				addBookProv = AddressBookRecordProvider.get(getApplicationContext());
			    mAddressBookRecords = addBookProv.getAllAddressBookRecords();
				
				if (mAddressBookRecords.size() > 0)
				{
					// Open a dialog to select a 'to' address
					LinearLayout dialogLayout = (LinearLayout) View.inflate(ComposeActivity.this, R.layout.dialog_compose_to_address_selection, null);
					toAddressSelectionDialog.setContentView(dialogLayout);
					
					WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
				    lp.copyFrom(toAddressSelectionDialog.getWindow().getAttributes());
				    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
					
				    toAddressSelectionDialog.show();
				    toAddressSelectionDialog.getWindow().setAttributes(lp);
				    
				    Button scanQRCodeButton = (Button) dialogLayout.findViewById(R.id.compose_dialog_to_address_selection_scan_qr_code_button);
				    scanQRCodeButton.setOnClickListener(new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							Log.i(TAG, "To address selection dialog scan qr code button pressed");
							
							toAddressSelectionDialog.dismiss();
							
							IntentIntegrator integrator = new IntentIntegrator(ComposeActivity.this);
			                integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
						}
					});
				    
				    Button cancelButton = (Button) dialogLayout.findViewById(R.id.compose_dialog_to_address_selection_cancel_button);
				    cancelButton.setOnClickListener(new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							Log.i(TAG, "To address selection dialog cancel button pressed");
							
							toAddressSelectionDialog.dismiss();
						}
					});
				    
				    mToAddressesAdapter = new AddressBookRecordAdapter(mAddressBookRecords);
				    
		    		// Sort the AddressBookRecords by label, in alphabetical order
		    		Collections.sort(mAddressBookRecords);
				    
				    mToAddressSelectionListView = new ListView(ComposeActivity.this);
				    mToAddressSelectionListView = (ListView) toAddressSelectionDialog.findViewById(android.R.id.list);
				    mToAddressSelectionListView.setAdapter(mToAddressesAdapter);
				    
				    // Used to detect screen rotation while the to or from address selection dialogs are open
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
					SharedPreferences.Editor editor = prefs.edit();
				    editor.putBoolean(KEY_ON_PAUSE_CALLED, false);
				    editor.commit();
				    
				    mToAddressSelectionListView.setOnItemClickListener(new AdapterView.OnItemClickListener() 
				    {
				        @Override
				        public void onItemClick(AdapterView<?> parent, final View view, int position, long id) 
				        {
				        	// Get the AddressBookRecord selected from the adapter
				  		    final AddressBookRecord listAddressBookRecord = (AddressBookRecord) parent.getItemAtPosition(position);
				  		    
				  		    int r = listAddressBookRecord.getColourR();
				  		    int g = listAddressBookRecord.getColourG();
				  		    int b = listAddressBookRecord.getColourB();
				  		    
				  		    // Check whether the activity has been paused while this dialog has been open (e.g. because of screen rotation)
				  		    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				  		    if (prefs.getBoolean(KEY_ON_PAUSE_CALLED, false))
				  		    {
					  		    // If we just set the text of the edit text directly then the selection will not appear if the screen is rotated
					  		    // while the dialog is open
								SharedPreferences.Editor editor = prefs.edit();
							    editor.putString(KEY_TO_ADDRESS_DIALOG_SELECTION, listAddressBookRecord.getLabel());
							    editor.commit();
							    
								Intent i = new Intent(getBaseContext(), ComposeActivity.class);
						        i.putExtra(EXTRA_TO_ADDRESS, listAddressBookRecord.getLabel());
						        i.putExtra(EXTRA_FROM_ADDRESS, mFromAddressEditText.getText().toString());
						        i.putExtra(EXTRA_SUBJECT, mSubjectEditText.getText().toString());
						        i.putExtra(EXTRA_BODY, mBodyEditText.getText().toString());
		                        i.putExtra(EXTRA_COLOUR_R, r);
		                        i.putExtra(EXTRA_COLOUR_G, g);
		                        i.putExtra(EXTRA_COLOUR_B, b);
						        startActivityForResult(i, 0);
				  		    }
				  		    else
				  		    {
				  		    	// If onPause() has not been called while this dialog has been open, we can just set the value of the edit text directly
				  		    	mToAddressEditText.setText(listAddressBookRecord.getLabel());
				  		    }
				  		    
				  		    // Set the colour member variables and extras for this activity to match those of this address book record
				  		    mColourR = r;
				  		    mColourG = g;
				  		    mColourB = b;
				  		    getIntent().putExtra(EXTRA_COLOUR_R, r);
				  		    getIntent().putExtra(EXTRA_COLOUR_G, g);
				  		    getIntent().putExtra(EXTRA_COLOUR_B, b);
				  		    
				  		    setColours();
				  			formatEditTexts();
				  			positionCursor();
				  			
				  		    toAddressSelectionDialog.dismiss();
						}
					});
				}
			}	
		});
		
		final Dialog fromAddressSelectionDialog = new Dialog(ComposeActivity.this);
		fromAddressSelectionDialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // This line has to be here so that it is not called repeatedly, which causes a crash
		mFromAddressEditText.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "From address edit text clicked");
				
				// Get all Addresses from the application's database
				AddressProvider addProv = AddressProvider.get(getApplicationContext());
				addProv = AddressProvider.get(getApplicationContext());
			    mAddresses = addProv.getAllAddresses();
			    
			    if (mAddresses.size() > 0)
			    {
			        // Open a dialog to select a from address				
					LinearLayout dialogLayout = (LinearLayout) View.inflate(ComposeActivity.this, R.layout.dialog_compose_from_address_selection, null);
					fromAddressSelectionDialog.setContentView(dialogLayout);
					
					WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
				    lp.copyFrom(fromAddressSelectionDialog.getWindow().getAttributes());
				    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
					
				    fromAddressSelectionDialog.show();
				    fromAddressSelectionDialog.getWindow().setAttributes(lp);
				     
				    Button cancelButton = (Button) dialogLayout.findViewById(R.id.compose_dialog_from_address_selection_cancel_button);
				    cancelButton.setOnClickListener(new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							Log.i(TAG, "From address selection dialog cancel button pressed");
							
							fromAddressSelectionDialog.dismiss();
						}
					});
				    
				    mFromAddressesAdapter = new AddressAdapter(mAddresses);
				    
				    mFromAddressSelectionListView = new ListView(ComposeActivity.this);
				    mFromAddressSelectionListView = (ListView) fromAddressSelectionDialog.findViewById(android.R.id.list);
				    mFromAddressSelectionListView.setAdapter(mFromAddressesAdapter);
				    
				    // Used to detect screen rotation while the to or from address selection dialogs are open
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
					SharedPreferences.Editor editor = prefs.edit();
				    editor.putBoolean(KEY_ON_PAUSE_CALLED, false);
				    editor.commit();
				    
				    mFromAddressSelectionListView.setOnItemClickListener(new AdapterView.OnItemClickListener() 
				    {
				        @Override
				        public void onItemClick(AdapterView<?> parent, final View view, int position, long id) 
				        {
					  		// Get the Address selected from the adapter
					  		final Address listAddress = (Address) parent.getItemAtPosition(position);
					  	    
				  		    // Check whether the activity has been paused while this dialog has been open (e.g. because of screen rotation)
				  		    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				  		    if (prefs.getBoolean(KEY_ON_PAUSE_CALLED, false))
				  		    {
					  		    // If we just set the text of the edit text directly then the selection will not appear if the screen is rotated
					  		    // while the dialog is open
								SharedPreferences.Editor editor = prefs.edit();
							    editor.putString(KEY_FROM_ADDRESS_DIALOG_SELECTION, listAddress.getLabel());
							    editor.commit();
							    
								Intent i = new Intent(getBaseContext(), ComposeActivity.class);
						        i.putExtra(EXTRA_TO_ADDRESS, mToAddressEditText.getText().toString());
						        i.putExtra(EXTRA_FROM_ADDRESS, listAddress.getLabel());
						        i.putExtra(EXTRA_SUBJECT, mSubjectEditText.getText().toString());
						        i.putExtra(EXTRA_BODY, mBodyEditText.getText().toString());
						        startActivityForResult(i, 0);
				  		    }
				  		    else
				  		    {
				  		    	// If onPause() has not been called while this dialog has been open, we can just set the value of the edit text directly
				  		    	mFromAddressEditText.setText(listAddress.getLabel());
				  		    }
				  		    
				  			formatEditTexts();
				  			positionCursor();
				  		    fromAddressSelectionDialog.dismiss();
						}
					});
			    }
			}
		});
	}
	
	/**
	 * Takes any extras bundled with the Intent that started this Activity
	 * and uses them. 
	 * 
	 * @param extras - The Intent
	 */
	private void useExtras(Intent intent)
	{
		Bundle extras = intent.getExtras();
		
		if(intent.hasExtra(EXTRA_TO_ADDRESS) == true)
		{
			mToAddress = extras.getString(EXTRA_TO_ADDRESS);
		}

		if(intent.hasExtra(EXTRA_FROM_ADDRESS) == true)
		{
			mFromAddress = extras.getString(EXTRA_FROM_ADDRESS);		
		}
		
		if(intent.hasExtra(EXTRA_SUBJECT) == true)
		{
			mSubject = extras.getString(EXTRA_SUBJECT);
		}
		
		if(intent.hasExtra(EXTRA_BODY) == true)
		{
			mBody = extras.getString(EXTRA_BODY);
		}
		
		if(intent.hasExtra(EXTRA_COLOUR_R) == true)
		{
			mColourR = extras.getInt(EXTRA_COLOUR_R);
		}
		
		if(intent.hasExtra(EXTRA_COLOUR_G) == true)
		{
			mColourG = extras.getInt(EXTRA_COLOUR_G);
		}
		
		if(intent.hasExtra(EXTRA_COLOUR_B) == true)
		{
			mColourB = extras.getInt(EXTRA_COLOUR_B);
		}
	}
	
	/**
	 * If we only have one address, auto-fill the 'from address'
	 * edit text
	 */
	private void autoSetFromAddress()
	{
		// If we only have one address, auto-fill the 'from' field
       	AddressProvider addProv = AddressProvider.get(getApplicationContext());
        ArrayList<Address> myAddresses = addProv.getAllAddresses();
        if (myAddresses.size() == 1)
        {	
        	mFromAddress = myAddresses.get(0).getLabel();
        }
	}
	
	/**
	 * Sets the colours to use in this activity
	 */
	private void setColours()
	{
		// Set the colors inherited from the sent list view
		int color = Color.argb(0, mColourR, mColourG, mColourB);
		mToAddressEditText.setBackgroundColor(color);
		mFromAddressEditText.setBackgroundColor(color);
		mSubjectEditText.setBackgroundColor(color);
		mBodyEditText.setBackgroundColor(color);
		
		if (getIntent().hasExtra(EXTRA_COLOUR_R))
		{
			int backgroundColor = Color.argb(COMPOSE_COLOURS_ALPHA_VALUE, mColourR, mColourG, mColourB);
			mMainView = (View) findViewById(R.id.compose_scrollView);
			mMainView.setBackgroundColor(backgroundColor);
		}
		else
		{
			int backgroundColor = Color.argb(0, mColourR, mColourG, mColourB);
			mMainView = (View) findViewById(R.id.compose_scrollView);
			mMainView.setBackgroundColor(backgroundColor);
		}
	}
	
	/**
	 * Populates the edit texts
	 */
	private void populateEditTexts()
	{
		mToAddressEditText.setText(mToAddress);
		mFromAddressEditText.setText(mFromAddress);
		mSubjectEditText.setText(mSubject);
		mBodyEditText.setText(mBody);
		
		// If we have stored values to use for the 'to' or 'from' address, use them
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String toAddressSelection = prefs.getString(KEY_TO_ADDRESS_DIALOG_SELECTION, "");
		if (toAddressSelection.equals("") == false)
		{
			mToAddressEditText.setText(toAddressSelection);
		}
		String fromAddressSelection = prefs.getString(KEY_FROM_ADDRESS_DIALOG_SELECTION, "");
		if (fromAddressSelection.equals("") == false)
		{
			mFromAddressEditText.setText(fromAddressSelection);
		}
	}
	
	/**
	 * Substitutes labels for addresses where possible. 
	 */
	private void useAddressLabels()
	{
		AddressBookRecordProvider addBookProv = AddressBookRecordProvider.get(getApplicationContext());
		AddressProvider addProv = AddressProvider.get(getApplicationContext());
		
		// Attempt to use a label for the 'to address'
		ArrayList<AddressBookRecord> addressBookRecords = addBookProv.searchAddressBookRecords(AddressBookRecordsTable.COLUMN_ADDRESS, mToAddressEditText.getText().toString());
		if (addressBookRecords.size() == 1)
		{
			mToAddressEditText.setText(addressBookRecords.get(0).getLabel());
		}
		
		// Attempt to use a label for the 'from address'
		ArrayList<Address> addresses = addProv.searchAddresses(AddressesTable.COLUMN_ADDRESS, mFromAddressEditText.getText().toString());
		if (addresses.size() == 1)
		{
			mFromAddressEditText.setText(addresses.get(0).getLabel());
		}
	}
	
	/**
	 * Formats the edit texts
	 */
	private void formatEditTexts()
	{
		AddressBookRecordProvider addBookProv = AddressBookRecordProvider.get(getApplicationContext());
		AddressProvider addProv = AddressProvider.get(getApplicationContext());
		
		// Format the 'to address' edit text
		ArrayList<AddressBookRecord> addressBookRecords = addBookProv.searchAddressBookRecords(AddressBookRecordsTable.COLUMN_LABEL, mToAddressEditText.getText().toString());
		if (addressBookRecords.size() > 0)
		{
			mToAddressEditText.setTextSize(15);
		}
		
		// Format the 'from address' edit text
		ArrayList<Address> addresses = addProv.searchAddresses(AddressesTable.COLUMN_LABEL, mFromAddressEditText.getText().toString());
		if (addresses.size() > 0)
		{
			mFromAddressEditText.setTextSize(15);
		}
	}
	
	/**
	 * Puts the cursor in the correct place.
	 */
	private void positionCursor()
	{
		if (mToAddressEditText.getText().toString().trim().equals(""))	
		{
			mToAddressEditText.requestFocus();
		}
		else if (mFromAddressEditText.getText().toString().trim().equals(""))	
		{
			mFromAddressEditText.requestFocus();
		}
		else if (mSubjectEditText.getText().toString().trim().equals(""))
		{
			mSubjectEditText.requestFocus();
		}
		else
		{
			mBodyEditText.requestFocus();
		}
	}
	
	/** An inner class used for the listview in the to address selection dialog */
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
           
           // Set the label
           mToAddressSelectionListItemLabelTextView = (TextView)convertView.findViewById(R.id.addressBook_list_item_label_textview);
           
           if (a.getLabel() == null)
           {
        	   mToAddressSelectionListItemLabelTextView.setText("[No label]");
           }
           
           else
           {
        	   mToAddressSelectionListItemLabelTextView.setText(a.getLabel());
           }
           
           // Set the 'to' address
           mToAddressSelectionListItemAddressTextView = (TextView)convertView.findViewById(R.id.addressBook_list_item_address_textview);
           mToAddressSelectionListItemAddressTextView.setText(a.getAddress());
           
           // Set the colour for this view
			int r = a.getColourR();
			int g = a.getColourG();
			int b = a.getColourB();
			 
           convertView.setBackgroundColor(Color.argb(COMPOSE_COLOURS_ALPHA_VALUE, r, g, b));

           int color = Color.argb(0, r, g, b);
           mToAddressSelectionListItemLabelTextView.setBackgroundColor(color);
           mToAddressSelectionListItemAddressTextView.setBackgroundColor(color);      

           return convertView;
       }
   }
    
    /** 
     * An inner class used for the ListView in the 'from address' selection dialog 
     */
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
          if (null == convertView) 
          {
           	 convertView = getLayoutInflater().inflate(R.layout.list_item_identities, parent, false);
          }

          // Configure the view for this Address
          Address a = getItem(position);

          mFromAddressSelectionListItemLabelTextView = (TextView)convertView.findViewById(R.id.identities_list_item_options_label_textview);
           
          if (a.getLabel() == null)
          {
        	  mFromAddressSelectionListItemLabelTextView.setText("[No label]");
          }
           
          else
          {
             mFromAddressSelectionListItemLabelTextView.setText(a.getLabel());
          }
           
          mFromAddressSelectionListItemAddressTextView = (TextView)convertView.findViewById(R.id.identities_list_item_options_address_textview);
          mFromAddressSelectionListItemAddressTextView.setText(a.getAddress());
          
          // Set the colours for this view
          int[] colourValues = ColourCalculator.calculateColoursFromAddress(a.getAddress());  
          convertView.setBackgroundColor(Color.argb(COMPOSE_COLOURS_ALPHA_VALUE, colourValues[0], colourValues[1], colourValues[2]));

          int color = Color.argb(0, colourValues[0], colourValues[1], colourValues[2]);
          mFromAddressSelectionListItemLabelTextView.setBackgroundColor(color);
          mFromAddressSelectionListItemAddressTextView.setBackgroundColor(color);

          return convertView;
      }
   }
	
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) 
 	{
 		// Inflate the menu; this adds items to the action bar if it is present.
 		getMenuInflater().inflate(R.menu.compose_activity_actions, menu);
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
	 	   case R.id.action_send:
	 			sendMessage();
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
 		    	// We are already here, so there's nothing to do
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