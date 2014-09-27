package org.bitseal.data;

/**
 * An object representing a plaintext message. 
 * 
 * @author Jonathan Coe
 */
public class Message implements Comparable<Message>
{
	private long mId;
	private long mMsgPayloadId; // The ID of the msg payload for this Message
	private long mAckPayloadId; // The ID of the ack payload for this Message
	private boolean mBelongsToMe;
	private boolean mRead; // Indicates whether or not the user of the app has read (opened) the message
	private String mStatus;
	private long mTime; // If the message belongs to me, this is the time at which it was created. If the messages does not belong to me, this is the time at which it was received.
	private String mToAddress;
	private String mFromAddress;
	private String mSubject;
	private String mBody;
	
	public static final String STATUS_REQUESTING_PUBKEY = "Retrieving encryption keys";
	public static final String STATUS_CONSTRUCTING_PAYLOAD = "Constructing message payload";
	public static final String STATUS_DOING_ACK_POW = "Doing proof of work for the acknowledgement";
	public static final String STATUS_ENCRYPTING_MESSAGE = "Encrypting the message";
	public static final String STATUS_DOING_POW = "Doing proof of work for the message";
	public static final String STATUS_SENDING_MESSAGE = "Sending message";
	public static final String STATUS_MSG_SENT = "Message sent, waiting for acknowledgment";
	public static final String STATUS_MSG_SENT_NO_ACK_EXPECTED = "Message sent, no acknowledgment expected";
	public static final String STATUS_ACK_RECEIVED = "Acknowledgment received";
	public static final String STATUS_SENDING_FAILED = "Failed to sent the message";
	
	public Message()
	{
		mRead = false; // The default value for the "read" field should be false
		mTime = System.currentTimeMillis() / 1000; // Gets the current time in seconds
	}
	
	/**
	 * Used to sort Messages by time, giving the most recent first
	 */
	@Override
	public int compareTo(Message m)
	{
	     return (int) (m.getTime() - mTime);
	}
	
	public long getId() 
	{
		return mId;
	}
	
	public void setId(long id) 
	{
		mId = id;
	}
	
	public long getMsgPayloadId()
	{
		return mMsgPayloadId;
	}
	
	public void setMsgPayloadId(long msgPayloadId) 
	{
		mMsgPayloadId = msgPayloadId;
	}
	
	public long getAckPayloadId()
	{
		return mAckPayloadId;
	}

	public void setAckPayloadId(long ackPayloadId)
	{
		mAckPayloadId = ackPayloadId;
	}

	public boolean belongsToMe() 
	{
		return mBelongsToMe;
	}
	
	public void setBelongsToMe(boolean belongsToMe) 
	{
		mBelongsToMe = belongsToMe;
	}
	
	public boolean hasBeenRead()
	{
		return mRead;
	}

	public void setRead(boolean read)
	{
		mRead = read;
	}

	public String getStatus()
	{
		return mStatus;
	}
	
	public void setStatus(String status)
	{
		mStatus = status;
	}

	public long getTime()
	{
		return mTime;
	}

	public void setTime(long time)
	{
		mTime = time;
	}

	public String getToAddress()
	{
		return mToAddress;
	}

	public void setToAddress(String toAddress)
	{
		mToAddress = toAddress;
	}

	public String getFromAddress()
	{
		return mFromAddress;
	}

	public void setFromAddress(String fromAddress)
	{
		mFromAddress = fromAddress;
	}

	public String getSubject()
	{
		return mSubject;
	}

	public void setSubject(String subject)
	{
		mSubject = subject;
	}

	public String getBody()
	{
		return mBody;
	}

	public void setBody(String body)
	{
		mBody = body;
	}
}