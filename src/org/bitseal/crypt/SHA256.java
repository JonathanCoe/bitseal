package org.bitseal.crypt;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.spongycastle.crypto.digests.RIPEMD160Digest;

/**
 * Provides methods for several hashing functions based on SHA-256
 * 
 * @author Sebastian Schmidt, modified by Jonathan Coe
 */
public class SHA256 
{
	/**
     * Calculates the double SHA-256 hash of a given byte[]. 
     */
    public static byte[] doubleDigest(byte[] input) 
    {
        return doubleDigest(input, 0, input.length);
    }

    /**
     * Calculates the SHA-256 hash of the given byte range, and then hashes the resulting hash again. This is
     * standard procedure in BitCoin. The resulting hash is in big endian form.
     */
    public static byte[] doubleDigest(byte[] input, int offset, int length) 
    {
        try 
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(input, offset, length);
            byte[] first = digest.digest();
            return digest.digest(first);
        } 
        catch (NoSuchAlgorithmException e) 
        {
        	throw new RuntimeException("NoSuchAlgorithmException occurred in DigestSHA256.doubleDigest()", e);
        }
    }

    /**
     * Calculates RIPEMD160(SHA256(input)). This is used in Address calculations.
     */
    public static byte[] sha256hash160(byte[] input) 
    {
        try 
        {
            byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(input);
            RIPEMD160Digest digest = new RIPEMD160Digest();
            digest.update(sha256, 0, sha256.length);
            byte[] out = new byte[20];
            digest.doFinal(out, 0);
            return out;
        } 
        catch (NoSuchAlgorithmException e) 
        {
        	throw new RuntimeException("NoSuchAlgorithmException occurred in DigestSHA256.sha256hash160()", e);
        }
    }
    
    /**
	 * Calculates the HmacSHA256 from the given key and data.
	 * 
	 * @param data - A byte[] containing the data.
	 * @param key - A byte[] containing the key.
	 * 
	 * @return A byte[] containing the HmacSHA256.
	 */
	public static byte[] hmacSHA256(byte[] data, byte[] key)
	{
		Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
		
		try 
		{
			Mac mac = Mac.getInstance("HmacSHA256", "SC");
			mac.init(new SecretKeySpec(key, "HmacSHA256"));
			return mac.doFinal(data);
		} 
		catch (NoSuchAlgorithmException e) 
		{
			throw new RuntimeException("NoSuchAlgorithmException occurred in hmacSHA256.sha256hash160()", e);
		}
		
		catch (NoSuchProviderException e) 
		{
			throw new RuntimeException("NoSuchProviderException occurred in hmacSHA256.sha256hash160()", e);
		}
		
		catch (InvalidKeyException e) 
		{
			throw new RuntimeException("InvalidKeyException occurred in hmacSHA256.sha256hash160()", e);
		}
	}
}