package org.bitseal.core;

import java.util.ArrayList;

import org.bitseal.data.Payload;
import org.bitseal.database.PayloadProvider;
import org.bitseal.database.PayloadsTable;
import org.bitseal.network.ServerCommunicator;
import org.bitseal.util.ArrayCopier;

import android.util.Log;

/**
 * This class handles all the processing that needs to be
 * done for handling acknowledgements. 
 * 
 * @author Jonathan Coe
 */
public class AckProcessor
{	
	private static final String TAG = "ACK_PROCESSOR";
	
	/**
	 * This method will look in the application database's "Payloads" table
	 * for any acknowledgement payloads that 'belong to me', and are thus 
	 * acknowledgements that are waiting to be sent for messages that the user
	 * of the app has received. 
	 * 
	 * @return - A boolean indicating whether or not all outstanding acknowledgements
	 * were successfully processed 
	 */
	public boolean sendAcknowledgments()
	{
		PayloadProvider payProv = PayloadProvider.get(App.getContext());
		ArrayList<Payload> allAcks = payProv.searchPayloads(PayloadsTable.COLUMN_ACK, String.valueOf(1)); // 1 stands for true in the database
		if (allAcks.size() > 0)
		{
			ArrayList<Payload> acksToSend = new ArrayList<Payload>();
			
			for (Payload p : allAcks)
			{
				if (p.belongsToMe() == false)
				{
					acksToSend.add(p);
				}
			}
			
			Log.i(TAG, "Number of acknowledgment messages that I need to send: " + acksToSend.size());
			
			// Attempt to send each ack payload retrieved from the database. If any of these payloads
			// are not processed successfully, that failure is recorded in 'numberOfAcksNotProcessedSuccessfully'
			int numberOfAcksNotProcessedSuccessfully = 0;
			for (Payload p : acksToSend)
			{
				boolean ackProcessedSuccessfully = false;
				try
				{
					ackProcessedSuccessfully = checkAndSendAcknowledgment(p);
				}
    	        catch (Exception e)
    	        {
    	        	Log.e(TAG, "While running AckProcessor.checkAndSendAcknowledgment(), and Exception was thrown. \n" +
    						"The exception message was: " + e.getMessage());
    	        }
				
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
	 * This method checks an acknowledgement message and, if it is found to be valid, 
	 * attempts to disseminate it to the Bitmessage network. 
	 * 
	 * @param p - A Payload object containing the acknowledgement msg to be sent
	 * 
	 * @return - A boolean indicating whether or not the acknowledgement was successfully
	 * processed 
	 */
	private boolean checkAndSendAcknowledgment(Payload p)
	{
		byte[] fullAckMessage = p.getPayload();
		
		// Bitmessage acknowledgements are full Message objects, including the header data (magic bytes, command, length, checksum). 
		// We only need the payload, so we will skip over the first 24 bytes. 
		byte[] ackObjectBytes = ArrayCopier.copyOfRange(fullAckMessage, 24, fullAckMessage.length);
				
		// Check whether this ack is a valid Bitmessage Object
		new ObjectProcessor().parseObject(ackObjectBytes);
		
		// Attempt to send the acknowledgement. 
		ServerCommunicator servCom = new ServerCommunicator();
		boolean disseminationSuccessful = servCom.disseminateMsg(ackObjectBytes);
		
		// If it is sent successfully, delete it from the database. If not, it will be kept
		// and we will try to send it again later. 
		if (disseminationSuccessful == true)
		{
			PayloadProvider payProv = PayloadProvider.get(App.getContext());
			payProv.deletePayload(p);
			return true;
		}
		else
		{
			return false;
		}
	}
}