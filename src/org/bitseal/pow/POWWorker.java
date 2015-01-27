package org.bitseal.pow;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;

import org.bitseal.util.ByteUtils;

import android.util.Log;

/**
 * A worker class to parallelize POW calculation.
 * 
 * @author Sebastian Schmidt, modified by Jonathan Coe
 */
public class POWWorker implements Runnable 
{
	protected boolean POWSuccessful;
	
	/** The collision quality that should be achieved. */
	private long target;

	/** The POW nonce. */
	private volatile long nonce;

	/** The initial hash value. */
	private byte[] initialHash;
	
	/** The increment that should be used for finding the next nonce. */
	private long increment;

	/** True if the calculation is running. */
	private volatile boolean running;

	/** A stop request can be made by setting this to true. */
	private volatile boolean stop;

	/** The listener to inform if we found the result. */
	private POWListener listener;
	
	private MessageDigest sha512;
		
	/** The number of double SHA-512 hashes calculated by this worker so far. */
	private int doubleHashesCalculated = 0;
	
	private static final String TAG = "POW_WORKER";

	/**
	 * Creates a new POWWorker.
	 * 
	 * @param target - A long representing the target collision quality.
	 * @param startNonce - A long representing the nonce to start with.
	 * @param increment - A long representing the step size. A POW worker calculates with: startNonce, 
	 * startNonce + increment, startNonce + 2 * increment.
	 * @param initialHash - A byte[] containing the hash of the message.
	 * @param listener - The POWListener object to inform if a result was found.
	 */
	public POWWorker(long target, long startNonce, long increment, byte[] initialHash, POWListener listener) 
	{
		if (listener == null) 
		{
			throw new NullPointerException("The listener field in POWWorker must not be null.");
		}

		this.target = target;
		this.nonce = startNonce;
		this.increment = increment;
		this.initialHash = initialHash;
		this.listener = listener;
		
		try 
		{
			sha512 = MessageDigest.getInstance("SHA-512");
		} 
		catch (NoSuchAlgorithmException e) 
		{
			throw new RuntimeException("NoSuchAlgorithmException occurred in POWWorker constructor", e);
		}
	}

	/**
	 * Returns true if the worker is actually calculating the POW.
	 * 
	 * @return True if the worker is actually calculating the POW.
	 */
	public boolean isRunning() 
	{
		return running;
	}

	/**
	 * Request the worker to stop.
	 */
	public void stop() 
	{
		stop = true;
	}

	/**
	 * Returns the current nonce. Note that it can be wrong if isRunning()
	 * returns true or no success was reported.
	 * 
	 * @return The current nonce.
	 */
	public long getNonce()
	{
		return nonce;
	}
	
	public boolean getSuccessResult()
	{
		return POWSuccessful;
	}
	
	public int getDoubleHashesCalculated()
	{
		return doubleHashesCalculated;
	}

	/**
	 * Calculates the POW.
	 */
	@Override
	public void run() 
	{
		running = true;
		
		long nonce = this.nonce;
		
		while (!stop)
		{
			// Calculate the double SHA512 hash of the current nonce concatenated with the payload (initial) hash
			sha512.reset();
			sha512.update(ByteUtils.longToBytes(nonce));
			byte[] hash = sha512.digest(initialHash);
			sha512.reset();
			hash = sha512.digest(hash);
			
			doubleHashesCalculated ++;
			
			// Get the resulting hash as a long
			long result = ByteUtils.bytesToLong(hash);
			
			// Check whether the current nonce gives a result that meets the POW target
			if (result <= target && result >= 0)
			{
				Log.d(TAG, "Found a valid nonce!     : " + NumberFormat.getIntegerInstance().format(nonce));
				stop();
				this.nonce = nonce;
				POWSuccessful = true;
				listener.powFinished(this);
				break;
			}
			// Increment the POW nonce
			else
			{
				nonce += increment;
			}
		}

		running = false;
	}
}