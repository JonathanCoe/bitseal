package org.bitseal.data;

/**
 * An object representing a plain-text message.<br><br>
 * 
 * <b>NOTE:</b> This class should not be confused with the 'Message' data
 * structure defined in the Bitmessage protocol. This class is for internal
 * use only and is NOT part of the Bitmessage protocol. 
 * 
 * @author Jonathan Coe
 */
public class Message implements Comparable<Message>
{
	private long id;
	private long msgPayloadId; // The ID of the msg payload for this Message
	private long ackPayloadId; // The ID of the ack payload for this Message
	private boolean belongsToMe;
	private boolean read; // Indicates whether or not the user of the app has read (opened) the message
	private String status;
	private long time; // The time at which this Message object was created
	private String toAddress;
	private String fromAddress;
	private String subject;
	private String body;
	
	// Status messages
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
		read = false; // The default value for the "read" field should be false
		time = System.currentTimeMillis() / 1000; // Gets the current time in seconds
	}
	
	/**
	 * Used to sort Messages by time, giving the most recent first
	 */
	@Override
	public int compareTo(Message m)
	{
	     return (int) (m.getTime() - time);
	}
	
	public long getId() 
	{
		return id;
	}
	public void setId(long id) 
	{
		this.id = id;
	}
	
	public long getMsgPayloadId()
	{
		return msgPayloadId;
	}
	public void setMsgPayloadId(long msgPayloadId) 
	{
		this.msgPayloadId = msgPayloadId;
	}
	
	public long getAckPayloadId()
	{
		return ackPayloadId;
	}
	public void setAckPayloadId(long ackPayloadId)
	{
		this.ackPayloadId = ackPayloadId;
	}

	public boolean belongsToMe() 
	{
		return belongsToMe;
	}
	public void setBelongsToMe(boolean belongsToMe) 
	{
		this.belongsToMe = belongsToMe;
	}
	
	public boolean hasBeenRead()
	{
		return read;
	}
	public void setRead(boolean read)
	{
		this.read = read;
	}

	public String getStatus()
	{
		return status;
	}
	public void setStatus(String status)
	{
		this.status = status;
	}

	public long getTime()
	{
		return time;
	}
	public void setTime(long time)
	{
		this.time = time;
	}

	public String getToAddress()
	{
		return toAddress;
	}
	public void setToAddress(String toAddress)
	{
		this.toAddress = toAddress;
	}

	public String getFromAddress()
	{
		return fromAddress;
	}
	public void setFromAddress(String fromAddress)
	{
		this.fromAddress = fromAddress;
	}

	public String getSubject()
	{
		return subject;
	}
	public void setSubject(String subject)
	{
		this.subject = subject;
	}

	public String getBody()
	{
		return body;
	}
	public void setBody(String body)
	{
		this.body = body;
	}
}