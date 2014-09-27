package org.bitseal.controllers;

import org.bitseal.core.App;
import org.bitseal.core.PubkeyProcessor;
import org.bitseal.crypt.PubkeyGenerator;
import org.bitseal.data.Address;
import org.bitseal.data.Payload;
import org.bitseal.data.Pubkey;
import org.bitseal.database.AddressProvider;
import org.bitseal.database.PubkeyProvider;
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
		PubkeyGenerator pubGen = new PubkeyGenerator();
		Pubkey pubkey = pubGen.generateAndSaveNewPubkey(address);
		
		PubkeyProcessor pubProc = new PubkeyProcessor();
		Payload pubkeyPayload = pubProc.constructPubkeyPayload(pubkey, doPOW);
		
		return pubkeyPayload;
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
		
		ServerCommunicator servCom = new ServerCommunicator();
		boolean disseminationSuccessful;
		
		if (POWDone == true)
		{
			disseminationSuccessful = servCom.disseminatePubkey(payload);
		}
		else
		{
			disseminationSuccessful = servCom.disseminatePubkeyNoPOW(payload);
		}
		if (disseminationSuccessful == true)
		{
			// Update the 'last dissemination time' of the Pubkey in our database
			AddressProvider addProv = AddressProvider.get(App.getContext());
			Address address = addProv.searchForSingleRecord(pubkeyPayload.getRelatedAddressId());
			PubkeyProvider pubProv = PubkeyProvider.get(App.getContext());
			Pubkey pubkey = pubProv.searchForSingleRecord(address.getCorrespondingPubkeyId());
			pubkey.setLastDisseminationTime(System.currentTimeMillis() / 1000);
			pubProv.updatePubkey(pubkey);
		}
		
		return disseminationSuccessful;
	}
} 