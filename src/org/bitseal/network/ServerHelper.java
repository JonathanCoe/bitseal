package org.bitseal.network;

import java.io.IOException;
import java.io.InputStream;

import org.bitseal.core.App;
import org.bitseal.data.ServerRecord;
import org.bitseal.database.ServerRecordProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * Provides helper methods related to Bitseal servers. 
 * 
 * @author Jonathan Coe
 */
public class ServerHelper
{
	// Constants related to the XML file containing the server records
	private static final String DEFAULT_SERVERS_XML_PATH = "DefaultServers.xml";
	private static final String DEFAULT_SERVERS_XML_ENCODING = "UTF-8";
	private static final String DEFAULT_SERVERS_XML_START_TAG = "DefaultServers";
	private static final String SERVER_RECORD_XML_START_TAG = "ServerRecord";
	private static final String SERVER_RECORD_URL_TAG = "url";
	private static final String SERVER_RECORD_USERNAME_TAG = "username";
	private static final String SERVER_RECORD_PASSWORD_TAG = "password";
	
	/**
	 * Sets up a default set of servers, saving them to the application's database
	 */
	public void setupDefaultServers()
	{
		try
		{
			XmlPullParserFactory factory;
			factory = XmlPullParserFactory.newInstance();
	        factory.setNamespaceAware(true);
	        XmlPullParser parser = factory.newPullParser();
	        InputStream inputStream = App.getContext().getAssets().open(DEFAULT_SERVERS_XML_PATH);
	        parser.setInput(inputStream, DEFAULT_SERVERS_XML_ENCODING);
	        int eventType = parser.getEventType();
	        String name = null;
	        ServerRecord currentServerRecord = null;
	        
	        while (eventType != XmlPullParser.END_DOCUMENT) 
	        {
				if(eventType == XmlPullParser.START_TAG) 
				{
					name = parser.getName();
					
                    if (name.equals(SERVER_RECORD_XML_START_TAG))
                    {
                    	currentServerRecord = new ServerRecord();
                    } 
                    else if (currentServerRecord != null)
                    {
                        if (name.equals(SERVER_RECORD_URL_TAG))
                        {
                        	currentServerRecord.setURL(parser.nextText());
                        } 
                        else if (name.equals(SERVER_RECORD_USERNAME_TAG))
                        {
                        	currentServerRecord.setUsername(parser.nextText());
                        } 
                        else if (name.equals(SERVER_RECORD_PASSWORD_TAG))
                        {
                        	currentServerRecord.setPassword(parser.nextText());
                        }  
                    }
				} 
				else if(eventType == XmlPullParser.END_TAG) 
				{
                    name = parser.getName();
                    if (name.equals(DEFAULT_SERVERS_XML_START_TAG))
                    {
                    	break;
                    }
                    if (name.equalsIgnoreCase(SERVER_RECORD_XML_START_TAG) && currentServerRecord != null)
                    {
                    	ServerRecordProvider servProv = ServerRecordProvider.get(App.getContext());
                    	servProv.addServerRecord(currentServerRecord);
                    }
				}
				 
				eventType = parser.next();
	        }
		} 
		catch (XmlPullParserException e)
		{
			throw new RuntimeException("XmlPullParserException occurred in ServerHelper.setupDefaultServers()", e);
		} 
		catch (IOException e)
		{
			throw new RuntimeException("IOException occurred in ServerHelper.setupDefaultServers()", e);
		}
	}
}