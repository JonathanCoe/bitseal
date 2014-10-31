package org.bitseal.network;

import java.util.ArrayList;

import org.bitseal.core.App;
import org.bitseal.core.PubkeyProcessor;
import org.bitseal.data.Payload;
import org.bitseal.data.Pubkey;
import org.bitseal.database.PayloadProvider;
import org.bitseal.database.PayloadsTable;
import org.bitseal.database.PubkeyProvider;
import org.bitseal.database.PubkeysTable;
import org.bitseal.util.ByteFormatter;
import org.bitseal.util.TimeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

/**
* Provides various methods used for communication between the 
* Bitseal client and Bitseal servers.
 * 
 * @author Jonathan Coe
 */
public class ServerCommunicator
{
	/**
	 * Determines the level of redundancy the client will attempt to maintain when
	 * attempting to disseminate msgs to the rest of the Bitmessage network. This
	 * redundancy is intended to provide some rudimentary defence against problems
	 * caused by malicious or unreliable servers, e.g. a server reporting that it 
	 * has disseminated a msg to the rest of the network but failing to actually do so.
	 * <br><br>
	 * Since Bitmessage is built around a flooding mechanism for data transmission, 
	 * this should not create any problems from duplication(e.g. a recipient getting
	 * the same message multiple times). 
	 */
	private static final int MSG_DISSEMINATION_REDUNDANCY_FACTOR = 3;
	
	/**
	 * Determines the level of redundancy the client will attempt to maintain when
	 * attempting to disseminate pubkeys to the rest of the Bitmessage network.
	 */
	private static final int PUBKEY_DISSEMINATION_REDUNDANCY_FACTOR = 4;
	
	/**
	 * Determines the level of redundancy the client will attempt to maintain when
	 * attempting to disseminate getpubkeys to the rest of the Bitmessage network.
	 */
	private static final int GETPUBKEY_DISSEMINATION_REDUNDANCY_FACTOR = 4;
	
	/**
	 * The modifier that we use to calculate the 'received since' time
	 * that we supply to the API the first time we check for msgs sent to a particular
	 * address. This is currently set to be equal to PyBitmessage's "lengthOfTimeToLeaveObjectsInInventory"
	 * value, which is currently 2.8 days or 237600 seconds. 
	 */
	private static final long FIRST_CHECK_RECEIVED_TIME_MODIFIER = 237600;
	
	/**
	 * The default modifier that we used to calculate the 'received since' time
	 * that we supply to the API when we check for msgs and getpubkeys. This
	 * allows a little overlap so that msgs which the server received
	 * very close to the last time we checked for msgs should not be missed. 
	 */
	private static final long DEFAULT_RECEIVED_TIME_MODIFIER = 10;
	
	/**
	 * The maximum period in seconds which for which we will check for new msgs in a single request. 
	 * This prevents us from being overwhelmed by a huge number of new msgs when we try
	 * to catch up with the network after some time offline. 
	 */
	private static final long MAXIMUM_MSG_CATCH_UP_PERIOD = 1800;
	
	/**
	 * The maximum number of servers to poll on each attempt to retrieve data. If we have
	 * a large number of servers it would take too long to poll all of them on each
	 * attempt to retrieve data. 
	 */
	private static final int MAX_SERVERS_TO_POLL = 2;
	
	/**
	 * The maximum size of an incoming payload that we will accept, in bytes.
	 **/
	private static final long MAX_PAYLOAD_SIZE_TO_ACCEPT = 256000;
	
	/** A key used to store the time of the last successful 'check for new msgs' server request */
	private static final String LAST_MSG_CHECK_TIME = "lastMsgCheckTime";
	
	// Api commands recognized by PyBitmessage
	private static final String API_METHOD_DISSEMINATE_MSG = "disseminateMsg";
	private static final String API_METHOD_DISSEMINATE_MSG_NO_POW = "disseminateMsgNoPOW";
	private static final String API_METHOD_DISSEMINATE_PUBKEY = "disseminatePubkey";
	private static final String API_METHOD_DISSEMINATE_PUBKEY_NO_POW = "disseminatePubkeyNoPOW";
	private static final String API_METHOD_DISSEMINATE_GETPUBKEY = "disseminateGetpubkey";
	private static final String API_METHOD_REQUEST_PUBKEY = "requestPubkey";
	private static final String API_METHOD_CHECK_FOR_NEW_MSGS = "checkForNewMsgs";
	
	// Strings returned by the PyBitmessage API which indicate the result of an API call
	private static final String RESULT_CODE_DISSEMINATE_MSG  = "Message disseminated successfully";
	private static final String RESULT_CODE_DISSEMINATE_PUBKEY = "Pubkey disseminated successfully";
	private static final String RESULT_CODE_DISSEMINATE_GETPUBKEY = "Getpubkey disseminated successfully";
	private static final String RESULT_CODE_REQUEST_PUBKEY = "No pubkeys found";
	private static final String RESULT_CODE_CHECK_FOR_NEW_MSGS = "No msgs found";
	
	// Strings used in the JSON formatting of data returned by servers
	private static final String JSON_NAME_PUBKEY_PAYLOAD = "pubkeyPayload";
	private static final String JSON_NAME_MSG_PAYLOADS = "msgPayloads";
	private static final String JSON_NAME_DATA = "data";
	
	private static final String TAG = "SERVER_COMMUNICATOR";
	
	/**
	 * Attempts to disseminate a message to the rest of the Bitmessage
	 * network by sending it to one or more Bitseal servers. The proof of work for 
	 * the message has already been done.
	 * 
	 * @param msgPayload - A byte[] containing the message to be disseminated. 
	 * 
	 * @return A boolean indicating whether or not the dissemination attempt was successful. 
	 */
	public boolean disseminateMsg(byte[] msgPayload)
	{
		String hexPayload = ByteFormatter.byteArrayToHexString(msgPayload);
		
		Log.d(TAG, "Attempting to disseminate an encrypted msg with POW done.\n"
				+ "Encrypted msg payload: " + hexPayload);
		
		// Attempt to make the API call
		ApiCaller caller = new ApiCaller();
		int successfulCalls = 0;
		for(int i = 0; i < MSG_DISSEMINATION_REDUNDANCY_FACTOR; i++)
		{
			Object callResult = caller.call(API_METHOD_DISSEMINATE_MSG, hexPayload);
			String resultString = callResult.toString();
			Log.d(TAG, "The result of the 'disseminate msg' API call was: " + resultString);
			
			if (resultString.equals(RESULT_CODE_DISSEMINATE_MSG)) 
			{
				successfulCalls = successfulCalls + 1;
			}
			else
			{
				Log.e(TAG, "While running disseminateMsgWithPOW(), a server connection was established \n" +
						"successfully, but the API call failed. The result of the api call was: " + resultString);
			}
			try
			{
				caller.switchToNextServer();
			}
			catch (RuntimeException e)
			{
				break;
			}
		}
		// If we successfully disseminated the payload to at least one server, return true
		if (successfulCalls > 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Attempts to disseminate a message to the rest of the Bitmessage
	 * network by sending it to one or more Bitseal servers. The proof of work for 
	 * the message has NOT yet been done, so the server will do the proof 
	 * of work and then disseminate the message.
	 * 
	 * @param msgPayload - A byte[] containing the message to be disseminated. 
	 * @param nonceTrialsPerByte - An int representing the nonceTrialsPerByte value for
	 * the proof of work required by the destination address
	 * @param extraBytes - An int representing the extraBytes value for
	 * the proof of work required by the destination address
	 * 
	 * @return A boolean indicating whether or not the dissemination attempt was successful. 
	 */
	public boolean disseminateMsgNoPOW(byte[] msgPayload, int nonceTrialsPerByte, int extraBytes)
	{
		String hexPayload = ByteFormatter.byteArrayToHexString(msgPayload);
		
		Log.d(TAG, "Attempting to disseminate an encrypted msg without POW done.\n"
				+ "Encrypted msg payload: " + hexPayload);
		
		// Attempt to make the API call
		ApiCaller caller = new ApiCaller();
		int successfulCalls = 0;
		for(int i = 0; i < MSG_DISSEMINATION_REDUNDANCY_FACTOR; i++)
		{
			Object callResult = caller.call(API_METHOD_DISSEMINATE_MSG_NO_POW, hexPayload);
			String resultString = callResult.toString();
			Log.d(TAG, "The result of the 'disseminate msg' API call was: " + resultString);
			
			if (resultString.equals(RESULT_CODE_DISSEMINATE_MSG)) 
			{
				successfulCalls = successfulCalls + 1;
			}
			else
			{
				Log.e(TAG, "While running disseminateMsgNoPOW(), a server connection was established \n" +
						"successfully, but the API call failed. The result of the api call was: " + resultString);
			}
			try
			{
				caller.switchToNextServer();
			}
			catch (RuntimeException e)
			{
				break;
			}
		}
		// If we successfully disseminated the payload to at least one server, return true
		if (successfulCalls > 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Attempts to disseminate  a pubkey to the rest of the Bitmessage network
	 * by sending it to one or more Bitseal servers. The proof of work for 
	 * the pubkey has already been done. 
	 * 
	 * @param pubkeyPayload -A byte[] containing the pubkey to be disseminated. 
	 * 
	 * @return A boolean indicating whether or not the dissemination attempt was successful. 
	 */
	public boolean disseminatePubkey(byte[] pubkeyPayload)
	{
		String hexPayload = ByteFormatter.byteArrayToHexString(pubkeyPayload);
		
		Log.d(TAG, "Attempting to disseminate a pubkey with POW done.\n"
				+ "Pubkey payload: " + hexPayload);
		
		// Attempt to make the API call
		ApiCaller caller = new ApiCaller();
		int successfulCalls = 0;
		for(int i = 0; i < PUBKEY_DISSEMINATION_REDUNDANCY_FACTOR; i++)
		{
			Object callResult = caller.call(API_METHOD_DISSEMINATE_PUBKEY, hexPayload);
			String resultString = callResult.toString();
			Log.d(TAG, "The result of the 'disseminate pubkey' API call was: " + resultString);
			
			if (resultString.equals(RESULT_CODE_DISSEMINATE_PUBKEY)) 
			{
				successfulCalls = successfulCalls + 1;
			}
			else
			{
				Log.e(TAG, "While running disseminatePubkeyWithPOW(), a server connection was established \n" +
						"successfully, but the API call failed. The result of the api call was: " + resultString);
			}
			try
			{
				caller.switchToNextServer();
			}
			catch (RuntimeException e)
			{
				break;
			}
		}
		// If we successfully disseminated the payload to at least one server, return true
		if (successfulCalls > 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Attempts to disseminate  a pubkey to the rest of the Bitmessage network
	 * by sending it to one or more Bitseal servers. The proof of work for 
	 * the pubkey has NOT yet been done, so the server will do the proof 
	 * of work and then disseminate the pubkey.
	 * 
	 * @param pubkeyPayload -A byte[] containing the pubkey to be disseminated. 
	 * 
	 * @return A boolean indicating whether or not the dissemination attempt was successful. 
	 */
	public boolean disseminatePubkeyNoPOW(byte[] pubkeyPayload)
	{
		String hexPayload = ByteFormatter.byteArrayToHexString(pubkeyPayload);
		
		Log.d(TAG, "Attempting to disseminate a pubkey without POW done.\n"
				+ "Pubkey payload: " + hexPayload);
		
		// Attempt to make the API call
		ApiCaller caller = new ApiCaller();
		int successfulCalls = 0;
		for(int i = 0; i < PUBKEY_DISSEMINATION_REDUNDANCY_FACTOR; i++)
		{
			Object callResult = caller.call(API_METHOD_DISSEMINATE_PUBKEY_NO_POW, hexPayload);
			String resultString = callResult.toString();
			Log.d(TAG, "The result of the 'disseminate pubkey' API call was: " + resultString);
			
			if (resultString.equals(RESULT_CODE_DISSEMINATE_PUBKEY)) 
			{
				successfulCalls = successfulCalls + 1;
			}
			else
			{
				Log.e(TAG, "While running disseminatePubkeyNoPOW(), a server connection was established \n" +
						"successfully, but the API call failed. The result of the api call was: " + resultString);
			}
			try
			{
				caller.switchToNextServer();
			}
			catch (RuntimeException e)
			{
				break;
			}
		}
		// If we successfully disseminated the payload to at least one server, return true
		if (successfulCalls > 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Sends a getpubkey object to a server, waits some time for the server
	 * to receive the pubkey specified by the getpubkey object, then requests
	 * that pubkey from the server. 
	 * 
	 * @param getpubkeyPayload - A byte[] containing the payload of the getpubkey object
	 * 
	 * @return - A Pubkey object containing the pubkey specified by the
	 * getpubkey payload
	 */
	public boolean disseminateGetpubkey(byte[] getpubkeyPayload)
	{	
		String hexPayload = ByteFormatter.byteArrayToHexString(getpubkeyPayload);
		
		Log.d(TAG, "Attempting to disseminate a getpubkey with POW done.\n"
				+ "Getpubkey payload: " + hexPayload);
		
		// Attempt to make the API call
		ApiCaller caller = new ApiCaller();
		int successfulCalls = 0;
		for(int i = 0; i < GETPUBKEY_DISSEMINATION_REDUNDANCY_FACTOR; i++)
		{
			Object callResult = caller.call(API_METHOD_DISSEMINATE_GETPUBKEY, hexPayload);
			String resultString = callResult.toString();
			Log.d(TAG, "The result of the 'disseminate getpubkey' API call was: " + resultString);
			
			if (resultString.equals(RESULT_CODE_DISSEMINATE_GETPUBKEY)) 
			{
				successfulCalls = successfulCalls + 1;
			}
			else
			{
				Log.e(TAG, "While running disseminateGetpubkeyWithPOW(), a server connection was established \n" +
						"successfully, but the API call failed. The result of the api call was: " + resultString);
			}
			try
			{
				caller.switchToNextServer();
			}
			catch (RuntimeException e)
			{
				break;
			}
		}
		
		// If we successfully disseminated the payload to at least one server, return true
		if (successfulCalls > 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Takes an identifier (ripe hash or tag) and requests the corresponding pubkey from a
	 * Bitseal server.<br><br>
	 * 
	 * Note: If the pubkey cannot be retrieved after trying all available servers,
	 * this method will throw a RuntimeException
	 * 
	 * @param addressString - A String containing the address which we are trying 
	 * to retrieve the pubkey of
	 * @param identifier - A byte[] containing the data used to identify the pubkey
	 * we wish to request. For address versions 3 and below, this is the ripe hash. For
	 * address versions 4 and above, this is the 'tag'.
	 * @param addressVersion - An int containing the version number of the address for
	 * which we are requesting the pubkey
	 * 
	 * @return A Pubkey object conatining the requested pubkey
	 */
	public Pubkey requestPubkeyFromServer(String addressString, byte[] identifier, int addressVersion)
	{
		Log.d(TAG, "Requesting the pubkey of address " + addressString);
		
		// Encode the payload to be sent into hex
		String hexPayload = ByteFormatter.byteArrayToHexString(identifier);

		// Work out how many servers to poll in this request
		ApiCaller caller = new ApiCaller();
		int serversToPoll = caller.getNumberOfServers();
		if (serversToPoll > MAX_SERVERS_TO_POLL)
		{
			serversToPoll = MAX_SERVERS_TO_POLL;
		}
		
		// Make the API call
		for (int i = 0; i < serversToPoll; i++)
		{
			Object callResult = caller.call(API_METHOD_REQUEST_PUBKEY, hexPayload, addressVersion);
			String resultString = callResult.toString();
			Log.d(TAG, "The result of the 'request pubkey from server' API call was: " + resultString);
			
			if ((resultString.equals(RESULT_CODE_REQUEST_PUBKEY)) == false) // If the call was successful
			{
				try
				{									
					// Parse the JSON
					JSONObject jObject = new JSONObject(resultString);
					JSONArray jArray = jObject.getJSONArray(JSON_NAME_PUBKEY_PAYLOAD);
					JSONObject object = jArray.getJSONObject(0); // There should never be more than one result for a 'request pubkey' call

			        String pubkeyHex = object.getString(JSON_NAME_DATA);
			        
			        long payloadByteSize = pubkeyHex.length() / 2;
			        if (payloadByteSize < MAX_PAYLOAD_SIZE_TO_ACCEPT)
			        {
						// Decode the pubkey data from hex
						byte[] pubkeyData = ByteFormatter.hexStringToByteArray(pubkeyHex);

						// Validate the pubkey
						PubkeyProcessor pubProc = new PubkeyProcessor();
						Pubkey pubkey = pubProc.reconstructPubkey(pubkeyData, addressString);
						
						// Validate the reconstructed pubkey.
						boolean pubkeyValid = pubProc.validatePubkey(pubkey, addressString);
						if (pubkeyValid == false)
						{
							Log.i(TAG, "While running ServerCommunicator.requestPubkeyFromServer() in order to retrieve the pubkey \n" +
									"for address " + addressString + ", a pubkey was reconstructed successfully from \n" +
									"a payload provided by a server, but the resulting pubkey was found to be invalid.");
							caller.switchToNextServer();
						}
						else
						{
							return pubkey;
						}
			        }
			        else
			        {
			        	long payloadKilobytes = payloadByteSize / 1000;
			        	
			        	Log.d(TAG, "While running ServerCommunicator.requestPubkeyFromServer(), we received a payload that was larger than "
			        			+ "the maximum size we are willing to accept. It has been ignored. \n"
			        			+ "The size of the rejected payload was " + payloadKilobytes + " kilobytes.");
			        }
				}
				catch (JSONException e)
				{
			    	throw new RuntimeException("JSONException occcurred in ServerCommunicator.requestPubkeyFromServer(). \n" +
							"The exception message was " + e.getLocalizedMessage() + "\n" +
			    			"The API call result string was: " + resultString);
				}
			}
			else
			{
				try
				{
					caller.switchToNextServer();
				}
				catch (RuntimeException e)
				{
					break;
				}
			}
		}
		// If we tried all the servers and none of them returned the correct pubkey
		throw new RuntimeException("Failed to retrieve the requested pubkey after trying all servers.");
	}
	
	/**
	 * Requests any new msgs for each of our addresses, using the stream number of each address
	 * and the 'lastMsgCheckTime' value to shape the request. 
	 * 
	 * @param address - The Bitmessage Address which we want to check for messages sent to.
	 */
	public void checkServerForNewMsgs()
	{
		// Work out the time values to use in this request
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		long lastMsgCheckTime = prefs.getLong(LAST_MSG_CHECK_TIME, 0);
		long receivedSinceTime = calculateReceivedSinceTime(lastMsgCheckTime);
		long receivedBeforeTime = calculateReceivedBeforeTime(receivedSinceTime, MAXIMUM_MSG_CATCH_UP_PERIOD);
		
		// Get the stream numbers of all our addresses
		PubkeyProvider pubProv = PubkeyProvider.get(App.getContext());
		ArrayList<Pubkey> myPubkeys = pubProv.searchPubkeys(PubkeysTable.COLUMN_BELONGS_TO_ME, String.valueOf(1));
		ArrayList<Integer> myStreamNumbers = new ArrayList<Integer>();
		for (Pubkey p : myPubkeys)
		{
			Integer streamNumber = p.getStreamNumber();
			if (myStreamNumbers.contains(streamNumber) == false)
			{
				myStreamNumbers.add(streamNumber);
			}
		}
		
		// For each stream number of ours, check for any new msgs in that stream
		for (Integer streamNumber : myStreamNumbers)
		{
			Log.d(TAG, "Making a server request for any msgs in stream " + streamNumber + " received between " + receivedSinceTime + " and " + receivedBeforeTime + "\n"
					+ "- a time period of " + TimeUtils.getTimeMessage((receivedBeforeTime - receivedSinceTime)) + ". " + TimeUtils.getTimeBehindNetworkMessage() + ".");
			
			// Work out how many servers to poll in this request
			ApiCaller caller = new ApiCaller();
			int serversToPoll = caller.getNumberOfServers();
			if (serversToPoll > MAX_SERVERS_TO_POLL)
			{
				serversToPoll = MAX_SERVERS_TO_POLL;
			}
			
			// Make the API call
			for (int i = 0; i < serversToPoll; i++)
			{
				Object callResult = caller.call(API_METHOD_CHECK_FOR_NEW_MSGS, streamNumber, receivedSinceTime, receivedBeforeTime);
				String resultString = callResult.toString();
				
				if ((resultString.equals(RESULT_CODE_CHECK_FOR_NEW_MSGS)) == false) // If the call was successful
				{
					try
					{
						ArrayList<String> msgStrings = new ArrayList<String>();
						
						// Parse the JSON
						JSONObject jObject = new JSONObject(resultString);
						JSONArray jArray = jObject.getJSONArray(JSON_NAME_MSG_PAYLOADS);
						for (int index = 0; index < jArray.length(); index++)
						{
					        JSONObject object = jArray.getJSONObject(index);
					        String msgHex = object.getString(JSON_NAME_DATA);
					        
					        long payloadByteSize = msgHex.length() / 2;
					        if (payloadByteSize < MAX_PAYLOAD_SIZE_TO_ACCEPT)
					        {
					        	msgStrings.add(msgHex);
					        }
					        else
					        {
					        	long payloadKilobytes = payloadByteSize / 1000;
					        	
					        	Log.d(TAG, "While running ServerCommunicator.checkServerForNewMsgs(), we received a payload that was larger than "
					        			+ "the maximum size we are willing to accept. It has been ignored. \n"
					        			+ "The size of the rejected payload was " + payloadKilobytes + " kilobytes.");
					        }					
						}
						
						// For each retrieved msg payload, check whether we have already received it
						int newPayloads = 0;
						for (String msgPayloadString : msgStrings)
						{
							byte[] msgBytes = ByteFormatter.hexStringToByteArray(msgPayloadString);
							String msgBase64 = Base64.encodeToString(msgBytes, Base64.DEFAULT);
							PayloadProvider payProv = PayloadProvider.get(App.getContext());
							ArrayList<Payload> retrievedPayloads = payProv.searchPayloads(PayloadsTable.COLUMN_PAYLOAD, msgBase64);
							if (retrievedPayloads.size() == 0)
							{
								Payload msgPayload = new Payload();
								msgPayload.setBelongsToMe(false);
								msgPayload.setProcessingComplete(false);
								msgPayload.setType(Payload.OBJECT_TYPE_MSG);
								msgPayload.setPayload(msgBytes);
								
								payProv.addPayload(msgPayload);
								newPayloads ++;
							}
						}
						
						Log.d(TAG, "Out of the " + msgStrings.size() + " msg payloads returned by the server, " + newPayloads + " were new.");
						
						if ((i + 1) < serversToPoll) // Do not attempt to switch to a new server if we have finished making all our API calls
						{
							caller.switchToNextServer();
						}	
					}
					catch (JSONException e)
					{
				    	throw new RuntimeException("JSONException occcurred in ServerCommunicator.checkServerForNewMsgs(). \n" +
								"The exception message was " + e.getLocalizedMessage() + "\n" +
				    			"The API call result string was: " + resultString);
					}
				}
				else
				{
					Log.d(TAG, "The result of the 'check for new msgs' API call was: " + resultString);
					
					try
					{
						caller.switchToNextServer();
					}
					catch (RuntimeException e)
					{
						break;
					}
				}
			}
		}
		
		// After trying all the selected servers, if no exceptions were thrown, update the 'last successful msg check time'
		SharedPreferences.Editor editor = prefs.edit();
	    editor.putLong(LAST_MSG_CHECK_TIME, receivedBeforeTime);
	    editor.commit();
		Log.i(TAG, "Updated the 'last successful msg check time' time stored in SharedPreferences to " + receivedBeforeTime);
	}
	
	/**
	 * Calculates the 'received since' time value that should be used when checking for
	 * new objects. 
	 * 
	 * @param lastCheckTime - A long representing the time at which the last successful
	 * check was completed for the address in question
	 * 
	 * @return A long containing the calculated 'received since' time value
	 */
	private long calculateReceivedSinceTime(long lastCheckTime)
	{
		long receivedSinceTime;
		long currentTime = System.currentTimeMillis() / 1000;
		if (lastCheckTime == 0) // If this is the first time we have checked for msgs sent to this address
		{
			receivedSinceTime = currentTime - FIRST_CHECK_RECEIVED_TIME_MODIFIER;
		}
		else
		{
			receivedSinceTime = lastCheckTime - DEFAULT_RECEIVED_TIME_MODIFIER;
		}
		
		return receivedSinceTime;
	}
	
	/**
	 * Calculates the 'received before' time value that should be used when checking for
	 * new objects. 
	 * 
	 * @param lastCheckTime - A long representing 'received since' time value that has
	 * been calculated for this request
	 * @param catchUpPeriod - A long containing the 'catch up period' to use. This should
	 * be a time value in seconds
	 * 
	 * @return A long containing the calculated 'received before' time value
	 */
	private long calculateReceivedBeforeTime(long receivedSinceTime, long catchUpPeriod)
	{
		// If the 'received since' time is a long time in the past, modify it so that we aren't overwhelmed
		// by a huge influx of new objects to process. Instead we will gradually catch up over time. 
		long currentTime = System.currentTimeMillis() / 1000;
		long receivedBeforeTime = currentTime; // By default we use the current time
		
		long timeGap = currentTime - receivedSinceTime;
		if (timeGap > catchUpPeriod)
		{
			receivedBeforeTime = receivedSinceTime + catchUpPeriod;
		}
		return receivedBeforeTime;
	}
}