package org.bitseal.data;

/**
 * An object representing a Bitmessage address. <br><br>
 * 
 * <b>NOTE:</b> This class is intended to only be used for addresses 
 * that belong to the user of the app.
 * 
 * @author Jonathan Coe
 */
public class Address
{
	// The first four fields are for internal use by Bitseal, and are not part of the Bitmessage protocol
	private long mId;
	private long mCorrespondingPubkeyId;
	private String mLabel;
	
	private String mAddress;
	private String mPrivateSigningKey;
	private String mPrivateEncryptionKey;
	private byte[] mRipeHash;
	private byte[] mTag;
	
	public long getId() 
	{
		return mId;
	}
	
	public void setId(long id) 
	{
		mId = id;
	}

	public long getCorrespondingPubkeyId() 
	{
		return mCorrespondingPubkeyId;
	}

	public void setCorrespondingPubkeyId(long correspondingPubkeyId) 
	{
		mCorrespondingPubkeyId = correspondingPubkeyId;
	}
	
	public String getLabel() 
	{
		return mLabel;
	}

	public void setLabel(String label) 
	{
		mLabel = label;
	}

	public String getAddress() 
	{
		return mAddress;
	}

	public void setAddress(String address) 
	{
		mAddress = address;
	}

	public String getPrivateSigningKey() 
	{
		return mPrivateSigningKey;
	}

	public void setPrivateSigningKey(String privateSigningKey) 
	{
		mPrivateSigningKey = privateSigningKey;
	}

	public String getPrivateEncryptionKey() 
	{
		return mPrivateEncryptionKey;
	}

	public void setPrivateEncryptionKey(String privateEncryptionKey) 
	{
		mPrivateEncryptionKey = privateEncryptionKey;
	}

	public byte[] getRipeHash() 
	{
		return mRipeHash;
	}

	public void setRipeHash(byte[] ripeHash) 
	{
		mRipeHash = ripeHash;
	}

	public byte[] getTag()
	{
		return mTag;
	}

	public void setTag(byte[] tag)
	{
		mTag = tag;
	}
}