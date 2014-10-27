package org.bitseal.core;

import java.util.ArrayList;

import org.bitseal.data.Payload;
import org.bitseal.database.PayloadProvider;
import org.bitseal.database.PayloadsTable;
import org.bitseal.network.ServerCommunicator;
import org.bitseal.pow.POWProcessor;
import org.bitseal.util.ArrayCopier;
import org.bitseal.util.ByteUtils;

import android.util.Log;

/**
 * This class handles all the processing that needs to be
 * done for handling acknowledgments. 
 * 
 * @author Jonathan Coe
 */
public class AckProcessor
{	
	private static final long THREE_HOURS_IN_SECONDS = 10800;
	
	/** In Bitmessage protocol version 3, the network standard value for nonce trials per byte is 1000. */
	public static final int NETWORK_NONCE_TRIALS_PER_BYTE = 1000;
	
	/** In Bitmessage protocol version 3, the network standard value for extra bytes is 1000. */
	public static final int NETWORK_EXTRA_BYTES = 1000;
	
	private static final String TAG = "ACK_PROCESSOR";
	
	/**
	 * This method will look in the application database's "Payloads" table
	 * for any acknowledgment payloads that 'belong to me', and are thus 
	 * acknowledgments that are waiting to be sent for messages that the user
	 * of the app has received. 
	 * 
	 * @return - A boolean indicating whether or not all outstanding acknowledgments
	 * were successfully processed 
	 */
	public boolean sendAcknowledgments()
	{
		PayloadProvider payProv = PayloadProvider.get(App.getContext());
		ArrayList<Payload> allAcks = payProv.searchPayloads(PayloadsTable.COLUMN_TYPE, "ack");
		if (allAcks.size() > 0)
		{
			ArrayList<Payload> acksToSend = new ArrayList<Payload>();
			
			for (Payload p : allAcks)
			{
				if (p.belongsToMe() == true)
				{
					acksToSend.add(p);
				}
			}
			
			Log.d(TAG, "Number of acknowledgment messages that I need to send: " + acksToSend.size());
			
			// Attempt to send each ack payload retrieved from the database. If any of these payloads
			// are not processed successfully, that failure is recorded in 'numberOfAcksNotProcessedSuccessfully'
			int numberOfAcksNotProcessedSuccessfully = 0;
			for (Payload p : acksToSend)
			{
				boolean ackProcessedSuccessfully = checkAndSendAcknowledgment(p);
				
				if (ackProcessedSuccessfully == false)
				{
					numberOfAcksNotProcessedSuccessfully = numberOfAcksNotProcessedSuccessfully + 1;
				}
			}
			
			if (numberOfAcksNotProcessedSuccessfully > 0)
			{
				// If we failed to process all acks successfully
				return false; 
			}
			else
			{
				// If we processed all acks successfully
				return true;
			}
		}
		else 
		{
			// If there are no acks to process
			return true;
		}
	}
	
	/**
	 * This method checks an acknowledgment message and, if it is found to be valid, 
	 * attempts to disseminate it to the Bitmessage network. 
	 * 
	 * @param p - A Payload object containing the acknowledgment msg to be sent
	 * 
	 * @return - A boolean indicating whether or not the acknowledgment was successfully
	 * processed 
	 */
	private boolean checkAndSendAcknowledgment(Payload p)
	{
		byte[] fullAckMessage = p.getPayload();
						
		// Bitmessage acknowledgments are full Message objects, including the header data (magic bytes, command, length, checksum). 
		// We only need the payload, so we will skip over the first 24 bytes. 
		byte[] ackPayload = ArrayCopier.copyOfRange(fullAckMessage, 24, fullAckMessage.length);
				
		// Check the proof of work
		long powNonce = ByteUtils.bytesToLong(ArrayCopier.copyOf(ackPayload, 8));
		long expirationTime = ByteUtils.bytesToLong(ArrayCopier.copyOfRange(ackPayload, 8, 16));
		byte[] payloadToCheck = ArrayCopier.copyOfRange(ackPayload, 8, ackPayload.length);
		POWProcessor powProc = new POWProcessor();
		boolean powValid = powProc.checkPOW(payloadToCheck, powNonce, NETWORK_NONCE_TRIALS_PER_BYTE, NETWORK_EXTRA_BYTES, expirationTime);
		PayloadProvider payProv = PayloadProvider.get(App.getContext());
		if (powValid == false)
		{
			Log.e(TAG, "While running AckProcessor.checkAndSendAcknowledgment(), an acknowledgment payload was found " +
					"to have an invalid proof of work nonce. The paylaod will now be deleted from the databse.");
			payProv.deletePayload(p);
			return true;
		}
				
		// Check that the time value is valid (no more than 3 hours in the future)
		long time = ByteUtils.bytesToInt((ArrayCopier.copyOfRange(ackPayload, 8, 12)));
		if (time == 0) // Need to check whether 4 or 8 byte time has been used
		{
			time = ByteUtils.bytesToLong((ArrayCopier.copyOfRange(ackPayload, 8, 16)));
		}
		long currentTime = System.currentTimeMillis() / 1000;
		boolean timeValid = ((currentTime + THREE_HOURS_IN_SECONDS) > time);
		if (timeValid == false)
		{
			Log.e(TAG, "While running AckProcessor.checkAndSendAcknowledgment(), an acknowledgment payload was found " +
					"to have an invalid embedded time value, which was: " + time + ". The paylaod will now be deleted from the database.");
			payProv.deletePayload(p);
			return true;
		}
		
		// Attempt to send the acknowledgment. 
		ServerCommunicator servCom = new ServerCommunicator();
		boolean disseminationSuccessful = servCom.disseminateMsg(ackPayload);
		
		// If it is sent successfully, delete it from the database. If not, it will be kept
		// and we will try to send it again later. 
		if (disseminationSuccessful == true)
		{
			payProv.deletePayload(p);
			return true;
		}
		else
		{
			return false;
		}
	}
}