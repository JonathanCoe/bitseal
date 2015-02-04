package org.bitseal.network;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;

import org.bitseal.core.App;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This class provides helper methods related to network connections. 
 * 
 * @author Jonathan Coe
 */
public class NetworkHelper
{
	// URLs of some websites that should have very high uptime
	private static final String URL_USED_TO_CHECK_CONNECTION_0 = "http://www.google.com";
	private static final String URL_USED_TO_CHECK_CONNECTION_1 = "http://www.facebook.com";
	private static final String URL_USED_TO_CHECK_CONNECTION_2 = "http://www.xinhuanet.com";
	private static final String URL_USED_TO_CHECK_CONNECTION_3 = "http://www.baidu.com";
	
	private static final int MAX_RESPONSE_TIME_MILLISECONDS = 1500;
	private static final int HTTP_RESPONSE_CODE_OK = 200;
	
    /** The key for a boolean variable that records whether or not the user has selected the 'wifi only' option*/
    private static final String WIFI_ONLY_SELECTED = "wifiOnlySelected";
	
	private static final String TAG = "NETWORK_UTILS";
	
	/**
	 * Checks whether an internet connection is available. <br><br>
	 * 
	 * Credit to THelper on StackOverflow for this method.<br> 
	 * See: https://stackoverflow.com/questions/6493517
	 * 
	 * @return A boolean indicating whether or not an internet
	 * connection is available
	 */
	public static boolean checkInternetAvailability()
	{
		// Check whether any network connection is available
		Context appContext = App.getContext();
		if (checkNetworkAvailablility(appContext))
	    {
			// If the user has the 'wifi only' option enabled, check whether we are connected to a wifi network
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
			if (prefs.getBoolean(WIFI_ONLY_SELECTED, false))
			{
				if (checkWifiConnected(appContext) == false)
			    {
			        Log.d(TAG, "The user has the 'wifi only' option enabled and we are not currently connected to a wifi network.");
			        return false;
			    }
			}
	
			// Set up the list of URLs used to check for an active internet connection
			ArrayList<String> urls = new ArrayList<String>();
	        urls.add(URL_USED_TO_CHECK_CONNECTION_0);
	        urls.add(URL_USED_TO_CHECK_CONNECTION_1);
	        urls.add(URL_USED_TO_CHECK_CONNECTION_2);
	        urls.add(URL_USED_TO_CHECK_CONNECTION_3);
	        
	        // Shuffle the list of URLs
	        Collections.shuffle(urls, new SecureRandom());
	    	
	        // Check each URL in turn. If any of them gives a response indicating a successful
	        // connection, return true.
	        for (String s : urls)
	        {
		    	try
		        {
		        	HttpURLConnection urlc = (HttpURLConnection) (new URL(s).openConnection());
		            urlc.setRequestProperty("User-Agent", "Test");
		            urlc.setRequestProperty("Connection", "close");
		            urlc.setConnectTimeout(MAX_RESPONSE_TIME_MILLISECONDS);
		            urlc.connect();  
		            if (urlc.getResponseCode() == HTTP_RESPONSE_CODE_OK)
		            {
		            	Log.i(TAG, "Internet availability check successfully connected to " + s);
		            	return true;
		            }
		        } 
		        catch (IOException e)
		        {
		            Log.e(TAG, "IOException occurred while running NetworkUtils.checkInternetAvailability. \n" +
		            		"The Exception message was: " + e.getMessage());
		        }
	        }
	    } 
	    else 
	    {
	        Log.d(TAG, "No network connection available!");
	    }
		
	    return false;
	}
	
	/**
	 * Checks whether the Android device is connected to a wifi network.<br><br>
	 * 
	 * Credit to 'Sandeep' on StackOverflow for this method.<br> 
	 * See: https://stackoverflow.com/questions/16689711
	 * 
	 * @param context - The Context for the currently running
	 * application
	 * 
	 * @return A boolean indicating whether or not the Android device
	 * is connected to a wifi network
	 */
	private static boolean checkWifiConnected(Context context)
	{
         ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
         NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
         
         if (wifiNetwork != null && wifiNetwork.isConnected())
         {
        	 return true;
         }
         else
         {
        	 return false;
         }
	}
	
	/**
	 * Checks whether any network connection is available. <br><br>
	 * 
	 * Credit to Alexandre Jasmin on StackOverflow for this method.<br> 
	 * See: https://stackoverflow.com/questions/4238921
	 * 
	 * @param context - The Context for the currently running
	 * application
	 * 
	 * @return A boolean indicating whether or not any network
	 * connection is available
	 */
	private static boolean checkNetworkAvailablility(Context context) 
	{
	    ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null;
	}
}