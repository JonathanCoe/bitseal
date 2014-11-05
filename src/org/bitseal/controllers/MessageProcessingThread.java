package org.bitseal.controllers;

import org.bitseal.core.App;
import org.bitseal.services.NotificationsService;

import android.content.Context;
import android.content.Intent;
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
    	            int totalNewMessages = 0;
    	            int newMessagesReceived = controller.processIncomingMessages();
    	            while (newMessagesReceived > 0)
    	            {
    	            	totalNewMessages = totalNewMessages + newMessagesReceived;
    	            	newMessagesReceived = controller.processIncomingMessages();
    	            }
    	            
    				if (totalNewMessages > 0)
    				{
    					// Attempt to send any pending acknowledgments
    					controller.sendAcknowledgments();
    					
    					// Display a notification for the new message(s)
    					Context appContext = App.getContext();
    					Intent intent = new Intent(appContext, NotificationsService.class);
    				    intent.putExtra(NotificationsService.EXTRA_DISPLAY_NEW_MESSAGES_NOTIFICATION, totalNewMessages);
    				    appContext.startService(intent);
    				}
    	            
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