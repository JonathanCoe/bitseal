package org.bitseal.crypt;

import java.math.BigInteger;

import org.bitseal.core.AddressProcessor;
import org.bitseal.core.App;
import org.bitseal.core.BehaviourBitfieldProcessor;
import org.bitseal.data.Address;
import org.bitseal.data.Pubkey;
import org.bitseal.database.AddressProvider;
import org.bitseal.database.PubkeyProvider;
import org.bitseal.util.TimeUtils;
import org.spongycastle.jce.interfaces.ECPrivateKey;

public class PubkeyGenerator
{
	/** The object type number for pubkeys, as defined by the Bitmessage protocol */
	private static final int OBJECT_TYPE_PUBKEY = 1;
	
	/** The 'time to live' value (in seconds) that we will use when creating new pubkey objects. */
	private static final long PUBKEY_TTL = 604800; // Currently set to 7 days
	
	/** In Bitmessage protocol version 3, the network standard value for nonce trials per byte is 1000. */
	public static final int NETWORK_NONCE_TRIALS_PER_BYTE = 1000;
	
	/** In Bitmessage protocol version 3, the network standard value for extra bytes is 1000. */
	public static final int NETWORK_EXTRA_BYTES = 1000;
	
	/**
	 * Generates a new Pubkey object for the given Address and saves it
	 * to the app's database. Also updates the "correspondingPubkeyId" field
	 * of the given Address and updates that Address's record in the database.
	 * 
	 * @param address - The Address object to generate a Pubkey for
	 * 
	 * @return A Pubkey object for the given Address
	 */
	public Pubkey generateAndSaveNewPubkey(Address address)
	{
		// Generate a new pubkey
		Pubkey pubkey = generateNewPubkey(address);
		
		// Save the new pubkey to the database
		PubkeyProvider pubProv = PubkeyProvider.get(App.getContext());
		long pubkeyId = pubProv.addPubkey(pubkey);
		pubkey.setId(pubkeyId);
		
		// Finally, set the "correspondingPubkeyId" of the Address we started with to match the ID of the Pubkey we have just generated
		AddressProvider addProv = AddressProvider.get(App.getContext());
		// Note: We retrieve the Address from the database rather than using the one passed to this method so that any changes
		// that have been made to the address since this method began (e.g. the user updating the address's label) will not be lost.
		Address addressFromDatabase = addProv.searchForSingleRecord(address.getId());
		addressFromDatabase.setCorrespondingPubkeyId(pubkeyId);
		addProv.updateAddress(addressFromDatabase);
		
		return pubkey;
	}
	
	/**
	 * Generates a Pubkey object for a given Address object. Used to create a new pubkey
	 * for one of my addresses. <br><br>
	 * 
	 * @param address - The Address object for which we want to generate a Pubkey
	 * 
	 * @return A Pubkey object corresponding to the given Address object. 
	 */
	private Pubkey generateNewPubkey(Address address)
	{		
		// Derive the public keys from the private keys of the address
		KeyConverter keyConv = new KeyConverter();
		
		ECPrivateKey privateSigningKey = keyConv.decodePrivateKeyFromWIF(address.getPrivateSigningKey());
		ECPrivateKey privateEncryptionKey = keyConv.decodePrivateKeyFromWIF(address.getPrivateEncryptionKey());
		
		BigInteger privateSigningKeyDValue = privateSigningKey.getD();
		BigInteger privateEncryptionKeyDValue = privateEncryptionKey.getD();
		
		ECKeyPair signingKeyPair = new ECKeyPair(privateSigningKeyDValue);
		ECKeyPair encryptionKeyPair = new ECKeyPair(privateEncryptionKeyDValue);
		
		byte[] publicSigningKey = signingKeyPair.getPubKey();
		byte[] publicEncryptionKey = encryptionKeyPair.getPubKey();
		
		byte[] ripeHash = address.getRipeHash();
		
		// Get the address version and stream number
		AddressProcessor addProc = new AddressProcessor();
		int[] decoded = addProc.decodeAddressNumbers(address.getAddress());
		int addressVersion = decoded[0];
		int streamNumber = decoded[1];
		
    	// Set the Behaviour Bitfield.
    	int behaviourBitfield = BehaviourBitfieldProcessor.getBitfieldForMyPubkeys();
		
		// Create a new Pubkey object and populate its fields. 
		Pubkey pubkey = new Pubkey();
		
		// Work out the 'end of life time' value to use
		long expirationTime = TimeUtils.getFuzzedExpirationTime(PUBKEY_TTL);
	
		pubkey.setCorrespondingAddressId(address.getId());
		pubkey.setBelongsToMe(true);
		pubkey.setRipeHash(ripeHash);
		pubkey.setExpirationTime(expirationTime);
		pubkey.setObjectType(OBJECT_TYPE_PUBKEY);
		pubkey.setObjectVersion(addressVersion);
		pubkey.setStreamNumber(streamNumber);
		pubkey.setBehaviourBitfield(behaviourBitfield);
		pubkey.setPublicSigningKey(publicSigningKey);
		pubkey.setPublicEncryptionKey(publicEncryptionKey);
		pubkey.setNonceTrialsPerByte(NETWORK_NONCE_TRIALS_PER_BYTE);   
		pubkey.setExtraBytes(NETWORK_EXTRA_BYTES);
		
		// Generate the signature for this pubkey
		SigProcessor sigProc = new SigProcessor();
		byte[] signaturePayload = sigProc.createPubkeySignaturePayload(pubkey);
		byte[] signature = sigProc.signWithWIFKey(signaturePayload, address.getPrivateSigningKey()); 
		pubkey.setSignature(signature);
		pubkey.setSignatureLength(signature.length);
		
		return pubkey;
	}
}