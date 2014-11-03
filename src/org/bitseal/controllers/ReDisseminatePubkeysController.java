package org.bitseal.controllers;

import java.util.ArrayList;

import org.bitseal.core.App;
import org.bitseal.core.PubkeyProcessor;
import org.bitseal.crypt.PubkeyGenerator;
import org.bitseal.data.Address;
import org.bitseal.data.Payload;
import org.bitseal.data.Pubkey;
import org.bitseal.database.AddressProvider;
import org.bitseal.database.PayloadProvider;
import org.bitseal.database.PayloadsTable;
import org.bitseal.database.PubkeyProvider;
import org.bitseal.database.PubkeysTable;
import org.bitseal.util.TimeUtils;

import android.util.Log;

/**
 * This class controls the operations necessary to check whether
 * any of our pubkeys need to be disseminated to the Bitmessage 
 * network again, and to do so when that time comes. 
 * 
 * @author Jonathan Coe
 */
public class ReDisseminatePubkeysController
{	
	private static final String TAG = "RE_DISSEMINATE_PUBKEYS_CONTROLLER";
	
	/**
	 * Checks whether any of our pubkeys are due to be disseminated again. 
	 * 
	 * @return An ArrayList<Address> containing any Addresses for which the Pubkeys
	 * have expired and are thus due to be disseminated again. If none are due, this
	 * ArrayList will be empty. 
	 */
	public ArrayList<Address> checkIfPubkeyDisseminationIsDue()
	{
		// Get all the user's pubkeys
		ArrayList<Pubkey> myPubkeys = PubkeyProvider.get(App.getContext()).searchPubkeys(PubkeysTable.COLUMN_BELONGS_TO_ME, String.valueOf(1)); // 1 stands for true in the database
		
		// Check whether any of our pubkeys need to be disseminated again
		ArrayList<Address> addressesWithExpiredPubkeys = new ArrayList<Address>();
		for (Pubkey p : myPubkeys)
		{
			Address address = AddressProvider.get(App.getContext()).searchForSingleRecord(p.getCorrespondingAddressId());
			
			long currentTime = System.currentTimeMillis() / 1000;
			long expirationTime = p.getExpirationTime();
			if (expirationTime < currentTime)
			{
				long timeSinceExpiration = currentTime - expirationTime;
				Log.d(TAG, "The pubkey for address " + address.getAddress() + " expired " + TimeUtils.getTimeMessage(timeSinceExpiration) + " ago.\n" +
						"We will now attempt to re-disseminate it.");
				
				addressesWithExpiredPubkeys.add(address);
			}
			else
			{
				long timeTillExpiration = expirationTime - currentTime;
				Log.i(TAG, "The pubkey for address " + address.getAddress() + " will expire and be due for re-dissemination in " + TimeUtils.getTimeMessage(timeTillExpiration));
			}
		}
		
		return addressesWithExpiredPubkeys;
	}
	
	/**
	 * Creates an updated payload for pubkeys that need to be disseminated again. 
	 * 
	 * @param address - The Address which requires its pubkey to be regenerated
	 * @param doPOW - A boolean indicating whether or not to do POW for the updated
	 * pubkey payload
	 * 
	 * @return The updated pubkey payload
	 */
	public Payload regeneratePubkey(Address address, boolean doPOW)
	{
		// Delete the old pubkey
		PubkeyProvider pubProv = PubkeyProvider.get(App.getContext());
		Pubkey oldPubkey = pubProv.searchForSingleRecord(address.getCorrespondingPubkeyId());
		pubProv.deletePubkey(oldPubkey);
		
		// Delete the old pubkey's corresponding Payload(s)
		PayloadProvider payProv = PayloadProvider.get(App.getContext());
		String [] columnNames = new String[]{PayloadsTable.COLUMN_TYPE, PayloadsTable.COLUMN_BELONGS_TO_ME, PayloadsTable.COLUMN_RELATED_ADDRESS_ID};
		String[] selections = new String[]{Payload.OBJECT_TYPE_PUBKEY, "1", String.valueOf(address.getId())}; // 1 stands for true in the database
		ArrayList<Payload> matchingPayloads = payProv.searchPayloads(columnNames, selections);
		for (Payload p : matchingPayloads)
		{
			payProv.deletePayload(p);
		}
		
		// Generate a new pubkey
		Pubkey regeneratedPubkey = new PubkeyGenerator().generateAndSaveNewPubkey(address);
		
		// Create an updated payload for the pubkey. We can then re-disseminate it to the network.
		return new PubkeyProcessor().constructPubkeyPayload(regeneratedPubkey, doPOW);
	}
}