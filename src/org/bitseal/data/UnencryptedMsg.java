package org.bitseal.data;

/**
 * An object representing an unencrypted Bitmessage msg object. Note that
 * unencrypted msg objects with an address version of 2 or lower do not have
 * values for nonceTrialsPerByte or extraBytes. In these instances those two
 * fields are left empty. <br><br>
 * 
 * For more information see https://bitmessage.org/wiki/Protocol_specification#Unencrypted_Message_Data
 * 
 * @author Jonathan Coe
 */
public class UnencryptedMsg
{
	// These first three fields are for internal use by Bitseal, and are not part of the Bitmessage protocol
	private long mId;
	private boolean mBelongsToMe;
	private long mTime; // If the message belongs to me, this is the time at which it was created. If the messages does not belong to me, this is the time at which it was received.
	
	// All fields below this are part of the data to be encrypted
	private int mMsgVersion;
	private int mAddressVersion; // The sender's address version
	private int mStreamNumber; // The sender's stream number
	private int mBehaviourBitfield; //A bitfield of optional behaviors and features that can be expected from the node with this pubkey. 4 bytes in length, e.g. '\x00\x00\x00\x01'
	private byte[] mPublicSigningKey; // Belongs to the sender of the message (see line 708 of class_singleWorker.py)
	private byte[] mPublicEncryptionKey; // Belongs to the sender of the message (see line 708 of class_singleWorker.py)
	private int mNonceTrialsPerByte;
	private int mExtraBytes;
	private byte[] mDestinationRipe;
	private int mEncoding;
	private int mMessageLength;
	private byte[] mMessage;
	private int mAckLength;
	private byte[] mAckMsg;
	private int mSignatureLength;
	private byte[] mSignature;
	
    public UnencryptedMsg()
    {
    	mTime = System.currentTimeMillis() / 1000; // Gets the current time in seconds
    }
	
	public long getId() 
	{
		
		return mId;
	}
	
	public void setId(long id) 
	{
		mId = id;
	}
	
	public boolean belongsToMe() 
	{
		return mBelongsToMe;
	}
	
	public void setBelongsToMe(boolean belongsToMe) 
	{
		mBelongsToMe = belongsToMe;
	}
	
	public long getTime()
	{
		return mTime;
	}

	public void setTime(long time)
	{
		this.mTime = time;
	}

	public int getMsgVersion() 
	{
		return mMsgVersion;
	}
	
	public void setMsgVersion(int msgVersion) 
	{
		mMsgVersion = msgVersion;
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
	
	public byte[] getDestinationRipe() 
	{
		return mDestinationRipe;
	}
	
	public void setDestinationRipe(byte[] destinationRipe) 
	{
		mDestinationRipe = destinationRipe;
	}
	
	public int getEncoding() 
	{
		return mEncoding;
	}
	
	public void setEncoding(int encoding) 
	{
		mEncoding = encoding;
	}
	
	public int getMessageLength() 
	{
		return mMessageLength;
	}
	
	public void setMessageLength(int messageLength) 
	{
		mMessageLength = messageLength;
	}
	
	public byte[] getMessage() 
	{
		return mMessage;
	}
	
	public void setMessage(byte[] message) 
	{
		mMessage = message;
	}
	
	public int getAckLength() 
	{
		return mAckLength;
	}
	
	public void setAckLength(int ackLength) 
	{
		mAckLength = ackLength;
	}
	
	public byte[] getAckMsg() 
	{
		return mAckMsg;
	}
	
	public void setAckMsg(byte[] ackMsg) 
	{
		mAckMsg = ackMsg;
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
}