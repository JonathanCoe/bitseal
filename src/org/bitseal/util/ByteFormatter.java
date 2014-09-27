package org.bitseal.util;

/**
 * A utility class that offers methods for formatting byte arrays.
 * 
 * @author Jonathan Coe
 */
public final class ByteFormatter
{
	private static final char[] hexChars = "0123456789abcdef".toCharArray();
	
	private ByteFormatter()
	{
		// The constructor of this class is private in order to prevent the class being instantiated
	}
	
	/**
	 * Converts a byte[] into a hex formatted String. 
	 * 
	 * @param bytes - A byte[] containing the bytes to be formatted as a hex String. 
	 * 
	 * @return A String containing the hex formatted bytes. 
	 */
	public static String byteArrayToHexString(byte[] bytes) 
	{
	    char[] hexString = new char[bytes.length * 2];
	    
	    for ( int i = 0; i < bytes.length; i++ )
	    {
	        int p = bytes[i] & 0xFF;
	        hexString[i * 2] = hexChars[p >>> 4];
	        hexString[i * 2 + 1] = hexChars[p & 0x0F];
	    }
	    
	    return new String(hexString);
	}
	
	/**
	 * Converts a String conatining hexadecimal data into a byte[]
	 * 
	 * @param string - A String conatining the hex data to be converted
	 * 
	 * @return A byte[] containing the converted data
	 */
	public static byte[] hexStringToByteArray(String string) 
	{
	    int len = string.length();
	    byte[] data = new byte[len / 2];
	    
	    for (int i = 0; i < len; i += 2) 
	    {
	        data[i / 2] = (byte) ((Character.digit(string.charAt(i), 16) << 4) + Character.digit(string.charAt(i+1), 16));
	    }
	    return data;
	}
}