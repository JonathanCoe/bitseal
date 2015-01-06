package org.bitseal.controllers;

import org.bitseal.core.App;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * A Singleton class that provides a thread to process newly
 * downloaded messages. 
 * 
 * @author JonathanCoe
 */
public class MessageProcessingThread 
{
	private Thread processingThread;
	
	/** A key used to store the time of the last successful 'check for new msgs' server request */
	private static final String LAST_MSG_CHECK_TIME = "lastMsgCheckTime";
	
	/** Stores the Unix timestamp of the last msg payload we processed. This can be used to tell us how far behind the network we are. */
	private static final String LAST_PROCESSED_MSG_TIME = "lastProcessedMsgTime";
	
	private static final String TAG = "MessageProcessingThread";
	
	private static class Holder 
    {
        static MessageProcessingThread INSTANCE = new MessageProcessingThread();
    }
	
	/**
	 * Returns a singleton instance of the MessageProcessingThread. This ensures that
	 * only one instance of the thread will ever be exist at once. 
	 */
	protected static MessageProcessingThread getInstance()
    {
        return Holder.INSTANCE;
    }
    
    /**
     * Starts the thread for processing new messages, in such a way that the 
     * thread will only be started if it is not already running. 
     */
    protected void startThread()
    {
    	if (processingThread.getState() == Thread.State.NEW) // The thread has not been started yet
    	{
    		processingThread.start();
    	}
    	else if (processingThread.getState() == Thread.State.TERMINATED) // The thread has run to completion
    	{
    		setNewThreadInstance();
    		MessageProcessingThread.getInstance().startThread();
    	}
    	else
    	{
    		Log.d(TAG, "MessageProcessingThread.startThread() was called, but the thread is already running.");
    	}
    }
    
    /**
     * Creates a new MessageProcessingThread instance and sets the
     * static INSTANCE variable to point to it. 
     */
    private void setNewThreadInstance()
    {
    	Holder.INSTANCE = new MessageProcessingThread();
    }
    
    private MessageProcessingThread()
    {
    	// Create a thread for processing the messages we download
    	processingThread = new Thread(new Runnable()
    	{
    		@Override
    	    public void run()
    	    {
    	        try
    	        {
    				Log.i(TAG, "Starting message processing thread.");
    				
    				CheckForMessagesController controller = new CheckForMessagesController();
    	            int newMessagesProcessed = controller.processIncomingMessages();
    	            while (newMessagesProcessed > 0)
    	            {
    	            	newMessagesProcessed = controller.processIncomingMessages();
    	            }
    	            
    	            // Once we have processed the last available new msg, update the 'last processed msg' time to equal the
    	            // 'last msg check' time
    	            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getContext());
    	            SharedPreferences.Editor editor = prefs.edit();
    	            long lastMsgCheckTime = prefs.getLong(LAST_MSG_CHECK_TIME, 0);	
    			    editor.putLong(LAST_PROCESSED_MSG_TIME, lastMsgCheckTime);
    			    editor.commit();
    				Log.i(TAG, "Updated the 'last processed msg time' value stored in SharedPreferences to " + lastMsgCheckTime);
    	            
					// Attempt to send any pending acknowledgements
					controller.sendAcknowledgments();
    	            
    	            Log.i(TAG, "Finishing message processing thread.");
    	        }
    	        catch (Exception e)
    	        {
    	        	Log.e(TAG, "While running MessageProcessingThread(), processingThread.run() threw an Execption. \n" +
    						"The exception message was: " + e.getMessage());
    	        }
    	    }
    	});
    }
}