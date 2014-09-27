package org.bitseal.core;

import org.bitseal.crypt.SHA512;
import org.bitseal.util.ArrayCopier;

/**
 * A class which provides various methods used for processing
 * inventory objects within Bitseal. 
 * 
 * @author Jonathan Coe
 */
public class InventoryProcessor
{
	/**
	 * Calculates the inventory hash of a given payload. 
	 * 
	 * @param A byte[] containing the payload
	 * 
	 * @return A byte[] containing the inventory hash
	 */
	public byte[] calculateInventoryHash(byte[] payload)
	{
		byte[] inventoryHash = ArrayCopier.copyOfRange(SHA512.doubleDigest(payload), 0, 32);
		return inventoryHash;
	}
}