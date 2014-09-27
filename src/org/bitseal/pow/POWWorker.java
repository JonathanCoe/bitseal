package org.bitseal.pow;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

	/**
	 * The time period in milliseconds to check if the pow calculation should be
	 * aborted.
	 */
	private static final int ROUND_TIME = 100;

	/** The collision quality that should be achieved. */
	private long target;

	/** The POW nonce. */
	private volatile long nonce;

	/** The initial hash value. */
	private byte[] initialHash;

	/** True if the calculation is running. */
	private volatile boolean running;

	/** A stop request can be made by setting this to true. */
	private volatile boolean stop;

	/** The listener to inform if we found the result. */
	private POWListener listener;

	/** The system load that should be created by this worker. */
	private float targetLoad;

	/** The increment that should be used for finding the next nonce. */
	private long increment;
	
	private long maxTime; 
	
	private MessageDigest sha512;
	
	private long startTime;
	
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
	 * @param targetLoad - A float representing the system load that should be created by this worker.
	 * @param maxTime - A long representing the maximum amount of time in seconds to be allowed for a POW calculation
	 */
	public POWWorker(long target, long startNonce, long increment, byte[] initialHash, POWListener listener,
			float targetLoad, long maxTime) 
	{
		if (listener == null) 
		{
			throw new NullPointerException("The listener field in POWWorker must not be null.");
		}

		this.target = target;
		this.nonce = startNonce;
		this.initialHash = initialHash;
		this.listener = listener;
		this.targetLoad = targetLoad;
		this.increment = increment;
		this.maxTime = maxTime;
		
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

	/**
	 * Calculates the POW.
	 */
	@Override
	public void run() 
	{
		running = true;
		
		startTime = System.currentTimeMillis();

		int iterations = 100 * ROUND_TIME;
		long sleepTime = (long) (ROUND_TIME * (1 - targetLoad));
		long result = Long.MAX_VALUE;
		long nonce = this.nonce;

		float topLoad = targetLoad * 1.1f;
		float bottomLoad = targetLoad * 0.9f;

		while (!stop) 
		{
			long ls = System.nanoTime();
			byte[] hash;

			for (int i = 0; i < iterations; i++) 
			{
				sha512.reset();
				sha512.update(ByteUtils.longToBytes(nonce));
				hash = sha512.digest(initialHash);
				sha512.reset();
				hash = sha512.digest(hash);
				
				result = ByteUtils.bytesToLong(hash);

				if (result <= target && result >= 0)
				{
					Log.i(TAG, "Found a valid nonce!");
					stop();
					this.nonce = nonce;
					POWSuccessful = true;
					listener.powFinished(this);
					break;
				}
				
				if ((startTime + (maxTime * 1000)) < System.currentTimeMillis())
				{
					// Throwing exceptions in this instance has proven to be very problematic, with the exceptions
					// causing the worker threads to crash instead of propogating up the call hierarchy correctly. 
					// Therefore we return 0 to convey that the POW calculations were not successful within the time allowed.
					stop();
					this.nonce = 0;
					POWSuccessful = false;
					listener.powFinished(this);
					break;
				}

				nonce += increment;
			}

			long lh = System.nanoTime();

			if (sleepTime > 0)
			{
				try 
				{
					Thread.sleep(sleepTime);
				} 
				catch (InterruptedException e) 
				{
					throw new RuntimeException("InterruptedException occurred in POWWorker.run()", e);
				}
			}

			long lf = System.nanoTime();

			float load = ((float) (lh - ls) / (float) (lf - ls));
			//System.out.println("Load: " + load);

			if (load > topLoad) 
			{
				iterations -= iterations >> 8;
				System.out.println("iterations: " + iterations);
			} 
			else if (load < bottomLoad) 
			{
				iterations += iterations >> 8;
				System.out.println("iterations: " + iterations);
			}
		}

		running = false;
	}
}