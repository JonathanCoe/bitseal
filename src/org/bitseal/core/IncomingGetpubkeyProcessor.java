package org.bitseal.core;

import java.util.ArrayList;
import java.util.Arrays;

import org.bitseal.data.Address;
import org.bitseal.data.Payload;
import org.bitseal.data.Pubkey;
import org.bitseal.database.AddressProvider;
import org.bitseal.database.PubkeyProvider;
import org.bitseal.network.ServerCommunicator;
import org.bitseal.pow.POWProcessor;
import org.bitseal.util.ArrayCopier;
import org.bitseal.util.ByteFormatter;
import org.bitseal.util.ByteUtils;
import org.bitseal.util.VarintEncoder;

import android.util.Log;

/**
 * A class which provides various methods used for processing
 * incoming getpubkey objects.  
 * 
 * @author Jonathan Coe
 */
public class IncomingGetpubkeyProcessor
{
	/** The length of time in seconds for which PyBitmessage will retain pubkeys it receives. 
	 *  If a pubkey requested through a getpubkey object was disseminated less than this amount
	 *  of time ago, then the getpubkey request can be safely ignored as the pubkey will still
	 *  be in the network's shared inventory. Currently equal to 28 days. 
	 */
	private static final int PYBITMESSAGE_PUBKEY_RETENTION_PERIOD = 2419200;
	
	/** This is the maximum age of an object (in seconds) that PyBitmessage will accept. */
	private static final int PYBITMESSAGE_NEW_OBJECT_ACCEPTANCE_PERIOD = 216000;
	
	/** See https://bitmessage.org/wiki/Proof_of_work for an explanation of these values **/
	private static final long DEFAULT_NONCE_TRIALS_PER_BYTE = 320;
	private static final long DEFAULT_EXTRA_BYTES = 14000;
	
	private static final String TAG = "INCOMING_GETPUBKEY_PROCESSOR";
	
	/**
	 * Takes a getpubkey Payload received from a server and processes it.
	 * 
	 *  @param - getpubkeyPayload - A Payload object containing the received
	 *  getpubkey payload. 
	 *  
	 *  @return A boolean indicating whether or not the getpubkey Payload was
	 *  successfully processed. 
	 */
	public boolean processGetpubkeyRequest(Payload getpubkeyPayload)
	{
		byte[] payload = getpubkeyPayload.getPayload();
		Log.i(TAG, "Processing a getpubkey object with the following payload:    " + ByteFormatter.byteArrayToHexString(payload));
		
		// Extract the data from the getpubkey payload
		int readPosition = 0;
		long powNonce = ByteUtils.bytesToLong(ArrayCopier.copyOfRange(payload, readPosition, readPosition + 8));
		readPosition += 8; //The pow nonce should always be 8 bytes in length
		Log.i(TAG, "getpubkey payload pow nonce:       " + powNonce);
		
		long payloadTime = ByteUtils.bytesToInt((ArrayCopier.copyOfRange(payload, readPosition, readPosition + 4)));
		if (payloadTime == 0) // Check whether 4 or 8 byte time has been used
		{
			payloadTime = ByteUtils.bytesToLong((ArrayCopier.copyOfRange(payload, readPosition, readPosition + 8)));
			readPosition += 8;
		}
		else
		{
			readPosition += 4;
		}
		Log.i(TAG, "getpubkey payload time:            " + payloadTime);
		
		long[] decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(payload, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		int addressVersion = (int) decoded[0]; // Get the var_int encoded value
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		
		decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(payload, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		
		byte[] identifier = ArrayCopier.copyOfRange(payload, readPosition, payload.length); // Either the ripe hash or the 'tag'
		
		// Now check that the values extracted from the getpubkey payload are valid
		POWProcessor powProc = new POWProcessor();
		boolean powNonceValid = powProc.checkPOW(ArrayCopier.copyOfRange(payload, 8, payload.length), powNonce, DEFAULT_NONCE_TRIALS_PER_BYTE, DEFAULT_EXTRA_BYTES);
		if (powNonceValid == false)
		{
			return true;
		}
		
		long currentTime = System.currentTimeMillis() / 1000; // Gets the current time in seconds
    	long payloadAge = currentTime - payloadTime;
    	if (payloadAge > PYBITMESSAGE_NEW_OBJECT_ACCEPTANCE_PERIOD)
    	{
    		return true;
    	}
		
		AddressProvider addProv = AddressProvider.get(App.getContext());
		ArrayList<Address> myAddresses = addProv.getAllAddresses();
    	
		if (addressVersion <= 3)
		{
			for (Address a : myAddresses)
			{
				if (Arrays.equals(identifier, a.getRipeHash()))
				{
					// Retrieve the requested pubkey
					PubkeyProvider pubProv = PubkeyProvider.get(App.getContext());
					Pubkey requestedPubkey = pubProv.searchForSingleRecord(a.getCorrespondingPubkeyId());
					
					// Check whether the requested pubkey has been disseminated recently enough that
					// it does not yet need to be disseminated again
					long pubkeyTime = requestedPubkey.getTime();
					long timeSinceLastPubkeyDissemination = currentTime - pubkeyTime;
					if (timeSinceLastPubkeyDissemination < PYBITMESSAGE_PUBKEY_RETENTION_PERIOD)
					{
						return true;
					}
					else
					{
						// This getpubkey request is valid. Let us respond to it
						// by re-disseminating the relevant pubkey. 
						return disseminateRequestedPubkey(requestedPubkey);
					}
				}
			}
		}
		else
		{
			for (Address a : myAddresses)
			{
				if (Arrays.equals(identifier, a.getTag()))
				{
					// Retrieve the requested pubkey
					PubkeyProvider pubProv = PubkeyProvider.get(App.getContext());
					Pubkey requestedPubkey = pubProv.searchForSingleRecord(a.getCorrespondingPubkeyId());
					
					// Check whether the requested pubkey has been disseminated recently enough that
					// it does not yet need to be disseminated again
					long pubkeyTime = requestedPubkey.getTime();
					long timeSinceLastPubkeyDissemination = currentTime - pubkeyTime;
					if (timeSinceLastPubkeyDissemination < PYBITMESSAGE_PUBKEY_RETENTION_PERIOD)
					{
						return true;
					}
					else
					{
						// This getpubkey request is valid. Let us respond to it
						// by re-disseminating the relevant pubkey. 
						return disseminateRequestedPubkey(requestedPubkey);
					}
				}
			}
		}
		// If the getpubkey object was valid but was not for any of my addresses. 
		return true;
	}
	
	/**
	 * Takes a pubkey that needs to be disseminated to the Bitmessage network
	 * in response to a getpubkey request,  
	 * 
	 * @param pubkey - The Pubkey that needs to be disseminated.
	 */
	private boolean disseminateRequestedPubkey(Pubkey pubkey)
	{	
		// Update the time of this pubkey - the Bitmessage network won't accept it if it is too old
		pubkey.setTime(System.currentTimeMillis() / 1000);
		
		// Create a payload for the updated pubkey that can be disseminated to the Bitmessage network
		// This includes doing POW again
		PubkeyProcessor pubProc = new PubkeyProcessor();
		Payload pubkeyPayload = pubProc.constructPubkeyPayload(pubkey, true);
		
		// Disseminate the pubkey payload
		ServerCommunicator servCom = new ServerCommunicator();
		boolean disseminationSuccessful = servCom.disseminatePubkey(pubkeyPayload.getPayload());
		if (disseminationSuccessful == true)
		{
			// Update the pubkey record with the new time and pow nonce
			PubkeyProvider pubProv = PubkeyProvider.get(App.getContext());
			pubProv.updatePubkey(pubkey);
			
			return true;
		}
		else
		{
			return false;
		}
	}
}