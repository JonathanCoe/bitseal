package org.bitseal.data;

import java.math.BigInteger;

/**
 * An object representing an encrypted payload.<br><br>
 * 
 * See: https://bitmessage.org/wiki/Encryption
 * 
 * @author Jonathan Coe
 */
public class EncryptedPayload
{
	private byte[] IV;
	private int curveType;
	private int xLength;
	private BigInteger x;
	private int yLength;
	private BigInteger y;
	private byte[] cipherText;
	private byte[] mac;

	public byte[] getIV()
	{
		return IV;
	}

	public void setIV(byte[] iV)
	{
		IV = iV;
	}

	public int getCurveType()
	{
		return curveType;
	}

	public void setCurveType(int curveType)
	{
		this.curveType = curveType;
	}

	public int getxLength()
	{
		return xLength;
	}

	public void setxLength(int xLength)
	{
		this.xLength = xLength;
	}

	public BigInteger getX()
	{
		return x;
	}

	public void setX(BigInteger x)
	{
		this.x = x;
	}

	public int getyLength()
	{
		return yLength;
	}

	public void setyLength(int yLength)
	{
		this.yLength = yLength;
	}

	public BigInteger getY()
	{
		return y;
	}

	public void setY(BigInteger y)
	{
		this.y = y;
	}

	public byte[] getCipherText()
	{
		return cipherText;
	}

	public void setCipherText(byte[] cipherText)
	{
		this.cipherText = cipherText;
	}

	public byte[] getMac()
	{
		return mac;
	}

	public void setMac(byte[] mac)
	{
		this.mac = mac;
	}
}