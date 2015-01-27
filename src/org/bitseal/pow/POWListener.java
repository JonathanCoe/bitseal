package org.bitseal.pow;

/**
 * Interface to notify objects if a POW calculation is finished.
 * 
 * @author Sebastian Schmidt
 */
public interface POWListener 
{
	/**
	 * Informs the listener that the POW was finished by the given thread.
	 * 
	 * @param powWorker - The thread that is finished.
	 */
	void powFinished(POWWorker powWorker);
}