package org.bitseal.data;

import java.util.Random;

/**
 * An object representing an encrypted Bitmessage msg object.
 * 
 * @author Jonathan Coe
 */
public class Msg
{
	// These first two fields are internal use by Bitseal, and are not part of the Bitmessage protocol
	private long mId;
	private boolean mBelongsToMe;
	
	private long mPOWNonce;
	private long mTime; // The time at which this object was created
	private int mStreamNumber;
	private byte[] mMessageData;
	
    public Msg()
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

	public boolean belongsToMe() 
	{
		return mBelongsToMe;
	}

	public void setBelongsToMe(boolean belongsToMe) 
	{
		mBelongsToMe = belongsToMe;
	}

	public long getPOWNonce() 
	{
		return mPOWNonce;
	}

	public void setPOWNonce(long pOWNonce) 
	{
		mPOWNonce = pOWNonce;
	}

	public long getTime() 
	{
		return mTime;
	}

	public void setTime(long time) 
	{
		mTime = time;
	}

	public int getStreamNumber() 
	{
		return mStreamNumber;
	}

	public void setStreamNumber(int streamNumber) 
	{
		mStreamNumber = streamNumber;
	}

	public byte[] getMessageData() 
	{
		return mMessageData;
	}

	public void setMessageData(byte[] messageData)
	{
		mMessageData = messageData;
	}
}