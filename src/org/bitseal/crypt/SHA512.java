package org.bitseal.crypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.bitseal.util.ArrayCopier;
import org.spongycastle.crypto.digests.RIPEMD160Digest;

/**
 * Provides methods for several hashing functions based on SHA-512
 * 
 * @author Sebastian Schmidt, modified by Jonathan Coe
 */
public final class SHA512 
{
	/**
	 * Returns the sha512 sum of a given byte[] of data.
	 * 
	 * @param data - A byte[] containing the input for sha512.
	 * 
	 * @return A byte[] containing the sha512 sum of the input data
	 */
	public static byte[] sha512(byte[] data) 
	{
		return sha512(data, 64); // 64 bytes is the standard output size for a SHA-512 hash
	}

	/**
	 * Returns the sha512 sum of all given bytes.
	 * 
	 * @param data - A byte[] containing the bytes
	 * 
	 * @return A byte[] containing the sha512 sum of all given bytes.
	 */
	public static byte[] sha512(byte[]... data) 
	{
		MessageDigest sha512;
		try 
		{
			sha512 = MessageDigest.getInstance("SHA-512");

			for (byte[] bytes : data) 
			{
				sha512.update(bytes);
			}

			return sha512.digest();
		} 
		catch (NoSuchAlgorithmException e) 
		{
            throw new RuntimeException("NoSuchAlgorithmException occurred in DigestSHA512.sha512()", e);
		}
	}

	/**
	 * Returns the first x number of bytes of the SHA-512 hash of the input data. 
	 * 
	 * @param bytes - A byte[] containing the input for sha512.
	 * @param digestLength - An int representing the number of bytes to return.
	 * 
	 * @return A byte[] containing the first x number of bytes of the SHA-512 hash of the input data. 
	 */
	public static byte[] sha512(byte[] bytes, int digestLength)
	{
		MessageDigest sha512;

		try 
		{
			sha512 = MessageDigest.getInstance("SHA-512");
			byte[] sum = sha512.digest(bytes);
			return ArrayCopier.copyOf(sum, digestLength);
		} 
		
		catch (NoSuchAlgorithmException e) 
		{
			throw new RuntimeException("NoSuchAlgorithmException occurred in DigestSHA512.sha512()", e);
		}
	}
	
	  /**
     * Calculates the double SHA-512 hash of a given byte[]
     * 
     * @param input - A byte[] containing the data to be hashed
     * 
     * @return A byte[] containing the double SHA-512 hash of the input data 
     */
    public static byte[] doubleHash(byte[] input) 
    {
        return doubleHash(input, 0, input.length);
    }
    
	/**
	 * Calculates the SHA-512 hash of the given byte range, and then hashes the resulting hash again. This is
     * standard procedure in BitCoin. The resulting hash is in big endian form.
	 * 
	 * @param input - A byte[] containing the data to hash
	 * @param offset - An int representing the index in the data byte[] at which to begin
	 * @param length - An int representing the number of bytes from the data byte[] to process
	 * 
	 * @return A byte[] containing the doubly SHA-512 hashed data
	 */
    public static byte[] doubleHash(byte[] input, int offset, int length) 
    {
        try 
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.update(input, offset, length);
            byte[] first = digest.digest();
            return digest.digest(first);
        } 
        catch (NoSuchAlgorithmException e) 
        {
        	throw new RuntimeException("NoSuchAlgorithmException occurred in DigestSHA512.doubleDigest()", e);
        }
    }
	
    /**
     * Calculates RIPEMD160(SHA512(input)). This is used in Address calculations.
     * 
     * @param input - A byte[] containing the data to hash
     * 
     * @return A byte[] containing the hash of the data
     */
    public static byte[] sha512hash160(byte[] input) 
    {
        try 
        {
            byte[] sha512 = MessageDigest.getInstance("SHA-512").digest(input);
            RIPEMD160Digest digest = new RIPEMD160Digest();
            digest.update(sha512, 0, sha512.length);
            byte[] out = new byte[20];
            digest.doFinal(out, 0);
            return out;
        } 
        catch (NoSuchAlgorithmException e) 
        {
        	throw new RuntimeException("NoSuchAlgorithmException occurred in DigestSHA512.sha512hash160()", e);
        }
    }
}