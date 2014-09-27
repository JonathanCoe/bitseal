package org.bitseal.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

import org.bitseal.crypt.CryptProcessor;
import org.bitseal.crypt.KeyConverter;
import org.bitseal.crypt.PubkeyGenerator;
import org.bitseal.crypt.SHA512;
import org.bitseal.crypt.SigProcessor;
import org.bitseal.data.Address;
import org.bitseal.data.Message;
import org.bitseal.data.Msg;
import org.bitseal.data.Payload;
import org.bitseal.data.Pubkey;
import org.bitseal.data.UnencryptedMsg;
import org.bitseal.database.AddressProvider;
import org.bitseal.database.AddressesTable;
import org.bitseal.database.MessageProvider;
import org.bitseal.database.PayloadProvider;
import org.bitseal.database.PubkeyProvider;
import org.bitseal.database.PubkeysTable;
import org.bitseal.pow.POWProcessor;
import org.bitseal.util.ArrayCopier;
import org.bitseal.util.ByteFormatter;
import org.bitseal.util.ByteUtils;
import org.bitseal.util.VarintEncoder;
import org.spongycastle.jce.interfaces.ECPublicKey;

import android.content.Intent;
import android.util.Log;

/**
 * This class processes outgoing messages (i.e. messages sent by the user of the app)
 * 
 * @author Jonathan Coe
 */
public class OutgoingMessageProcessor
{
	private static final String TAG = "OUTGOING_MESSAGE_PROCESSOR";
	
	private static final int MESSAGE_VERSION = 1;
	
	/** In the Bitmessage protocol this value corresponds to a normal, text-based message */
	private static final int MESSAGE_ENCODING_TYPE = 2;
	
	/** A magic hexadecimal value used by Bitmessage to identify network packets. See https://bitmessage.org/wiki/Protocol_specification#Message_structure */
	private static final String BITMESSAGE_MAGIC_IDENTIFIER = "E9BEB4D9";
	
	/** Identifies the msg contents. See https://bitmessage.org/wiki/Protocol_specification#Message_structure */
	private static final String BITMESSAGE_MSG_COMMAND = "msg";
	
	/** Padding for the command. See https://bitmessage.org/wiki/Protocol_specification#Message_structure */
	private static final String BITMESSAGE_MSG_COMMAND_PADDING = "000000000000000000";
	
	/** The character encoding used in the Bitmessage command data */
	private static final String BITMESSAGE_COMMAND_ENCODING = "US-ASCII";
	
	/** Used when broadcasting Intents to the UI so that it can refresh the data it is displaying */
	public static final String UI_NOTIFICATION = "uiNotification";
	
	/**
	 * Takes a Message object and does all the work necessary to 
	 * transform it into an encrypted message that is ready to be sent
	 * over the Bitmessage network. This includes encryption and proof 
	 * of work.
	 * 
	 * @param message - The Message object to be processed
	 * @param toPubkey - The Pubkey of the address that the message is 
	 * being sent to
	 * @param doPOW - A boolean value indicating whether or not POW should
	 * be done for this message AND for pubkeys generated during the message
	 * sending process
	 * 
	 * @return A Payload object containing the encrypted message data ready to
	 * be sent over the Bitmessage network
	 */
	public Payload processOutgoingMessage (Message message, Pubkey toPubkey, boolean doPOW)
	{
		// Convert the message into a new UnencryptedMsg object
		UnencryptedMsg unencMsg = constructUnencryptedMsg(message, toPubkey, doPOW);
		
		// Encrypt the message and, if enabled, do POW
		Msg encMsg = constructMsg(message, unencMsg, toPubkey, doPOW);

		// Construct the msg payload that will be sent over the network
		Payload msgPayload = constructMsgPayloadForDissemination(encMsg, doPOW, toPubkey);
		
		return msgPayload;
	}
	
	/**
	 * Constructs an UnencryptedMsg object from a given Message object. Used when sending a message. <br><br>
	 * 
	 * <b>NOTE!</b> Calling this method results in proof of work calculations being done for the acknowledgement
	 * data of the message. This can take a long time and lots of CPU power!<br><br>
	 * 
	 * <b>NOTE!</b> Calling this method can result in requests to a Bitseal server to retrieve pubkey data. These
	 * requests may take some time to complete!
	 * 
	 * @param message - The Message object to convert into an UnencryptedMsg object
	 * @param toPubkey - A Pubkey obejct containing the public keys of the address the message is being sent to
	 * @param doPOW - A boolean indicating whether or not POW should be done for pubkeys generated during this process
	 * 
	 * @return An UnencryptedMsg object based on the supplied Message object. 
	 */
	private UnencryptedMsg constructUnencryptedMsg(Message message, Pubkey toPubkey, boolean doPOW)
	{
		String messageSubject = message.getSubject();
		String messageBody = message.getBody();
		
		// First let us check that the to address and from address Strings taken from the Message object are in fact valid Bitmessage addresses
		String toAddressString = message.getToAddress();
		String fromAddressString = message.getFromAddress();
		AddressProcessor addProc = new AddressProcessor();
		
		if (addProc.validateAddress(toAddressString) != true)
		{
			throw new RuntimeException("During the execution of constructUnencryptedMsg(), it was found that the 'to' address in the supplied Message was not a valid Bitmessage address");
		}
		if (addProc.validateAddress(fromAddressString) != true)
		{
			throw new RuntimeException("During the execution of constructUnencryptedMsg(), it was found that the 'from' address in the supplied Message was not a valid Bitmessage address");
		}
		
		// Now that we have validated the to address and the from address, let us retrieve or create their corresponding Address and Pubkey objects.
		Address fromAddress = null;
		AddressProvider addProv = AddressProvider.get(App.getContext());
		ArrayList<Address> retrievedAddresses = addProv.searchAddresses(AddressesTable.COLUMN_ADDRESS, fromAddressString);
		if (retrievedAddresses.size() != 1)
		{
			Log.e(TAG, "There should be exactly 1 record found in this search. Instead " + retrievedAddresses.size() + " records were found");
		}
		else
		{
			fromAddress = retrievedAddresses.get(0);
		}
				
		// Now we need to get the behaviour bitfield from the pubkey which corresponds to the from address, so let us retrieve that pubkey. 
		PubkeyProvider pubProv = PubkeyProvider.get(App.getContext());
		ArrayList<Pubkey> retrievedPubkeys = pubProv.searchPubkeys(PubkeysTable.COLUMN_CORRESPONDING_ADDRESS_ID, String.valueOf(fromAddress.getId()));
		Pubkey fromPubkey = null;
		if (retrievedPubkeys.size() != 1)
		{
			Log.e(TAG, "There should be exactly 1 record found in this search. Instead " + retrievedPubkeys.size() + " records were found");
		}
		else
		{
			fromPubkey = retrievedPubkeys.get(0);
		}
		
		if (fromPubkey == null)
		{
			Log.e(TAG, "Could not find the Pubkey which corresponds to the from address, even though it should be one of our own. Something is wrong!");
			Log.d(TAG, "Regenerating the Pubkey for the from address");
			fromPubkey = new PubkeyGenerator().generateAndSaveNewPubkey(fromAddress); // If we can't find the pubkey we need then let us generate it again
		}
		
		// Now extract the public signing and public encryption keys from the "from" pubkey
		// If the public signing and encryption keys taken from the Pubkey object have an "\x04" byte at their beginning, we need to remove it now. 
		byte[] publicSigningKey = fromPubkey.getPublicSigningKey();
		byte[] publicEncryptionKey = fromPubkey.getPublicEncryptionKey();
		
		if (publicSigningKey[0] == (byte) 4 && publicSigningKey.length == 65)
		{
			publicSigningKey = ArrayCopier.copyOfRange(publicSigningKey, 1, publicSigningKey.length);
		}
		
		if (publicEncryptionKey[0] == (byte) 4 && publicEncryptionKey.length == 65)
		{
			publicEncryptionKey = ArrayCopier.copyOfRange(publicEncryptionKey, 1, publicEncryptionKey.length);
		}
		
		// Calculate the ack data (32 random bytes)
		byte[] ackData = new byte[32];
		new SecureRandom().nextBytes(ackData);
		
		// Generate the full ack msg that will be included in this unencrypted msg.
		// NOTE: Calling the generateFullAckMsg() method results in Proof of Work calculations being done for the
		//       acknowledgement msg. This can take a long time and lots of CPU power!
		byte[] fullAckMsg = generateFullAckMsg(message, ackData, toPubkey.getStreamNumber());
		Log.d(TAG, "Full ack msg: " + ByteFormatter.byteArrayToHexString(fullAckMsg));
			
		// Create the single "message" text String which contains both the subject and the body of the message
		// See https://bitmessage.org/wiki/Protocol_specification#Message_Encodings
		String messsageText = "Subject:" + messageSubject + "\n" + "Body:" + messageBody;
		
		// Now create the UnencryptedMsg object and populate its fields. 
		UnencryptedMsg unencMsg = new UnencryptedMsg();
		
		unencMsg.setBelongsToMe(true);
		unencMsg.setMsgVersion(MESSAGE_VERSION);
		unencMsg.setAddressVersion(fromPubkey.getAddressVersion());
		unencMsg.setStreamNumber(fromPubkey.getStreamNumber());
		unencMsg.setBehaviourBitfield(fromPubkey.getBehaviourBitfield());
		unencMsg.setPublicSigningKey(publicSigningKey);
		unencMsg.setPublicEncryptionKey(publicEncryptionKey);
		unencMsg.setNonceTrialsPerByte(fromPubkey.getNonceTrialsPerByte());
		unencMsg.setExtraBytes(fromPubkey.getExtraBytes());
		unencMsg.setDestinationRipe(new KeyConverter().calculateRipeHashFromPubkey(toPubkey));
		unencMsg.setEncoding(MESSAGE_ENCODING_TYPE);
		unencMsg.setMessageLength(messsageText.getBytes().length); // We have to use the byte length rather than the string length - some characters take more bytes than others
		unencMsg.setMessage(messsageText.getBytes()); // PyBitmessage also uses UTF-8 as its character set, so this ought to be adequate
		unencMsg.setAckLength(fullAckMsg.length);
		unencMsg.setAckMsg(fullAckMsg);
		
		// Save the acknowledgment data to the database so that when we receive the acknowledgment for this message we will recognise it
		Payload ackPayload = new Payload();
		ackPayload.setBelongsToMe(false); // i.e. This is an acknowledgment that will be sent by someone else (the recipient of this message)
		ackPayload.setPOWDone(true);
		ackPayload.setType(Payload.OBJECT_TYPE_ACK);
		ackPayload.setPayload(ackData);	
		PayloadProvider payProv = PayloadProvider.get(App.getContext());
		long ackPayloadId = payProv.addPayload(ackPayload);
		
		// Set the "ackPayloadId" field of the original Message object so that we know which Message this ack data is for
		message.setAckPayloadId(ackPayloadId);
		MessageProvider msgProv = MessageProvider.get(App.getContext());
		msgProv.updateMessage(message);
		
		// Now create the signature for this message
		SigProcessor sigProc = new SigProcessor();
		byte[] signaturePayload = sigProc.createUnencryptedMsgSignaturePayload(unencMsg);
		byte[] signature = sigProc.signWithWIFKey(signaturePayload, fromAddress.getPrivateSigningKey());
		
		unencMsg.setSignature(signature);
		unencMsg.setSignatureLength(signature.length);
		
		return unencMsg;
	}
	
	/**
	 * Takes an UnencryptedMsg object and does all the work necessary to transform it into an EncyrptedMsg
	 * object that is ready to be serialized and sent out to the Bitmessage network. The two major parts of this
	 * process are encryption and proof of work. <br><br>
	 * 
	 * <b>NOTE!</b> Calling this method results in proof of work calculations being done for the
	 * message. This can take a long time and lots of CPU power!<br><br>
	 * 
	 * @param message - The original plain text Message object, provided so that its status can be updated during the process
	 * @param unencMsg - The UnencryptedMsg object to be encrypted
	 * @param toPubkey - The Pubkey object containing the public encryption key of the intended message recipient
	 * @param doPOW - A boolean value indicating whether or not POW should be done for this message
	 * 
	 * @return A Msg object containing the encrypted message data
	 */
	private Msg constructMsg (Message message, UnencryptedMsg unencMsg, Pubkey toPubkey, boolean doPOW)
	{		
		// Reconstruct the ECPublicKey object from the byte[] found the the relevant PubKey
		ECPublicKey publicEncryptionKey = new KeyConverter().reconstructPublicKey(toPubkey.getPublicEncryptionKey());
		
		// Construct the payload to be encrypted
		byte[] msgDataForEncryption = constructMsgPayloadForEncryption(unencMsg);
		
		updateMessageStatus(message, Message.STATUS_ENCRYPTING_MESSAGE);
		
		// Encrypt the payload
		CryptProcessor cryptProc = new CryptProcessor();
		byte[] encryptedPayload = cryptProc.encrypt(msgDataForEncryption, publicEncryptionKey);
		
		// Create a new Msg object and populate its fields
		Msg msg = new Msg();
		msg.setBelongsToMe(true); // NOTE: This method assumes that any message I am encrypting 'belongs to me' (i.e. The user of the application is the author of the message)
		msg.setStreamNumber(toPubkey.getStreamNumber());
		msg.setMessageData(encryptedPayload);
		
		if (doPOW == true)
		{
			updateMessageStatus(message, Message.STATUS_DOING_POW);
			
			// Do proof of work for the Msg object
			Log.i(TAG, "About to do POW calculations for a msg that we are sending");
			byte[] powPayload = constructMsgPayloadForPOW(msg);
			long powNonce = new POWProcessor().doPOW(powPayload, toPubkey.getNonceTrialsPerByte(), toPubkey.getExtraBytes());
			msg.setPOWNonce(powNonce);
		}
		else
		{
			msg.setPOWNonce((long) 0); // If POW is not to be done for this message, set the powNonce as zero for now.
		}
		
		return msg;
	}
	
	/**
	 * Updates the status of a Message object in the database and prompts the
	 * UI to refresh itself so that the new status will be displayed
	 * 
	 * @param message - The Message object to update the status of
	 * @param status - The status String to use
	 */
	private void updateMessageStatus(Message message, String status)
	{
		// Update the status of the Message and then prompt the UI to update the list of sent messages it is displaying
		message.setStatus(status);
		MessageProvider msgProv = MessageProvider.get(App.getContext());
		msgProv.updateMessage(message);	
		Intent intent = new Intent(UI_NOTIFICATION);
		App.getContext().sendBroadcast(intent);
	}
	
	/**
	 * Takes a Msg and constructs the payload needed to do POW for it. 
	 * 
	 * @param msg - The Msg to construct the POW payload for
	 * 
	 * @return The POW payload
	 */
	private byte[] constructMsgPayloadForPOW (Msg msg)
	{
		try
		{
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			outputStream.write(ByteUtils.longToBytes(msg.getTime())); // This conversion results in a byte[] of length 8, which is what we want
			outputStream.write(VarintEncoder.encode(msg.getStreamNumber())); 
			outputStream.write(msg.getMessageData());
			
			return outputStream.toByteArray();
		}
		catch (IOException e)
		{
			throw new RuntimeException("IOException occurred in OutgoingMessageProcessor.constructMsgPayloadForPOW()", e);
		}
	}
	
	/**
	 * Takes an UnencryptedMsg object and extracts only the data needed to encrypt the message, discarding
	 * data that is only used by Bitseal internally, such as the ID number.   
	 * 
	 * @param inputMsgData - The UnencryptedMsg object from which the data is to be extracted
	 * 
	 * @return A byte[] containing the message data needed for encryption
	 */
	private byte[] constructMsgPayloadForEncryption (UnencryptedMsg unencMsg)
	{		
		byte[] msgDataForEncryption = null;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try 
		{
			outputStream.write(VarintEncoder.encode(unencMsg.getMsgVersion())); 
			outputStream.write(VarintEncoder.encode(unencMsg.getAddressVersion())); 
			outputStream.write(VarintEncoder.encode(unencMsg.getStreamNumber())); 
			outputStream.write(ByteBuffer.allocate(4).putInt(unencMsg.getBehaviourBitfield()).array()); //The behaviour bitfield should always be 4 bytes in length
			
			// If the public signing and public encryption keys have their leading 0x04 byte in place then we need to remove them
			byte[] publicSigningKey = unencMsg.getPublicSigningKey();
			if (publicSigningKey[0] == (byte) 4  && publicSigningKey.length == 65)
			{
				publicSigningKey = ArrayCopier.copyOfRange(publicSigningKey, 1, publicSigningKey.length);
			}
			outputStream.write(publicSigningKey);
			
			byte[] publicEncryptionKey = unencMsg.getPublicEncryptionKey();
			if (publicEncryptionKey[0] == (byte) 4  && publicEncryptionKey.length == 65)
			{
				publicEncryptionKey = ArrayCopier.copyOfRange(publicEncryptionKey, 1, publicEncryptionKey.length);
			}
			outputStream.write(publicEncryptionKey);
			
			if (unencMsg.getAddressVersion() >= 3) // The nonceTrialsPerByte and extraBytes fields are only included when the address version is >= 3
			{
				outputStream.write(VarintEncoder.encode(unencMsg.getNonceTrialsPerByte())); 
				outputStream.write(VarintEncoder.encode(unencMsg.getExtraBytes())); 
			}
			
			outputStream.write(unencMsg.getDestinationRipe());
			outputStream.write(VarintEncoder.encode(unencMsg.getEncoding())); 
			outputStream.write(VarintEncoder.encode(unencMsg.getMessageLength())); 
			outputStream.write(unencMsg.getMessage());
			outputStream.write(VarintEncoder.encode(unencMsg.getAckLength())); 
			outputStream.write(unencMsg.getAckMsg());
			outputStream.write(VarintEncoder.encode(unencMsg.getSignatureLength())); 
			outputStream.write(unencMsg.getSignature());
		
			msgDataForEncryption = outputStream.toByteArray();
			outputStream.close();
		}
		catch (IOException e) 
		{
			throw new RuntimeException("IOException occurred in DataProcessor.constructMsgPayloadForEncryption()", e);
		}
		
		return msgDataForEncryption;
	}

	/**
	 * Calculates the acknowledgement msg for a message. <br><br>
	 * 
	 * The process for this is as follows:<br><br>
	 * 1) initialPayload = time || stream number || 32 bytes of random data<br><br>
	 * 2) Do POW for the initialPayload<br><br>
	 * 3) ackData = POWnonce || msgHeader || initialPayload<br><br>
	 * 
	 * @param message - The original plain text Message object, provided so that its status can be updated during the process
	 * @param ackData - A byte[] containing the 32 bytes of random data which is the acknowledgment data
	 * @param toStreamNumber - An int representing the stream number of the destination address of the message to be sent
	 * 
	 * @return A byte[] containing the acknowledgement data for the message we wish to send
	 */
	private byte[] generateFullAckMsg (Message message, byte[] ackData, int toStreamNumber)
	{	
		// Get the current time + or - 5 minutes and encode it into byte form
		long time = System.currentTimeMillis() / 1000L; // Gets the current time in seconds
    	int timeModifier = (new Random().nextInt(600)) - 300;
    	time = time + timeModifier; // Gives us the current time plus or minus 300 seconds (five minutes). This is also done by PyBitmessage. 
		byte[] timeBytes = ByteBuffer.allocate(8).putLong(time).array();
		
		// Encode the 'to' stream number into byte form
		byte[] streamBytes = VarintEncoder.encode((long) toStreamNumber); 
		
		// Combine the time, stream number, and random data bytes to create the acknowledgment message payload
		byte[] initialPayload = ByteUtils.concatenateByteArrays(timeBytes, streamBytes);
		initialPayload = ByteUtils.concatenateByteArrays(initialPayload, ackData);
		
		updateMessageStatus(message, Message.STATUS_DOING_ACK_POW);
				
		// Do proof of work for the acknowledgement payload
		Log.i(TAG, "About to do POW calculations for the acknowledgment payload of a msg that we are sending");
		long powNonce = new POWProcessor().doPOW(initialPayload, POWProcessor.NETWORK_NONCE_TRIALS_PER_BYTE, POWProcessor.NETWORK_EXTRA_BYTES);
		
		byte[] powNonceBytes = ByteUtils.longToBytes(powNonce);
		
		byte[] payload = ByteUtils.concatenateByteArrays(powNonceBytes, initialPayload);
		
		byte[] headerData = constructMsgHeader(payload);
		
		byte[] fullAckMsg = ByteUtils.concatenateByteArrays(headerData, payload);
		
		return fullAckMsg;
	}
	
	/**
	 * Creates the header for a message to be sent between Bitmessage nodes. For a specification of the header see
	 * https://bitmessage.org/wiki/Protocol_specification#Message_structure
	 * 
	 * @param - payload - A byte[] containing the message to construct a header for
	 * 
	 * @return A byte[] containing the message header
	 */
	private byte[] constructMsgHeader (byte[] payload)
	{
		// Get the byte values of all the data that needs to go in the message header
		byte[] magicBytes = ByteFormatter.hexStringToByteArray(BITMESSAGE_MAGIC_IDENTIFIER);
		byte[] command = null;
		try
		{
			command = BITMESSAGE_MSG_COMMAND.getBytes(BITMESSAGE_COMMAND_ENCODING);
			byte[] commandPadding = ByteFormatter.hexStringToByteArray(BITMESSAGE_MSG_COMMAND_PADDING);
			command = ByteUtils.concatenateByteArrays(command, commandPadding);
		} 
		catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException("UnsupportedEncodingException occurred in OutgoingMessageProcessor.constructMsgHeader()", e);
		}
		
		int payloadLength = payload.length;
		byte[] payloadLengthBytes = ByteBuffer.allocate(4).putInt(payloadLength).array();
		
		byte[] checksumFullHash = SHA512.sha512(payload);
		byte[] checksum = ArrayCopier.copyOfRange(checksumFullHash, 0, 4);
		
		// Now combine all the assembled data into a single byte[]. This is the message header. 
		byte[] msgHeader = null;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try
		{		
			outputStream.write(magicBytes);
			outputStream.write(command);
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
	
	/**
	 * Takes a Msg and encodes it into a single byte[], in a way that is compatible
	 * with the way that PyBitmessage does. This payload can then be sent to a server
	 * to be disseminated across the network. The payload is stored as a Payload object.
	 * 
	 * @param encMsg - A Msg object containing the message data used to create
	 * the payload.
	 * @param powDone - A boolean value indicating whether or not POW has been done for this message
	 * @param toPubkey - A Pubkey object containing the data for the Pubkey of the address that this 
	 * message is being sent to
	 * 
	 * @return A Payload object containing the message payload
	 */
	private Payload constructMsgPayloadForDissemination (Msg encMsg, boolean powDone, Pubkey toPubkey)
	{
		// Create a new Payload object to hold the payload data
		Payload msgPayload = new Payload();
		msgPayload.setBelongsToMe(true);
		msgPayload.setPOWDone(powDone);
		msgPayload.setType(Payload.OBJECT_TYPE_MSG);
		
		byte[] powNonceBytes = ByteBuffer.allocate(8).putLong(encMsg.getPOWNonce()).array();
		byte[] timeBytes = ByteBuffer.allocate(8).putLong(encMsg.getTime()).array();
		byte streamNumberByte = (byte) encMsg.getStreamNumber();
		
		byte[] payload = null;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try 
		{
			if (powDone == true)
			{
				outputStream.write(powNonceBytes);
			}		
			outputStream.write(timeBytes);
			outputStream.write(streamNumberByte);
			outputStream.write(encMsg.getMessageData());
		
			payload = outputStream.toByteArray();
			outputStream.close();
		}
		catch (IOException e) 
		{
			throw new RuntimeException("IOException occurred in DataProcessor.constructMsgPayloadForDissemination()", e);
		}
		
		msgPayload.setPayload(payload);
		
		// Save the Payload object to the database
		PayloadProvider payProv = PayloadProvider.get(App.getContext());
		long msgPayloadId = payProv.addPayload(msgPayload);
		
		// Finally, set the msg payload ID to the one generated by the SQLite database
		msgPayload.setId(msgPayloadId);
		
		return msgPayload;
	}
}