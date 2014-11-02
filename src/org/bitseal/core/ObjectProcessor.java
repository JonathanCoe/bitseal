package org.bitseal.core;

import org.bitseal.data.Object;
import org.bitseal.pow.POWProcessor;
import org.bitseal.util.ArrayCopier;
import org.bitseal.util.ByteUtils;
import org.bitseal.util.VarintEncoder;

/**
 * A class which provides various methods used for processing
 * Bitmessage Objects within Bitseal. 
 * 
 * @author Jonathan Coe
 */
public class ObjectProcessor
{
	private static final int MIN_VALID_OBJECT_VERSION = 1;
	private static final int MAX_VALID_OBJECT_VERSION = 4;
	
	private static final int MIN_VALID_STREAM_NUMBER = 1;
	private static final int MAX_VALID_STREAM_NUMBER = 1;
	
	/** In Bitmessage protocol version 3, the network standard value for nonce trials per byte is 1000. */
	public static final int NETWORK_NONCE_TRIALS_PER_BYTE = 1000;
	
	/** In Bitmessage protocol version 3, the network standard value for extra bytes is 1000. */
	public static final int NETWORK_EXTRA_BYTES = 1000;
	
	/**
	 * Validates a set of bytes that may contain a Bitmessage Object
	 * 
	 * @param objectBytes - A byte[] containing the Object data
	 * 
	 * @return A boolean indicating whether or not the provided bytes are
	 * a valid Bitmessage Object
	 */
	public boolean validateObject (byte[] objectBytes)
	{
		try
		{
			parseObject(objectBytes);
			return true;
		}
		catch(RuntimeException e)
		{
			return false;
		}
	}
	
	/**
	 * Takes a byte[] containing the data of a Bitmessage Object (e.g. a msg)
	 * and parses it, returning an Object. 
	 * 
	 * @param objectBytes - A byte[] containing the Object data
	 * 
	 * @return An Object created from the parsed data
	 */
	public Object parseObject (byte[] objectBytes)
	{	
		// Parse the data from the byte[] 
		int readPosition = 0;
		
		long powNonce = ByteUtils.bytesToLong((ArrayCopier.copyOfRange(objectBytes, readPosition, readPosition + 8)));
		readPosition += 8; //The POW nonce should always be 8 bytes in length
		
		long expirationTime = ByteUtils.bytesToLong((ArrayCopier.copyOfRange(objectBytes, readPosition, readPosition + 8)));
		readPosition += 8;
		
		int objectType = ByteUtils.bytesToInt((ArrayCopier.copyOfRange(objectBytes, readPosition, readPosition + 4)));
		readPosition += 4;
		
		// --------------------------------------------------Upgrade period code------------------------------------------------------
		long currentTime = System.currentTimeMillis() / 1000;
		int objectVersion = 0;
		if ((currentTime < 1416175200 && objectType == 2) == false) // All objects apart from msgs received before Sun, 16 November 2014 22:00:00 GMT
		{
			long[] decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(objectBytes, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
			objectVersion = (int) decoded[0]; // Get the var_int encoded value
			readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
			if (objectVersion < MIN_VALID_OBJECT_VERSION || objectVersion > MAX_VALID_OBJECT_VERSION)
			{
				throw new RuntimeException("While running ObjectProcessor.parseObject(), the decoded object version number was invalid. The invalid value was " + objectVersion);
			}
		}
		// --------------------------------------------------------------------------------------------------------------------------------	
		
		long[] decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(objectBytes, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		int streamNumber = (int) decoded[0]; // Get the var_int encoded value
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		if (streamNumber < MIN_VALID_STREAM_NUMBER || streamNumber > MAX_VALID_STREAM_NUMBER)
		{
			throw new RuntimeException("While running ObjectProcessor.parseObject(), the decoded object stream number was invalid. The invalid value was " + streamNumber);
		}
		
		// Now deal with the remaining data.
		byte[] payload = ArrayCopier.copyOfRange(objectBytes, readPosition, objectBytes.length);
		
		// Check whether the POW for this Object is valid
		byte[] powPayload = ArrayCopier.copyOfRange(objectBytes, 8, objectBytes.length);
		boolean powValid = new POWProcessor().checkPOW(powPayload, powNonce, expirationTime, NETWORK_NONCE_TRIALS_PER_BYTE, NETWORK_EXTRA_BYTES);
		if (powValid == false)
		{
			throw new RuntimeException("While running ObjectProcessor.parseObject(), the POW nonce was found to be invalid. The invalid value was " + powNonce);
		}
		
		// Create a new Object and use the parsed data to populate its fields
		Object object = new Object();
		object.setBelongsToMe(false); // i.e. this object was not created by me
		object.setPOWNonce(powNonce);
		object.setExpirationTime(expirationTime);
		object.setObjectType(objectType);
		
		// --------------------------------------------------Upgrade period code------------------------------------------------------
		if (currentTime < 1416175200 && objectType == 2) // Msgs received before Sun, 16 November 2014 22:00:00 GMT
		{
			object.setObjectVersion(1);
		}
		else
		{
			object.setObjectVersion(objectVersion);
		}
		// -------------------------------------------------------------------------------------------------------------------------------
		
		object.setStreamNumber(streamNumber);
		object.setPayload(payload);
		
		return object;
	}
}