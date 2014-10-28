package org.bitseal.data;

/**
 * Class for a Bitmessage 'pubkey' object.<br><br>
 * 
 * Note: Version 2 pubkeys do NOT have the following fields: <br>
 * nonceTrialsPerByte, extraBytes, signatureLength, signature.<br><br> 
 * 
 * For more information see https://bitmessage.org/wiki/Protocol_specification#pubkey
 * 
 * @author Jonathan Coe
 */
public class Pubkey extends Object
{
	// The first three fields of this class are for internal use by the Bitseal app and are not part of the data to be sent over the wire
	private long correspondingAddressId;
	private byte[] ripeHash; // The ripe hash calculated from the two public keys of this pubkey
	
	private int behaviourBitfield; //A bitfield of optional behaviours and features that can be expected from the node with this pubkey. 4 bytes in length, e.g. '\x00\x00\x00\x01'
	private byte[] publicSigningKey;
	private byte[] publicEncryptionKey;
	private int nonceTrialsPerByte;
	private int extraBytes;
	private int signatureLength;
	private byte[] signature;
    
	public long getCorrespondingAddressId()
	{
		return correspondingAddressId;
	}
	public void setCorrespondingAddressId(long correspondingAddressId) 
	{
		this.correspondingAddressId = correspondingAddressId;
	}

	public byte[] getRipeHash()
	{
		return ripeHash;
	}
	public void setRipeHash(byte[] ripeHash)
	{
		this.ripeHash = ripeHash;
	}

	public int getBehaviourBitfield() 
	{
		return behaviourBitfield;
	}
	public void setBehaviourBitfield(int behaviourBitfield) 
	{
		this.behaviourBitfield = behaviourBitfield;
	}

	public byte[] getPublicSigningKey() 
	{
		return publicSigningKey;
	}
	public void setPublicSigningKey(byte[] publicSigningKey) 
	{
		this.publicSigningKey = publicSigningKey;
	}

	public byte[] getPublicEncryptionKey() 
	{
		return publicEncryptionKey;
	}
	public void setPublicEncryptionKey(byte[] publicEncryptionKey) 
	{
		this.publicEncryptionKey = publicEncryptionKey;
	}

	public int getNonceTrialsPerByte() 
	{
		return nonceTrialsPerByte;
	}
	public void setNonceTrialsPerByte(int nonceTrialsPerByte) 
	{
		this.nonceTrialsPerByte = nonceTrialsPerByte;
	}

	public int getExtraBytes() 
	{
		return extraBytes;
	}
	public void setExtraBytes(int extraBytes) 
	{
		this.extraBytes = extraBytes;
	}

	public int getSignatureLength() 
	{
		return signatureLength;
	}
	public void setSignatureLength(int signatureLength) 
	{
		this.signatureLength = signatureLength;
	}

	public byte[] getSignature() 
	{
		return signature;
	}
	public void setSignature(byte[] signature) 
	{
		this.signature = signature;
	}
}