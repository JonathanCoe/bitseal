package org.bitseal.util;

import java.util.Random;

import org.bitseal.core.App;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * This class provides convenience methods for getting and formatting
 * time values. 
 * 
 * @author Jonathan Coe
 */
public final class TimeUtils 
{   
	/**
	 * The range of time (in seconds) by which we 'fuzz' (obscure) certain time values.
	 */
	private static final int FUZZ_TIME_RANGE = 300; // Currently set to five minutes
	
	private static final int SECONDS_IN_A_DAY = 86400;
	private static final int SECONDS_IN_AN_HOUR = 3600;
	private static final int MINUTES_IN_AN_HOUR = 60;
	private static final int SECONDS_IN_A_MINUTE = 60;
	private static final int HOURS_IN_A_DAY = 24;
	
	/** A key used to store the time of the last successful 'check for new msgs' server request */
	private static final String LAST_MSG_CHECK_TIME = "lastMsgCheckTime";
	
	private TimeUtils()
	{
    	// The constructor of this class is private in order to prevent the class being instantiated
	}
	
	/**
	 * Returns a 'time to live' value and uses it to produce a
	 * fuzzed expiration time - which is the current time plus
	 * the time to live, giving us a time value in the future. 
	 * 
	 * @param timeToLive - The 'time to live' value, in seconds
	 * 
	 * @return The fuzzed expiration time
	 */
	public static long getFuzzedExpirationTime(long timeToLive)
	{
		return getFuzzedTime() + timeToLive;
	}
	
	/**
	 * Returns the current Unix time, plus or minus a random value in a
	 * pre-defined range. This fuzzing of time values is not strictly part
	 * of the Bitmessage protocol, but is commonly done in order to reduce
	 * the potential for security breaches caused by the various time values
	 * embedded in Bitmessage data.
	 */
	private static long getFuzzedTime()
	{
		long currentTime = System.currentTimeMillis() / 1000; // Gets the current Unix time
    	int timeModifier = (new Random().nextInt(FUZZ_TIME_RANGE * 2)) - FUZZ_TIME_RANGE;
    	return currentTime + timeModifier; // Gives us the current Unix time plus or minus a random value within the 'fuzz time range'
	}
    
    /**
     * Returns a String containing a message describing how far
     * in time behind the network Bitseal is. <br><br>
     * 
     * e.g. "Bitseal is 1 minute and 12 seconds behind the network."
     */
    public static String getTimeBehindNetworkMessage()
    {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		long lastMsgCheckTime = prefs.getLong(LAST_MSG_CHECK_TIME, 0);
    	
    	long currentTime = System.currentTimeMillis() / 1000;
		long secondsBehindNetwork = currentTime - lastMsgCheckTime;
		
		String timeMessage = getTimeMessage(secondsBehindNetwork);
		timeMessage = "Bitseal is " + timeMessage;
		timeMessage = timeMessage + " behind the network";
		
		return timeMessage;
    }
    
    /**
     * Takes a time value in seconds and returns a String which 
     * gives the same time value in an easily human readable form.<br><br>
     * 
     * NOTE: This method is designed to return a concise representation of the time,
     * therefore if the provided value is sufficiently large, the resulting message
     * may be 'rounded off'. If the time value provided is equal to many days, then the returned
     * message will contain the number of days and hours, but not the number of
     * remainder seconds. <br><br>
     * 
     * e.g. 777329 returns "8 days and 23 hours"
     * 
     * @param time
     * @return
     */
    public static String getTimeMessage(long time)
    {
    	String timeMessage;
    	
    	long seconds = time;
    	long minutes = seconds / SECONDS_IN_A_MINUTE;
    	long hours = seconds / SECONDS_IN_AN_HOUR;
    	long days = seconds / SECONDS_IN_A_DAY;	
    	
    	if (seconds > SECONDS_IN_A_DAY)
    	{
			long remainderHours = hours % HOURS_IN_A_DAY;
    		
    		if (days > 1)
			{
				if (remainderHours == 1)
				{
					timeMessage = days + " days and " + remainderHours + " hour";
				}
				else
				{
					timeMessage = days + " days and " + remainderHours + " hours";
				}
			}
			else
			{
				if (remainderHours == 1)
				{
					timeMessage = days + " day and " + remainderHours + " hour";
				}
				else
				{
					timeMessage = days + " day and " + remainderHours + " hours";
				}
			}
    	}
    	else if (seconds > SECONDS_IN_AN_HOUR)
		{
			long remainderMinutes = minutes % MINUTES_IN_AN_HOUR;
    		
    		if (hours > 1)
			{
    			if (remainderMinutes == 1)
    			{
    				timeMessage = hours + " hours and " + remainderMinutes + " minute";
    			}
    			else
    			{
    				timeMessage = hours + " hours and " + remainderMinutes + " minutes";
    			}
			}
			else
			{
    			if (remainderMinutes == 1)
    			{
    				timeMessage = hours + " hour and " + remainderMinutes + " minute";
    			}
    			else
    			{
    				timeMessage = hours + " hour and " + remainderMinutes + " minutes";
    			}
			}
		}
		else if (seconds > SECONDS_IN_A_MINUTE)
		{
			long remainderSeconds = seconds % SECONDS_IN_A_MINUTE;
			
			if (minutes > 1)
			{
				if (remainderSeconds == 1)
				{
					timeMessage = minutes + " minutes and " + remainderSeconds + " second";
				}
				else
				{
					timeMessage = minutes + " minutes and " + remainderSeconds + " seconds";
				}
			}
			else
			{
				if (remainderSeconds == 1)
				{
					timeMessage = minutes + " minute and " + remainderSeconds + " second";
				}
				else
				{
					timeMessage = minutes + " minute and " + remainderSeconds + " seconds";
				}
			}
		}
		else
		{			
			if (seconds != 1)
			{
				timeMessage = seconds + " seconds";
			}
			else
			{
				timeMessage = "1 second";
			}	
		}
    	
    	return timeMessage;
    }
}