package org.bitseal.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import org.bitseal.data.Payload;
import org.bitseal.database.PayloadProvider;
import org.bitseal.network.ServerCommunicator;
import org.bitseal.pow.POWProcessor;
import org.bitseal.util.ByteUtils;
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
	 * Creates a new getpubkey object for the given Bitmessage address and
	 * disseminates it to the rest of the Bitmessage network. 
	 * 
	 * @param addressString - A String containing the Bitmessage address to create
	 * a getpubkey object for
	 * 
	 * @return - A Payload object containing the newly created getpubkey object
	 */
	public Payload constructAndDisseminateGetpubkeyRequst(String addressString)
	{
		// We were unable to retrieve the pubkey after trying all servers. Now we must create a getpubkey 
		// object which can be sent out to servers. We should then be able to retrieve the required pubkey.
		Payload getpubkeyPayload = constructGetpubkeyPayload(addressString);
		
		// Save the getpubkey object to the database
		PayloadProvider payProv = PayloadProvider.get(App.getContext());
		long id = payProv.addPayload(getpubkeyPayload);
		getpubkeyPayload.setId(id);
		
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
		}
		catch (RuntimeException e)
		{
			Log.e(TAG, "RuntimeException occurred in PubkeyProcessor.constructAndDisseminateGetpubkeyRequst()\n"
					+ "The exception message was: " + e.getMessage());
			return getpubkeyPayload;
		}
		
		return getpubkeyPayload;
	}
	
	/**
	 * Constructs a getpubkey object which can be sent out to servers, allowing
	 * them to request the required pubkey, which we can then retrieve from them. 
	 * 
	 * @param addressString - A String containing the address of the pubkey which the
	 * getpubkey object will be used to retrieve
	 * 
	 * @return A Payload object containing the getpubkey object
	 */
	private Payload constructGetpubkeyPayload(String addressString)
	{
		Log.i(TAG, "Constructing a new getpubkey Payload to request the pubkey of address " + addressString);
		
		// Get the current time + or - 5 minutes and encode it into byte form
		long time = System.currentTimeMillis() / 1000L; // Gets the current time in seconds
    	int timeModifier = (new Random().nextInt(600)) - 300;
    	time = time + timeModifier; // Gives us the current time plus or minus 300 seconds (five minutes). This is also done by PyBitmessage. 
		
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
			outputStream.write((ByteBuffer.allocate(8).putLong(time).array())); 
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
		long powNonce = powProc.doPOW(payload, POWProcessor.NETWORK_NONCE_TRIALS_PER_BYTE, POWProcessor.NETWORK_EXTRA_BYTES);
		byte[] powNonceBytes = ByteBuffer.allocate(8).putLong(powNonce).array();
		
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