package org.bitseal.pow;

import java.math.BigInteger;

import org.bitseal.crypt.SHA512;
import org.bitseal.util.ByteUtils;

import android.util.Log;

/**
 * Offers various methods relating to Proof of Work calculations. 
 * 
 * @author Jonathan Coe
 */
public class POWProcessor
{
	/** 
	 * The network standard 'nonce trials per byte' value. For an explanation of this, see: <br><br>
	 * https://bitmessage.org/wiki/Proof_of_work 
     */
	public static final long NETWORK_NONCE_TRIALS_PER_BYTE = 320;
	
	/** 
	 * The network standard 'extra bytes' value. For an explanation of this, see: <br><br>
	 * https://bitmessage.org/wiki/Proof_of_work 
     */
	public static final long NETWORK_EXTRA_BYTES = 14000;
	
	/** 
	 * The maximum time in seconds that will be allowed for the app to attempt to complete a POW calculation
	 */
	private static final long MAX_TIME_ALLOWED = 3600;
	
	private static final String TAG = "POW_PROCESSOR";
	
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
	 * Does the proof of work for a given payload.<br><br>
	 * 
	 * <b>WARNING: Takes a long time!!!</b>
	 * 
	 * @param payload - A byte[] containing the payload to do the POW for
	 * @param nonceTrialsPerByte - The nonceTrialsPerByte value to use
	 * @param extraBytes - The extraBytes value to use
	 * 
	 * @return A long containing the calculated POW nonce. 
	 */
	public long doPOW(byte[] payload, long nonceTrialsPerByte, long extraBytes) 
	{
		Log.d(TAG, "Doing POW calculations for a payload " + payload.length + " bytes in length.\n" +
				"Nonce trials per byte: " + nonceTrialsPerByte + "\n" +
				"Extra bytes: " + extraBytes);
		
		POWCalculator pow = new POWCalculator();
		pow.setTarget(getPOWTarget(payload.length, nonceTrialsPerByte, extraBytes));
		pow.setInitialHash(SHA512.sha512(payload));
		pow.setTargetLoad(1);
		
		long powResult = pow.execute(MAX_TIME_ALLOWED);
		if (powResult == 0) 
		{
			// POW could not be completed within the time allowed. See POWCalculator and POWWorker classes for more on this.
			throw new RuntimeException("Failed to find a valid POW nonce in the time allowed!");
		}
		else
		{
			return powResult;
		}
	}
	
	/**
	 * Checks if the proof of work done for the given data is sufficient.
	 * 
	 * @param payload - A byte[] containing the payload.
	 * @param nonce - A long containing the POW nonce.
	 * @param nonceTrialsPerByte - The nonceTrialsPerByte value to use
	 * @param extraBytes - The extraBytes value to use
	 * 
	 * @return A boolean value indicating whether or not the POW is suffcient. 
	 */
	public boolean checkPOW(byte[] payload, long nonce, long nonceTrialsPerByte, long extraBytes)
	{		
		byte[] initialHash = SHA512.sha512(payload);
		byte[] dataToHash = ByteUtils.concatenateByteArrays(ByteUtils.longToBytes(nonce), initialHash);
		byte[] doubleHash = SHA512.doubleDigest(dataToHash);
		
		long value = ByteUtils.bytesToLong(doubleHash);
		long target = getPOWTarget(payload.length, nonceTrialsPerByte, extraBytes);
		
		return value >= 0 && target >= value;
	}
	
	/**
	 * Returns the POW target for a payload with the given length.
	 * 
	 * @param length - The message length.
	 * @param nonceTrialsPerByte - The nonceTrialsPerByte value to use
	 * @param extraBytes - The extraBytes value to use
	 * 
	 * @return An int representing the POW target for a message with the given length.
	 */
	private long getPOWTarget(int length, long nonceTrialsPerByte, long extraBytes)
	{
		BigInteger powTarget = BigInteger.valueOf(2);
		powTarget = powTarget.pow(64);
		powTarget = powTarget.divide(BigInteger.valueOf((length + extraBytes + 8) * nonceTrialsPerByte));
		return powTarget.longValue();
	}
}