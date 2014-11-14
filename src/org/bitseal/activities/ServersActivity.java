package org.bitseal.activities;

import java.util.ArrayList;

import org.bitseal.R;
import org.bitseal.data.ServerRecord;
import org.bitseal.database.ServerRecordProvider;
import org.bitseal.network.ServerHelper;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewGroup;
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

/**
 * The Activity class for the app's Server Settings screen. 
 * 
 * @author Jonathan Coe
 */
public class ServersActivity extends ListActivity
{	
	private ArrayList<ServerRecord> mServerRecords;
    
	private Button mAddNewServerButton;
	private Button mRestoreDefaultServersButton;
	private TextView mListItemUrlTextView;
    private ListView mServersListView;
    
    private static final String TAG = "SERVERS_ACTIVITY";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_servers);
		
		// Set up the data for this activity
		ServerRecordProvider servProv = ServerRecordProvider.get(getApplicationContext());
		mServerRecords = servProv.getAllServerRecords();
		if (mServerRecords.size() == 0)
		{
			ServerHelper servHelp = new ServerHelper();
			servHelp.setupDefaultServers();
			mServerRecords = servProv.getAllServerRecords();
		}
		
		// Set up this activity's view
		ServerRecordAdapter adapter = new ServerRecordAdapter(mServerRecords);
        mServersListView = new ListView(this);
        mServersListView = (ListView)findViewById(android.R.id.list);         
        setListAdapter(adapter);
		
		mAddNewServerButton = (Button) findViewById(R.id.servers_add_new_button);
		mAddNewServerButton.setOnClickListener(new View.OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Add new server button clicked");
			    
		        // Open a dialog to enter the data for the new server record
				final Dialog listItemDialog = new Dialog(ServersActivity.this);
				LinearLayout dialogLayout = (LinearLayout) View.inflate(ServersActivity.this, R.layout.dialog_servers_add_new, null);
				listItemDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				listItemDialog.setContentView(dialogLayout);
				
				WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
			    lp.copyFrom(listItemDialog.getWindow().getAttributes());
			    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
				
			    listItemDialog.show();
			    listItemDialog.getWindow().setAttributes(lp);
			    
			    final EditText listItemDialogUrlEditText= (EditText) dialogLayout.findViewById(R.id.servers_dialog_add_new_url_edittext);
			    final EditText listItemDialogUsernameEditText = (EditText) dialogLayout.findViewById(R.id.servers_dialog_add_new_username_edittext);
			    final EditText listItemDialogPasswordEditText = (EditText) dialogLayout.findViewById(R.id.servers_dialog_add_new_password_edittext);
			    
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				{
			        // Show soft keyboard when the URL Edit Text gains focus
					listItemDialogUrlEditText.setOnFocusChangeListener(new OnFocusChangeListener() 
				    {
			            @Override
			            public void onFocusChange(View v, boolean hasFocus) 
			            {
			            	listItemDialogUrlEditText.post(new Runnable() 
			            	{
			                    @Override
			                    public void run() {
			                        InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
			                        imm.showSoftInput(listItemDialogUrlEditText, InputMethodManager.SHOW_IMPLICIT);
			                    }
			                });
			            }
			        });
				    
				    // Show soft keyboard when the Username Edit Text gains focus
					listItemDialogUsernameEditText.setOnFocusChangeListener(new OnFocusChangeListener() 
				    {
			            @Override
			            public void onFocusChange(View v, boolean hasFocus) 
			            {
			            	listItemDialogUsernameEditText.post(new Runnable() 
			            	{
			                    @Override
			                    public void run() {
			                        InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
			                        imm.showSoftInput(listItemDialogUsernameEditText, InputMethodManager.SHOW_IMPLICIT);
			                    }
			                });
			            }
			        });
					
					// Show soft keyboard when the Password Edit Text gains focus
					listItemDialogPasswordEditText.setOnFocusChangeListener(new OnFocusChangeListener() 
				    {
			            @Override
			            public void onFocusChange(View v, boolean hasFocus) 
			            {
			            	listItemDialogPasswordEditText.post(new Runnable() 
			            	{
			                    @Override
			                    public void run() {
			                        InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
			                        imm.showSoftInput(listItemDialogPasswordEditText, InputMethodManager.SHOW_IMPLICIT);
			                    }
			                });
			            }
			        });
				}
			    
			    Button saveButton = (Button) dialogLayout.findViewById(R.id.servers_dialog_add_new_save_button);
			    saveButton.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						Log.i(TAG, "Add new server dialog save button pressed");
						
						String url = listItemDialogUrlEditText.getText().toString();
						if (url.equals(""))
						{
							Toast.makeText(getApplicationContext(), "The URL must not be blank", Toast.LENGTH_LONG).show();
							return;
						}
						
						String username = listItemDialogUsernameEditText.getText().toString();
						if (username.equals(""))
						{
							Toast.makeText(getApplicationContext(), "The Username must not be blank", Toast.LENGTH_LONG).show();
							return;
						}
						
						String password = listItemDialogPasswordEditText.getText().toString();
						if (password.equals(""))
						{
							Toast.makeText(getApplicationContext(), "The Password must not be blank", Toast.LENGTH_LONG).show();
							return;
						}
						
						ServerRecord newServer = new ServerRecord();
						newServer.setURL(listItemDialogUrlEditText.getText().toString());
						newServer.setUsername(listItemDialogUsernameEditText.getText().toString());
						newServer.setPassword(listItemDialogPasswordEditText.getText().toString());
						
						ServerRecordProvider servProv = ServerRecordProvider.get(getApplicationContext());
						servProv.addServerRecord(newServer);
						
						((ServerRecordAdapter)mServersListView.getAdapter()).notifyDataSetChanged();
						
						listItemDialog.dismiss();
						
						updateListView();
						
						// Our current method for detecting and closing the soft keyboard only works with API 16 and later
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
						{
							closeKeyboardIfOpen();
						}
					}
				});
			    
			    Button cancelButton = (Button) dialogLayout.findViewById(R.id.servers_dialog_add_new_cancel_button);
			    cancelButton.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						Log.i(TAG, "Add new server dialog cancel button pressed");		
						
						listItemDialog.dismiss();
					}
				});
			}
		});
		
		mRestoreDefaultServersButton = (Button) findViewById(R.id.servers_restore_defaults_button);
		mRestoreDefaultServersButton.setOnClickListener(new View.OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "Restore default servers button clicked");
				
				// Open a dialog to confirm or cancel the restoration of the default set of servers
				final Dialog restoreDefaultsDialog = new Dialog(ServersActivity.this);
				LinearLayout dialogLayout = (LinearLayout) View.inflate(ServersActivity.this, R.layout.dialog_servers_restore_defaults, null);
				restoreDefaultsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				restoreDefaultsDialog.setContentView(dialogLayout);
				
				WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
			    lp.copyFrom(restoreDefaultsDialog.getWindow().getAttributes());
			    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
				
			    restoreDefaultsDialog.show();
			    restoreDefaultsDialog.getWindow().setAttributes(lp);		  
			    
			    Button confirmButton = (Button) dialogLayout.findViewById(R.id.servers_restore_defaults_dialog_confirm_button);
			    confirmButton.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						Log.i(TAG, "Restore default servers dialog confirm button pressed");							
						
						// Delete any servers in the database
						ServerRecordProvider servProv = ServerRecordProvider.get(getApplicationContext());	
						servProv.deleteAllServerRecords();
						
						// Restore the default set of servers
						ServerHelper servHelp = new ServerHelper();
						servHelp.setupDefaultServers();
				        
						restoreDefaultsDialog.dismiss();
						
						updateListView();
					}
				});
			    
			    Button cancelButton = (Button) dialogLayout.findViewById(R.id.servers_restore_defaults_dialog_cancel_button);
			    cancelButton.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						Log.i(TAG, "Restore default servers dialog cancel button pressed");							
						
						restoreDefaultsDialog.dismiss();
					}
				});
			}
		});
	}
	
	/**
	 * If the soft keyboard is open, this method will close it. 
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void closeKeyboardIfOpen()
	{
		final View activityRootView = getWindow().getDecorView().getRootView();	
		final OnGlobalLayoutListener globalListener = new OnGlobalLayoutListener()
		{
			@Override
			public void onGlobalLayout() 
			{
			    Rect rect = new Rect();
			    // rect will be populated with the coordinates of your view that area still visible.
			    activityRootView.getWindowVisibleDisplayFrame(rect);

			    int heightDiff = activityRootView.getRootView().getHeight() - (rect.bottom - rect.top);
			    if (heightDiff > 100)
			    {
			    	// If the difference is more than 100 pixels, it's probably cause by the soft keyboard being open. Now we want to close it.
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0); // Toggle the soft keyboard. 
			    }
			    
			    // Now we have to remove the OnGlobalLayoutListener, otherwise we will experience errors
			    activityRootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		};
		activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(globalListener);
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
	
	// Needed to update the ListView after restoring the default list of servers
    private void updateListView()
    {
		// Set up the data for this activity
		ServerRecordProvider servProv = ServerRecordProvider.get(getApplicationContext());
		mServerRecords = servProv.getAllServerRecords();
    	
		ServerRecordAdapter adapter = new ServerRecordAdapter(mServerRecords);
        mServersListView = new ListView(this);
        mServersListView = (ListView)findViewById(android.R.id.list);         
        setListAdapter(adapter);
    }
	
	public void onListItemClick(ListView l, View v, int position, long id) 
    {
        // Get the ServerRecord selected from the adapter
		final ServerRecord selectedRecord = ((ServerRecordAdapter)mServersListView.getAdapter()).getItem(position);
	    
        // Open a dialog to view the data for the selected server record
		final Dialog listItemDialog = new Dialog(ServersActivity.this);
		LinearLayout dialogLayout = (LinearLayout) View.inflate(ServersActivity.this, R.layout.dialog_servers_list_item_options, null);
		listItemDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		listItemDialog.setContentView(dialogLayout);
		
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
	    lp.copyFrom(listItemDialog.getWindow().getAttributes());
	    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
		
	    listItemDialog.show();
	    listItemDialog.getWindow().setAttributes(lp);
	    
	    final EditText listItemDialogUrlEditText= (EditText) dialogLayout.findViewById(R.id.servers_dialog_list_item_url_edittext);
	    final EditText listItemDialogUsernameEditText = (EditText) dialogLayout.findViewById(R.id.servers_dialog_list_item_username_edittext);
	    final EditText listItemDialogPasswordEditText = (EditText) dialogLayout.findViewById(R.id.servers_dialog_list_item_password_edittext);
	    
	    // Set the text of the two EditTexts in the dialog
	    listItemDialogUrlEditText.setText(selectedRecord.getURL());
	    listItemDialogUsernameEditText.setText(selectedRecord.getUsername());
	    listItemDialogPasswordEditText.setText(selectedRecord.getPassword());
	    
	    // Set the position of the cursor in each EditText to the end of the text
	    listItemDialogUrlEditText.setSelection(listItemDialogUrlEditText.getText().length());
	    listItemDialogUsernameEditText.setSelection(listItemDialogUsernameEditText.getText().length());
	    listItemDialogPasswordEditText.setSelection(listItemDialogPasswordEditText.getText().length());
	    
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
	        // Show soft keyboard when the URL Edit Text gains focus
			listItemDialogUrlEditText.setOnFocusChangeListener(new OnFocusChangeListener() 
		    {
	            @Override
	            public void onFocusChange(View v, boolean hasFocus) 
	            {
	            	listItemDialogUrlEditText.post(new Runnable() 
	            	{
	                    @Override
	                    public void run() {
	                        InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	                        imm.showSoftInput(listItemDialogUrlEditText, InputMethodManager.SHOW_IMPLICIT);
	                    }
	                });
	            }
	        });
		    
		    // Show soft keyboard when the Username Edit Text gains focus
			listItemDialogUsernameEditText.setOnFocusChangeListener(new OnFocusChangeListener() 
		    {
	            @Override
	            public void onFocusChange(View v, boolean hasFocus) 
	            {
	            	listItemDialogUsernameEditText.post(new Runnable() 
	            	{
	                    @Override
	                    public void run() {
	                        InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	                        imm.showSoftInput(listItemDialogUsernameEditText, InputMethodManager.SHOW_IMPLICIT);
	                    }
	                });
	            }
	        });
			
			// Show soft keyboard when the Password Edit Text gains focus
			listItemDialogPasswordEditText.setOnFocusChangeListener(new OnFocusChangeListener() 
		    {
	            @Override
	            public void onFocusChange(View v, boolean hasFocus) 
	            {
	            	listItemDialogPasswordEditText.post(new Runnable() 
	            	{
	                    @Override
	                    public void run() {
	                        InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	                        imm.showSoftInput(listItemDialogPasswordEditText, InputMethodManager.SHOW_IMPLICIT);
	                    }
	                });
	            }
	        });
		}
	    
	    Button saveButton = (Button) dialogLayout.findViewById(R.id.servers_dialog_list_item_save_button);
	    saveButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "List item dialog save button pressed");
				
				String url = listItemDialogUrlEditText.getText().toString();
				if (url.equals(""))
				{
					Toast.makeText(getApplicationContext(), "The URL must not be blank", Toast.LENGTH_LONG).show();
					return;
				}
				
				String username = listItemDialogUsernameEditText.getText().toString();
				if (username.equals(""))
				{
					Toast.makeText(getApplicationContext(), "The Username must not be blank", Toast.LENGTH_LONG).show();
					return;
				}
				
				String password = listItemDialogPasswordEditText.getText().toString();
				if (password.equals(""))
				{
					Toast.makeText(getApplicationContext(), "The Password must not be blank", Toast.LENGTH_LONG).show();
					return;
				}
				
				selectedRecord.setURL(listItemDialogUrlEditText.getText().toString());
				selectedRecord.setUsername(listItemDialogUsernameEditText.getText().toString());
				selectedRecord.setPassword(listItemDialogPasswordEditText.getText().toString());
				
				ServerRecordProvider servProv = ServerRecordProvider.get(getApplicationContext());
				servProv.updateServerRecord(selectedRecord);
				
				((ServerRecordAdapter)mServersListView.getAdapter()).notifyDataSetChanged();
				
				listItemDialog.dismiss();
				
				// Our current method for detecting and closing the soft keyboard only works with API 16 and later
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
				{
					closeKeyboardIfOpen();
				}
			}
		});
	    
	    Button copyButton = (Button) dialogLayout.findViewById(R.id.servers_dialog_list_item_copy_button);
	    copyButton.setOnClickListener(new View.OnClickListener()
		{
			@SuppressWarnings("deprecation")
			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
			@Override
			public void onClick(View v)
			{
				Log.i(TAG, "List item dialog copy button pressed");							
				
				selectedRecord.getURL();
				
				int sdk = android.os.Build.VERSION.SDK_INT;
				
				if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) 
				{
				    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				    clipboard.setText(selectedRecord.getURL());
				} 
				
				else 
				{
				    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
				    android.content.ClipData clip = android.content.ClipData.newPlainText("COPIED_URL", selectedRecord.getURL());
				    clipboard.setPrimaryClip(clip);
				}
				
				listItemDialog.dismiss();
				
				Toast.makeText(getApplicationContext(), "Server URL copied to the clipboard", Toast.LENGTH_LONG).show();
			}
		});
	    
	    Button deleteButton = (Button) dialogLayout.findViewById(R.id.servers_dialog_list_item_delete_button);
	    deleteButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{ 
				Log.i(TAG, "List item dialog delete button pressed");
				
		        // Open a dialog to confirm or cancel the deletion of the message
				final Dialog deleteDialog = new Dialog(ServersActivity.this);
				LinearLayout dialogLayout = (LinearLayout) View.inflate(ServersActivity.this, R.layout.dialog_servers_server_record_delete, null);
				deleteDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				deleteDialog.setContentView(dialogLayout);
				
				WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
			    lp.copyFrom(deleteDialog.getWindow().getAttributes());
			    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
				
			    deleteDialog.show();
			    deleteDialog.getWindow().setAttributes(lp);		  
			    
			    Button confirmButton = (Button) dialogLayout.findViewById(R.id.servers_delete_dialog_confirm_button);
			    confirmButton.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						Log.i(TAG, "Server record delete dialog confirm button pressed");
						
						mServerRecords.remove(selectedRecord);
						
						// Delete the selected ServerRecord from the application database
						ServerRecordProvider servProv = ServerRecordProvider.get(getApplicationContext());
						servProv.deleteServerRecord(selectedRecord);
						
						((ServerRecordAdapter)mServersListView.getAdapter()).notifyDataSetChanged();
				        deleteDialog.dismiss();
						listItemDialog.dismiss();
					}
				});
			    
			    Button cancelButton = (Button) dialogLayout.findViewById(R.id.servers_delete_dialog_cancel_button);
			    cancelButton.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						Log.i(TAG, "Server Record delete dialog cancel button pressed");							
						
						deleteDialog.dismiss();
					}
				});
			}
		});
     }
    
     private class ServerRecordAdapter extends ArrayAdapter<ServerRecord> 
     {
        public ServerRecordAdapter(ArrayList<ServerRecord> serverRecords) 
        {
            super(getBaseContext(), android.R.layout.simple_list_item_1, serverRecords);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) 
        {
            // If we weren't given a view, inflate one
            if (null == convertView) 
            {
            	convertView = getLayoutInflater().inflate(R.layout.list_item_servers, parent, false);
            }

            // Configure the view for this Server Record
            ServerRecord s = getItem(position);

            mListItemUrlTextView = (TextView)convertView.findViewById(R.id.servers_list_item_url_textview);
            
            mListItemUrlTextView.setText(s.getURL());

            return convertView;
        }
    }
}