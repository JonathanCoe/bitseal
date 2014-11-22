package org.bitseal.crypt;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import org.bitseal.data.EncryptedPayload;
import org.bitseal.util.ArrayCopier;
import org.bitseal.util.ByteUtils;
import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.BlockCipherPadding;
import org.spongycastle.crypto.paddings.PKCS7Padding;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.interfaces.ECPrivateKey;
import org.spongycastle.jce.interfaces.ECPublicKey;
import org.spongycastle.jce.provider.asymmetric.ec.EC5Util;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECPoint;

/**
 * Offers methods for encryption and decryption. 
 * 
 * @author Sebastian Schmidt, modified by Jonathan Coe
 */
public class CryptProcessor
{
	private static final String ALGORITHM_ECDSA = "ECDSA";
	private static final String ALGORITHM_ECIES = "ECIES";
	private static final String PROVIDER = "SC"; // Spongy Castle
	private static final String CURVE = "secp256k1";
	private static final int CURVE_TYPE = 714;
	
	private KeyPairGenerator kpg;
	private KeyPairGenerator skpg;
	
	public CryptProcessor()
	{
		Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());

		try 
		{
			kpg = KeyPairGenerator.getInstance(ALGORITHM_ECIES, PROVIDER);			
			kpg.initialize(ECNamedCurveTable.getParameterSpec(CURVE), new SecureRandom());

			skpg = KeyPairGenerator.getInstance(ALGORITHM_ECDSA, PROVIDER);
			skpg.initialize(ECNamedCurveTable.getParameterSpec(CURVE), new SecureRandom());
		} 
		catch (NoSuchAlgorithmException e) 
		{
			throw new RuntimeException("NoSuchAlgorithmException occurred in CryptProcessor constructor", e);
		}
		catch (NoSuchProviderException e) 
		{
			throw new RuntimeException("NoSuchProviderException occurred in CryptProcessor constructor", e);
		}
		catch (InvalidAlgorithmParameterException e) 
		{
			throw new RuntimeException("InvalidAlgorithmParameterException occurred in CryptProcessor constructor", e);
		}
	}
	
	/**
	 * Encrypts the given data using the supplied public key.<br><br>
	 * 
	 * See https://bitmessage.org/wiki/Encryption and https://bitmessage.org/forum/index.php?topic=2848.0
	 * 
	 * @param plain - A byte[] containing the data to be encrypted.
	 * @param K - An ECPublicKey object containing the public key 'K' to encrypt the data with.
	 * 
	 * @return A byte[] containing the encrypted payload.
	 */
	public byte[] encrypt (byte[] plain, ECPublicKey K)
	{
		KeyPair random = generateEncryptionKeyPair();
		ECPublicKey R = (ECPublicKey) random.getPublic();
		BigInteger r = ((ECPrivateKey)random.getPrivate()).getD();
		
		ECPoint P = K.getQ().multiply(r);

		byte[] tmpKey = deriveKey(P);
		byte[] key_e = ArrayCopier.copyOfRange(tmpKey, 0, 32);
		byte[] key_m = ArrayCopier.copyOfRange(tmpKey, 32, 64);

		byte[] iv = new byte[16];
		new SecureRandom().nextBytes(iv);

		byte[] cipherText = doAES(key_e, iv, plain, true);
		
		byte[] x = ByteUtils.getUnsignedBytes(R.getQ().getX().toBigInteger(), 32);
		byte[] y = ByteUtils.getUnsignedBytes(R.getQ().getY().toBigInteger(), 32);

		int xLength = x.length;
		int yLength = y.length;
		
		byte[] encodedR = ByteUtils.concatenateByteArrays(ByteUtils.shortToBytes((short) 714), ByteUtils.shortToBytes((short) xLength), x, ByteUtils.shortToBytes((short) yLength), y);
		
		byte[] dataForMac = ByteUtils.concatenateByteArrays(iv, encodedR, cipherText);
		byte[] mac = SHA256.hmacSHA256(dataForMac, key_m);

		byte[] encryptedPayload = ByteUtils.concatenateByteArrays(iv, encodedR, cipherText, mac);
		
		return encryptedPayload;
	}
	
	/** 
	 * Decrypts an encrypted msg.<br><br>
	 * 
	 * <b>NOTE! If decryption fails, this method will throw a RuntimeException</b>
	 *  
	 * @param encryptedPayload - A byte[] containing the data to be decrypted
	 * @param k - The ECPrivateKey object used to decrypt the data
	 * 
	 * @return A byte[] containing the decrypted plain text
	 */
	public byte[] decrypt (byte[] encryptedPayload, ECPrivateKey k)
	{
		// Parse the data from the encrypted payload
		EncryptedPayload encPay = parseEncryptedPayload(encryptedPayload);
		byte[] iv = encPay.getIV();
		BigInteger x = encPay.getX();
		BigInteger y = encPay.getY();
		byte[] cipherText = encPay.getCipherText();
		byte[] mac = encPay.getMac();
		
		// Reconstruct public key R
		ECPublicKey R = createPublicEncryptionKey(x, y);

		// Now that we have parsed all the data from the encrypted payload, we can begin the decryption process.
		// First, do an EC point multiply with private key k and public key R. This gives you public key P. 
		ECPoint P = R.getQ().multiply(k.getD());

		byte[] tmpKey = deriveKey(P);
		byte[] key_e = ArrayCopier.copyOf(tmpKey, 32);
		byte[] key_m = ArrayCopier.copyOfRange(tmpKey, 32, 64);

		// Check whether the mac is valid	
		byte[] dataForMac = ArrayCopier.copyOfRange(encryptedPayload, 0, encryptedPayload.length - 32); // The mac now covers everything except itself
		byte[] expectedMAC = SHA256.hmacSHA256(dataForMac, key_m);
		
		if (Arrays.equals(mac, expectedMAC) == false)
		{
			// The mac is invalid
			throw new RuntimeException("While attempting to decrypt an encrypted payload in CryptProcessor.decryptMsg(), the mac was found to be invalid");
		}
		else
		{
			// The mac is valid. Decrypt the parsed data
			return doAES(key_e, iv, cipherText, false);
		}
	}
	
	/**
	 * Parses an encrypted payload, for example from a msg or broadcast,
	 * and uses it to create a new EncryptedPayload object.
	 * 
	 * @param encryptedPayload - A byte[] containing the encrypted payload data
	 * 
	 * @return An EncryptedPayload object containing the parsed data. 
	 */
	private EncryptedPayload parseEncryptedPayload(byte[] encryptedPayload)
	{
		// Parse the data from the payload
		int readPosition = 0;
				
		byte[] iv = ArrayCopier.copyOfRange(encryptedPayload, readPosition, readPosition + 16);
		readPosition += 16;
		
		int curveType = ByteUtils.bytesToShort(ArrayCopier.copyOfRange(encryptedPayload, readPosition, readPosition + 2));
		readPosition += 2;
		if (curveType != CURVE_TYPE)
		{
			throw new RuntimeException("While running CryptProcessor.parseEncryptedPayload(), the curve type was not 714. Something is wrong!\n"
					+ "The curve type read was " + curveType);
		}
		
		int xLength = ByteUtils.bytesToShort(ArrayCopier.copyOfRange(encryptedPayload, readPosition, readPosition + 2));
		readPosition += 2;
		if (xLength > 32 || xLength < 0) 
		{
			throw new RuntimeException("While running CryptProcessor.parseEncryptedPayload(), the xLength value was found to not be between 0 and 32. Something is wrong!\n"
					+ "The xLength read was " + xLength);
		}
		
		BigInteger x = ByteUtils.getUnsignedBigInteger(ArrayCopier.copyOfRange(encryptedPayload, readPosition, readPosition + xLength), 0, xLength);

		readPosition += xLength;
		
		int yLength = ByteUtils.bytesToShort(ArrayCopier.copyOfRange(encryptedPayload, readPosition, readPosition + 2));
		readPosition += 2;
		if (yLength > 32 || yLength < 0)
		{
			throw new RuntimeException("While running CryptProcessor.parseEncryptedPayload(), the yLength value was found to not be between 0 and 32. Something is wrong!\n"
					+ "The yLength read was " + yLength);
		}
		
		BigInteger y = ByteUtils.getUnsignedBigInteger(ArrayCopier.copyOfRange(encryptedPayload, readPosition, readPosition + yLength), 0, yLength);
		readPosition += yLength;

		byte[] cipherText = ArrayCopier.copyOfRange(encryptedPayload, readPosition, encryptedPayload.length - 32);
		byte[] mac = ArrayCopier.copyOfRange(encryptedPayload, encryptedPayload.length - 32, encryptedPayload.length);
		
		// Now use the parsed data to create a new EncryptedPayload object
		EncryptedPayload encPay = new EncryptedPayload();
		encPay.setIV(iv);
		encPay.setCurveType(curveType);
		encPay.setxLength(xLength);
		encPay.setX(x);
		encPay.setyLength(yLength);
		encPay.setY(y);
		encPay.setCipherText(cipherText);
		encPay.setMac(mac);
		return encPay;
	}

	/**
	 * Creates an ECPublicKey with the given coordinates. The key will have valid parameters.
	 * 
	 * @param x - A BigInteger object denoting the x coordinate on the curve.
	 * @param y -A BigInteger object denoting the y coordinate on the curve.
	 *
	 * @return An ECPublicKey object with the given coordinates.
	 */
	private ECPublicKey createPublicEncryptionKey (BigInteger x, BigInteger y) 
	{
		try 
		{
			java.security.spec.ECPoint w = new java.security.spec.ECPoint(x, y);
			ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec(CURVE);
			KeyFactory fact = KeyFactory.getInstance(ALGORITHM_ECDSA, PROVIDER);
			ECCurve curve = params.getCurve();
			java.security.spec.EllipticCurve ellipticCurve = EC5Util.convertCurve(curve, params.getSeed());
			java.security.spec.ECParameterSpec params2 = EC5Util.convertSpec(ellipticCurve, params);
			java.security.spec.ECPublicKeySpec keySpec = new java.security.spec.ECPublicKeySpec(w, params2);
			return (ECPublicKey) fact.generatePublic(keySpec);
		} 
		catch (InvalidKeySpecException e) 
		{
			throw new RuntimeException("InvalidKeySpecException occurred in CryptProcessor.createPublicEncryptionKey()", e);
		}
		catch (NoSuchAlgorithmException e) 
		{
			throw new RuntimeException("NoSuchAlgorithmException occurred in CryptProcessor.createPublicEncryptionKey()", e);
		}
		catch (NoSuchProviderException e) 
		{
			throw new RuntimeException("NoSuchProviderException occurred in CryptProcessor.createPublicEncryptionKey()", e);
		}
	}

	/**
	 * Generates a new random ECIES key pair.
	 * 
	 * @return A KeyPair object containing the new random ECIES key pair.
	 */
	private KeyPair generateEncryptionKeyPair() 
	{
		synchronized (kpg) 
		{
			return kpg.generateKeyPair();
		}
	}
	
	/**
	 * Derives a 64 byte key from the given ECPoint.
	 * 
	 * @param p - An ECPoint object corresponding to the given point.
	 * 
	 * @return A byte[] containing the 64 byte key.
	 */
	private byte[] deriveKey (ECPoint p)
	{
		return SHA512.sha512(ByteUtils.getUnsignedBytes(p.getX().toBigInteger(), 32));
	}

	/**
	 * Encrypts or decrypts the given data with the given key.
	 * 
	 * @param keyBytes - A byte[] containing the AES key.
	 * @param iv - A byte[] containing the initialization vector to be used. 
	 * @param data - A byte[] containing the data to process.
	 * @param encrypt - A boolean value: true if the data should be encrypted, false if it should be decrypted.
	 * 
	 * @return A byte[] containing the encrypted or decrypted data.
	 */
	private byte[] doAES (byte[] keyBytes, byte[] iv, byte[] data, boolean encrypt)
	{
		BlockCipherPadding padding = new PKCS7Padding();
		BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), padding);

		KeyParameter key = new KeyParameter(keyBytes);
		CipherParameters params = new ParametersWithIV(key, iv);

		cipher.init(encrypt, params);

		byte[] buffer = new byte[cipher.getOutputSize(data.length)];
		int length = cipher.processBytes(data, 0, data.length, buffer, 0);

		try 
		{
			length += cipher.doFinal(buffer, length);
		} 
		catch (DataLengthException e) 
		{
			throw new RuntimeException("DataLengthException occurred in CryptProcessor.doAES()", e);
		}
		catch (IllegalStateException e) 
		{
			throw new RuntimeException("IllegalStateException occurred in CryptProcessor.doAES()", e);
		}
		catch (InvalidCipherTextException e) 
		{
			throw new RuntimeException("InvalidCipherTextException occurred in CryptProcessor.doAES()", e);
		}
		
		return ArrayCopier.copyOf(buffer, length);
	}
}