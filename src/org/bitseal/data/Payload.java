package org.bitseal.data;

/**
 * Class for an object containing the payload of a single Bitmessage object,
 * along with some related data. 
 * 
 * @author Jonathan Coe
 */
public class Payload
{
	private long id;
	private long relatedAddressId;
	private boolean belongsToMe;
	private boolean processingComplete;
	private long time; // The time at which this this Payload was created
	private String type;
	private boolean ack; // Whether the object contained in this Payload should be treated as an acknowledgment
	private boolean powDone;
	private byte[] payload;
	
	public static final String OBJECT_TYPE_MSG = "msg";
	public static final String OBJECT_TYPE_PUBKEY = "pubkey";
	public static final String OBJECT_TYPE_GETPUBKEY = "getpubkey";
	public static final String OBJECT_TYPE_BROADCAST = "broadcast";
	
    public Payload()
    {
    	time = System.currentTimeMillis() / 1000; // The current time in seconds
    }

	public long getId() 
	{
		return id;
	}
	public void setId(long id) 
	{
		this.id = id;
	}

	public long getRelatedAddressId()
	{
		return relatedAddressId;
	}
	public void setRelatedAddressId(long relatedAddressId)
	{
		this.relatedAddressId = relatedAddressId;
	}

	public boolean belongsToMe()
	{
		return belongsToMe;
	}
	public void setBelongsToMe(boolean belongsToMe)
	{
		this.belongsToMe = belongsToMe;
	}

	public boolean processingComplete()
	{
		return processingComplete;
	}
	public void setProcessingComplete(boolean processingComplete)
	{
		this.processingComplete = processingComplete;
	}

	public long getTime() 
	{
		return time;
	}
	public void setTime(long time) 
	{
		this.time = time;
	}
	
	public String getType()
	{
		return type;
	}
	public void setType(String type)
	{
		this.type = type;
	}
	
	public boolean isAck()
	{
		return ack;
	}
	public void setAck(boolean ack)
	{
		this.ack = ack;
	}

	public boolean powDone()
	{
		return powDone;
	}
	public void setPOWDone(boolean powDone)
	{
		this.powDone = powDone;
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