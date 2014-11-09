package org.bitseal.data;

/**
 * Represents an 'Object' in the Bitmessage protocol. Named this way
 * to distinguish it from java.lang.Object <br><br>
 * 
 * See https://bitmessage.org/wiki/Protocol_specification#objects
 * 
 * @author Jonathan Coe
 */
public class BMObject
{
	// These first two fields are internal use by Bitseal, and are not part of the Bitmessage protocol
	private long id;
	private boolean belongsToMe;
	
	private long powNonce;
	private long expirationTime;
	private int objectType;
	private int objectVersion;
	private int streamNumber;
	private byte[] payload;
	
	public long getId() 
	{
		return id;
	}
	public void setId(long id) 
	{
		this.id = id;
	}

	public boolean belongsToMe() 
	{
		return belongsToMe;
	}
	public void setBelongsToMe(boolean belongsToMe) 
	{
		this.belongsToMe = belongsToMe;
	}
	
	public long getPOWNonce()
	{
		return powNonce;
	}
	public void setPOWNonce(long powNonce)
	{
		this.powNonce = powNonce;
	}
	
	public long getExpirationTime()
	{
		return expirationTime;
	}
	public void setExpirationTime(long expirationTime)
	{
		this.expirationTime = expirationTime;
	}
	
	public int getObjectType()
	{
		return objectType;
	}
	public void setObjectType(int objectType)
	{
		this.objectType = objectType;
	}
	
	public int getObjectVersion()
	{
		return objectVersion;
	}
	public void setObjectVersion(int objectVersion)
	{
		this.objectVersion = objectVersion;
	}
	
	public int getStreamNumber()
	{
		return streamNumber;
	}
	public void setStreamNumber(int streamNumber)
	{
		this.streamNumber = streamNumber;
	}
	
	public byte[] getPayload()
	{
		return payload;
	}
	public void setPayload(byte[] payload)
	{
		this.payload = payload;
	}
}