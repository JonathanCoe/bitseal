package org.bitseal.core;

import org.bitseal.data.BMObject;
import org.bitseal.pow.POWProcessor;
import org.bitseal.util.ArrayCopier;
import org.bitseal.util.ByteFormatter;
import org.bitseal.util.ByteUtils;
import org.bitseal.util.TimeUtils;
import org.bitseal.util.VarintEncoder;

/**
 * A class which provides various methods used for processing
 * Bitmessage Objects within Bitseal. 
 * 
 * @author Jonathan Coe
 */
public class ObjectProcessor
{
	private static final long MAX_TIME_TILL_EXPIRATION = 2430000; // 28 days and 3 hours
	
	private static final int MIN_VALID_OBJECT_TYPE = 0;
	private static final int MAX_VALID_OBJECT_TYPE = 3;
	
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
	 * and parses it, returning a BMObject. 
	 * 
	 * @param objectBytes - A byte[] containing the Object data
	 * 
	 * @return A BMObject created from the parsed data
	 */
	public BMObject parseObject (byte[] objectBytes)
	{
		// Read the POW Nonce
		int readPosition = 0;
		long powNonce = ByteUtils.bytesToLong((ArrayCopier.copyOfRange(objectBytes, readPosition, readPosition + 8)));
		readPosition += 8; //The POW nonce should always be 8 bytes in length
		
		// Read and check the expiration time
		long expirationTime = ByteUtils.bytesToLong((ArrayCopier.copyOfRange(objectBytes, readPosition, readPosition + 8)));
		readPosition += 8;
		long currentTime = System.currentTimeMillis() / 1000;
		if (expirationTime < currentTime)
		{
			throw new RuntimeException("While running ObjectProcessor.parseObject(), it was found that the object's expiration time passed " + TimeUtils.getTimeMessage(currentTime - expirationTime) + " ago.\n"
					+ "The full object containing the passed expriation time was: " + ByteFormatter.byteArrayToHexString(objectBytes));
		}
		else if (expirationTime > currentTime + MAX_TIME_TILL_EXPIRATION)
		{
			throw new RuntimeException("While running ObjectProcessor.parseObject(), the embedded expiration time was found to be too far in the future. \n" 
					+ "The embedded expiration time was " + expirationTime + ", which is " + TimeUtils.getTimeMessage(expirationTime - currentTime) + " in the future.\n"
					+ "The full object containing the invalid expiration time was: " + ByteFormatter.byteArrayToHexString(objectBytes));
		}
		
		// Read and check the object type
		int objectType = ByteUtils.bytesToInt((ArrayCopier.copyOfRange(objectBytes, readPosition, readPosition + 4)));
		readPosition += 4;
		if (objectType < MIN_VALID_OBJECT_TYPE || objectType > MAX_VALID_OBJECT_TYPE)
		{
			throw new RuntimeException("While running ObjectProcessor.parseObject(), the decoded object type number was invalid. The invalid value was " + objectType + ".\n"
					+ "The full object containing the invalid object type number was: " + ByteFormatter.byteArrayToHexString(objectBytes));
		}
		
		// Read and check the object version
		long[] decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(objectBytes, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		int objectVersion = (int) decoded[0]; // Get the var_int encoded value
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		if (objectVersion < MIN_VALID_OBJECT_VERSION || objectVersion > MAX_VALID_OBJECT_VERSION)
		{
			throw new RuntimeException("While running ObjectProcessor.parseObject(), the decoded object version number was invalid. The invalid value was " + objectVersion + ".\n"
					+ "The full object containing the invalid object version number was: " + ByteFormatter.byteArrayToHexString(objectBytes));
		}
		
		// Read and check the stream number
		decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(objectBytes, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		int streamNumber = (int) decoded[0]; // Get the var_int encoded value
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		if (streamNumber < MIN_VALID_STREAM_NUMBER || streamNumber > MAX_VALID_STREAM_NUMBER)
		{
			throw new RuntimeException("While running ObjectProcessor.parseObject(), the decoded object stream number was invalid. The invalid value was " + streamNumber + ".\n"
					+ "The full object containing the invalid stream number was: " + ByteFormatter.byteArrayToHexString(objectBytes));
		}
		
		// Read the remaining data.
		byte[] payload = ArrayCopier.copyOfRange(objectBytes, readPosition, objectBytes.length);
		
		// Check whether the POW for this Object is valid
		byte[] powPayload = ArrayCopier.copyOfRange(objectBytes, 8, objectBytes.length);
		boolean powValid = new POWProcessor().checkPOW(powPayload, powNonce, expirationTime, NETWORK_NONCE_TRIALS_PER_BYTE, NETWORK_EXTRA_BYTES);
		if (powValid == false)
		{
			throw new RuntimeException("While running ObjectProcessor.parseObject(), the POW nonce was found to be invalid. The invalid value was " + powNonce + ".\n"
					+ "The full object containing the invalid POW nonce was: " + ByteFormatter.byteArrayToHexString(objectBytes));
		}
		
		// Create a new BMObject and use the parsed data to populate its fields
		BMObject bmObject = new BMObject();
		bmObject.setBelongsToMe(false); // i.e. this BMObject was not created by me
		bmObject.setPOWNonce(powNonce);
		bmObject.setExpirationTime(expirationTime);
		bmObject.setObjectType(objectType);
		bmObject.setObjectVersion(objectVersion);
		bmObject.setStreamNumber(streamNumber);
		bmObject.setPayload(payload);
		
		return bmObject;
	}
}