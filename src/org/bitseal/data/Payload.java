package org.bitseal.data;

/**
 * Contains the payload of a single Bitmessage object, along with some related data. 
 * 
 * @author Jonathan Coe
 */
public class Payload
{
	private long mId;
	private long mRelatedAddressId;
	private boolean mBelongsToMe;
	private boolean mProcessingComplete;
	private long mTime;
	private String mType;
	private boolean mPOWDone;
	private byte[] mPayload;
	
	public static final String OBJECT_TYPE_MSG = "msg";
	public static final String OBJECT_TYPE_ACK = "ack";
	public static final String OBJECT_TYPE_PUBKEY = "pubkey";
	public static final String OBJECT_TYPE_GETPUBKEY = "getpubkey";
	
    public Payload()
    {
    	mTime = System.currentTimeMillis() / 1000; // The current time in seconds
    }

	public long getId() 
	{
		return mId;
	}

	public void setId(long id) 
	{
		mId = id;
	}

	public long getRelatedAddressId()
	{
		return mRelatedAddressId;
	}

	public void setRelatedAddressId(long relatedAddressId)
	{
		mRelatedAddressId = relatedAddressId;
	}

	public boolean belongsToMe()
	{
		return mBelongsToMe;
	}

	public void setBelongsToMe(boolean belongsToMe)
	{
		mBelongsToMe = belongsToMe;
	}

	public boolean processingComplete()
	{
		return mProcessingComplete;
	}

	public void setProcessingComplete(boolean processingComplete)
	{
		mProcessingComplete = processingComplete;
	}

	public long getTime() 
	{
		return mTime;
	}

	public void setTime(long time) 
	{
		mTime = time;
	}
	
	public String getType()
	{
		return mType;
	}
	
	public void setType(String type)
	{
		mType = type;
	}

	public boolean powDone()
	{
		return mPOWDone;
	}

	public void setPOWDone(boolean powDone)
	{
		mPOWDone = powDone;
	}

	public byte[] getPayload()
	{
		return mPayload;
	}

	public void setPayload(byte[] payload)
	{
		mPayload = payload;
	}
}