package org.bitseal.controllers;

import org.bitseal.R;
import org.bitseal.core.App;
import org.bitseal.core.OutgoingGetpubkeyProcessor;
import org.bitseal.core.OutgoingMessageProcessor;
import org.bitseal.core.PubkeyProcessor;
import org.bitseal.data.Message;
import org.bitseal.data.Payload;
import org.bitseal.data.Pubkey;
import org.bitseal.database.PayloadProvider;
import org.bitseal.network.NetworkHelper;
import org.bitseal.network.ServerCommunicator;
import org.bitseal.services.MessageStatusHandler;

import android.util.Log;

/**
 * This class controls the operations necessary to send a message.
 * 
 * @author Jonathan Coe
 */
public class SendMessageController
{
	private static final String TAG = "SEND_MESSAGE_CONTROLLER";
	
	/**
	 * Attempts to retrieve a Pubkey for a given Bitmessage address. <br><br>
	 * 
	 * @param message - The Message object which we attempting to send
	 * @param getpubkeyPayload - A Payload containing a getpubkey object which has 
	 * been created in order to retrieve this pubkey. 
	 * @param timeToLive - The 'time to live' value (in seconds) to be used in creating 
	 * a getpubkey object to retrieve the pubkey
	 * 
	 * @return An Object, which will either be a Pubkey (if the pubkey was successfully
	 * retrieved) or a Payload containing a getpubkey (if we could not retrieve the pubkey
	 * and had to send a getpubkey request). 
	 */
	public Object retrievePubkey(Message message, Payload getpubkeyPayload, long timeToLive)
	{	
		try
		{
			Pubkey toPubkey = new PubkeyProcessor().retrievePubkeyForMessage(message);
			
			// If we successfully retrieved the pubkey, delete any getpubkey we may have created
			if (getpubkeyPayload != null)
			{
				PayloadProvider payProv = PayloadProvider.get(App.getContext());
				payProv.deletePayload(getpubkeyPayload);
			}
			
			return toPubkey;
		}
		catch (RuntimeException e) // If we were unable to retrieve the pubkey
		{	
			Log.i(TAG, "Failed to retrieve the requested pubkey. The exception message was: " + e.getMessage());
			
			// Check whether we already have a getpubkey Payload for retrieving this pubkey
			if (getpubkeyPayload != null)
			{
				// Check whether the getpubkey object has already been successfully disseminated 
				if (getpubkeyPayload.getTime() != 0) // If the payload has been disseminated already
				{
					Log.i(TAG, "Skipping dissemination of getpubkey payload because it was successfully disseminated and its time to live has not yet expired");
					
					// Update the status of this message displayed in the UI
					String messageStatus = App.getContext().getString(R.string.message_status_waiting_for_pubkey);
					MessageStatusHandler.updateMessageStatus(message, messageStatus);
					
					return getpubkeyPayload; // Do not disseminate the payload again - there is no need to
				}
				
				// Check whether an Internet connection is available. 
				if (NetworkHelper.checkInternetAvailability() == true)
				{
					// Disseminate the getpubkey Payload that we created earlier
					MessageStatusHandler.updateMessageStatus(message, App.getContext().getString(R.string.message_status_requesting_pubkey));
					new OutgoingGetpubkeyProcessor().disseminateGetpubkeyRequest(getpubkeyPayload);
				}
				else
				{
					MessageStatusHandler.updateMessageStatus(message, App.getContext().getString(R.string.message_status_waiting_for_connection));
				}
				return getpubkeyPayload;
			}
			else
			{
				// Create a new getpubkey Payload and disseminate it
				return new OutgoingGetpubkeyProcessor().constructAndDisseminateGetpubkeyRequst(message, timeToLive);
			}
		}
	}
	
	/**
	 * Processes a message to be sent by me, returning a msg payload that is ready
	 * to be sent over the network. 
	 * 
	 * @param message - A Message object containing the message data to be sent
	 * @param toPubkey - A Pubkey object containing the public keys of the address
	 * that the message is to be sent to
	 * @param doPOW - A boolean indicating whether or not proof of work calculations
	 * should be done for the msg created
	 * during this process.
	 * @param timeToLive - The 'time to live' value (in seconds) to be used in sending
	 * this message
	 * 
	 * @return A Payload object containing the msg payload for this message
	 */
	public Payload processOutgoingMessage(Message message, Pubkey toPubkey, boolean doPOW, long timeToLive)
	{
		return new OutgoingMessageProcessor().processOutgoingMessage(message, toPubkey, doPOW, timeToLive);
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
		if (POWDone)
		{
			return new ServerCommunicator().disseminateMsg(msgPayload.getPayload());
		}
		else
		{
			return new ServerCommunicator().disseminateMsgNoPOW(msgPayload.getPayload(), toPubkey.getNonceTrialsPerByte(), toPubkey.getExtraBytes());
		}
	}
}