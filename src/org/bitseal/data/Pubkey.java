package org.bitseal.data;

import java.util.Random;

/**
 * A class representing a pubkey in the Bitmessage protocol. Note that version 2 pubkeys do not
 * have the fields nonceTrialsPerByte, extraBytes, signatureLength, and signature.
 * 
 * For more information see https://bitmessage.org/wiki/Protocol_specification#pubkey
 * 
 * @author Jonathan Coe
 */
public class Pubkey
{
	// The first four fields of this class are for internal use by the Bitseal app and are not part of the data to be sent over the wire
	private long mId;
	private long mCorrespondingAddressId;
	private boolean mBelongsToMe;
	private byte[] mRipeHash; // The ripe hash calculated from the two public keys of this pubkey
	private long mLastDisseminationTime; // The last time at which this pubkey was successfully disseminated to the Bitmessage network
	
	private long mPOWNonce;
	private long mTime; // The time that this pubkey was generated
	private int mAddressVersion;
	private int mStreamNumber;
	private int mBehaviourBitfield; //A bitfield of optional behaviors and features that can be expected from the node with this pubkey. 4 bytes in length, e.g. '\x00\x00\x00\x01'
	private byte[] mPublicSigningKey;
	private byte[] mPublicEncryptionKey;
	private int mNonceTrialsPerByte;
	private int mExtraBytes;
	private int mSignatureLength;
	private byte[] mSignature;
	    
    public Pubkey()
    {
    	mTime = System.currentTimeMillis() / 1000; // Gets the current time in seconds
    	int timeModifier = (new Random().nextInt(600)) - 300;
    	mTime = mTime + timeModifier; // Gives us the current time plus or minus 300 seconds (five minutes). This is also done by PyBitmessage. 
    }
    
	public long getId() 
	{
		return mId;
	}
	
	public void setId(long id) 
	{
		mId = id;
	}

	public long getCorrespondingAddressId()
	{
		return mCorrespondingAddressId;
	}

	public void setCorrespondingAddressId(long correspondingAddressId) 
	{
		mCorrespondingAddressId = correspondingAddressId;
	}

	public long getPOWNonce() 
	{
		return mPOWNonce;
	}

	public void setPOWNonce(long POWNonce) 
	{
		mPOWNonce = POWNonce;
	}

	public boolean belongsToMe() 
	{
		return mBelongsToMe;
	}

	public void setBelongsToMe(boolean belongsToMe) 
	{
		mBelongsToMe = belongsToMe;
	}

	public byte[] getRipeHash()
	{
		return mRipeHash;
	}

	public void setRipeHash(byte[] ripeHash)
	{
		this.mRipeHash = ripeHash;
	}

	public long getTime() 
	{
		return mTime;
	}

	public void setTime(long time) 
	{
		mTime = time;
	}

	public int getAddressVersion() 
	{
		return mAddressVersion;
	}

	public void setAddressVersion(int addressVersion) 
	{
		mAddressVersion = addressVersion;
	}

	public int getStreamNumber() 
	{
		return mStreamNumber;
	}

	public void setStreamNumber(int streamNumber) 
	{
		mStreamNumber = streamNumber;
	}

	public int getBehaviourBitfield() 
	{
		return mBehaviourBitfield;
	}

	public void setBehaviourBitfield(int behaviourBitfield) 
	{
		mBehaviourBitfield = behaviourBitfield;
	}

	public byte[] getPublicSigningKey() 
	{
		return mPublicSigningKey;
	}

	public void setPublicSigningKey(byte[] publicSigningKey) 
	{
		mPublicSigningKey = publicSigningKey;
	}

	public byte[] getPublicEncryptionKey() 
	{
		return mPublicEncryptionKey;
	}

	public void setPublicEncryptionKey(byte[] publicEncryptionKey) 
	{
		mPublicEncryptionKey = publicEncryptionKey;
	}

	public int getNonceTrialsPerByte() 
	{
		return mNonceTrialsPerByte;
	}

	public void setNonceTrialsPerByte(int nonceTrialsPerByte) 
	{
		mNonceTrialsPerByte = nonceTrialsPerByte;
	}

	public int getExtraBytes() 
	{
		return mExtraBytes;
	}

	public void setExtraBytes(int extraBytes) 
	{
		mExtraBytes = extraBytes;
	}

	public int getSignatureLength() 
	{
		return mSignatureLength;
	}

	public void setSignatureLength(int signatureLength) 
	{
		mSignatureLength = signatureLength;
	}

	public byte[] getSignature() 
	{
		return mSignature;
	}

	public void setSignature(byte[] signature) 
	{
		mSignature = signature;
	}

	public long getLastDisseminationTime()
	{
		return mLastDisseminationTime;
	}

	public void setLastDisseminationTime(long lastDisseminationTime)
	{
		mLastDisseminationTime = lastDisseminationTime;
	}
}