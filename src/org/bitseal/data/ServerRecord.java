package org.bitseal.data;

/**
 * Class for an object representing a server record. Includes the URL, username, and password.
 * This should be all the data necessary to access the server.
 * 
 * @author Jonathan Coe
 */
public class ServerRecord
{
	private long id;
	private String url;
	private String username;
	private String password;
	
	public long getId()
	{
		return id;
	}
	public void setId(long id)
	{
		this.id = id;
	}
	
	public String getURL()
	{
		return url;
	}
	public void setURL(String url)
	{
		this.url = url;
	}

	public String getUsername()
	{
		return username;
	}
	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getPassword()
	{
		return password;
	}
	public void setPassword(String password)
	{
		this.password = password;
	}
}