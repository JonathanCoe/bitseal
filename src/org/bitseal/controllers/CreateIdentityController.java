package org.bitseal.controllers;

import org.bitseal.core.PubkeyProcessor;
import org.bitseal.crypt.PubkeyGenerator;
import org.bitseal.data.Address;
import org.bitseal.data.Payload;
import org.bitseal.data.Pubkey;
import org.bitseal.network.ServerCommunicator;

/**
 * This class controls the operations necessary to create a new
 * identity and disseminate the public data of that identity to 
 * the rest of the Bitmessage network.
 * 
 * @author Jonathan Coe
 */
public class CreateIdentityController
{
	/**
	 * Generates and saves a new Pubkey for a given Address and constructs
	 * the payload for that Pubkey. 
	 * 
	 * @param address - The Address object containing the Bitmessage address
	 * to create pubkey data for
	 * @param doPOW - A boolean indicating whether or not proof of 
	 * work calculations should be done for the Pubkey created
	 * during this process 
	 * 
	 * @return A Payload object containing the pubkey payload that is
	 * ready to be sent over the network
	 */
	public Payload generatePubkeyData(Address address, boolean doPOW)
	{
		Pubkey pubkey = new PubkeyGenerator().generateAndSaveNewPubkey(address);
		
		return new PubkeyProcessor().constructPubkeyPayload(pubkey, doPOW);
	}
	
	/**
	 * Attempts to disseminate a pubkey to the Bitmessage network. 
	 * 
	 * @param pubkeyPayload - The pubkey payload to be sent across the network
	 * @param POWDone - A boolean indicating whether or not proof of work has
	 * been done for this pubkey. If not, a server will be expected to do the 
	 * proof of work. 
	 * 
	 * @return A boolean indicating whether or not the pubkey was
	 * successfully disseminated to the network
	 */
	public boolean disseminatePubkey(Payload pubkeyPayload, boolean POWDone)
	{
		byte[] payload = pubkeyPayload.getPayload();
		
		boolean disseminationSuccessful;
		
		if (POWDone == true)
		{
			disseminationSuccessful = new ServerCommunicator().disseminatePubkey(payload);
		}
		else
		{
			disseminationSuccessful = new ServerCommunicator().disseminatePubkeyNoPOW(payload);
		}
		
		return disseminationSuccessful;
	}
} 