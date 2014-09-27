package org.bitseal.controllers;

import java.util.ArrayList;

import org.bitseal.core.App;
import org.bitseal.core.PubkeyProcessor;
import org.bitseal.data.Address;
import org.bitseal.data.Payload;
import org.bitseal.data.Pubkey;
import org.bitseal.database.AddressProvider;
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
	/**
	 * The period of time (in seconds) after which we consider our pubkeys due to
	 * be disseminated again. Currently based on the period for which PyBitmessage
	 * retains all pubkeys, minus two days. 
	 */
	private static final long PUBKEY_RE_DISSEMINATION_PERIOD = 2246400;
	
	private static final String TAG = "RE_DISSEMINATE_PUBKEYS_CONTROLLER";
	
	/**
	 * Checks whether any of our pubkeys are due to be disseminated again. 
	 * 
	 * @return An ArrayList<Pubkey> containing any Pubkeys of ours that are
	 * due to be disseminated again. If none are due, this ArrayList will be
	 * empty. 
	 */
	public ArrayList<Pubkey> checkIfPubkeyDisseminationIsDue()
	{
		// Get all the user's pubkeys
		PubkeyProvider pubProv = PubkeyProvider.get(App.getContext());
		ArrayList<Pubkey> myPubkeys = pubProv.searchPubkeys(PubkeysTable.COLUMN_BELONGS_TO_ME, String.valueOf(1)); // 1 stands for true in the database
		
		long currentTime = System.currentTimeMillis() / 1000;
		
		ArrayList<Pubkey> pubkeysToReDisseminate = new ArrayList<Pubkey>();
		
		AddressProvider addProv = AddressProvider.get(App.getContext());
		
		// Check whether any of our pubkeys need to be disseminated again
		for (Pubkey p : myPubkeys)
		{
			Address address = addProv.searchForSingleRecord(p.getCorrespondingAddressId());
			
			long lastDisseminationTime = p.getLastDisseminationTime();
			long timeSinceLastDissemination = currentTime - lastDisseminationTime;
			if (timeSinceLastDissemination > PUBKEY_RE_DISSEMINATION_PERIOD)
			{
				Log.d(TAG, "The pubkey for address " + address.getAddress() + " was last disseminated " + TimeUtils.getTimeMessage(timeSinceLastDissemination) + " ago." +
						"We will now attempt to re-disseminate it.");
				
				pubkeysToReDisseminate.add(p);				
			}
			else
			{
				long timeTillDisseminationDue = PUBKEY_RE_DISSEMINATION_PERIOD - timeSinceLastDissemination;
				Log.i(TAG, "The pubkey for address " + address.getAddress() + " was last disseminated " + TimeUtils.getTimeMessage(timeSinceLastDissemination) + " ago." +
						"It will be due for dissemination again in " + TimeUtils.getTimeMessage(timeTillDisseminationDue) + ".");
			}
		}
		
		return pubkeysToReDisseminate;
	}
	
	/**
	 * Creates an updated payload for pubkeys that need to be disseminated again. 
	 * 
	 * @param pubkeysToReDisseminate - The Pubkeys to be re-disseminated
	 * @param doPOW - A boolean indicating whether or not to do POW for the updated
	 * pubkey payload
	 * 
	 * @return The updated pubkey payload
	 */
	public Payload reDisseminatePubkeys(Pubkey pubkeyToReDisseminate, boolean doPOW)
	{
		AddressProvider addProv = AddressProvider.get(App.getContext());
		Address address = addProv.searchForSingleRecord(pubkeyToReDisseminate.getCorrespondingAddressId());
		
		Log.d(TAG, "Re-disseminating the pubkey for address " + address.getAddress());
		
		pubkeyToReDisseminate.setTime(System.currentTimeMillis() / 1000);
		
		// Create an updated payload for the pubkey. We can then re-disseminate it to the network.
		return new PubkeyProcessor().constructPubkeyPayload(pubkeyToReDisseminate, doPOW);
	}
}