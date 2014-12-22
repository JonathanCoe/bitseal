package org.bitseal.network;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.bitseal.core.App;
import org.bitseal.data.ServerRecord;
import org.bitseal.database.ServerRecordProvider;

import android.util.Log;
import de.timroes.axmlrpc.XMLRPCClient;
import de.timroes.axmlrpc.XMLRPCException;

/**
 * An object which uses the XMLRPC client class to connect to servers running
 * PyBitmessage and call methods from the PyBitmessage API.
 * 
 * @author Jonathan Coe
 */
public class ApiCaller
{
	private URL url;
	private String username;
	private String password;
	
	private XMLRPCClient newClient;
	private XMLRPCClient client;
	
	private ArrayList<URL> urlList;
	private ArrayList<String> usernameList;
	private ArrayList<String> passwordList;
	
	private int urlCounter;
	private int usernameCounter;
	private int passwordCounter;
	
	private int numberOfServers;
	
	/**
	 * This constant defines the timeout period for API calls.
	 */
	private static final int TIMEOUT_SECONDS = 10;
	
	/**
	 * API command used for connection testing
	 */
	private static final String API_METHOD_ADD = "add";
	
	private static final String TAG = "API_CALLER";
	
	/**
	 * Creates a new ApiCaller object and sets the URL, username, and password values needed
	 * to connect to the PyBitmessage servers.
	 */
	public ApiCaller()
	{	
		// Check if any server records exist in app storage. If not, set up the default list of server records. 
		ServerRecordProvider servProv = ServerRecordProvider.get(App.getContext());
		ArrayList<ServerRecord> retrievedServerRecords = servProv.getAllServerRecords();
		if (retrievedServerRecords.size() == 0)
		{
			Log.i(TAG, "No server records found in app storage. Setting up list of default servers.");
			ServerHelper servHelp = new ServerHelper();
			servHelp.setupDefaultServers();
			// Now the server records should be available from the database
			retrievedServerRecords = servProv.getAllServerRecords();
		}
		numberOfServers = retrievedServerRecords.size();
		
		// TODO: This can be removed in the version after 0.5.1
		retrievedServerRecords = updateDefaultServers(retrievedServerRecords);
				
        // Set up ArrayLists for the URLs, usernames, and passwords of the servers
    	urlList = new ArrayList<URL>();
		usernameList = new ArrayList<String>();
		passwordList = new ArrayList<String>();

		// Randomize the order of the server records list in order to avoid servers always being called in 
		// the same order. 
		Collections.shuffle(retrievedServerRecords, new SecureRandom());
		
		int arrayListIndex = 0;
		for(ServerRecord s : retrievedServerRecords)
		{
			try
			{
				urlList.add(arrayListIndex, new URL(s.getURL()));
				usernameList.add(arrayListIndex, s.getUsername());
				passwordList.add(arrayListIndex, s.getPassword());
				
				arrayListIndex ++;
			}
			catch (MalformedURLException e)
			{
				Log.e(TAG, "Malformed URL exception occurred in ApiCaller constructor. We will ignore the ServerRecord that contains this " +
						"url. The String representation of the url was " + s.getURL());
				arrayListIndex ++;
			}
		}
        
		// Start at the beginning of each of the three lists
		urlCounter = 0;
		usernameCounter = 0;
		passwordCounter = 0;
		
		url = urlList.get(urlCounter);
		username = usernameList.get(usernameCounter);
		password = passwordList.get(passwordCounter);
		
		client = setUpClient(url, username, password);
		
		Log.i(TAG, "ApiCaller setup completed");
	}
	
	/**
	 * Temporary code used to update the IP address of one of the
	 * default servers. 
	 */
	private ArrayList<ServerRecord> updateDefaultServers(ArrayList<ServerRecord> currentServerRecords)
	{
		// Check whether the old server IP address is in use
		for (ServerRecord s : currentServerRecords)
		{
			if (s.getURL().equals("http://128.199.211.11:8442"))
			{
				// Restore the default server list (which now contains the updated IP address)
				Log.d(TAG, "Updating the list of servers");
				
				ServerRecordProvider servProv = ServerRecordProvider.get(App.getContext());
				servProv.deleteAllServerRecords();
				
				ServerHelper servHelp = new ServerHelper();
				servHelp.setupDefaultServers();
				
				return servProv.getAllServerRecords();
			}
		}
		
		// If the old IP address is not in use
		return currentServerRecords;
	}
	
	/**
     * Makes a call to the PyBitmessage XMLRPC API. <br><br>
     * 
     * Attempts to establish a connection to one of the listed servers. The method will attempt to
	 * connect to each server in sequence, until either a connection is successfully established or
	 * all servers have been tested without any successful connection. If a connection is successfully
	 * established, then the API call will be made.
     *
     * @param method - A String which specifies the API method to be called
     * @param params - One or more Objects which provide the parameters for the API call
     * 
     * @return An Object containing the result of the API call
     */  
	public Object call(String method, Object... params)
	{				
		while (urlCounter < urlList.size())
		{
			boolean connectionSuccessful = doConnectionTest();
			
			if (connectionSuccessful == true)
			{
				Log.i(TAG, "Successfully connected to " + url.toString());
				
				try
				{	
					Log.i(TAG, "About to make an API call to " + url.toString());
					
					Object result = client.call(method, params);
					
					return result;
				}
				
				catch (XMLRPCException e)
				{
					Log.e(TAG, "XMLRPCException occurred in ApiCaller.call() \n" + 
							"Execption message was: " + e.getMessage());
					switchToNextServer();
				}
				catch (IllegalStateException e)
				{
					Log.e(TAG, "IllegalStateException occurred in ApiCaller.call() \n" + 
							"Execption message was: " + e.getMessage());
					switchToNextServer();
				}
				catch (Exception e)
				{
					Log.e(TAG, "An Exception occurred in ApiCaller.call() \n" + 
							"Execption message was: " + e.getMessage());
					switchToNextServer();
				}
			}
			else
			{
				switchToNextServer();
			}
		}
		throw new RuntimeException("API call failed after trying all listed servers. Last attempted URL was " + url.toString());
	}
	
	/**
	 * Sets up the XMLRPC client to use the next server in the list. If the end
	 * of the list has been reached, throws a RuntimeException. 
	 */
	public void switchToNextServer()
	{
		if (urlCounter < (urlList.size() - 1))
		{
			Log.i(TAG, "Currently the URL in use is " + url.toString() + ", about to change to next URL");
			
			urlCounter ++;
			usernameCounter ++;
			passwordCounter ++;
			
			url = urlList.get(urlCounter);
			username = usernameList.get(usernameCounter);
			password = passwordList.get(passwordCounter);
			
			client = setUpClient(url, username, password);
		}
		else
		{
			throw new RuntimeException("API call failed after trying all listed servers. Last attempted URL was " + url.toString());
		}
	}
	
	/**
	 * Returns the number of servers in use. 
	 */
	public int getNumberOfServers()
	{
		return numberOfServers;
	}
		
	/**
     * Performs a connection test by calling the "add" method from the PyBitmessage API and
     * checking if the returned result (if any) is correct. 
     *  
     * @return A boolean indicating whether or not a connection was successfully established
     */
    private boolean doConnectionTest() 
    {   	
    	Object rawResult = null;
    	
    	try 
		{
			Log.i(TAG, "Running doConnectionTest() with server at " + url.toString());
			
			int result = -1; // Explicitly set this value to ensure a meaningful test. The testInt values will always be >=0, so the test should never give a false positive result.
			
			Random rand = new Random();
			int testInt1 = rand.nextInt(5000);
			int testInt2 = rand.nextInt(5000);
			int sumOfTestInts = testInt1 + testInt2;
			
			rawResult = client.call(API_METHOD_ADD, testInt1, testInt2); // Test the connection with some random values that are unlikely to come up by chance
			result = (Integer) rawResult;
			
			if (result == sumOfTestInts)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		
		catch (XMLRPCException e) 
		{
			Log.e(TAG, "XMLRPCException occurred in ApiCaller.doConnectionTest() \n" + 
					"Execption message was: " + e.getMessage());
					e.printStackTrace();
			return false;
		}
		
		catch (IllegalStateException e) 
		{
			Log.e(TAG, "IllegalStateException occurred in ApiCaller.doConnectionTest() \n" + 
					"Execption message was: " + e.getMessage());
			return false;
		}
		
		catch (Exception e) 
		{
			Log.e(TAG, "An Exception occurred in ApiCaller.doConnectionTest() \n" + 
					"Execption message was: " + e.getMessage());
			Log.e(TAG, "The raw result of the connection test was: " + rawResult.toString());
			return false;
		}
    }
	
	/**
	 * Sets up a XMLRPC client and provides it with the data necessary to connect with 
	 * the server via the XMLRPC API. 
	 * 
	 * @param url A URL object containing the IP address and port number of the server, in the form "http://23.21.148.16:8442"
	 * @param username A String containing the username required to access the PyBitmessage API. Specified in the server's
	 * local copy of the "keys.dat" file
	 * @param password A String containing the password required to access the PyBitmessage API. Specified in the server's
	 * local copy of the "keys.dat" file
	 * 
	 * @return An XMLRPCClient object that can be used to make XMLRPC calls to Bitseal servers
	 */
	private XMLRPCClient setUpClient(URL url, String username, String password)
	{
		newClient = new XMLRPCClient(url);	
		newClient.setLoginData(username, password);
		newClient.setTimeout(TIMEOUT_SECONDS);
		return newClient;
	}
}