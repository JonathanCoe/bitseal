package org.bitseal.pow;

import java.math.BigInteger;

import org.bitseal.crypt.SHA512;
import org.bitseal.util.ByteUtils;

import android.util.Log;

/**
 * Offers various methods relating to Proof of Work calculations.<br><br>
 * 
 * Updated for version 3 of the Bitmessage protocol.<br><br> 
 * See: https://bitmessage.org/wiki/Protocol_specification_v3
 * 
 * @author Jonathan Coe
 */
public class POWProcessorVersion3
{
	private static final String TAG = "POW_PROCESSOR_VERSION_3";
	
	/** The maximum time in seconds that will be allowed for the app to attempt to complete a POW calculation */
	private static final long MAX_TIME_ALLOWED = 1800;
	
//	/**
//	 * For testing, use this version of the doPOW method to avoid waiting for POW
//	 * to be calculated.
//	 * 
//	 * @param payload - A byte[] containing the payload to do the POW for
//	 * @param nonceTrialsPerByte - The nonceTrialsPerByte value to use
//	 * @param extraBytes - The extraBytes value to use
//	 * 
//	 * @return A random long that can act as a placeholder for a POW nonce
//	 */
//	public long doPOW(byte[] payload, long nonceTrialsPerByte, long extraBytes) 
//	{	
//		SecureRandom sr = new SecureRandom();
//		byte[] POWNonce = new byte[8];
//		sr.nextBytes(POWNonce);
//		return ByteUtils.bytesToLong(POWNonce);
//	}
	
	/**
	 * Does the POW for the given payload.<br />
	 * <b>WARNING: Takes a long time!!!</b>
	 * 
	 * @param payload - A byte[] containing the payload to do the POW for.
	 * @param nonceTrialsPerByte - The nonceTrialsPerByte value to use
	 * @param extraBytes - The extraBytes value to use
	 * @param timeToLive - The 'time to live' value to use
	 * 
	 * @return A long containing the calculated POW nonce. 
	 */
	public long doPOW(byte[] payload, long nonceTrialsPerByte, long extraBytes, long timeToLive) 
	{
		Log.d(TAG, "Doing POW calculations for a payload " + payload.length + " bytes in length.\n" +
				"Nonce trials per byte: " + nonceTrialsPerByte + "\n" +
				"Extra bytes          : " + extraBytes + "\n" +
				"Time to live         : " + timeToLive);
		
		POWCalculator pow = new POWCalculator();
		pow.setTarget(calculatePOWTarget(payload.length, nonceTrialsPerByte, extraBytes, timeToLive));
		pow.setInitialHash(SHA512.sha512(payload));
		pow.setTargetLoad(1);
		
		return pow.execute(MAX_TIME_ALLOWED);
	}
	
	/**
	 * Checks if the proof of work done for the given data is sufficient.
	 * 
	 * @param payload - A byte[] containing the payload.
	 * @param nonce - A long containing the POW nonce.
	 * @param nonceTrialsPerByte - The nonceTrialsPerByte value to use
	 * @param extraBytes - The extraBytes value to use
	 * @param timeToLive - The 'time to live' value to use
	 * 
	 * @return A boolean value indicating whether or not the POW is suffcient. 
	 */
	public boolean checkPOW(byte[] payload, long nonce, long nonceTrialsPerByte, long extraBytes, long timeToLive) 
	{		
		byte[] initialHash = SHA512.sha512(payload);
		byte[] hash = SHA512.sha512(SHA512.sha512(ByteUtils.longToBytes(nonce), initialHash));
		
		long value = ByteUtils.bytesToLong(hash);
		long target = calculatePOWTarget(payload.length, nonceTrialsPerByte, extraBytes, timeToLive);
		
		return value >= 0 && target >= value;
	}
	
	/**
	 * Returns the POW target for a payload of the given length.
	 * 
	 * @param length - The message length.
	 * @param nonceTrialsPerByte - The nonceTrialsPerByte value to use
	 * @param extraBytes - The extraBytes value to use
	 * @param timeToLive - The 'time to live' value to use
	 * 
	 * @return An int representing the POW target for a message with the given length.
	 */
	private long calculatePOWTarget(int length, long nonceTrialsPerByte, long extraBytes, long timeToLive)
	{
		BigInteger powTarget = BigInteger.valueOf(2);
		powTarget = powTarget.pow(64);
		
		BigInteger lengthValue = BigInteger.valueOf(length + extraBytes);
		
		long tempTimeValue = length + extraBytes;
		tempTimeValue = tempTimeValue * timeToLive;
		BigInteger timeValue = BigInteger.valueOf(tempTimeValue);
		
		BigInteger timeTarget = BigInteger.valueOf(2);
		timeTarget = timeTarget.pow(16);
		timeValue = timeValue.divide(timeTarget);
		
		BigInteger divisorValue = lengthValue.add(timeValue);
		divisorValue = divisorValue.multiply(BigInteger.valueOf(nonceTrialsPerByte));
		
		powTarget = powTarget.divide(divisorValue);
		
		// Note that we are dividing through at least 8, so that the value is
		// smaller than 2^61 and fits perfectly into a long.
		return powTarget.longValue();
	}
}