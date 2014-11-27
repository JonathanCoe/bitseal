package org.bitseal.activities;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.cacheword.PassphraseSecrets;

import org.bitseal.R;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The Activity class for the app's 'Security Settings' screen. 
 * 
 * @author Jonathan Coe
 */
public class SecurityActivity extends Activity implements ICacheWordSubscriber
{
    private CheckBox databaseEncryptionCheckbox;
    
    private TextView enterPassphraseLabelTextView;
    private TextView confirmPassphraseLabelTextView;
    
    private EditText enterPassphraseEditText;
    private EditText confirmPassphraseEditText;
    
    private TextWatcher passphraseEditTextsWatcher;
    
    private Button changePassphraseButton;
    private Button savePassphraseButton;
    private Button cancelPassphraseButton;
    
    private CacheWordHandler mCacheWordHandler;
    
    /** The minimum length we will allow for a database encryption passphrase */
    private static final int MINIMUM_PASSPHRASE_LENGTH = 8;
    
    /** A placeholder String that we use to fill out the passphrase edit texts when a user-defined passphrase
     * has previously been set. This means that we avoid leaking the length of the existing passphrase through the UI. */
    private static final String PLACEHOLDER_PASSPHRASE = "thisIsARelativelyLongString";
    
    /** The key for a boolean variable that records whether or not the "enable database encryption" checkbox has been selected
     * by the user */
    private static final String KEY_DATABASE_ENCRYPTION_SELECTED = "databaseEncryptionSelected";
    
    /** The key for a boolean variable that records whether or not a user-defined database encryption passphrase has been saved */
    private static final String KEY_DATABASE_PASSPHRASE_SAVED = "databasePassphraseSaved";
    	
	private static final String TAG = "SECURITY_ACTIVITY";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_security);
		
		// Connect to the CacheWordService
		mCacheWordHandler = new CacheWordHandler(this);
		mCacheWordHandler.connectToService();
				
		enterPassphraseLabelTextView = (TextView) findViewById(R.id.security_enter_passphrase_label_textview);
		confirmPassphraseLabelTextView = (TextView) findViewById(R.id.security_confirm_passphrase_label_textview);
		
		enterPassphraseEditText = (EditText) findViewById(R.id.security_enter_passphrase_edittext);
		confirmPassphraseEditText = (EditText) findViewById(R.id.security_confirm_passphrase_edittext);
		
		passphraseEditTextsWatcher = new TextWatcher()
		{			
			boolean runningInTextWatcher = false; // Used to prevent infinite loops with the TextWatcher causes itself to be called
			
			public void afterTextChanged(Editable s) 
			{
				// Nothing to do here
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
				if (runningInTextWatcher)
				{
					return;
				}
				
				runningInTextWatcher = true;
				
				if (enterPassphraseEditText.getText().toString().equals(PLACEHOLDER_PASSPHRASE) && confirmPassphraseEditText.getText().toString().equals(PLACEHOLDER_PASSPHRASE))
			    {
			    	enterPassphraseEditText.setText("");
					confirmPassphraseEditText.setText("");
			    }
				
			    runningInTextWatcher = false;
			}

			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				savePassphraseButton.setVisibility(View.VISIBLE);
			    cancelPassphraseButton.setVisibility(View.VISIBLE);
			}
		};
		
		enterPassphraseEditText.addTextChangedListener(passphraseEditTextsWatcher);
		confirmPassphraseEditText.addTextChangedListener(passphraseEditTextsWatcher);
		
		changePassphraseButton = (Button) findViewById(R.id.security_change_passphrase_button);
		changePassphraseButton.setVisibility(View.GONE);
		changePassphraseButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Security settings change passphrase button clicked");
				
				showDatabaseEncryptionUI();
				
				changePassphraseButton.setVisibility(View.GONE);
			}
		});
		
		savePassphraseButton = (Button) findViewById(R.id.security_save_passphrase_button);
		savePassphraseButton.setOnClickListener(new View.OnClickListener()
		{		
			@SuppressLint("InlinedApi")
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Security settings save passphrase button clicked");
				
				String enteredPassphrase = enterPassphraseEditText.getText().toString();
				String confirmedPassphrase = confirmPassphraseEditText.getText().toString();
				
				if (validateDatabasePassphrase(enteredPassphrase, confirmedPassphrase))
				{
					boolean passphraseModificationSuccessful;
					
					// Check whether this is the first time that the user has set a database passphrase
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
					boolean databasePassphraseSaved= prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false);
					if (databasePassphraseSaved)
					{
						// Change the database passphrase
						passphraseModificationSuccessful = changeDatabasePassphrase(enteredPassphrase);
					}
					else
					{
						// Set the database passphrase for the first time
						passphraseModificationSuccessful = setDatabasePassphrase(enteredPassphrase);
					}
					
					if (passphraseModificationSuccessful)
					{
						databaseEncryptionCheckbox.setChecked(true);
						databaseEncryptionCheckbox.setText("Database encryption enabled");
						
						Log.d(TAG, "TEMPORARY: About to run close keyboard routine");
						closeKeyboardIfOpen();
						
					    savePassphraseButton.setVisibility(View.GONE);
					    cancelPassphraseButton.setVisibility(View.GONE);
					}
					else
					{
						Toast.makeText(getBaseContext(), "An error occurred while attempting to set the database passphrase", Toast.LENGTH_LONG).show();
					}
				}
			}
		});
		
		cancelPassphraseButton = (Button) findViewById(R.id.security_cancel_passphrase_button);
		cancelPassphraseButton.setOnClickListener(new View.OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Security settings cancel passphrase button clicked");
				
        		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        		boolean databasePassphraseSaved= prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false);
        		if (databasePassphraseSaved)
        		{
    				hideDatabaseEncryptionUI();
    				
    				changePassphraseButton.setVisibility(View.VISIBLE);
				    
				    closeKeyboardIfOpen();
        		}
        		else
        		{
        			disableDatabaseEncryptionRoutine();
        		}
			}
		});
		
		databaseEncryptionCheckbox = (CheckBox) findViewById(R.id.security_database_encryption_checkbox);
		databaseEncryptionCheckbox.setOnClickListener(new View.OnClickListener() 
        {
        	@Override
			public void onClick(View v) 
        	{
	            if (databaseEncryptionCheckbox.isChecked()) 
	            {
	            	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	            	SharedPreferences.Editor editor = prefs.edit();
	    		    editor.putBoolean(KEY_DATABASE_ENCRYPTION_SELECTED, true);
	    		    editor.commit();
	    		    
	    		    showDatabaseEncryptionUI();
	            } 
	            else 
	            {
	        		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	        		boolean databasePassphraseSaved= prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false);
	        		if (databasePassphraseSaved)
	        		{
	        			openDisableEncryptionConfirmDialog();
	        		}
	        		else
	        		{
	        			disableDatabaseEncryptionRoutine();
	        		}
	            }
        	}
        });
		
		// Read the Shared Preferences to determine whether or not the database encryption settings should be visible
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		boolean databasePassphraseSaved = prefs.getBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false);
		Log.i(TAG, "Database passphrase saved is set to " + databasePassphraseSaved);
		if (databasePassphraseSaved == true)
		{
			changePassphraseButton.setVisibility(View.VISIBLE);
			
			databaseEncryptionCheckbox.setChecked(true);
			databaseEncryptionCheckbox.setText("Database encryption enabled");
			
			enterPassphraseEditText.setText(PLACEHOLDER_PASSPHRASE);
			confirmPassphraseEditText.setText(PLACEHOLDER_PASSPHRASE);
		}
		else
		{		
			hideDatabaseEncryptionUI();
			
			databaseEncryptionCheckbox.setChecked(false);
		}
		
		hideDatabaseEncryptionUI();
	}
    
	/**
	 * Opens a dialog box to confirm that the user wants to disable database encryption
	 */
    private void openDisableEncryptionConfirmDialog()
    {
		// Open a dialog to confirm or cancel the creation of a new address
		final Dialog disableEncryptionConfirmDialog = new Dialog(SecurityActivity.this);
		LinearLayout dialogLayout = (LinearLayout) View.inflate(SecurityActivity.this, R.layout.dialog_security_disable_database_encryption_confirm, null);
		disableEncryptionConfirmDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		disableEncryptionConfirmDialog.setContentView(dialogLayout);
		
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
	    lp.copyFrom(disableEncryptionConfirmDialog.getWindow().getAttributes());
	    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
		
	    disableEncryptionConfirmDialog.show();
	    disableEncryptionConfirmDialog.getWindow().setAttributes(lp);		  
	    
	    Button confirmButton = (Button) dialogLayout.findViewById(R.id.security_disable_database_encryption_confirm_confirm_button);
	    confirmButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Security settings screen enable database encryption confirm button pressed");
				
				disableDatabaseEncryptionRoutine();
				
				disableEncryptionConfirmDialog.dismiss();
			}
		});
	    Button cancelButton = (Button) dialogLayout.findViewById(R.id.security_disable_database_encryption_confirm_cancel_button);
	    cancelButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Security settings screen enable database encryption cancel button pressed");
				
				databaseEncryptionCheckbox.setChecked(true);
				
				disableEncryptionConfirmDialog.dismiss();
			}
		});
	}
    
    /**
     * A small collection of things to be done when the user disables database encryption
     */
    private void disableDatabaseEncryptionRoutine()
    {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	SharedPreferences.Editor editor = prefs.edit();
	    editor.putBoolean(KEY_DATABASE_ENCRYPTION_SELECTED, false);
	    editor.putBoolean(KEY_DATABASE_PASSPHRASE_SAVED, false);
	    editor.commit();
    	
	    hideDatabaseEncryptionUI();
	    
	    databaseEncryptionCheckbox.setChecked(false);
	    databaseEncryptionCheckbox.setText("Enable database encryption");
	    
		closeKeyboardIfOpen();
		
		clearPassphraseEditTexts();
		
		savePassphraseButton.setVisibility(View.GONE);
	    cancelPassphraseButton.setVisibility(View.GONE);
    }
	
	private void showDatabaseEncryptionUI()
	{
	    enterPassphraseLabelTextView.setVisibility(View.VISIBLE);
	    confirmPassphraseLabelTextView.setVisibility(View.VISIBLE);
	    enterPassphraseEditText.setVisibility(View.VISIBLE);
	    confirmPassphraseEditText.setVisibility(View.VISIBLE);
	    savePassphraseButton.setVisibility(View.VISIBLE);
	    cancelPassphraseButton.setVisibility(View.VISIBLE);
	}
	
	private void hideDatabaseEncryptionUI()
	{
	    enterPassphraseLabelTextView.setVisibility(View.GONE);
	    confirmPassphraseLabelTextView.setVisibility(View.GONE);
	    enterPassphraseEditText.setVisibility(View.GONE);
	    confirmPassphraseEditText.setVisibility(View.GONE);
	    savePassphraseButton.setVisibility(View.GONE);
	    cancelPassphraseButton.setVisibility(View.GONE);
	}
	
	/**
	 * Takes a pair Strings and validates them, determining whether or not
	 * they contain a valid database passphrase. 
	 * 
	 * @param enteredPassphrase - The entered passphrase
	 * @param confirmedPassphrase - The confirmed passphrase
	 * 
	 * @return A boolean indicating whether the given Strings are a valid passphrase
	 */
	private boolean validateDatabasePassphrase(String enteredPassphrase, String confirmedPassphrase)
	{
		// Check whether the passphrases matched
		if (enteredPassphrase.equals(confirmedPassphrase) == false)
		{
			Toast.makeText(this, "The passphrases do not match", Toast.LENGTH_LONG).show();
			Log.e(TAG, "The passphrases do not match");
			return false;
		}
		
		// Check the length of the passphrase
		if (enteredPassphrase.length() < MINIMUM_PASSPHRASE_LENGTH)
		{
			Toast.makeText(this, "The passphrase must be at least " + MINIMUM_PASSPHRASE_LENGTH + " characters long", Toast.LENGTH_LONG).show();
			Log.e(TAG, "The passphrase entered is too short - only " + enteredPassphrase.length() + " characters in length.\n" +
					"The passphrase must be at least " + MINIMUM_PASSPHRASE_LENGTH + " characters in length");
			return false;
		}
		
		// The passphrases entered appear to match and be valid
		return true;
	}
	
	/**
	 * Sets the passphrase of the encrypted database
	 * 
	 * @param passphrase - The new passphrase
	 */
	private boolean setDatabasePassphrase(String newPassphrase)
	{
		try
		{
			mCacheWordHandler.setPassphrase(newPassphrase.toCharArray());
		}
		catch (Exception e)
		{
			Log.e(TAG, "Attempt to set the database encryption passphrase failed.\n" + 
					"The exception message was: " + e.getMessage());
			return false;
		}
		
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	SharedPreferences.Editor editor = prefs.edit();
	    editor.putBoolean(KEY_DATABASE_PASSPHRASE_SAVED, true); 
	    editor.commit();
	    
		Toast.makeText(this, "Database encryption passphrase set successfully", Toast.LENGTH_LONG).show();
	    
	    return true;
	}
	
	/**
	 * Changes the passphrase of the encrypted database
	 * 
	 * @param passphrase - The new passphrase
	 */
	private boolean changeDatabasePassphrase(String newPassphrase)
	{
		try
		{
			mCacheWordHandler.changePassphrase((PassphraseSecrets) mCacheWordHandler.getCachedSecrets(), newPassphrase.toCharArray());
		}
		catch (Exception e)
		{
			Log.e(TAG, "Attempt to change the database encryption passphrase failed.\n" + 
					"The exception message was: " + e.getMessage());
			return false;
		}
		
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	SharedPreferences.Editor editor = prefs.edit();
	    editor.putBoolean(KEY_DATABASE_PASSPHRASE_SAVED, true);
	    editor.commit();
	    
	    Toast.makeText(this, "Database encryption passphrase changed successfully", Toast.LENGTH_LONG).show();
	    
	    return true;
	}
	
	/**
	 * Clears the 'enter passphrase' and 'confirm passphrase' edit texts.
	 */
	private void clearPassphraseEditTexts()
	{
		enterPassphraseEditText.setText("");
		confirmPassphraseEditText.setText("");
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
				    
				    Log.d(TAG, "TEMPORARY: About to check height diff");
	
				    int heightDiff = activityRootView.getRootView().getHeight() - (rect.bottom - rect.top);
				    if (heightDiff > 100)
				    {
				    	Log.d(TAG, "TEMPORARY: Found a height diff of more than 100 pixels");
				    	
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
		Log.d(TAG, "TEMPORARY: SecurityActivity.onCacheWordLocked() called.");
		
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
		Log.d(TAG, "TEMPORARY: SecurityActivity.onCacheWordOpened() called.");
		
		// Nothing to do here currently
	}
	
	@Override
	public void onCacheWordUninitialized()
	{
		Log.d(TAG, "TEMPORARY: SecurityActivity.onCacheWordUninitialized() called.");
		
		// Database encryption is currently not enabled by default, so there is nothing to do here
	}
}