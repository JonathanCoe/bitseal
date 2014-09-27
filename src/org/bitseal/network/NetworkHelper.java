package org.bitseal.network;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;

import org.bitseal.core.App;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
		// First check whether any network connection is available
		Context appContext = App.getContext();
		if (checkNetworkAvailablility(appContext) == true)
	    {
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
	        Log.i(TAG, "No network connection available!");
	    }
	    return false;
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