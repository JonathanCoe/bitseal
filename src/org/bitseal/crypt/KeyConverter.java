package org.bitseal.crypt;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import org.bitseal.data.Pubkey;
import org.bitseal.util.ArrayCopier;
import org.bitseal.util.Base58;
import org.bitseal.util.ByteUtils;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.interfaces.ECPrivateKey;
import org.spongycastle.jce.interfaces.ECPublicKey;
import org.spongycastle.jce.provider.asymmetric.ec.EC5Util;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.math.ec.ECCurve;

/**
 * Offers methods for converting cryptographic keys
 * between different formats. 
 * 
 * @author Jonathan Coe
 */
public class KeyConverter 
{
	private static final String ALGORITHM = "ECDSA";
	private static final String PROVIDER = "SC"; // Spongy Castle
	private static final String CURVE = "secp256k1";
	
	/**
	 * Calculates the ripe hash for the public signing and public encryption keys of a given Pubkey object. 
	 * 
	 * Note: In Bitmessage the ripe hash is the result of RIPEMD160(SHA512(public_signing_key || public_encryption_key)
	 * 
	 * @param pubkey The Pubkey object containing the keys which we want to calculate the ripe hash of. 
	 * 
	 * @return A byte[] containing the ripe hash. 
	 */
	public byte[] calculateRipeHashFromPubkey (Pubkey pubkey)
	{
		byte[] publicSigningKey = pubkey.getPublicSigningKey();
		byte[] publicEncryptionKey = pubkey.getPublicEncryptionKey();
		
		byte[] mConcatenatedPublicKeys = ByteUtils.concatenateByteArrays(publicSigningKey, publicEncryptionKey);
		byte[] ripeHash = SHA512.sha512hash160(mConcatenatedPublicKeys);
		
		return ripeHash;
	}
	
	/**
	 * Takes an encryption key derived from the double hash of a Bitmessage address
	 * and uses it to create a public encryption key
	 * 
	 * @param encryptionKey - A byte[] containing the encryption key
	 * 
	 * @return An ECPublicKey object containing the new public key
	 */
	public ECPublicKey calculatePublicKeyFromDoubleHashKey (byte[] encryptionKey)
	{
		// First calculate the private key, using the 'encryption key' derived from the double
		// hash of the address data, and extract its 'D' value. 
		ECPrivateKey privKey = reconstructPrivateKey(encryptionKey);
		BigInteger privKeyDValue = privKey.getD();
				
		// Use the 'D' value from the private key to create a new ECKeyPair object
		ECKeyPair keyPair = new ECKeyPair(privKeyDValue);
		
		// Takes the public key from the new key pair. 
		byte[] publicKeyBytes = keyPair.getPubKey();
		
		// Convert the public key bytes into a new ECPublicKey object
		ECPublicKey publicKey = reconstructPublicKey(publicKeyBytes);
		
		return publicKey;
	}
	
	/**
	 * Takes an encryption key derived from the double hash of a Bitmessage address
	 * and uses it to create a private encryption key
	 * 
	 * @param encryptionKey - A byte[] containing the encryption key
	 * 
	 * @return An ECPrivateKey object containing the new private key
	 */
	public ECPrivateKey calculatePrivateKeyFromDoubleHashKey (byte[] encryptionKey)
	{
		return reconstructPrivateKey(encryptionKey);
	}
	
	/** 
	 * Converts a private key in byte array form into a Bitcoin-esque "Wallet Import Format" string. The 
	 * process is as follows: <br><br>
	 * 
	 * 1) Prepend the decimal value 128 to the private key <br><br>
	 * 2) Calculate the double SHA-256 hash of the private key with the extra byte <br><br>
	 * 3) Take the first 4 bytes of that hash as a checksum for the private key<br><br>
	 * 4) Add the checksum bytes onto the end of the private key with its extra byte<br><br>
	 * 5) Convert the byte array containing the private key, extra byte, and checksum into a Base 58 encoded String.<br><br>
	 * 
	 * <b>NOTE:</b> Somewhat confusingly, Bitmessage uses SHA-512 for its address generation and proof of work, 
	 * but uses SHA-256 for converting private keys into wallet import format. 
	 *  
	 * @param privateKey - The private key in byte[] format
	 * 
	 * @return WIFPrivateKey - A String representation of the private key in "Wallet Import Format"
	 */
	public String encodePrivateKeyToWIF (byte[] privateKey)
	{		
		// If first byte of the private encryption key generated is zero, remove it. 
		if (privateKey[0] == 0)
		{
			privateKey = ArrayCopier.copyOfRange(privateKey, 1, privateKey.length);
		}
		
		byte[] valueToPrepend = new byte[1];
		valueToPrepend[0] = (byte) 128;
		
		byte[] privateKeyWithExtraByte = ByteUtils.concatenateByteArrays(valueToPrepend, privateKey);
		
		byte[] hashOfPrivateKey = SHA256.doubleDigest(privateKeyWithExtraByte);
		
		byte[] checksum = ArrayCopier.copyOfRange(hashOfPrivateKey, 0, 4);
		
		byte[] convertedPrivateKey = ByteUtils.concatenateByteArrays(privateKeyWithExtraByte, checksum);
				
		String walletImportFormatPrivateKey = Base58.encode(convertedPrivateKey);
		
		return walletImportFormatPrivateKey;
	}
	
	/** 
	 * Converts a private key in Bitcoin-esque "Wallet Import Format" into an ECPrivateKey object. The process to do
	 * so is as follows: <br><br>
	 * 
	 * 1) Convert the Base58 encoded String into byte[] form.<br><br>
	 * 2) Drop the last four bytes, which are the checksum.<br><br>
	 * 3) Check that the checksum is valid for the remaining bytes.<br><br>
	 * 4) Drop the first byte, which is the special value prepended to the key bytes during the WIF encoding process.<br><br>
	 * 5) Check that the first byte equates to the decimal value 128.<br><br>
	 * 6) The remaining bytes are the private key in two's complement form. Convert them into a BigInteger <br><br>
	 * 7) Use newly created BigInteger value to create a new ECPrivateKey object.<br><br>
	 * 
	 * <b>NOTE:</b> Somewhat confusingly, Bitmessage uses SHA-512 for its address generation and proof of work, 
	 * but uses SHA-256 for converting private keys into wallet import format. 
	 *  
	 * @param wifPrivateKey - A String representation of the private key in "Wallet Import Format"
	 * 
	 * @return An ECPrivateKey object containing the private key 
	 */
	public ECPrivateKey decodePrivateKeyFromWIF (String wifPrivateKey)
	{
		byte[] privateKeyBytes = Base58.decode(wifPrivateKey);
		
		byte[] privateKeyWithoutChecksum = ArrayCopier.copyOfRange(privateKeyBytes, 0, (privateKeyBytes.length - 4));
		
		byte[] checksum = ArrayCopier.copyOfRange(privateKeyBytes, (privateKeyBytes.length - 4), privateKeyBytes.length);
		
		byte[] hashOfPrivateKey = SHA256.doubleDigest(privateKeyWithoutChecksum);
		
		byte[] testChecksum = ArrayCopier.copyOfRange(hashOfPrivateKey, 0, 4);
		
		if (Arrays.equals(checksum, testChecksum) == false)
		{
			throw new RuntimeException("While decoding a private key from WIF in KeyConverter.decodePrivateKeyFromWIF(), the checksum was " +
					"found to be invalid. Something is wrong!");
		}
		
		// Check that the prepended 128 byte is in place
		if (privateKeyWithoutChecksum[0] != (byte) 128)
		{
			throw new RuntimeException("While decoding a private key from WIF in KeyConverter.decodePrivateKeyFromWIF(), its prepended value " +
					"was found to be invalid. Something is wrong!");
		}
		
		// Drop the prepended 128 byte
		byte[] privateKeyFinalBytes = ArrayCopier.copyOfRange(privateKeyWithoutChecksum, 1, privateKeyWithoutChecksum.length);
		
		// If the decoded private key has a negative value, this means that it originally
		// began with a zero byte which was stripped off during the encodeToWIF process. We
		// must now restore this leading zero byte.
		BigInteger privateKeyBigIntegerValue = new BigInteger(privateKeyFinalBytes);
		if (privateKeyBigIntegerValue.signum() < 1)
		{
			byte[] valueToPrepend = new byte[1];
			valueToPrepend[0] = (byte) 0;
			
			privateKeyFinalBytes = ByteUtils.concatenateByteArrays(valueToPrepend, privateKeyFinalBytes);
		}
		
		ECPrivateKey ecPrivateKey = reconstructPrivateKey(privateKeyFinalBytes);
		
		return ecPrivateKey;
	}
	
	/**
	 * Converts an encoded private key in byte[] form into a new ECPrivateKey object.
	 * 
	 * @param encodedPrivateKey - A byte[] containing the encoded private key
	 * 
	 * @return An ECPrivateKey object containing the private key
	 */
	public ECPrivateKey reconstructPrivateKey(byte[] encodedPrivateKey)
	{
		// If the encoded private key has a negative value, this means that it originally
		// began with a zero byte which was stripped off during the encoding process. We
		// must now restore this leading zero byte.
		BigInteger privateKeyDValue = new BigInteger(encodedPrivateKey);
		if (privateKeyDValue.signum() < 1)
		{
			byte[] valueToPrepend = new byte[1];
			valueToPrepend[0] = (byte) 0;
			byte[] privateKeyFinalBytes = ByteUtils.concatenateByteArrays(valueToPrepend, encodedPrivateKey);
			privateKeyDValue = new BigInteger(privateKeyFinalBytes);
		}
		
		// Reconstruct the encoded private key, giving us a new ECPrivateKey object
		ECPrivateKey ecPrivateKey = null;
		try
		{
			Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
			
			ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec(CURVE);
			KeyFactory fact = KeyFactory.getInstance(ALGORITHM, PROVIDER);
			ECCurve curve = params.getCurve();
			java.security.spec.EllipticCurve ellipticCurve = EC5Util.convertCurve(curve, params.getSeed());
			java.security.spec.ECParameterSpec params2 = EC5Util.convertSpec(ellipticCurve, params);
			java.security.spec.ECPrivateKeySpec keySpec = new java.security.spec.ECPrivateKeySpec(privateKeyDValue, params2);
			
			ecPrivateKey = (ECPrivateKey) fact.generatePrivate(keySpec);
			// Log.i(TAG, "New ECPrivateKey D value bytes: " + ByteFormatter.byteArrayToHexString(ecPrivateKey.getD().toByteArray()));
		}
		catch (InvalidKeySpecException e)
		{
			throw new RuntimeException("InvalidKeySpecException occurred in KeyConverter.reconstructPrivateKey()", e);
		} 
		catch (NoSuchAlgorithmException e)
		{
			throw new RuntimeException("NoSuchAlgorithmException occurred in KeyConverter.reconstructPrivateKey()", e);
		} 
		catch (NoSuchProviderException e)
		{
			throw new RuntimeException("NoSuchProviderException occurred in KeyConverter.reconstructPrivateKey()", e);
		}
		
		return ecPrivateKey;
	}
	
	/**
	 * Converts an encoded public key in byte[] form into a new ECPublicKey object.
	 * 
	 * @param encodedpublicKey - A byte[] containing the encoded public key.
	 * 
	 * @return An ECPublicKey object containing the public key. 
	 */
	public ECPublicKey reconstructPublicKey(byte[] encodedPublicKey)
	{
		int keyLength = encodedPublicKey.length;
		
		// The public key should be either 64 or 65 bytes, depending upon whether or not the 0x04 value has been prepended to it
		if (keyLength < 64 || keyLength > 65)
		{
			throw new RuntimeException("While reconstructing a public key in KeyConverter.reconstructPublicKey(), the " +
					"encoded public key is not between 64 and 65 bytes in length. Something is wrong!");
		}
		
		if (keyLength == 65 && encodedPublicKey[0] != (byte) 4)
		{
			throw new RuntimeException("While reconstructing a public key in KeyConverter.reconstructPublicKey(), the encoded " +
					"public key is 65 bytes in length, but the first byte is not 0x04. Something is wrong!");
		}
		
		byte[] xBytes = null;
		byte[] yBytes = null;
		
		if (encodedPublicKey[0] == (byte) 4)
		{
			xBytes = ArrayCopier.copyOfRange(encodedPublicKey, 1, 33);
			yBytes = ArrayCopier.copyOfRange(encodedPublicKey, 33, 65);
		}
		
		else
		{
			xBytes = ArrayCopier.copyOfRange(encodedPublicKey, 0, 32);
			yBytes = ArrayCopier.copyOfRange(encodedPublicKey, 32, 64);
		}
		
		BigInteger x = ByteUtils.getUnsignedBigInteger(xBytes, 0, 32);
		BigInteger y = ByteUtils.getUnsignedBigInteger(yBytes, 0, 32);
		
		ECPublicKey reconstructedECPublicKey = null;
		try 
		{			
			Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
			
			java.security.spec.ECPoint w = new java.security.spec.ECPoint(x, y);
			ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec(CURVE);
			KeyFactory fact = KeyFactory.getInstance(ALGORITHM, PROVIDER);
			ECCurve curve = params.getCurve();
			java.security.spec.EllipticCurve ellipticCurve = EC5Util.convertCurve(curve, params.getSeed());
			java.security.spec.ECParameterSpec params2 = EC5Util.convertSpec(ellipticCurve, params);
			java.security.spec.ECPublicKeySpec keySpec = new java.security.spec.ECPublicKeySpec(w, params2);
			reconstructedECPublicKey = (ECPublicKey) fact.generatePublic(keySpec);
		} 
		catch (InvalidKeySpecException e)
		{
			throw new RuntimeException("InvalidKeySpecException occurred in KeyConverter.reconstructPrivateKey()", e);
		} 
		catch (NoSuchAlgorithmException e)
		{
			throw new RuntimeException("NoSuchAlgorithmException occurred in KeyConverter.reconstructPrivateKey()", e);
		} 
		catch (NoSuchProviderException e)
		{
			throw new RuntimeException("NoSuchProviderException occurred in KeyConverter.reconstructPrivateKey()", e);
		}

		return reconstructedECPublicKey;
	}
}