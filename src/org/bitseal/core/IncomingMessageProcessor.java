package org.bitseal.core;

import java.util.ArrayList;
import java.util.Arrays;

import org.bitseal.R;
import org.bitseal.crypt.AddressGenerator;
import org.bitseal.crypt.CryptProcessor;
import org.bitseal.crypt.KeyConverter;
import org.bitseal.crypt.SigProcessor;
import org.bitseal.data.Address;
import org.bitseal.data.BMObject;
import org.bitseal.data.Message;
import org.bitseal.data.Payload;
import org.bitseal.data.Pubkey;
import org.bitseal.data.QueueRecord;
import org.bitseal.data.UnencryptedMsg;
import org.bitseal.database.AddressProvider;
import org.bitseal.database.AddressesTable;
import org.bitseal.database.MessageProvider;
import org.bitseal.database.MessagesTable;
import org.bitseal.database.PayloadProvider;
import org.bitseal.database.PayloadsTable;
import org.bitseal.database.PubkeyProvider;
import org.bitseal.database.PubkeysTable;
import org.bitseal.database.QueueRecordProvider;
import org.bitseal.database.QueueRecordsTable;
import org.bitseal.services.MessageStatusHandler;
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
		
	/** In Bitmessage protocol version 3, the network standard value for nonce trials per byte is 1000. */
	public static final int NETWORK_NONCE_TRIALS_PER_BYTE = 1000;
	
	/** In Bitmessage protocol version 3, the network standard value for extra bytes is 1000. */
	public static final int NETWORK_EXTRA_BYTES = 1000;
	
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
		// Attempt to reconstruct the payload into a Msg object
		BMObject msgObject = null;
		try
		{		
			msgObject = new ObjectProcessor().parseObject(msgPayload.getPayload());
		}
		catch (RuntimeException runEx)
		{
			Log.i(TAG, "RuntimeException occurred in IncomingMessageProcessor.processReceivedMsg().\n" +
					"The exception message was: " + runEx.getMessage());
			return null;
		}
		
		// Check whether this msg is an acknowledgement
		if (msgObject.getPayload().length == ACK_DATA_LENGTH)
		{
			// If this msg is an acknowledgement, process it (checking whether it is one that I am awaiting)
			processAck(msgObject);
			return null;
		}
		else
		{
			// This msg is not an acknowledgement. Attempt to decrypt it using each of our addresses
			ArrayList<Address> myAddresses = AddressProvider.get(App.getContext()).getAllAddresses();
			UnencryptedMsg unencMsg = null;
			for (Address a : myAddresses)
			{
				try
				{
					unencMsg = attemptMsgDecryption(msgObject, a);
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
			// If we were unable to decrypt the msg with any of our addresses
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
				pubkey.setObjectVersion(unencMsg.getSenderAddressVersion());
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
	 * Takes a msg that we have determined to be an acknowledgement and
	 * checks whether it is one which we are awaiting. If so, the status
	 * of the corresponding Message is updated. 
	 * 
	 * @param msg - A msg object containing the acknowledgement to be processed
	 */
	private void processAck(BMObject msg)
	{
		// Get the ack data from the msg
		byte[] ackData = msg.getPayload();
		
		// Get all acknowledgements that I am awaiting
		PayloadProvider payProv = PayloadProvider.get(App.getContext());
		String[] columnNames = new String[]{PayloadsTable.COLUMN_ACK, PayloadsTable.COLUMN_BELONGS_TO_ME};
		String[] searchTerms = new String[]{"1", "1"}; // 1 stands for true in the database
		ArrayList<Payload> expectedAckPayloads = payProv.searchPayloads(columnNames, searchTerms);
		
		// Check if this is an acknowledgement bound for me
		for (Payload p : expectedAckPayloads)
		{
			if (Arrays.equals(p.getPayload(), ackData))
			{
				// This is an acknowledgement that I am expecting!
				// Update the status of the Message that this acknowledgement is for
				MessageProvider msgProv = MessageProvider.get(App.getContext());
				ArrayList<Message> retrievedMessages = msgProv.searchMessages(MessagesTable.COLUMN_ACK_PAYLOAD_ID, String.valueOf(p.getId()));
				if (retrievedMessages.size() == 1)
				{
					Message originalMessage = retrievedMessages.get(0);
					MessageStatusHandler.updateMessageStatus(originalMessage, App.getContext().getString(R.string.message_status_ack_received));
					Log.d(TAG, "Acknowledgement received!\n" +
							"Message subject:    " + originalMessage.getSubject() + "\n" +
							"Message to address: " +  originalMessage.getToAddress());
					
					// Update the UI
					Intent intent = new Intent(UI_NOTIFICATION);
					App.getContext().sendBroadcast(intent);
					
					// Delete any QueueRecords for sending this message
					QueueRecordProvider queueProv = QueueRecordProvider.get(App.getContext());
					ArrayList<QueueRecord> retrievedRecords = queueProv.searchQueueRecords(QueueRecordsTable.COLUMN_OBJECT_0_ID, String.valueOf(originalMessage.getId()));
					for (QueueRecord q : retrievedRecords)
					{
						// If this is a QueueRecord for one of the three 'send message' tasks
						if (q.getTask().equals(QueueRecordProcessor.TASK_SEND_MESSAGE) || q.getTask().equals(QueueRecordProcessor.TASK_PROCESS_OUTGOING_MESSAGE) || q.getTask().equals(QueueRecordProcessor.TASK_DISSEMINATE_MESSAGE))
						{
							queueProv.deleteQueueRecord(q);
						}
					}
				}
				else
				{
					Log.d(TAG, "We received an acknowledgement that we were awaiting, but the original message could not be found in the database.");
				}
				
				// We have now received this acknowledgement, so delete the 'awaiting' ack payload from the database
				payProv.deletePayload(p);
				return;
			}
		}
		// If the acknowledgement was not one that we are awaiting
		Log.i(TAG, "Processed a msg that was found to be an acknowledgement bound for someone else");
	}

	/**
	 * Attempts to decrypt a msg. If decryption is successful, the decrypted
	 * data is used to create a new UnencryptedMsg object. <br><br>
	 * 
	 * <b>NOTE:</b>If decryption of the msg fails, this method will return null 
	 * 
	 * @param msgObject - A msg Object containing the msg to attempt to decrypt
	 * 
	 * @return If decryption is successful, returns an UnencryptedMsg object
	 * containing the decrypted message data. Otherwise returns null.
	 */
	private UnencryptedMsg attemptMsgDecryption(BMObject msgObject, Address address)
	{
		try
		{
			// Create the ECPrivateKey object that we will use to decrypt the message data
			ECPrivateKey k = new KeyConverter().decodePrivateKeyFromWIF(address.getPrivateEncryptionKey());
			
			// Attempt to decrypt the encrypted message data
			byte[] decryptedMsgData = null;
			try
			{	
				decryptedMsgData = new CryptProcessor().decrypt(msgObject.getPayload(), k);
			}
			catch (RuntimeException e)
			{
				// If decryption fails (as is to be expected when we processes msgs not bound for us)
				return null;
			}
			// Use the decrypted message data to construct a new UnencryptedMsg object
			UnencryptedMsg unencMsg = parseDecryptedMessage(msgObject, decryptedMsgData, address);
			
			return unencMsg;
		}
		catch (Exception e)
		{
			throw new RuntimeException("Exception occurred in IncomingMessageProcessor.attemptMsgDecryption().\n"
					+ "The exception message was: " + e.getLocalizedMessage().toString());
		}
	}
	
	/** 
	 * Parses the data of a decrypted msg, using it to construct a new 
	 * UnencryptedMsg object.<br><br>
	 *  
	 * @param msg - The msg Object which the decrypted data came from
	 * @param plainText - A byte[] containing the decrypted msg data
	 * @param toAddress - The Bitmessage address (presumably belonging to me) which the message was sent to
	 * 
	 * @return An UnencryptedMsg object containing the decrypted message data
	 */
	private UnencryptedMsg parseDecryptedMessage(BMObject msg, byte[] plainText, Address toAddress)
	{		
		// Parse the individual fields from the decrypted msg data
		int readPosition = 0;
		
		// Read and check the sender's address version number
		long [] decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		int senderAddressVersion = (int) decoded[0]; // Get the var_int encoded value
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		if (senderAddressVersion < MIN_VALID_ADDRESS_VERSION || senderAddressVersion > MAX_VALID_ADDRESS_VERSION)
		{
			throw new RuntimeException("Decrypted address version number was invalid. Aborting message decryption. The invalid value was " + senderAddressVersion);
		}
		
		// Read and check the sender's stream number
		decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		int senderStreamNumber = (int) decoded[0]; // Get the var_int encoded value
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		if (senderStreamNumber < MIN_VALID_STREAM_NUMBER || senderStreamNumber > MAX_VALID_STREAM_NUMBER)
		{
			throw new RuntimeException("Decrypted stream number was invalid. Aborting message decryption. The invalid value was " + senderStreamNumber);
		}
		
		// Read the behaviour bitfield
		int behaviourBitfield = ByteUtils.bytesToInt((ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 4)));
		readPosition += 4; //The behaviour bitfield should always be 4 bytes in length
		
		// Read the public signing key
		byte[] publicSigningKey = ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 64);
		readPosition += 64;
		
		// Read the public encryption key
		byte[] publicEncryptionKey = ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 64);
		readPosition += 64;
		
		// Set the nonceTrialsPerByte and extraBytes values to the network standard values. If the unencryptedMsg address version is 
		// 3 or greater, we will then set these two values to those specified in the message. Otherwise they remain at
		// their default values.
		int nonceTrialsPerByte = NETWORK_NONCE_TRIALS_PER_BYTE;
		int extraBytes = NETWORK_EXTRA_BYTES;
		
		if (senderAddressVersion >= 3) // Only unencrypted msgs of address version 3 or greater contain nonceTrialsPerByte and extraBytes values
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
		
		// Strip any leading zeros from the extraction destination ripe hash
		destinationRipe = ByteUtils.stripLeadingZeros(destinationRipe);
		
		if (Arrays.equals(destinationRipe, toAddress.getRipeHash()) == false)
		{
			throw new RuntimeException("The ripe hash read from the decrypted message text does not match the ripe hash of the address that " +
					" the key we have used to decrypt the message belongs to. This may mean that the original sender of this message did not" +
					" send it to you and that someone is attempting a Surreptitious Forwarding Attack. " +
					"\n The expected ripe hash  is: " + ByteFormatter.byteArrayToHexString(toAddress.getRipeHash()) +
					"\n The ripe hash read from the decrypted message text is: " + ByteFormatter.byteArrayToHexString(destinationRipe));
		}
		
		// Read the message encoding type
		decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		int encoding = (int) decoded[0]; // Get the var_int encoded value
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		
		// Read the message length
		decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		int messageLength = (int) decoded[0]; // Get the var_int encoded value
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		
		// Read the message
		byte[] message = (ArrayCopier.copyOfRange(plainText, readPosition, readPosition + messageLength));
		readPosition += messageLength;
		
		// Read the ack length
		decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		int ackLength = (int) decoded[0]; // Get the var_int encoded value
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly

		// Read the ack data
		byte[] ackData = (ArrayCopier.copyOfRange(plainText, readPosition, readPosition + ackLength));
		readPosition += ackLength;
		
		// Read the signature length
		decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(plainText, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
		int signatureLength = (int) decoded[0]; // Get the var_int encoded value
		readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		
		// Read the signature
		byte[] signature = (ArrayCopier.copyOfRange(plainText, readPosition, readPosition + signatureLength));
		
		// Create a new UnencryptedMsg object and populate its fields using the decrypted msg data
		UnencryptedMsg unencMsg = new UnencryptedMsg();
		unencMsg.setBelongsToMe(false);
		unencMsg.setExpirationTime(msg.getExpirationTime());
		unencMsg.setObjectType(msg.getObjectType());
		unencMsg.setObjectVersion(msg.getObjectVersion());
		unencMsg.setStreamNumber(msg.getStreamNumber());
		unencMsg.setSenderAddressVersion(senderAddressVersion);
		unencMsg.setSenderStreamNumber(senderStreamNumber);
		unencMsg.setBehaviourBitfield(behaviourBitfield);
		unencMsg.setPublicSigningKey(publicSigningKey);
		unencMsg.setPublicEncryptionKey(publicEncryptionKey);
		unencMsg.setNonceTrialsPerByte(nonceTrialsPerByte);
		unencMsg.setExtraBytes(extraBytes);
		unencMsg.setDestinationRipe(destinationRipe);
		unencMsg.setEncoding(encoding);
		unencMsg.setMessageLength(messageLength);
		unencMsg.setMessage(message);
		unencMsg.setAckLength(ackLength);
		unencMsg.setAckMsg(ackData);
		unencMsg.setSignatureLength(signatureLength);
		unencMsg.setSignature(signature);
				
		// Verify the signature of the decrypted message
		ECPublicKey ecPublicSigningKey = new KeyConverter().reconstructPublicKey(publicSigningKey);
		SigProcessor sigProc = new SigProcessor();
		byte[] payloadToVerify = sigProc.createUnencryptedMsgSignaturePayload(unencMsg);
		if (sigProc.verifySignature(payloadToVerify, signature, ecPublicSigningKey) == false)
		{
			// The signature of the message is invalid. Abort the process. 
			throw new RuntimeException("While attempting to parse a decrypted message in IncomingMessageProcessor.parseDecryptedMessage(), the signature was found to be invalid");
		}
		
		// In some rare instances, such as PyBitmessage sending a message to one of its own addresses, no ack data will be included
		if (ackData.length != 0)
		{
			// Save the acknowledgement data of this msg as a Payload object and save it to 
			// the database so that we can send it later
			Payload ackPayload = new Payload();
			ackPayload.setBelongsToMe(false); // i.e This is the acknowledgement of a msg created by someone else
			ackPayload.setProcessingComplete(true); // Set 'processing complete' to true so that we won't attempt to process this as a new incoming msg
			ackPayload.setPOWDone(true);
			ackPayload.setAck(true); // This payload is an acknowledgement
			ackPayload.setType(Payload.OBJECT_TYPE_MSG); // Currently we treat all acks from other people as msgs. Strictly though they can be objects of any type, so this may change
			ackPayload.setPayload(ackData);
			
			PayloadProvider payProv = PayloadProvider.get(App.getContext());
			payProv.addPayload(ackPayload);
		}

		return unencMsg;
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
		String fromAddress = new AddressGenerator().recreateAddressString(unencMsg.getSenderAddressVersion(), unencMsg.getStreamNumber(), 
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