package org.bitseal.pow;

import java.text.NumberFormat;

import org.bitseal.util.TimeUtils;

import android.util.Log;

/**
 * Does the POW calculation, uses multiple threads.
 * 
 * @author Sebastian Schmidt, modified by Jonathan Coe
 */
public class POWCalculator implements POWListener 
{
	/** The number of threads to use per CPU. */
	private static final int THREADS_PER_CPU = 1;

	/** The target collision quality. */
	private long target;

	/** The hash of the message. */
	private byte[] initialHash;
	
	/** The worker that found a valid nonce. */
	private POWWorker finishedWorker;
	
	/** The number of double SHA-512 hashes calculated. */
	private int doubleHashesCalculated = 0;
	
	private static final String TAG = "POW_CALCULATOR";
	
	public void setTarget(long newTarget)
	{
		target = newTarget;
	}
	
	public void setInitialHash(byte[] newInitialHash)
	{
		initialHash = newInitialHash;
	}

	/**
	 * Do the Proof of Work calculations.<br><br>
	 * <b>WARNING: This can take a long time.</b><br><br>
	 * 
	 * <b>Note: If POW is not completed within the time allowed, this method will return 0.</b>
	 * 
	 * @return A long containing a nonce that fulfils the collision quality condition.
	 */
	public synchronized long execute() 
	{
		// Create a new worker thread for each CPU core
		POWWorker[] workers = new POWWorker[Runtime.getRuntime().availableProcessors() * THREADS_PER_CPU];
		
		long startTime = System.currentTimeMillis();
		
		// Start the worker threads
		for (int i = 0; i < workers.length; i++) 
		{
			workers[i] = new POWWorker(target, i, workers.length, initialHash, this);
			new Thread(workers[i], "POW Worker No. " + i).start();
		}
		
		// Wait for POW to be completed
		try 
		{
			wait();
		}
		catch (InterruptedException e) 
		{
			throw new RuntimeException("InterruptedException occurred in POWCalculator.execute()", e);
		}
		
		// Once POW has completed successfully or been interrupted, stop any worker threads that are still running
		for (POWWorker w : workers) 
		{
			w.stop();
			
			doubleHashesCalculated = doubleHashesCalculated + w.getDoubleHashesCalculated();
		}
		
		// Calculate the time statistics for this POW session
		long endTime = System.currentTimeMillis();
		long totalTime = (endTime - startTime) / 1000;
		Log.d(TAG, "Double hashes calculated : " + NumberFormat.getIntegerInstance().format(doubleHashesCalculated));
		Log.d(TAG, "Time taken               : " + TimeUtils.getTimeMessage(totalTime));
		Log.d(TAG, "Hash rate                : " + NumberFormat.getIntegerInstance().format((doubleHashesCalculated / totalTime)) + " double-hashes per second");
		
		return finishedWorker.getNonce();
	}

	@Override
	public synchronized void powFinished(POWWorker powWorker) 
	{
		if (finishedWorker == null)
		{
			finishedWorker = powWorker;
		}

		notifyAll();
	}
}