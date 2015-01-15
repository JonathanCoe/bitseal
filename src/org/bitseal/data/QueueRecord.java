package org.bitseal.data;

/**
 * Class for an object that serves as a record in the queue of tasks to be done by the 
 * application. <br><br> 
 * 
 * Each QueueRecord can store references to up to two items in 
 * the database (e.g. a msg payload to send and the destination pubkey). 
 * 
 * @author Jonathan Coe
 */
public class QueueRecord implements Comparable<QueueRecord>
{
	/** The unique ID number of this QueueRecord" */
	private long id;
	
	/** The task that this record refers to - e.g. "sendMsg" */
	private String task;
	
	/** A Unix time value (in seconds). If this is set to a time in the future, this QueueRecord will not 
	 * be processed until that time. This value can also be set to zero, in which case the QueueRecord will be processed immediately. */
	private long triggerTime;
	
	/** 
	 * The number of QueueRecords that have already been created for the task referred to. This is done because some tasks
	 * may need to be completed several times. <br><br>
	 * 
	 * For example, when we send a message, if we do not receive the acknowledgement for that message before its time to
	 * live has expired, we will need to send that message again until we receive the acknowledgement. 
	 * */
	private int recordCount;
	
	/** The last time that the task referred to by this record was attempted */
	private long lastAttemptTime;
	/** The number of times that we have attempted to process this particular QueueRecord. Note that this is separate from the 'record count'. */
	private int attempts;
	
	/** The id number of the first object that is to be processed, if any */
	private long object0Id;
	/** The type of the first object that this record refers to - e.g. "Pubkey" or "UnencryptedsMsg" */
	private String object0Type;
	
	/** The id number of the second object that is to be processed, if any */
	private long object1Id;
	/** The type of the second object that this record refers to - e.g. "Pubkey" or "UnencryptedsMsg" */
	private String object1Type;
	
	/** The id number of the third object that is to be processed, if any */
	private long object2Id;
	/** The type of the third object that this record refers to - e.g. "Pubkey" or "UnencryptedsMsg" */
	private String object2Type;
	
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
	     return (int) (lastAttemptTime - q.getLastAttemptTime());
	}
	
	public long getId()
	{
		return id;
	}
	public void setId(long mId)
	{
		this.id = mId;
	}
	
	public String getTask()
	{
		return task;
	}
	public void setTask(String task)
	{
		this.task = task;
	}
	
	public long getTriggerTime()
	{
		return triggerTime;
	}
	public void setTriggerTime(long triggerTime)
	{
		this.triggerTime = triggerTime;
	}

	public int getRecordCount()
	{
		return recordCount;
	}
	public void setRecordCount(int recordCount)
	{
		this.recordCount = recordCount;
	}

	public long getLastAttemptTime()
	{
		return lastAttemptTime;
	}
	public void setLastAttemptTime(long lastAttemptTime)
	{
		this.lastAttemptTime = lastAttemptTime;
	}
	
	public int getAttempts()
	{
		return attempts;
	}
	public void setAttempts(int attempts)
	{
		this.attempts = attempts;
	}
	
	public long getObject0Id()
	{
		return object0Id;
	}
	
	public void setObject0Id(long id)
	{
		this.object0Id = id;
	}

	public String getObject0Type()
	{
		return object0Type;
	}
	public void setObject0Type(String type)
	{
		this.object0Type = type;
	}
	
	public long getObject1Id()
	{
		return object1Id;
	}
	public void setObject1Id(long id)
	{
		this.object1Id = id;
	}

	public String getObject1Type()
	{
		return object1Type;
	}
	public void setObject1Type(String type)
	{
		this.object1Type = type;
	}
	
	public long getObject2Id()
	{
		return object2Id;
	}
	public void setObject2Id(long id)
	{
		this.object2Id = id;
	}

	public String getObject2Type()
	{
		return object2Type;
	}
	public void setObject2Type(String type)
	{
		this.object2Type = type;
	}
}