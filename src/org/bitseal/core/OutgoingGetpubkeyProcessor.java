package org.bitseal.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bitseal.R;
import org.bitseal.data.Message;
import org.bitseal.data.Payload;
import org.bitseal.database.PayloadProvider;
import org.bitseal.network.NetworkHelper;
import org.bitseal.network.ServerCommunicator;
import org.bitseal.pow.POWProcessor;
import org.bitseal.services.MessageStatusHandler;
import org.bitseal.util.ByteUtils;
import org.bitseal.util.TimeUtils;
import org.bitseal.util.VarintEncoder;

import android.util.Log;

/**
 * A class which provides various methods used for processing
 * outgoing getpubkey objects.  
 * 
 * @author Jonathan Coe
 */
public class OutgoingGetpubkeyProcessor
{
	/** The object type number for getpubkeys, as defined by the Bitmessage protocol */
	private static final int OBJECT_TYPE_GETPUBKEY = 0;
	
	private static final String TAG = "OUTGOING_GETPUBKEY_PROCESSOR";
	
	/**
	 * Creates a new getpubkey object for the given Bitmessage address and
	 * disseminates it to the rest of the Bitmessage network. 
	 * 
	 * @param getpubkeyPayload - A Payload object containing the getpubkey 
	 * object to be disseminated
	 * 
	 * @return - A Payload object containing the newly created getpubkey object
	 */
	public void disseminateGetpubkeyRequest(Payload getpubkeyPayload)
	{
		// Disseminate the getpubkey payload
		ServerCommunicator servCom = new ServerCommunicator();
		boolean disseminationSuccessful = servCom.disseminateGetpubkey(getpubkeyPayload.getPayload());
		if (disseminationSuccessful == true)
		{
			getpubkeyPayload.setTime(System.currentTimeMillis() / 1000); // Set the 'time' value of the getpubkey Payload to be the last time at which the payload
																		 // was successfully disseminated. This allows us to determine when to disseminate it again. 
			PayloadProvider payProv = PayloadProvider.get(App.getContext());
			payProv.updatePayload(getpubkeyPayload);
		}
	}
	
	/**
	 * Creates a new getpubkey object for the 'to address' of a given Message and
	 * disseminates it to the rest of the Bitmessage network. 
	 * 
	 * @param addressString - The Message we are trying to send
	 * @param timeToLive - The 'time to live' value (in seconds) to be used in creating 
	 * this getpubkey
	 * 
	 * @return - A Payload object containing the newly created getpubkey object
	 */
	public Payload constructAndDisseminateGetpubkeyRequst(Message message, long timeToLive)
	{
		// We were unable to retrieve the pubkey after trying all servers. Now we must create a getpubkey 
		// object which can be sent out to servers. We should then be able to retrieve the required pubkey.
		Payload getpubkeyPayload = constructGetpubkeyPayload(message.getToAddress(), timeToLive);
		
		// Save the getpubkey object to the database
		PayloadProvider payProv = PayloadProvider.get(App.getContext());
		long id = payProv.addPayload(getpubkeyPayload);
		getpubkeyPayload.setId(id);
		
		// Check whether an Internet connection is available. 
		if (NetworkHelper.checkInternetAvailability() == true)
		{
			MessageStatusHandler.updateMessageStatus(message, App.getContext().getString(R.string.message_status_requesting_pubkey));
			
			// Disseminate the getpubkey payload
			try
			{
				ServerCommunicator servCom = new ServerCommunicator();
				boolean disseminationSuccessful = servCom.disseminateGetpubkey(getpubkeyPayload.getPayload());
				if (disseminationSuccessful == true)
				{
					getpubkeyPayload.setTime(System.currentTimeMillis() / 1000);
					payProv.updatePayload(getpubkeyPayload);
				}
				return getpubkeyPayload;
			}
			catch (RuntimeException e)
			{
				Log.e(TAG, "RuntimeException occurred in PubkeyProcessor.constructAndDisseminateGetpubkeyRequst()\n"
						+ "The exception message was: " + e.getMessage());
				return getpubkeyPayload;
			}
		}
		else
		{
			MessageStatusHandler.updateMessageStatus(message, App.getContext().getString(R.string.message_status_waiting_for_connection));
			return getpubkeyPayload;
		}	
	}
	
	/**
	 * Constructs a getpubkey object which can be sent out to servers, allowing
	 * them to request the required pubkey, which we can then retrieve from them. 
	 * 
	 * @param addressString - A String containing the address of the pubkey which the
	 * getpubkey object will be used to retrieve
	 * @param timeToLive - The 'time to live' value (in seconds) to be used in creating 
	 * this getpubkey
	 * 
	 * @return A Payload object containing the getpubkey object
	 */
	private Payload constructGetpubkeyPayload(String addressString, long timeToLive)
	{
		Log.i(TAG, "Constructing a new getpubkey Payload to request the pubkey of address " + addressString);
		
		// Get the fuzzed expiration time and encoded it into byte[] form
		long expirationTime = TimeUtils.getFuzzedExpirationTime(timeToLive);
		byte[] expirationTimeBytes = ByteUtils.longToBytes(expirationTime);
		
		// Get the address version and stream number
		AddressProcessor addProc = new AddressProcessor();
		int[] decodedAddressNumbers = addProc.decodeAddressNumbers(addressString);
		int addressVersion = decodedAddressNumbers[0];
		int streamNumber = decodedAddressNumbers[1];
		
		byte[] pubkeyIdentifier = null;
		if (addressVersion <= 3)
		{
			pubkeyIdentifier = addProc.extractRipeHashFromAddress(addressString);
		}
		else
		{
			pubkeyIdentifier = addProc.calculateAddressTag(addressString);
		}
		
		byte[] payload = null;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try 
		{
			outputStream.write(expirationTimeBytes);
			outputStream.write(ByteUtils.intToBytes(OBJECT_TYPE_GETPUBKEY));
			outputStream.write(VarintEncoder.encode(addressVersion)); 
			outputStream.write(VarintEncoder.encode(streamNumber)); 
			outputStream.write(pubkeyIdentifier);
		
			payload = outputStream.toByteArray();
			outputStream.close();
		}
		catch (IOException e) 
		{
			throw new RuntimeException("IOException occurred in PubkeyProcessor.constructGetpubkeyPayload()", e);
		}
		
		// Do the POW for the payload we have constructed
		POWProcessor powProc = new POWProcessor();
		long powNonce = powProc.doPOW(payload, expirationTime, POWProcessor.NETWORK_NONCE_TRIALS_PER_BYTE, POWProcessor.NETWORK_EXTRA_BYTES);
		byte[] powNonceBytes = ByteUtils.longToBytes(powNonce);
		
		// Add the POW nonce to the payload
		payload = ByteUtils.concatenateByteArrays(powNonceBytes, payload);
		
		Payload getpubkeyPayload = new Payload();
		getpubkeyPayload.setBelongsToMe(true);
		// We set the time to zero to convey that the getpubkey Payload has not yet been successfully disseminated. 
		// This is a slightly obscure way of handling the need to record the last time at which the getpubkey was
		// successfully disseminated, and should perhaps be changed to something more straightforward in the future. 
		getpubkeyPayload.setTime(0);
		getpubkeyPayload.setType(Payload.OBJECT_TYPE_GETPUBKEY);
		getpubkeyPayload.setPOWDone(true);
		getpubkeyPayload.setPayload(payload);
		
		return getpubkeyPayload;
	}
}