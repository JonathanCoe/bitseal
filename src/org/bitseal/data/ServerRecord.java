package org.bitseal.data;

/**
 * An object representing an server record. Includes the URL, username, and password.
 * This should be all the data necessary to access the server.
 * 
 * @author Jonathan Coe
 */
public class ServerRecord
{
	private long mId;
	private String mURL;
	private String mUsername;
	private String mPassword;
	
	public long getId()
	{
		return mId;
	}

	public void setId(long id)
	{
		mId = id;
	}
	
	public String getURL()
	{
		return mURL;
	}

	public void setURL(String url)
	{
		mURL = url;
	}

	public String getUsername()
	{
		return mUsername;
	}

	public void setUsername(String username)
	{
		mUsername = username;
	}

	public String getPassword()
	{
		return mPassword;
	}

	public void setPassword(String password)
	{
		mPassword = password;
	}
}