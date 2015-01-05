package org.bitseal.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

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
import org.bitseal.database.AddressProvider;
import org.bitseal.database.PayloadProvider;
import org.bitseal.database.PubkeyProvider;
import org.bitseal.database.PubkeysTable;
import org.bitseal.network.NetworkHelper;
import org.bitseal.network.ServerCommunicator;
import org.bitseal.pow.POWProcessor;
import org.bitseal.services.MessageStatusHandler;
import org.bitseal.util.ArrayCopier;
import org.bitseal.util.ByteUtils;
import org.bitseal.util.VarintEncoder;
import org.spongycastle.jce.interfaces.ECPrivateKey;
import org.spongycastle.jce.interfaces.ECPublicKey;

import android.util.Base64;
import android.util.Log;

/**
 * A class which provides various methods used for processing pubkeys within Bitseal. 
 * 
 * @author Jonathan Coe
 */
public class PubkeyProcessor
{
	/** In Bitmessage protocol version 3, the network standard value for nonce trials per byte is 1000. */
	public static final int NETWORK_NONCE_TRIALS_PER_BYTE = 1000;
	
	/** In Bitmessage protocol version 3, the network standard value for extra bytes is 1000. */
	public static final int NETWORK_EXTRA_BYTES = 1000;
	
	private static final int EMPTY_SIGNATURE_LENGTH = 0; // Pubkeys of version 2 and below do not have signatures
	private static final byte[] EMPTY_SIGNATURE = new byte[]{0};
	
	private static final String TAG = "PUBKEY_PROCESSOR";
	
	/**
	 * Checks whether a given Pubkey and Bitmessage address are valid for
	 * each other. 
	 * 
	 * @param pubkey - A Pubkey object to be validated
	 * @param addressString - A String containing the Bitmessage address to 
	 * validate the Pubkey against
	 * 
	 * @return A boolean indicating whether or not the Pubkey and address String
	 * are valid for each other
	 */
	public boolean validatePubkey (Pubkey pubkey, String addressString)
	{
		// First check that the given address string is a valid Bitmessage address.
		AddressProcessor addProc = new AddressProcessor();
		boolean addressStringValid = addProc.validateAddress(addressString);
		if (addressStringValid == false)
		{
			Log.i(TAG, "While running PubkeyProcessor.validatePubkey(), it was found that the supplied \n" +
					"address String was NOT a valid Bitmessage address");
			return false;
		}
		
		// Check that the pubkey is valid by using its public signing key, public encryption key, 
		// address version number, and stream number to recreate the address string that it corresponds to.
		// This should match the address string that we started with.
		AddressGenerator addGen = new AddressGenerator();
		String recreatedAddress = addGen.recreateAddressString(pubkey.getObjectVersion(), pubkey.getStreamNumber(),
				pubkey.getPublicSigningKey(), pubkey.getPublicEncryptionKey());
		
		Log.i(TAG, "Recreated address String: " + recreatedAddress);
		boolean recreatedAddressValid = recreatedAddress.equals(addressString);
		if (recreatedAddressValid == false)
		{
			Log.i(TAG, "While running PubkeyProcessor.validatePubkey(), it was found that the recreated address String \n" +
					    "generated using data from the pubkey did not match the original address String. \n" +
						"The original address String was : " + addressString + "\n" +
						"The recreated address String was: " + recreatedAddress);
			return false;
		}
		
		// If this pubkey is of version 2 or above, also check that the signature of the pubkey is valid
		int[] addressNumbers = addProc.decodeAddressNumbers(addressString);
		int addressVersion = addressNumbers[0];
		if (addressVersion > 2)
		{
			// To verify the signature we first have to convert the public signing key from the retrieved pubkey into an ECPublicKey object
			KeyConverter keyConv = new KeyConverter();
			ECPublicKey publicSigningKey = keyConv.reconstructPublicKey(pubkey.getPublicSigningKey());
			
			SigProcessor sigProc = new SigProcessor();
			byte[] signaturePayload = sigProc.createPubkeySignaturePayload(pubkey);
			boolean sigValid = (sigProc.verifySignature(signaturePayload, pubkey.getSignature(), publicSigningKey));
			
			if (sigValid == false)
			{
				Log.i(TAG, "While running PubkeyProcessor.validatePubkey(), it was found that the pubkey's signature was invalid");
				return false;
			}
		}
		
		// If the recreated address String and signature were both valid
		return true;
	}
	
	/**
	 * Takes a Message and attempts to retrieve the Pubkey of the Message's 'to address'<br><br>
	 * 
	 * Note: If the pubkey has to be retrieved from a server and the attempt to do so fails, 
	 * this method will throw a RuntimeException.
	 * 
	 * @param addressString - The Message we are attempting to send
	 * 
	 * @return A Pubkey object that represents the pubkey for the supplied Message's 'to address'
	 */
	public Pubkey retrievePubkeyForMessage (Message message)
	{
		String addressString = message.getToAddress();
		
		// Extract the ripe hash from the address String
		byte[] ripeHash = new AddressProcessor().extractRipeHashFromAddress(addressString);
		
		Pubkey pubkey = retrievePubkeyFromDatabase(ripeHash);
		if (pubkey != null)
		{
			return pubkey;
		}
		else
		{
			Log.i(TAG, "Unable to find the requested pubkey in the application database. The pubkey will now be requested from a server.");
			
			MessageStatusHandler.updateMessageStatus(message, App.getContext().getString(R.string.message_status_requesting_pubkey));
			
			// Check whether an Internet connection is available.
			if (NetworkHelper.checkInternetAvailability() == true)
			{
				return retrievePubkeyFromServer(addressString, ripeHash);
			}
			else
			{
				MessageStatusHandler.updateMessageStatus(message, App.getContext().getString(R.string.message_status_waiting_for_connection));
				throw new RuntimeException("Unable to retrieve the pubkey because no internet connection is available");
			}
		}
	}
	
	/**
	 * Takes a String representing a Bitmessage address and uses it to retrieve the Pubkey that
	 * corresponds to that address. <br><br>
	 * 
	 * This method is intended to be used to retrieve the Pubkey of another person 
	 * when we have their address and wish to send them a message.<br><br>
	 * 
	 * Note: If the pubkey has to be retrieved from a server and the attempt to do so fails, 
	 * this method will throw a RuntimeException.
	 * 
	 * @param addressString - A String containing the Bitmessage address that we wish to retrieve
	 * the pubkey for - e.g. "BM-NBpe4wbtC59sWFKxwaiGGNCb715D6xvY"
	 * 
	 * @return A Pubkey object that represents the pubkey for the supplied address
	 */
	public Pubkey retrievePubkeyByAddressString (String addressString)
	{
		// Extract the ripe hash from the address String
		byte[] ripeHash = new AddressProcessor().extractRipeHashFromAddress(addressString);
		
		Pubkey pubkey = retrievePubkeyFromDatabase(ripeHash);
		if (pubkey != null)
		{
			return pubkey;
		}
		else
		{
			Log.i(TAG, "Unable to find the requested pubkey in the application database. The pubkey will now be requested from a server.");
			
			return retrievePubkeyFromServer(addressString, ripeHash);
		}
	}
	
	/**
	 * Attempts to retrieve the Pubkey with a given ripe hash from the database.<br><br>
	 * 
	 * Note! If the Pubkey cannot be found, this method will return null
	 * 
	 * @param ripeHash A byte[] containing the ripe hash of the Pubkey to be retrieved
	 * 
	 * @return A Pubkey, or null if the Pubkey cannot be found
	 */
	private Pubkey retrievePubkeyFromDatabase(byte[] ripeHash)
	{
		// Search the application's database to see if the pubkey we need is stored there
		// Note that ripe hashes in the database have their leading zeros removed
		PubkeyProvider pubProv = PubkeyProvider.get(App.getContext());
		ArrayList<Pubkey> retrievedPubkeys = pubProv.searchPubkeys(PubkeysTable.COLUMN_RIPE_HASH, Base64.encodeToString(ByteUtils.stripLeadingZeros(ripeHash), Base64.DEFAULT));
		if (retrievedPubkeys.size() > 1)
		{
			Log.i(TAG, "We seem to have found duplicate pubkeys during the database search. We will use the first one and delete the duplicates.");
			
			for (Pubkey p : retrievedPubkeys)
			{
				if (retrievedPubkeys.indexOf(p) != 0) // Keep the first record and delete all the others
				{
					pubProv.deletePubkey(p);
				}
			}
			
			Pubkey pubkey = retrievedPubkeys.get(0);
			return pubkey;
		}
		else if (retrievedPubkeys.size() == 1)
		{
			Pubkey pubkey = retrievedPubkeys.get(0);
			return pubkey;
		}
		else
		{
			return null;
		}
	}
	
	/**
	 * Attempts to retrieve the pubkey with a given address string and ripe hash from a server.<br><br>
	 * 
	 * Note! If the pubkey cannot be found, this method will throw a RuntimeException
	 * 
	 * @param addressString - A String containing the address of pubkey to be retrieved 
	 * @param ripeHash - A byte[] containing the ripe hash of the pubkey to be retrieved
	 * 
	 * @return A Pubkey, or null if the Pubkey cannot be found
	 */
	private Pubkey retrievePubkeyFromServer(String addressString, byte[] ripeHash)
	{
		// Extract the address version from the address string in order to determine whether the pubkey will
		// be encrypted (version 4 and above)
		AddressProcessor addProc = new AddressProcessor();
		int[] decodedAddressValues = addProc.decodeAddressNumbers(addressString);
		int addressVersion = decodedAddressValues[0];
		
		// Retrieve the pubkey from a server
		ServerCommunicator servCom = new ServerCommunicator();
		Pubkey pubkey = null;
		
		if (addressVersion >= 4) // The pubkey will be encrypted
		{
			// Calculate the tag that will be used to request the encrypted pubkey
			byte[] tag = addProc.calculateAddressTag(addressString);
			
			// Retrieve the encrypted pubkey from a server
			pubkey = servCom.requestPubkeyFromServer(addressString, tag, addressVersion);
		}
		else // The pubkey is of version 3 or below, and will therefore not be encrypted
		{
			pubkey = servCom.requestPubkeyFromServer(addressString, ripeHash, addressVersion);
		}
		
		// Save the pubkey to the database and set its ID with the one generated by the database
		PubkeyProvider pubProv = PubkeyProvider.get(App.getContext());
		long id = pubProv.addPubkey(pubkey);
		pubkey.setId(id);
		
		return pubkey; // If the ServerCommunicator fails to retrieve the Pubkey then it will throw a RuntimeException. This will be passed
					   // up the method call hierarchy and handled. 
	}
		
	/**
	 * Reconstructs a pubkey from its encoded byte[] form, typically
	 * the data received from a server after requesting a pubkey. 
	 * 
	 * @param pubkeyData - A byte[] containing the encoded data for a pubkey
	 * @param addressString - If the pubkey is to be reconstructed is of address
	 * version 4 or above, then a String representing the Bitmessage address
	 * corresponding to the pubkey must be supplied, in order for the encrypted
	 * part of the pubkey to be decrypted. Otherwise, the addressString parameter
	 * will not be used. 
	 * 
	 * @return A Pubkey object constructed from the data provided
	 */
	public Pubkey reconstructPubkey (byte[] pubkeyData, String addressString)
	{
		// First parse the standard Bitmessage object data
		BMObject pubkeyObject = new ObjectProcessor().parseObject(pubkeyData);
		
		// Now parse the pubkey-specific data
		byte[] pubkeyPayload = pubkeyObject.getPayload();
		int readPosition = 0;
		
		// Pubkeys of version 4 and above have most of their data encrypted. 
		if (pubkeyObject.getObjectVersion() >= 4)
		{
			byte[] encryptedData = ArrayCopier.copyOfRange(pubkeyPayload, readPosition + 32, pubkeyPayload.length); // Skip over the tag
			
			// Create the ECPrivateKey object that we will use to decrypt encrypted the pubkey data
			AddressProcessor addProc = new AddressProcessor();
			byte[] encryptionKey = addProc.calculateAddressEncryptionKey(addressString);
			KeyConverter keyConv = new KeyConverter();
			ECPrivateKey k = keyConv.calculatePrivateKeyFromDoubleHashKey(encryptionKey);
			
			// Attempt to decrypt the encrypted pubkey data
			CryptProcessor cryptProc = new CryptProcessor();
			pubkeyPayload = cryptProc.decrypt(encryptedData, k);
			readPosition = 0; // Reset the read position so that we start from the beginning of the decrypted data
		}
		
		int behaviourBitfield = ByteUtils.bytesToInt((ArrayCopier.copyOfRange(pubkeyPayload, readPosition, readPosition + 4))); 
		readPosition += 4; //The behaviour bitfield should always be 4 bytes in length
		
		byte[] publicSigningKey = ArrayCopier.copyOfRange(pubkeyPayload, readPosition, readPosition + 64);
		readPosition += 64;
		// Both the public signing and public encryption keys need to have the 0x04 byte which was stripped off for transmission
		// over the wire added back on to them
		byte[] fourByte = new byte[]{4};
		publicSigningKey = ByteUtils.concatenateByteArrays(fourByte, publicSigningKey); 
		
		byte[] publicEncryptionKey = ArrayCopier.copyOfRange(pubkeyPayload, readPosition, readPosition + 64);
		readPosition += 64;
		publicEncryptionKey = ByteUtils.concatenateByteArrays(fourByte, publicEncryptionKey);
		
		// Set the nonceTrialsPerByte and extraBytes values to the network standard values. If the pubkey address version is 
		// 3 or greater, we will then set these two values to those specified in the pubkey. Otherwise they remain at
		// their default values.
		int nonceTrialsPerByte = NETWORK_NONCE_TRIALS_PER_BYTE;
		int extraBytes = NETWORK_EXTRA_BYTES;
		
		// Set the signature and signature length to some default blank values. Pubkeys of address version 2 and below
		// do not have signatures.
		int signatureLength = EMPTY_SIGNATURE_LENGTH;
		byte[] signature = EMPTY_SIGNATURE;
		
		// Only unencrypted msgs of address version 3 or greater contain
		// values for nonceTrialsPerByte, extraBytes, signatureLength, and
		// signature
		if (pubkeyObject.getObjectVersion() >= 3)
		{
			long[] decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(pubkeyPayload, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
			nonceTrialsPerByte = (int) decoded[0]; // Get the var_int encoded value
			readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
			
			decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(pubkeyPayload, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
			extraBytes = (int) decoded[0]; // Get the var_int encoded value
			readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
		
			decoded = VarintEncoder.decode(ArrayCopier.copyOfRange(pubkeyPayload, readPosition, readPosition + 9)); // Take 9 bytes, the maximum length for an encoded var_int
			signatureLength = (int) decoded[0]; // Get the var_int encoded value
			readPosition += (int) decoded[1]; // Find out how many bytes the var_int was in length and adjust the read position accordingly
			
			signature = (ArrayCopier.copyOfRange(pubkeyPayload, readPosition, readPosition + signatureLength));
		}
				
		// Recalculate the ripe hash of this pubkey so that it can be stored in the database
		byte[] ripeHash = new AddressGenerator().calculateRipeHash(publicSigningKey, publicEncryptionKey);

		Pubkey pubkey = new Pubkey();
		pubkey.setBelongsToMe(false);
		pubkey.setPOWNonce(pubkeyObject.getPOWNonce());
		pubkey.setExpirationTime(pubkeyObject.getExpirationTime());
		pubkey.setObjectType(pubkeyObject.getObjectType());
		pubkey.setObjectVersion(pubkeyObject.getObjectVersion());
		pubkey.setStreamNumber(pubkeyObject.getStreamNumber());
		pubkey.setRipeHash(ripeHash);
		pubkey.setBehaviourBitfield(behaviourBitfield);
		pubkey.setPublicSigningKey(publicSigningKey);
		pubkey.setPublicEncryptionKey(publicEncryptionKey);
		pubkey.setNonceTrialsPerByte(nonceTrialsPerByte);
		pubkey.setExtraBytes(extraBytes);
		pubkey.setSignatureLength(signatureLength);
		pubkey.setSignature(signature);
		
		return pubkey;
	}
	
	/**
	 * Takes a Pubkey and encodes it into a single byte[] (in a way that is compatible
	 * with the way that PyBitmessage does), and does POW for this payload. This payload
	 * can then be sent to a server to be disseminated across the network. <br><br>
	 * 
	 * Note: This method is currently only valid for version 4 pubkeys
	 * 
	 * @param pubkey - An Pubkey object containing the pubkey data used to create
	 * the payload.
	 * @param doPOW - A boolean value indicating whether or not to do POW for this pubkey
	 * 
	 * @return A Payload object containing the pubkey payload
	 */
	public Payload constructPubkeyPayload (Pubkey pubkey, boolean doPOW)
	{
		// Construct the pubkey payload
		byte[] payload = null;
		ByteArrayOutputStream payloadStream = new ByteArrayOutputStream();
		try
		{
			payloadStream.write(ByteUtils.longToBytes(pubkey.getExpirationTime()));
			payloadStream.write(ByteUtils.intToBytes(pubkey.getObjectType()));
			payloadStream.write(VarintEncoder.encode(pubkey.getObjectVersion())); 
			payloadStream.write(VarintEncoder.encode(pubkey.getStreamNumber())); 
			
			// Assemble the pubkey data that will be encrypted
			ByteArrayOutputStream dataToEncryptStream = new ByteArrayOutputStream();
			
			dataToEncryptStream.write(ByteUtils.intToBytes(pubkey.getBehaviourBitfield()));
			
			// If the public signing and public encryption keys have their leading 0x04 byte in place then we need to remove them
			byte[] publicSigningKey = pubkey.getPublicSigningKey();
			if (publicSigningKey[0] == (byte) 4  && publicSigningKey.length == 65)
			{
				publicSigningKey = ArrayCopier.copyOfRange(publicSigningKey, 1, publicSigningKey.length);
			}
			dataToEncryptStream.write(publicSigningKey);
			
			byte[] publicEncryptionKey = pubkey.getPublicEncryptionKey();
			if (publicEncryptionKey[0] == (byte) 4  && publicEncryptionKey.length == 65)
			{
				publicEncryptionKey = ArrayCopier.copyOfRange(publicEncryptionKey, 1, publicEncryptionKey.length);
			}
			dataToEncryptStream.write(publicEncryptionKey);
			
			dataToEncryptStream.write(VarintEncoder.encode(pubkey.getNonceTrialsPerByte()));
			dataToEncryptStream.write(VarintEncoder.encode(pubkey.getExtraBytes()));
			dataToEncryptStream.write(VarintEncoder.encode(pubkey.getSignatureLength()));
			dataToEncryptStream.write(pubkey.getSignature());
			
			// Create the ECPublicKey object that we will use to encrypt the data. First we will
			// retrieve the Address corresponding to this pubkey, so that we can calculate the encryption
			// key derived from the double hash of the address data.
			Address address = AddressProvider.get(App.getContext()).searchForSingleRecord(pubkey.getCorrespondingAddressId());
			String addressString = address.getAddress();
			byte[] encryptionKey = new AddressProcessor().calculateAddressEncryptionKey(addressString);
			ECPublicKey K = new KeyConverter().calculatePublicKeyFromDoubleHashKey(encryptionKey);
			
			// Encrypt the pubkey data
			byte[] dataToEncrypt = dataToEncryptStream.toByteArray();
			byte[] encryptedPayload = new CryptProcessor().encrypt(dataToEncrypt, K);
			
			// Get the tag used to identify the pubkey payload
			byte[] tag = address.getTag();
			
			// Add the tag and the encrypted data to the rest of the pubkey payload
			payloadStream.write(tag);
			payloadStream.write(encryptedPayload);

			payload = payloadStream.toByteArray();
		} 
		catch (IOException e)
		{
			throw new RuntimeException("IOException occurred in PubkeyProcessor.constructPubkeyPayloadForDissemination()", e);
		}
			
		if (doPOW == true)
		{
			long powNonce = new POWProcessor().doPOW(payload, pubkey.getExpirationTime(), POWProcessor.NETWORK_NONCE_TRIALS_PER_BYTE, POWProcessor.NETWORK_EXTRA_BYTES);
			payload = ByteUtils.concatenateByteArrays(ByteUtils.longToBytes(powNonce), payload);
		}
		
		// Create a new Payload object to hold the payload data
		Payload pubkeyPayload = new Payload();
		pubkeyPayload.setRelatedAddressId(pubkey.getCorrespondingAddressId());
		pubkeyPayload.setBelongsToMe(true);
		pubkeyPayload.setPOWDone(doPOW);
		pubkeyPayload.setType(Payload.OBJECT_TYPE_PUBKEY);
		pubkeyPayload.setPayload(payload);
		
		// Save the Payload object to the database
		PayloadProvider payProv = PayloadProvider.get(App.getContext());
		long pubkeyPayloadID = payProv.addPayload(pubkeyPayload);
		
		// Finally, set the pubkey payload's ID to the one generated by the database
		pubkeyPayload.setId(pubkeyPayloadID);
		
		return pubkeyPayload;
	}
}