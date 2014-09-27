package org.bitseal.data;

/**
 * A class that serves as a record in the queue of tasks to be done by the 
 * application. <br><br> 
 * 
 * Each QueueRecord can store references to up to two items in 
 * the database (e.g. a msg payload to send and the destination Pubkey). 
 * 
 * @author Jonathan Coe
 */
public class QueueRecord implements Comparable<QueueRecord>
{
	/** The unique ID number of this QueueRecord" */
	private long mId;
	
	/** The task that this record refers to - e.g. "sendMsg" */
	private String mTask;
	
	/** The last time that the task referred to by this record was attempted */
	private long mLastAttemptTime;
	
	/** The number of times that the task this record refers to has been attempted */
	private int mAttempts;
	
	/** The id number of the first object that is to be processed, if any */
	private long mObject0Id;
	
	/** The type of the first object that this record refers to - e.g. "Pubkey" or "UnencryptedsMsg" */
	private String mObject0Type;
	
	/** The id number of the second object that is to be processed, if any */
	private long mObject1Id;
	
	/** The type of the second object that this record refers to - e.g. "Pubkey" or "UnencryptedsMsg" */
	private String mObject1Type;
	
	// Constant values for the "Object Type" Strings in QueueRecords
	public static final String QUEUE_RECORD_OBJECT_TYPE_ADDRESS = "Address";
	public static final String QUEUE_RECORD_OBJECT_TYPE_MESSAGE = "Message";
	public static final String QUEUE_RECORD_OBJECT_TYPE_PUBKEY = "Pubkey";
	public static final String QUEUE_RECORD_OBJECT_TYPE_PAYLOAD = "Payload";
	
	/**
	 * Used to sort QueueRecords by time
	 */
	@Override
	public int compareTo(QueueRecord q)
	{
	     return (int) (mLastAttemptTime - q.getLastAttemptTime());
	}
	
	public long getId()
	{
		return mId;
	}
	public void setId(long mId)
	{
		this.mId = mId;
	}
	
	public String getTask()
	{
		return mTask;
	}
	public void setTask(String task)
	{
		this.mTask = task;
	}
	
	public long getLastAttemptTime()
	{
		return mLastAttemptTime;
	}
	public void setLastAttemptTime(long lastAttemptTime)
	{
		this.mLastAttemptTime = lastAttemptTime;
	}
	
	public int getAttempts()
	{
		return mAttempts;
	}
	public void setAttempts(int attempts)
	{
		this.mAttempts = attempts;
	}
	
	public long getObject0Id()
	{
		return mObject0Id;
	}
	
	public void setObject0Id(long id)
	{
		this.mObject0Id = id;
	}

	public String getObject0Type()
	{
		return mObject0Type;
	}
	public void setObject0Type(String type)
	{
		this.mObject0Type = type;
	}
	
	public long getObject1Id()
	{
		return mObject1Id;
	}
	public void setObject1Id(long id)
	{
		this.mObject1Id = id;
	}

	public String getObject1Type()
	{
		return mObject1Type;
	}
	public void setObject1Type(String type)
	{
		this.mObject1Type = type;
	}
}