package org.bitseal.data;

/**
 * An object representing a Bitmessage address. <br><br>
 * 
 * <b>NOTE:</b> This class is intended to only be used for addresses 
 * that belong to the user of the app. Other people's addresses should
 * be represented using the AddressBookRecord class.
 * 
 * @author Jonathan Coe
 */
public class Address
{
	// The first three fields are for internal use by Bitseal, and are not part of the Bitmessage protocol
	private long id;
	private long correspondingPubkeyId;
	private String label;
	
	private String address;
	private String privateSigningKey;
	private String privateEncryptionKey;
	private byte[] ripeHash;
	private byte[] tag;
	
	public long getId() 
	{
		return id;
	}
	public void setId(long id) 
	{
		this.id = id;
	}

	public long getCorrespondingPubkeyId() 
	{
		return correspondingPubkeyId;
	}
	public void setCorrespondingPubkeyId(long correspondingPubkeyId) 
	{
		this.correspondingPubkeyId = correspondingPubkeyId;
	}
	
	public String getLabel() 
	{
		return label;
	}
	public void setLabel(String label) 
	{
		this.label = label;
	}

	public String getAddress() 
	{
		return address;
	}
	public void setAddress(String address) 
	{
		this.address = address;
	}
	
	public String getPrivateSigningKey() 
	{
		return privateSigningKey;
	}
	public void setPrivateSigningKey(String privateSigningKey) 
	{
		this.privateSigningKey = privateSigningKey;
	}

	public String getPrivateEncryptionKey() 
	{
		return privateEncryptionKey;
	}
	public void setPrivateEncryptionKey(String privateEncryptionKey) 
	{
		this.privateEncryptionKey = privateEncryptionKey;
	}

	public byte[] getRipeHash() 
	{
		return ripeHash;
	}
	public void setRipeHash(byte[] ripeHash) 
	{
		this.ripeHash = ripeHash;
	}
	
	public byte[] getTag()
	{
		return tag;
	}
	public void setTag(byte[] tag)
	{
		this.tag = tag;
	}
}