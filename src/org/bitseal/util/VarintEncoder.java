package org.bitseal.util;

/**
 * A utility class offering methods for encoding and decoding between
 * bytes and var_int encoded integers.<br><br>
 *  
 * At the time of writing, PyBitmessages's implementation of this can
 * be found in the "addresses.py" file in the source code. <br><br>
 * 
 * See https://bitmessage.org/wiki/Protocol_specification#Variable_length_integer
 * 
 * @author Jonathan Coe
 */
public final class VarintEncoder
{
	private VarintEncoder()
	{
		// The constructor of this class is private in order to prevent the class being instantiated
	}
	
	/**
	 * Encodes a long into a var_int encoded byte[]. 
	 * 
	 * @param input - A long representing the value to encode
	 * 
	 * @return A byte[] containing the var_int encoded bytes
	 */
	public static byte[] encode(long input)
	{	
		// Credit to Sebastian Schmidt for this way of doing the encoding
		
		byte[] b = ByteUtils.longToBytes(input);
		byte[] varInt;
		
		if (input < 0)
		{
            throw new IllegalArgumentException("VarintEncoder.encode was called with a negative value as its parameter. This is " +
            		"invalid. The parameter given was " + input + ". Throwing new IllegalArgumentException.");
		}
		else if (input < 0xfd) 
		{
			varInt = new byte[] { b[7] };
		} 
		else if (input < 0xffff) 
		{
			varInt = new byte[] { (byte) 0xfd, b[6], b[7] };
		} 
		else if (input < 4294967295L)
		{
			varInt = new byte[] { (byte) 0xfe, b[4], b[5], b[6], b[7] };
		} 
		else 
		{
			varInt = new byte[] { (byte) 0xff, b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7] };
		}

		return varInt;
	}
	
	/**
	 * Decodes a byte[] from var_int format into a long[] representing the value of the
	 * var_int and the length of the var_int in its encoded byte form
	 * 
	 * @param input - A byte[] containing the bytes to decode
	 * 
	 * @return An long[] containing exactly 2 longs. The first long is the value of the var_int and
	 * the second long is the length of the var_int in its encoded byte form
	 */
	public static long[] decode(byte[] input)
	{	
		long encodedValue = 0;
		long encodedLength = 0;
		int firstByteValue = (int) (input[0] & 0xFF); // Variable length integer encoding uses unsinged bytes, so it is necessary to convert them for use in Java
		
		// In variable length ingteger encoding, the first byte is a special value. If the value encoded is less than 253 decimal, then the first byte is that value
		// (in unsinged byte form). If the encoded value is greater than or equal to 253, then the first byte is used as a flag to denote how many bytes have been used
		// to encode the value - 2, 4, or 8 bytes, also in unsigned form. Thus the total number of bytes used is 1, 3, 5, or 9.
		
		if (input.length == 0)
		{
			return new long[]{0,0};
		}
		
	    if (firstByteValue < 253)
	    {
	    	encodedValue = firstByteValue;
	    	encodedLength = 1;
	    }
	        	    
	    if (firstByteValue == 253)
	    {
	        encodedValue = ByteUtils.bytesToShort(ArrayCopier.copyOfRange(input, 1, 3));
	        encodedLength = 3;
	    }
	    
	    if (firstByteValue == 254)
	    {
	        encodedValue = ByteUtils.bytesToInt(ArrayCopier.copyOfRange(input, 1, 5));
	        encodedLength = 5;
	    }
	    
	    if (firstByteValue == 255)
	    {
	        encodedValue = ByteUtils.bytesToLong((ArrayCopier.copyOfRange(input, 1, 9)));
	        encodedLength = 9;
	    }

		return new long[]{encodedValue, encodedLength};
	}
}