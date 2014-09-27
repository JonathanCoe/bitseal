package org.bitseal.crypt;

import java.math.BigInteger;

import org.bitseal.core.AddressProcessor;
import org.bitseal.core.App;
import org.bitseal.core.BehaviourBitfieldProcessor;
import org.bitseal.data.Address;
import org.bitseal.data.Pubkey;
import org.bitseal.database.AddressProvider;
import org.bitseal.database.PubkeyProvider;
import org.spongycastle.jce.interfaces.ECPrivateKey;

public class PubkeyGenerator
{
	private static final int DEFAULT_NONCE_TRIALS_PER_BYTE = 320;
	private static final int DEFAUlT_EXTRA_BYTES = 14000;
	
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
	
		pubkey.setCorrespondingAddressId(address.getId());	
		pubkey.setBelongsToMe(true);
		pubkey.setRipeHash(ripeHash);
		pubkey.setAddressVersion(addressVersion);
		pubkey.setStreamNumber(streamNumber);
		pubkey.setBehaviourBitfield(behaviourBitfield);
		pubkey.setPublicSigningKey(publicSigningKey);
		pubkey.setPublicEncryptionKey(publicEncryptionKey);
		pubkey.setNonceTrialsPerByte(DEFAULT_NONCE_TRIALS_PER_BYTE);   
		pubkey.setExtraBytes(DEFAUlT_EXTRA_BYTES);
		
		// Generate the signature for this pubkey
		SigProcessor sigProc = new SigProcessor();
		byte[] signaturePayload = sigProc.createPubkeySignaturePayload(pubkey);
		byte[] signature = sigProc.signWithWIFKey(signaturePayload, address.getPrivateSigningKey()); 
		pubkey.setSignature(signature);
		pubkey.setSignatureLength(signature.length);
		
		return pubkey;
	}
}