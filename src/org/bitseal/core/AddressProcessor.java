package org.bitseal.core;

import java.util.Arrays;

import org.bitseal.crypt.AddressGenerator;
import org.bitseal.crypt.SHA256;
import org.bitseal.crypt.SHA512;
import org.bitseal.data.Address;
import org.bitseal.services.BackgroundService;
import org.bitseal.util.ArrayCopier;
import org.bitseal.util.Base58;
import org.bitseal.util.ByteFormatter;
import org.bitseal.util.ByteUtils;
import org.bitseal.util.VarintEncoder;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * A class which provides various methods used for processing
 * Bitmessage addresses within Bitseal. 
 * 
 * @author Jonathan Coe
 */
public final class AddressProcessor
{
	private static final String BITMESSAGE_ADDRESS_PREFIX = "BM-";
	private static final int BITMESSAGE_ADDRESS_MIN_LENGTH = 35;
	private static final int BITMESSAGE_ADDRESS_MAX_LENGTH = 38;
	
	private static final int SECONDS_IN_A_DAY = 86400;
	
	private static final String TAG = "ADDRESS_PROCESSOR";
	
	/**
	 * Checks whether or not a given String is a valid Bitmessage address. 
	 * 
	 * @param address - A String containing the Bitmessage address to be validated
	 * 
	 * @return A boolean indicating whether or not the String is a valid Bitmessage address
	 */
	public boolean validateAddress (String address)
	{		
		// Validation check 1: Check the length of the String. NOTE: This check assumes that all valid addresses are between 35 and 38 characters in length
		if ((address.length() >= BITMESSAGE_ADDRESS_MIN_LENGTH) != true)
		{
			Log.i(TAG, "An address String supplied to AddressProcessor.validateAddress() was found to be shorter than the minimum length. \n" +
					"The invalid address String was: " + address);
			return false;
		}
		if ((address.length() <= BITMESSAGE_ADDRESS_MAX_LENGTH) != true)
		{
			Log.i(TAG, "An address String supplied to AddressProcessor.validateAddress() was found to be longer than the maximum length. \n" +
					"The invalid address String was: " + address);
			return false;
		}
		
		// Validation check 2: Check whether or not the first 3 characters of the String match the required Bitmessage address prefix
		String addressPrefix = address.substring(0, 3);
		
		if (addressPrefix.equals(BITMESSAGE_ADDRESS_PREFIX) != true)
		{
			Log.i(TAG, "An address String supplied to AddressProcessor.validateAddress() was found to have an invalid prefix. \n" +
					"The invalid address String was: " + address);
			return false;
		}
		
		// Validation check 3: Check whether or not the final 4 characters of the String are a valid checksum for all other characters after the "BM-" prefix
		String addressData = address.substring(3, address.length());
		
		byte[] addressDataBytes = null;
		
		addressDataBytes = Base58.decode(addressData);
		
		byte[] combinedChecksumData = ArrayCopier.copyOfRange(addressDataBytes, 0, (addressDataBytes.length - 4));
		
		byte[] checksum = ArrayCopier.copyOfRange(addressDataBytes, (addressDataBytes.length - 4), addressDataBytes.length);
		
		byte[] testChecksumFullHash = SHA512.doubleHash(combinedChecksumData);
		
		byte[] testChecksum = ArrayCopier.copyOfRange(testChecksumFullHash, 0, 4);
		
		if (Arrays.equals(checksum, testChecksum) != true)
		{
			Log.i(TAG, "An address String supplied to AddressProcessor.validateAddress() was found to have an invalid checksum. \n" +
					"The invalid address String was: " + address);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Returns a boolean indicating whether a given String is a valid Bitmessage private key.
	 */
	public boolean validatePrivateKey(String privateKey)
	{
		byte[] privateKeyBytes = null;
		
		try
		{
			privateKeyBytes = Base58.decode(privateKey);
		}
		catch (IllegalArgumentException e)
		{
			Log.i(TAG, "While validating a private key in AddressProcessor.validatePrivateKey(), the given String was found to contain an invalid character.");
			return false;
		}
		
		byte[] privateKeyWithoutChecksum = ArrayCopier.copyOfRange(privateKeyBytes, 0, (privateKeyBytes.length - 4));
		
		byte[] checksum = ArrayCopier.copyOfRange(privateKeyBytes, (privateKeyBytes.length - 4), privateKeyBytes.length);
		
		byte[] hashOfPrivateKey = SHA256.doubleDigest(privateKeyWithoutChecksum);
		
		byte[] testChecksum = ArrayCopier.copyOfRange(hashOfPrivateKey, 0, 4);
		
		// Check the checksum
		if (Arrays.equals(checksum, testChecksum) == false)
		{
			Log.i(TAG, "While validating a private key in AddressProcessor.validatePrivateKey(), the checksum was found to be invalid.");
			return false;
		}
		
		// Check that the prepended 128 byte is in place
		if (privateKeyWithoutChecksum[0] != (byte) 128)
		{
			Log.i(TAG, "While validating a private key in AddressProcessor.validatePrivateKey(), its prepended value was found to be invalid.");
			return false;
		}
		
		// Drop the prepended 128 byte
		byte[] privateKeyFinalBytes = ArrayCopier.copyOfRange(privateKeyWithoutChecksum, 1, privateKeyWithoutChecksum.length);
		
		// Check the length of the key
		if (privateKeyFinalBytes.length != 32)
		{
			Log.i(TAG, "While validating a private key in AddressProcessor.validatePrivateKey(), its length was found to be " + privateKeyFinalBytes.length + " instead of 32.");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Takes a String representing a Bitmessage address (e.g. "BM-NBniqBpDRZHLx7rVWyyrEf1XmPgSiSrr"
	 * and extracts the address version number and stream number encoded in the address.
	 * 
	 * @param addressString - A String containing the Bitmessage address that we wish to extract the
	 * address version number and stream number from
	 * 
	 * @return A int[] of exactly two elements. The first is an int representing the address version
	 * number and the second is an int representing the stream number
	 */
	public int[] decodeAddressNumbers (String addressString)
	{
		// First check that the String supplied is a valid Bitmessage address
		if (validateAddress(addressString) == false)
		{
			throw new RuntimeException("Address String supplied to AddressProcessor.extractAddressVersionAndStreamNumberFromAddress() was found" +
					"to be an invalid address by the AddressValidator.validateAddress() method. Throwing new RuntimeException.");
		}
		else
		{
			// Remove leading 3 characters ("BM-") 
			String addressDataString = addressString.substring(3, addressString.length());
			
			// Do Base58 decode on the remaining string to get the "combinedAddressData" byte[]
			byte[] combinedAddressData = Base58.decode(addressDataString);
			
			// Do Varint check on first 9 bytes (for the address version)
			long[] decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(combinedAddressData, 0, 9));
			int addressVersion = (int) decoded[0];
			int bytesUsedForAddressVersion = (int) decoded[1]; // The number of bytes that were used to encode the address version
			
			// Taking account of length result from first varint check, do second varint check (for the stream number)
			decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(combinedAddressData, bytesUsedForAddressVersion, bytesUsedForAddressVersion + 9));
			int streamNumber = (int) decoded[0];
			
			return new int[]{addressVersion, streamNumber};
		}
	}
	
	/**
	 * Takes a String representing a Bitmessage address (e.g. "BM-NBniqBpDRZHLx7rVWyyrEf1XmPgSiSrr"
	 * and extracts the ripe hash encoded in the address.
	 * 
	 * @param addressString - A String containing the Bitmessage address that we wish to extract the
	 * ripe hash from
	 * 
	 * @return A byte[] containing the ripe hash extracted from the address
	 */
	public byte[] extractRipeHashFromAddress (String addressString)
	{
		// First check that the String supplied is a valid Bitmessage address
		if (validateAddress(addressString) == false)
		{
			throw new RuntimeException("Address String supplied to AddressProcessor.extractRipeHashFromAddressString() was found" +
					"to be an invalid address by the AddressValidator.validateAddress() method. Throwing new RuntimeException.");
		}
		else
		{
			// Remove leading 3 characters ("BM-") 
			String addressDataString = addressString.substring(3, addressString.length());
			
			// Do Base58 decode on the remaining string to get the "combinedAddressData" byte[]
			byte[] combinedAddressData = Base58.decode(addressDataString);
			
			// Discard the final 4 bytes (the checksum)
			byte[] combinedChecksumData = ArrayCopier.copyOfRange(combinedAddressData, 0, combinedAddressData.length - 4);
			
			// Do varint check on first 9 bytes (for the address version)
			long[] decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(combinedChecksumData, 0, 9));
			int bytesUsedForAddressVersion = (int) decoded[1]; // The number of bytes that were used to encode the address version
			
			// Taking account of length result from first varint check, do second varint check (for the stream number)
			decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(combinedChecksumData, bytesUsedForAddressVersion, bytesUsedForAddressVersion + 9));
			int bytesUsedForStreamNumber = (int) decoded[1]; // The number of bytes that were used to encode the stream number
			
			// The remaining bytes are the ripe hash.
			byte [] ripeHash = ArrayCopier.copyOfRange(combinedChecksumData, bytesUsedForAddressVersion + bytesUsedForStreamNumber, combinedChecksumData.length);
			
			// If the ripe hash is less than 20 bytes in length, it needs to be padded with zero bytes until it is
			while (ripeHash.length < 20)
			{
				byte[] zeroByte = new byte[]{0};
				ripeHash = ByteUtils.concatenateByteArrays(zeroByte, ripeHash);
			}
			Log.i(TAG, "Ripe hash extracted from address string:                " + ByteFormatter.byteArrayToHexString(ripeHash));
			
			return ripeHash;
		}
	}
	
	/**
	 * Calculates the encryption key of a given Bitmessage address. The encryption
	 * key is the first half of the double SHA-512 hash of the combined address data. 
	 * 
	 * @param addressString - A String containing the Bitmessage address to calculate
	 * the encryption key of
	 * 
	 * @return A byte[] containing the encryption key
	 */
	public byte[] calculateAddressEncryptionKey (String addressString)
	{
		byte[] doubleHash = calculateDoubleHashOfAddressData(addressString);
		byte[] encryptionKey = ArrayCopier.copyOfRange(doubleHash, 0, 32);
		return encryptionKey;
	}
	
	/**
	 * Calculates the 'tag' of a given Bitmessage address. The 'tag' is the second
	 * half of the double SHA-512 hash of the combined address data. 
	 * 
	 * @param addressString - A String containing the Bitmessage address to calculate
	 * the tag of
	 * 
	 * @return A byte[] containing the tag
	 */
	public byte[] calculateAddressTag (String addressString)
	{
		byte[] doubleHash = calculateDoubleHashOfAddressData(addressString);
		byte[] tag = ArrayCopier.copyOfRange(doubleHash, 32, doubleHash.length);
		return tag;
	}
	
	/**
	 * Calculates the double hash of the data encoded in a Bitmessage address.
	 * This 'double hash of address data' is used in the encryption of pubkeys.
	 * 
	 * @param addressString - A String containing the Bitmessage address to calculate
	 * the double hash of
	 * 
	 * @return A byte[] containing the 'double hash of address data'
	 */
	public byte[] calculateDoubleHashOfAddressData (String addressString)
	{
		// First check that the String supplied is a valid Bitmessage address
		if (validateAddress(addressString) == false)
		{
			throw new RuntimeException("Address String supplied to AddressProcessor.calculateDoubleHashOfAddressData() was found" +
					"to be an invalid address by the AddressValidator.validateAddress() method. Throwing new RuntimeException.");
		}
		else
		{
			// Calculate the 'encoded address data'. This is the data to be hashed. 
			byte[] dataToHash = extractEncodedAddressData(addressString);
			
			// Double-hash the address data
			byte[] doubleHashOfAddressData = SHA512.doubleHash(dataToHash);
			
			return doubleHashOfAddressData;
		}
	}
	
	/**
	 * Calculates the message tag for a given address at the current time. 
	 * 
	 * @param addressString - A String containing the Bitmessage address to calculate
	 * the message tag of
	 * 
	 * @return A byte[] containing the message tag
	 */
	public byte[] calculateCurrentMessageTag (String addressString)
	{
		return calculateMessageTag(addressString, System.currentTimeMillis() / 1000);
	}
	
	/**
	 * Calculates the message tags for a given address since a given time. 
	 * 
	 * @param addressString - A String containing the Bitmessage address to calculate
	 * the message tags of
	 * @param pastTime - A long containing the time value to use to calculate the
	 * message tags
	 * 
	 * @return A byte[] containing the message tags
	 */
	public byte[] calculateMessageTagsSince (String addressString, long pastTime)
	{
		long currentTime = System.currentTimeMillis() / 1000;
		long timeElapsed = currentTime - pastTime;
		long numberOfDaysSince = timeElapsed / SECONDS_IN_A_DAY;
		
		byte[] messageTags = new byte[0];
		for (int i = 0; i <= numberOfDaysSince; i++)
		{
			byte[] tag = calculateMessageTag(addressString, pastTime);
			messageTags = ByteUtils.concatenateByteArrays(messageTags, tag);
			pastTime += SECONDS_IN_A_DAY; // Advance to the next day
		}
		
		return messageTags;
	}
	
	/**
	 * Calculates the message tag for a given address and a given time value. 
	 * 
	 * @param addressString - A String containing the Bitmessage address to calculate
	 * the message tag of
	 * @param time - A long containing the time value to use to calculate the message tag
	 * 
	 * @return A byte[] containing the message tag
	 */
	public byte[] calculateMessageTag (String addressString, long time)
	{
		// First check that the String supplied is a valid Bitmessage address
		if (validateAddress(addressString) == false)
		{
			throw new RuntimeException("Address String supplied to AddressProcessor.calculateMessageTag() was found" +
					"to be an invalid address by the AddressValidator.validateAddress() method. Throwing new RuntimeException.");
		}
		else
		{
			byte[] encodedAddressData = extractEncodedAddressData(addressString);
			
			// Calculate the time value to use in the hash
			long remainderSeconds = time % SECONDS_IN_A_DAY;
			long timeValue = time - remainderSeconds;
			
			// Get the byte form of the time value
			byte[] timeValueBytes = ByteUtils.longToBytes(timeValue);
			
			// Combine the bytes for the encoded address data and the time value into a single byte[]. This is the data to be hashed. 
			byte[] dataToHash = ByteUtils.concatenateByteArrays(encodedAddressData, timeValueBytes);
			
			// Hash the input data to get the full tag
			byte[] fullTag = SHA512.doubleHash(dataToHash);
			
			// Get the first 32 bytes of the full tag. The result is the message tag. 
			byte[] messageTag = ArrayCopier.copyOf(fullTag, 32);
			return messageTag;
		}
	}
	
	/**
	 * Takes a pair of private keys and re-generates the Bitmessage address that
	 * they correspond to. The imported address is then saved to the database. 
	 * 
	 * @param privateSigningKey -  The private signing key
	 * @param privateEncryptionKey - The private encryption key
	 * 
	 * @return A boolean indicating whether or not the address was successfully imported
	 */
	public boolean importAddress (String privateSigningKey, String privateEncryptionKey)
	{
		try
		{
			// Re-create and save the Address
			Address recreatedAddress = new AddressGenerator().importAddress(privateSigningKey, privateEncryptionKey);
						
			// Make a BackgroundService request for the task, using the QueueRecord
			Context appContext = App.getContext();
		    Intent intent = new Intent(appContext, BackgroundService.class);
		    intent.putExtra(BackgroundService.UI_REQUEST, BackgroundService.UI_REQUEST_CREATE_IDENTITY);
		    intent.putExtra(BackgroundService.ADDRESS_ID, recreatedAddress.getId());
		    appContext.startService(intent);
		}
		catch (Exception e)
		{
			Log.e(TAG, "Exception occurred while running AddressProcessor.importAddress(). The exception message was: " + e.getMessage());
			return false;
		}
		
		return true;
	}
	
	/**
	 * Extracts the 'encoded address data' of a given Bitmessage address. 
	 * The 'encoded address data' is made up of the address version number, 
	 * stream number, and ripe hash of the address, encoded into a single byte[]. 
	 * 
	 * @param addressString - A String containing the Bitmessage address to calculate
	 * the 'encoded address data' of 
	 * 
	 * @return A byte[] containing the 'encoded address data'
	 */
	private byte[] extractEncodedAddressData (String addressString)
	{
		// Remove leading 3 characters ("BM-") 
		String addressDataString = addressString.substring(3, addressString.length());
		
		// Do Base58 decode on the remaining string to get the "combinedAddressData" byte[]
		byte[] combinedAddressData = Base58.decode(addressDataString);
		
		// Discard the final 4 bytes (the checksum)
		byte[] combinedChecksumData = ArrayCopier.copyOfRange(combinedAddressData, 0, combinedAddressData.length - 4);
		
		// Do Varint check on first 9 bytes (for the address version)
		long[] decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(combinedChecksumData, 0, 9));
		int addressVersion = (int) decoded[0];
		int bytesUsedForAddressVersion = (int) decoded[1]; // The number of bytes that were used to encode the address version
		
		// Taking account of length result from first varint check, do second varint check (for the stream number)
		decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(combinedChecksumData, bytesUsedForAddressVersion, bytesUsedForAddressVersion + 9));
		int streamNumber = (int) decoded[0];
		int bytesUsedForStreamNumber = (int) decoded[1]; // The number of bytes that were used to encode the stream number
		
		// The remaining bytes are the ripe hash.
		byte [] ripeHash = ArrayCopier.copyOfRange(combinedChecksumData, bytesUsedForAddressVersion + bytesUsedForStreamNumber, combinedChecksumData.length);
		
		// If the ripe hash is less than 20 bytes in length, it needs to be padded with zero bytes until it is
		while (ripeHash.length < 20)
		{
			byte[] zeroByte = new byte[]{0};
			ripeHash = ByteUtils.concatenateByteArrays(zeroByte, ripeHash);
		}
		
		// Convert the address version and stream number into varint-encoded byte form
		byte[] encodedAddressVersion = VarintEncoder.encode(addressVersion);
		byte[] encodedStreamNumber = VarintEncoder.encode(streamNumber);
		
		// Combine the bytes for the address version, stream number, and ripe hash into a single byte[]
		byte[] encodedAddressData = ByteUtils.concatenateByteArrays(encodedAddressVersion, encodedStreamNumber);
		encodedAddressData = ByteUtils.concatenateByteArrays(encodedAddressData, ripeHash);
		return encodedAddressData;
	}
}