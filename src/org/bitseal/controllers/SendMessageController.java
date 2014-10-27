package org.bitseal.controllers;

import org.bitseal.core.App;
import org.bitseal.core.OutgoingGetpubkeyProcessor;
import org.bitseal.core.OutgoingMessageProcessor;
import org.bitseal.core.PubkeyProcessor;
import org.bitseal.data.Message;
import org.bitseal.data.Payload;
import org.bitseal.data.Pubkey;
import org.bitseal.database.PayloadProvider;
import org.bitseal.network.ServerCommunicator;

import android.util.Log;


/**
 * This class controls the operations necessary to send a message.
 * 
 * @author Jonathan Coe
 */

public class SendMessageController
{
	/** This is the maximum age of an object (in seconds) that PyBitmessage will accept. */
	private static final int PYBITMESSAGE_NEW_OBJECT_ACCEPTANCE_PERIOD = 216000;
	
	private static final String TAG = "SEND_MESSAGE_CONTROLLER";
	
	/**
	 * Attempts to retrieve a Pubkey for a given Bitmessage address. <br><br>
	 * 
	 * @param toAddress - The Bitmessage address for which we which 
	 * to retrieve the pubkey
	 * @param getpubkeyPayload - A Payload containing a getpubkey object which has 
	 * been created in order to retrieve this pubkey. 
	 * @param timeToLive - The 'time to live' value (in seconds) to be used in creating 
	 * a getpubkey object to retrieve the pubkey
	 * 
	 * @return An Object, which will either be a Pubkey (if the pubkey was successfully
	 * retrieved) or a Payload (if we could not retrieve the pubkey and had to send a 
	 * getpubkey request). 
	 */
	public Object retrieveToPubkey(String toAddress, Payload getpubkeyPayload, long timeToLive)
	{
		PubkeyProcessor pubProc = new PubkeyProcessor();
		Pubkey toPubkey = null;
		try
		{
			toPubkey = pubProc.retrievePubkeyByAddressString(toAddress);
		}
		catch (RuntimeException e) // If we were unable to retrieve the pubkey
		{
			Log.i(TAG, "Failed to retrieve the requested pubkey. The exception message was: " + e.getMessage());
			OutgoingGetpubkeyProcessor getProc = new OutgoingGetpubkeyProcessor();
			
			if (getpubkeyPayload != null)
			{	
				// Check whether the getpubkey object has been successfully disseminated within the 'new object
				// acceptance period' defined by PyBitmessage. If so then there is no need to disseminate it again, 
				// as the getpubkey request will still be circulating around the Bitmessage network. 
				long lastDisseminationTime = getpubkeyPayload.getTime();
				if (lastDisseminationTime != 0) // If the payload has been disseminated already
				{
					long currentTime = System.currentTimeMillis() / 1000;
					long timeSinceLastDissemination = currentTime - lastDisseminationTime;
					if (timeSinceLastDissemination < PYBITMESSAGE_NEW_OBJECT_ACCEPTANCE_PERIOD)
					{
						Log.i(TAG, "Skipping dissemination of getpubkey payload because it was successfully disseminated " + timeSinceLastDissemination + " seconds ago");
						return getpubkeyPayload; // Do not disseminate the payload again - there is no need to
					}
				}
				
				// Disseminate the getpubkey Payload that we created earlier
				getProc.disseminateGetpubkeyRequest(getpubkeyPayload);
				return getpubkeyPayload;
			}
			else
			{
				// Create a new getpubkey Payload and disseminate it
				Payload newGetpubkeyPayload = getProc.constructAndDisseminateGetpubkeyRequst(toAddress, timeToLive);
				return newGetpubkeyPayload;
			}
		}
		// If we successfully retrieved the pubkey
		if (getpubkeyPayload != null)
		{
			PayloadProvider payProv = PayloadProvider.get(App.getContext());
			payProv.deletePayload(getpubkeyPayload);
		}
		return toPubkey;
	}
	
	/**
	 * Processes a message to be sent by me, returning a msg payload that
	 * is ready to be sent over the network. 
	 * 
	 * @param messageToSend - A Message object containing the message data to be sent
	 * @param toPubkey - A Pubkey object containing the public keys of the address that the
	 * message is to be sent to
	 * @param doPOW - A boolean indicating whether or not proof of 
	 * work calculations should be done for the msg created
	 * during this process.
	 * @param timeToLive - The 'time to live' value (in seconds) to be used in sending this message
	 * 
	 * @return A Payload object containing the msg payload for this message
	 */
	public Payload processOutgoingMessage (Message messageToSend, Pubkey toPubkey, boolean doPOW, long timeToLive)
	{
		OutgoingMessageProcessor outMsgProc = new OutgoingMessageProcessor();
		Payload msgPayload = outMsgProc.processOutgoingMessage(messageToSend, toPubkey, doPOW, timeToLive);
		
		return msgPayload;
	}
	
	/**
	 * Attempts to disseminate a msg to the rest of the Bitmessage network. 
	 * 
	 * @param msgPayload - A Payload object containing the msg payload to be sent
	 * @param toPubkey - A Pubkey object containing the public keys of the message 
	 * destination address
	 * @param POWDone - A boolean indicating whether or not proof of work has
	 * been done for this msg. If not, a server will be expected to do the 
	 * proof of work.
	 * 
	 * @return A boolean indicating whether or not the msg was successfully sent
	 */
	public boolean disseminateMessage(Payload msgPayload, Pubkey toPubkey, boolean POWDone)
	{
		ServerCommunicator servCom = new ServerCommunicator();
		boolean disseminationSuccessful;
		
		if (POWDone == true)
		{
			disseminationSuccessful = servCom.disseminateMsg(msgPayload.getPayload());
		}
		else
		{
			disseminationSuccessful = servCom.disseminateMsgNoPOW(msgPayload.getPayload(), toPubkey.getNonceTrialsPerByte(), toPubkey.getExtraBytes());
		}
		
		return disseminationSuccessful;
	}
}