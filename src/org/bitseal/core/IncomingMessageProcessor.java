package org.bitseal.core;

import java.util.ArrayList;
import java.util.Arrays;

import org.bitseal.crypt.AddressGenerator;
import org.bitseal.crypt.CryptProcessor;
import org.bitseal.crypt.KeyConverter;
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
import org.bitseal.database.MessagesTable;
import org.bitseal.database.PayloadProvider;
import org.bitseal.database.PayloadsTable;
import org.bitseal.database.PubkeyProvider;
import org.bitseal.database.PubkeysTable;
import org.bitseal.util.ArrayCopier;
import org.bitseal.util.ByteFormatter;
import org.bitseal.util.ByteUtils;
import org.bitseal.util.VarintEncoder;
import org.spongycastle.jce.interfaces.ECPrivateKey;
import org.spongycastle.jce.interfaces.ECPublicKey;

import android.content.Intent;
import android.util.Base64;
import android.util.Log;

/**
 * This class processes incoming messages (i.e. messages received by
 * the user of the application)
 * 
 * @author Jonathan Coe
 */
public class IncomingMessageProcessor
{
	private static final int MIN_VALID_ADDRESS_VERSION = 1;
	private static final int MAX_VALID_ADDRESS_VERSION = 4;
	
	private static final int MIN_VALID_STREAM_NUMBER = 1;
	private static final int MAX_VALID_STREAM_NUMBER = 1;
	
	private static final int MIN_VALID_MESSAGE_VERSION = 1;
	private static final int MAX_VALID_MESSAGE_VERSION = 1;
	
	private static final int DEFAULT_NONCE_TRIALS_PER_BYTE = 320; // CONSTANT: See https://bitmessage.org/wiki/Proof_of_work for an explanation of this
	private static final int DEFAULT_EXTRA_BYTES = 14000; // CONSTANT: See https://bitmessage.org/wiki/Proof_of_work for an explanation of this
	
	private static final int ACK_DATA_LENGTH = 32;
	
	/** Used when broadcasting Intents to the UI so that it can refresh the data it is displaying */
	public static final String UI_NOTIFICATION = "uiNotification";
	
	private static final String TAG = "INCOMING_MESSAGE_PROCESSOR";
	
	/**
	 * Takes a Payload containing the data of a msg encrypted messages and
	 * processes it, returning a new Message object for each valid message
	 * found in the given data. <br><br>
	 * 
	 * @param msgPayload - An Payload containing the payload a possible new msg
	 * 
	 * @return An boolean indicating whether or not the given Payload contained a new message
	 * for us
	 */
	public Message processReceivedMsg(Payload msgPayload)
	{	
		// Reconstruct the payload into a Msg object
		Msg msg = reassembleMsg(msgPayload.getPayload());
		
		// Check whether this msg is an acknowledgment
		byte[] messageData = msg.getMessageData();
		if (messageData.length == ACK_DATA_LENGTH)
		{
			// If this msg is an acknowledgment, process it (checking if it is one I am awaiting)
			processAck(msg);
			return null;
		}
		else
		{
			// This msg is not an acknowledgment. Attempt to decrypt it using each of our addresses
			AddressProvider addProv = AddressProvider.get(App.getContext());
			ArrayList<Address> myAddresses = addProv.getAllAddresses();
			UnencryptedMsg unencMsg = null;
			for (Address a : myAddresses)
			{
				try
				{
					unencMsg = attemptMsgDecryption(msg, a);
					if (unencMsg != null)
					{
						// Decryption was successful! Now use the reconstructed message to create a new Message object,
						// containing the data that will be shown in the UI
						Message message = extractMessageFromUnencryptedMsg(unencMsg);
						
						// Check whether this message is a duplicate
						MessageProvider msgProv = MessageProvider.get(App.getContext());
						boolean messageIsADuplicate = msgProv.detectDuplicateMessage(message);
						if (messageIsADuplicate)
						{
							Log.d(TAG, "Processed a msg which we decrypted successfully but then found to be a duplicate of a message we had already received.\n" +
									"This message will therefore be ignored.\n" + 
									"Message to address:   " + message.getToAddress() + "\n" + 
									"Message from address: " + message.getFromAddress() + "\n" + 
									"Message subject:      " + message.getSubject() + "\n" + 
									"Message body:         " + message.getBody());
							return null;
						}
						else
						{
							checkPubkeyAndSaveIfNew(unencMsg);
							
							Log.d(TAG, "We received a new message!\n" +
									   "Message subject: " + message.getSubject());
							
							return message;
						}
					}
				}
				catch (RuntimeException e)
				{
					// If the attempt to decrypt the message fails, move on to the next address
					Log.e(TAG, "Runtime exception occurred in IncomingMessageProccessor.processReceivedMsg(). The exception message was: \n"
							+ e.getLocalizedMessage());
					continue;
				}
			}
			// If we were unable to decrypt the msg with any of our addreses
			Log.i(TAG, "Processed a msg which we failed to decrypt with any of our addresses");
			return null;
		}
	}
	
	/**
	 * Takes the embedded pubkey data from a decrypted msg that we have received
	 * and checks whether or not we have that pubkey data already. If we do not,
	 * then we use that data to create a new Pubkey object and save it to the database. 
	 * 
	 * @param unencMsg - The unencrypted msg to check. 
	 */
	private void checkPubkeyAndSaveIfNew(UnencryptedMsg unencMsg)
	{
		try
		{
			byte[] publicSigningKey = unencMsg.getPublicSigningKey();
			byte[] publicEncryptionKey = unencMsg.getPublicEncryptionKey();
			
			// We have to restore the leading 0x04 byte that is stripped off when public keys are transmitted
			byte[] fourByte = new byte[]{4};
			publicSigningKey = ByteUtils.concatenateByteArrays(fourByte, publicSigningKey);
			publicEncryptionKey = ByteUtils.concatenateByteArrays(fourByte, publicEncryptionKey);
			
			// Check whether or not we have the pubkey for the sender of this message stored in our database
			byte[] ripeHash = new AddressGenerator().calculateRipeHash(publicSigningKey, publicEncryptionKey);
			PubkeyProvider pubProv = PubkeyProvider.get(App.getContext());
			ArrayList<Pubkey> retrievedPubkeys = pubProv.searchPubkeys(PubkeysTable.COLUMN_RIPE_HASH, Base64.encodeToString(ripeHash, Base64.DEFAULT));
			if (retrievedPubkeys.size() == 0)
			{
				Log.i(TAG, "We received a message and found that we do not have the embedded pubkey data already. Therefore we will now save that pubkey data to our database");
				
				// If we do not have it already, save the public key data to the database
				Pubkey pubkey = new Pubkey();
				pubkey.setBelongsToMe(false);
				pubkey.setRipeHash(ripeHash);
				pubkey.setAddressVersion(unencMsg.getAddressVersion());
				pubkey.setStreamNumber(unencMsg.getStreamNumber());
				pubkey.setBehaviourBitfield(unencMsg.getBehaviourBitfield());
				pubkey.setPublicSigningKey(publicSigningKey);
				pubkey.setPublicEncryptionKey(publicEncryptionKey);
				pubkey.setNonceTrialsPerByte(unencMsg.getNonceTrialsPerByte());
				pubkey.setExtraBytes(unencMsg.getExtraBytes());
				
				// We don't have all the data that we normally would for a pubkey, so we shall use dummy values as placeholders
				pubkey.setPOWNonce(0);
				pubkey.setSignatureLength(0);
				pubkey.setSignature(new byte[0]);
				
				pubProv.addPubkey(pubkey);
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException("Exception occurred in IncomingMessageProcessor.checkForPubkeyAndSaveIfNew().\n"
					+ "The exception message was: " + e.getLocalizedMessage().toString());
		}
	}
	
	/**
	 * Takes a byte[] containing the data for a single received msg and attempts
	 * to reconstruct it into a Msg object.<br><br>
	 * 
	 * @param messageBytes - A byte[] containing the data for a single encrypted message
	 * 
	 * @return An UnencryptedMsg object constructed from the decrypted message data
	 */
	private Msg reassembleMsg(byte[] messageBytes)
	{
		// Parse the data from the byte[] 
		int readPosition = 0;
		
		long powNonce = ByteUtils.bytesToLong((ArrayCopier.copyOfRange(messageBytes, readPosition, readPosition + 8)));
		readPosition += 8; //The pow nonce should always be 8 bytes in length
		
		long time = ByteUtils.bytesToInt((ArrayCopier.copyOfRange(messageBytes, readPosition, readPosition + 4)));
		if (time == 0) // Need to check whether 4 or 8 byte time has been used
		{
			time = ByteUtils.bytesToLong((ArrayCopier.copyOfRange(messageBytes, readPosition, readPosition + 8)));
			readPosition += 8;
		}
		else
		{
			readPosition += 4;
		}
		
		long[] decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(messageBytes, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		int streamNumber = (int) decoded[0]; // Get the var_int encoded value
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		if (streamNumber < MIN_VALID_STREAM_NUMBER || streamNumber > MAX_VALID_STREAM_NUMBER)
		{
			throw new RuntimeException("Decoded stream number was invalid. Aborting msg decoding. The invalid value was " + streamNumber);
		}
		
		// Now deal with the remaining data. This should be either the encrypted payload of a normal msg or the ack data of an acknowledgment msg
		byte[] payload = ArrayCopier.copyOfRange(messageBytes, readPosition, messageBytes.length);
		
		// Create a new Msg object and use the parsed data to populate its fields
		Msg msg = new Msg();
		msg.setBelongsToMe(false); // i.e. This message was not written by me
		msg.setPOWNonce(powNonce);
		msg.setTime(time);
		msg.setStreamNumber(streamNumber);
		msg.setMessageData(payload);
				
		return msg;
	}
	
	/**
	 * Takes a msg that we have determined to be an acknowledgment and
	 * checks whether it is one which we are awating. If so, the status
	 * of the corresponding Message is updated. 
	 * 
	 * @param msg - A Msg object conatining the acknowledgment to be processed
	 */
	private void processAck(Msg msg)
	{
		// Get the ack data from the msg
		byte[] ackData = msg.getMessageData();
		
		// Get all acknowledgments that I am awaiting
		PayloadProvider payProv = PayloadProvider.get(App.getContext());
		ArrayList<Payload> ackPayloads = payProv.searchPayloads(PayloadsTable.COLUMN_TYPE, Payload.OBJECT_TYPE_ACK);
		ArrayList<Payload> expectedAckPayloads = new ArrayList<Payload>();
		for (Payload p : ackPayloads)
		{
			if (p.belongsToMe() == false) // i.e. The acknowledgment is being sent by someone else 
			{
				expectedAckPayloads.add(p);
			}
		}
		
		// Check if this is an acknowledgment bound for me
		for (Payload p : expectedAckPayloads)
		{
			if (Arrays.equals(p.getPayload(), ackData))
			{
				// This is an acknowledgment that I am expecting!
				// Update the status of the Message that this acknowledgment is for
				MessageProvider msgProv = MessageProvider.get(App.getContext());
				ArrayList<Message> retrievedMessages = msgProv.searchMessages(MessagesTable.COLUMN_ACK_PAYLOAD_ID, String.valueOf(p.getId()));
				if (retrievedMessages.size() == 1)
				{
					Message originalMessage = retrievedMessages.get(0);
					originalMessage.setStatus(Message.STATUS_ACK_RECEIVED);
					msgProv.updateMessage(originalMessage);
					Log.d(TAG, "Acknowledgment received!\n" +
							"Message subject:    " + originalMessage.getSubject() + "\n" +
							"Message to address: " +  originalMessage.getToAddress());
					// Update the UI
					Intent intent = new Intent(UI_NOTIFICATION);
					App.getContext().sendBroadcast(intent);
				}
				else
				{
					Log.d(TAG, "We receied an acknowledgment that we were awaiting, but the original message could not be found in the database.");
				}
				
				// We have now received this acknowledgment, so delete the 'awaiting' ack payload from the database
				payProv.deletePayload(p);
				return;
			}
		}
		// If the acknowledgment was not one that we are awaiting
		Log.i(TAG, "Processed a msg that was found to be an acknowledgment bound for someone else");
	}

	/**
	 * Attempts to decrypt a msg. If decryption is successful, the decrypted
	 * data is used to create a new UnencryptedMsg object. <br><br>
	 * 
	 * <b>NOTE:</b>If decryption of the msg fails, this method will return null 
	 * 
	 * @param msg - A Msg object containing the msg to attempt to decrypt
	 * 
	 * @return If decryption is succesful, returns an UnencryptedMsg object
	 * containing the decrypted message data. Otherwise returns null.
	 */
	private UnencryptedMsg attemptMsgDecryption(Msg msg, Address address)
	{
		byte[] encryptedMsgData = msg.getMessageData();
		
		// Create the ECPrivateKey object that we will use to decrypt the message data
		KeyConverter keyConv = new KeyConverter();
		ECPrivateKey k = keyConv.decodePrivateKeyFromWIF(address.getPrivateEncryptionKey());
		
		// Attempt to decrypt the encrypted message data
		byte[] decryptedMsgData = null;
		try
		{
			CryptProcessor cryptProc = new CryptProcessor();
			decryptedMsgData = cryptProc.decrypt(encryptedMsgData, k);
		}
		catch (RuntimeException e)
		{
			return null;
		}
		// Use the decrypted message data to construct a new UnencryptedMsg object
		UnencryptedMsg unencMsg = parseDecryptedMessage(decryptedMsgData, address);
		
		return unencMsg;
	}
	
	/** 
	 * Parses the data of a decrypted message, using it to construct a new 
	 * UnencryptedMsg object.<br><br>
	 *  
	 * @param plainText - A byte[] containing the decrypted message data
	 * @param toAddress - The Bitmessage address (presumably belonging to me) which the message was sent to
	 * 
	 * @return An UnencryptedMsg object containing the decrypted message data
	 */
	private UnencryptedMsg parseDecryptedMessage(byte[] plainText, Address toAddress)
	{		
		// Parse the individual fields from the decrypted msg data
		int readPosition = 0;
		long[] decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		int msgVersion = (int) decoded[0]; // Get the var_int encoded value
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		if (msgVersion < MIN_VALID_MESSAGE_VERSION || msgVersion > MAX_VALID_MESSAGE_VERSION)
		{
			throw new RuntimeException("Decrypted message version number was invalid. Aborting message decryption. The invalid value was " + msgVersion);
		}
		
		decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		int addressVersion = (int) decoded[0]; // Get the var_int encoded value
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		if (addressVersion < MIN_VALID_ADDRESS_VERSION || addressVersion > MAX_VALID_ADDRESS_VERSION)
		{
			throw new RuntimeException("Decrypted address version number was invalid. Aborting message decryption. The invalid value was " + addressVersion);
		}
		
		decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		int streamNumber = (int) decoded[0]; // Get the var_int encoded value
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		if (streamNumber < MIN_VALID_STREAM_NUMBER || streamNumber > MAX_VALID_STREAM_NUMBER)
		{
			throw new RuntimeException("Decrypted stream number was invalid. Aborting message decryption. The invalid value was " + streamNumber);
		}
		
		int behaviourBitfield = ByteUtils.bytesToInt((ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 4)));
		readPosition += 4; //The behaviour bitfield should always be 4 bytes in length
		
		byte[] publicSigningKey = ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 64);
		readPosition += 64;
		
		byte[] publicEncryptionKey = ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 64);
		readPosition += 64;
		
		// Set the nonceTrialsPerByte and extraBytes values to their defaults. If the unencryptedMsg adrress version is 
		// 3 or greater, we will then set these two values to those specified in the message. Otherwise they remain at
		// their default values.
		int nonceTrialsPerByte = DEFAULT_NONCE_TRIALS_PER_BYTE;
		int extraBytes = DEFAULT_EXTRA_BYTES;
		
		if (addressVersion >= 3) // Only unencrypted msgs of address version 3 or greater contain nonceTrialsPerByte and extraBytes values
		{
			decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
			nonceTrialsPerByte = (int) decoded[0]; // Get the var_int encoded value
			readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
			
			decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
			extraBytes = (int) decoded[0]; // Get the var_int encoded value
			readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		}
		
		byte[] destinationRipe = ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 20);
		readPosition += 20;
		if (Arrays.equals(destinationRipe, toAddress.getRipeHash()) == false)
		{
			throw new RuntimeException("The ripe hash read from the decrypted message text does not match the ripe hash of the address that " +
					" the key we have used to decrypt the message belongs to. This may mean that the original sender of this message did not" +
					" send it to you and that someone is attempting a Surreptitious Forwarding Attack. " +
					"\n The expected ripe hash  is: " + ByteFormatter.byteArrayToHexString(toAddress.getRipeHash()) +
					"\n The ripe hash read from the decrypted message text is: " + ByteFormatter.byteArrayToHexString(destinationRipe));
		}
		
		decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		int encoding = (int) decoded[0]; // Get the var_int encoded value
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		
		decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		int messageLength = (int) decoded[0]; // Get the var_int encoded value
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		
		byte[] message = (ArrayCopier.copyOfRange(plainText, readPosition, readPosition + messageLength));
		readPosition += messageLength;
		
		decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		int ackLength = (int) decoded[0]; // Get the var_int encoded value
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly

		byte[] ackData = (ArrayCopier.copyOfRange(plainText, readPosition, readPosition + ackLength));
		readPosition += ackLength;
		// Save this read position so that when we verify the signature we can selected the correct payload (msg_version through to ack_data)
		int signaturePayloadEndPosition = readPosition;
		
		decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		int signatureLength = (int) decoded[0]; // Get the var_int encoded value
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		
		byte[] signature = (ArrayCopier.copyOfRange(plainText, readPosition, readPosition + signatureLength));
		
		// Create a new UnencryptedMsg object and populate its fields using the decrypted msg data
		UnencryptedMsg unencryptedMsg = new UnencryptedMsg();
		
		unencryptedMsg.setMsgVersion(msgVersion);
		unencryptedMsg.setAddressVersion(addressVersion);
		unencryptedMsg.setStreamNumber(streamNumber);
		unencryptedMsg.setBehaviourBitfield(behaviourBitfield);
		unencryptedMsg.setPublicSigningKey(publicSigningKey);
		unencryptedMsg.setPublicEncryptionKey(publicEncryptionKey);
		unencryptedMsg.setNonceTrialsPerByte(nonceTrialsPerByte);
		unencryptedMsg.setExtraBytes(extraBytes);
		unencryptedMsg.setDestinationRipe(destinationRipe);
		unencryptedMsg.setEncoding(encoding);
		unencryptedMsg.setMessageLength(messageLength);
		unencryptedMsg.setMessage(message);
		unencryptedMsg.setAckLength(ackLength);
		unencryptedMsg.setAckMsg(ackData);
		unencryptedMsg.setSignatureLength(signatureLength);
		unencryptedMsg.setSignature(signature);
				
		// Verify the signature of the decrypted message
		ECPublicKey ecPublicSigningKey = new KeyConverter().reconstructPublicKey(publicSigningKey);
		SigProcessor sigProc = new SigProcessor();
		byte[] payloadToVerify = ArrayCopier.copyOfRange(plainText, 0, signaturePayloadEndPosition);
		if (sigProc.verifySignature(payloadToVerify, signature, ecPublicSigningKey) == false)
		{
			// The signature of the message is invalid. Abort the process. 
			throw new RuntimeException("While attempting to parse a decrypted message in IncomingMessageProcessor.parseDecryptedMessage(), the signature was found to be invalid");
		}
		
		// Save the acknowledgment data of this message as a Payload object and save it to 
		// the database so that we can send it later
		Payload ackPayload = new Payload();
		ackPayload.setBelongsToMe(true); // i.e This is an acknowledgment that I will send
		ackPayload.setPOWDone(true);
		ackPayload.setType(Payload.OBJECT_TYPE_ACK);
		ackPayload.setPayload(ackData);
		
		PayloadProvider payProv = PayloadProvider.get(App.getContext());
		payProv.addPayload(ackPayload);

		return unencryptedMsg;
	}
	
	/**
	 * Extracts the basic message data from an UnencryptedMsg object. Used when receiving a message.
	 * 
	 * @param unencMsg - An UnencryptedMsg object containing the message we wish to extract.
	 *  
	 * @return A Message object containing the extracted data. 
	 */
	private Message extractMessageFromUnencryptedMsg (UnencryptedMsg unencMsg)
	{		
		// Extract the message subject and body
		// See https://bitmessage.org/wiki/Protocol_specification#Message_Encodings
		String rawMessage = new String(unencMsg.getMessage()); // PyBitmessage also uses UTF-8, so this ought to be adequate.
		String messageSubject = rawMessage.substring(rawMessage.indexOf("Subject:") + 8);
		messageSubject = messageSubject.substring(0, messageSubject.indexOf("\n"));
		String messageBody = rawMessage.substring(rawMessage.lastIndexOf("Body:") + 5);
		
		// Get the String representation of the 'to' address
		String toAddressString = null;
		AddressProvider addProv = AddressProvider.get(App.getContext());
		// Match the ripe hash from the UnencryptedMsg to the address of mine which shares that ripe hash
		ArrayList<Address> retrievedAddresses = addProv.searchAddresses(
				AddressesTable.COLUMN_RIPE_HASH, Base64.encodeToString(unencMsg.getDestinationRipe(), Base64.DEFAULT));
		if (retrievedAddresses.size() != 1)
		{
			throw new RuntimeException("We successfully decrypted a msg sent to one of our addresses, but a database search for that address \n" +
					"did not return exactly one result. Something is wrong! The ripe hash used to search for the address was " + 
					ByteFormatter.byteArrayToHexString(unencMsg.getDestinationRipe()));
		}
		else
		{
			Address toAddress = retrievedAddresses.get(0);
			toAddressString = toAddress.getAddress();
		}
		
		// Recreate the String representation of the 'from' address. Before we do this we must be sure that 
		// the public keys from the UnencryptedMsg have their leading 0x04 byte in place. If it is not in place,
		// we must restore it before the keys can be used.
		byte[] publicSigningKey = unencMsg.getPublicSigningKey();
		if (publicSigningKey[0] != (byte) 4 && publicSigningKey.length == 64)
		{
			byte[] fourByte = new byte[]{4};
			publicSigningKey = ByteUtils.concatenateByteArrays(fourByte, publicSigningKey); 
		}
		byte[] publicEncryptionKey = unencMsg.getPublicEncryptionKey();
		if (publicEncryptionKey[0] != (byte) 4 && publicEncryptionKey.length == 64)
		{
			byte[] fourByte = new byte[]{4};
			publicEncryptionKey = ByteUtils.concatenateByteArrays(fourByte, publicEncryptionKey); 
		}
		String fromAddress = new AddressGenerator().recreateAddressString(unencMsg.getAddressVersion(), unencMsg.getStreamNumber(), 
				publicSigningKey, publicEncryptionKey);
		
		// Create a new Message object and populate its fields with the extracted data
		Message message = new Message();
		message.setBelongsToMe(unencMsg.belongsToMe());
		message.setToAddress(toAddressString);
		message.setFromAddress(fromAddress);
		message.setSubject(messageSubject);
		message.setBody(messageBody);

		return message;
	}
}