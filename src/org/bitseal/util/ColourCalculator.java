package org.bitseal.util;

import java.math.BigInteger;

/**
 * Offers utility methods related to calculating colour values.
 *
 * @author Jonathan Coe
 */
public class ColourCalculator
{
	/**
	 * Takes a String containing a Bitmessage address and deterministically
	 * calculates 3 int values in the range 0-256 from it. These can be used
	 * as the RGB values of a colour. <br><br>
	 * 
	 * @param address - The input address
	 * 
	 * @return An int[] of length 3 containing the calculated color values
	 * 
	 */
	public static int[] calculateColoursFromAddress(String address)
	{		
		// Skip the prefix characters and the address version + stream number characters, then select
		// only the next 7 characters of the remainder in order to speed the calculations up
		address = address.substring(5, 12);
		
		// Convert the String to decimal
		BigInteger addressNumber = Base58.decodeToBigInteger(address);
		
		// Get 3 numbers to use
		int value0 = (addressNumber).intValue();
		int value1 = (addressNumber.divide(BigInteger.valueOf(1000))).intValue();
		int value2 = (addressNumber.divide(BigInteger.valueOf(1000000))).intValue();
		
		int[] array = new int[3];
		
		// Convert the 3 numbers into the range 0-256
		int r = Math.abs((value0 % 256));
		int g = Math.abs(value1 % 256);
		int b = Math.abs(value2 % 256);
		
		array[0] = r;
		array[1] = g;
		array[2] = b;
		
		return array;
	}
}