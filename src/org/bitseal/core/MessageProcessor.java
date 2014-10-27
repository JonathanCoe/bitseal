package org.bitseal.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.bitseal.crypt.SHA512;
import org.bitseal.util.ArrayCopier;
import org.bitseal.util.ByteFormatter;
import org.bitseal.util.ByteUtils;

public class MessageProcessor
{
	/** A magic hexadecimal value used by Bitmessage to identify network packets. See https://bitmessage.org/wiki/Protocol_specification#Message_structure */
	private static final String BITMESSAGE_MAGIC_IDENTIFIER = "E9BEB4D9";
	
	/** Identifies the Message contents. See https://bitmessage.org/wiki/Protocol_specification#Message_structure */
	private static final String BITMESSAGE_OBJECT_COMMAND = "object";
	
	/** Padding for the command. See https://bitmessage.org/wiki/Protocol_specification#Message_structure */
	private static final String BITMESSAGE_OBJECT_COMMAND_PADDING = "000000000000";
	
	/** The character encoding used in the Bitmessage command data */
	private static final String BITMESSAGE_COMMAND_ENCODING = "US-ASCII";
	
	protected byte[] generateObjectHeader (byte[] payload)
	{
		return generateMessageHeader(BITMESSAGE_OBJECT_COMMAND, payload);
	}
	
	/**
	 * Creates the header for a Message to be sent between Bitmessage nodes. For a specification of the header see
	 * https://bitmessage.org/wiki/Protocol_specification#Message_structure
	 * 
	 * @param - command - A String that identifies the Message type
	 * @param - payload - A byte[] containing the Message to construct a header for
	 * 
	 * @return A byte[] containing the Message header
	 */
	private byte[] generateMessageHeader (String command, byte[] payload)
	{
		// Get the byte values of all the data that needs to go in the message header
		byte[] magicBytes = ByteFormatter.hexStringToByteArray(BITMESSAGE_MAGIC_IDENTIFIER);
		byte[] commandBytes = null;
		try
		{
			commandBytes = command.getBytes(BITMESSAGE_COMMAND_ENCODING);
			byte[] commandPadding = ByteFormatter.hexStringToByteArray(BITMESSAGE_OBJECT_COMMAND_PADDING);
			commandBytes = ByteUtils.concatenateByteArrays(commandBytes, commandPadding);
		} 
		catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException("UnsupportedEncodingException occurred in OutgoingMessageProcessor.constructMsgHeader()", e);
		}
		
		int payloadLength = payload.length;
		byte[] payloadLengthBytes = ByteUtils.intToBytes(payloadLength);
		
		byte[] checksumFullHash = SHA512.sha512(payload);
		byte[] checksum = ArrayCopier.copyOfRange(checksumFullHash, 0, 4);
		
		// Now combine all the assembled data into a single byte[]. This is the message header. 
		byte[] msgHeader = null;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try
		{		
			outputStream.write(magicBytes);
			outputStream.write(commandBytes);
			outputStream.write(payloadLengthBytes);
			outputStream.write(checksum);
			
			msgHeader = outputStream.toByteArray();
			outputStream.close();
		}
		catch (IOException e) 
		{
			throw new RuntimeException("IOException occurred in DataProcessor.constructMsgHeader()", e);
		}		
		
		return msgHeader;
	}
}