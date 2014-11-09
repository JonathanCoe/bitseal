package org.bitseal.data;

/**
 * An object representing an unencrypted Bitmessage msg object. Note that
 * unencrypted msg objects with an address version of 2 or lower do not have
 * values for nonceTrialsPerByte or extraBytes. In these instances those two
 * fields are left empty. <br><br>
 * 
 * See https://bitmessage.org/wiki/Protocol_specification#Unencrypted_Message_Data
 * 
 * @author Jonathan Coe
 */
public class UnencryptedMsg extends BMObject
{	
	// All fields below this are part of the data to be encrypted
	private int senderAddressVersion; // The sender's address version
	private int senderStreamNumber; // The sender's stream number
	private int behaviourBitfield; //A bitfield of optional behaviours and features that can be expected from the node with this pubkey. 4 bytes in length, e.g. '\x00\x00\x00\x01'
	private byte[] publicSigningKey; // Belongs to the sender of the message
	private byte[] publicEncryptionKey; // Belongs to the sender of the message
	private int nonceTrialsPerByte;
	private int extraBytes;
	private byte[] destinationRipe;
	private int encoding;
	private int messageLength;
	private byte[] message;
	private int ackLength;
	private byte[] ackMsg;
	private int signatureLength;
	private byte[] signature;
		
	public int getSenderAddressVersion() 
	{
		return senderAddressVersion;
	}
	public void setSenderAddressVersion(int addressVersion) 
	{
		this.senderAddressVersion = addressVersion;
	}
	
	public int getSenderStreamNumber() 
	{
		return senderStreamNumber;
	}
	public void setSenderStreamNumber(int streamNumber) 
	{
		this.senderStreamNumber = streamNumber;
	}
	
	public int getBehaviourBitfield() 
	{
		return behaviourBitfield;
	}
	public void setBehaviourBitfield(int behaviourBitfield) 
	{
		this.behaviourBitfield = behaviourBitfield;
	}
	
	public byte[] getPublicSigningKey() 
	{
		return publicSigningKey;
	}
	public void setPublicSigningKey(byte[] publicSigningKey) 
	{
		this.publicSigningKey = publicSigningKey;
	}
	
	public byte[] getPublicEncryptionKey() 
	{
		return publicEncryptionKey;
	}
	public void setPublicEncryptionKey(byte[] publicEncryptionKey) 
	{
		this.publicEncryptionKey = publicEncryptionKey;
	}
	
	public int getNonceTrialsPerByte() 
	{
		return nonceTrialsPerByte;
	}
	public void setNonceTrialsPerByte(int nonceTrialsPerByte) 
	{
		this.nonceTrialsPerByte = nonceTrialsPerByte;
	}
	
	public int getExtraBytes() 
	{
		return extraBytes;
	}
	public void setExtraBytes(int extraBytes) 
	{
		this.extraBytes = extraBytes;
	}
	
	public byte[] getDestinationRipe() 
	{
		return destinationRipe;
	}
	public void setDestinationRipe(byte[] destinationRipe) 
	{
		this.destinationRipe = destinationRipe;
	}
	
	public int getEncoding() 
	{
		return encoding;
	}
	public void setEncoding(int encoding) 
	{
		this.encoding = encoding;
	}
	
	public int getMessageLength() 
	{
		return messageLength;
	}
	public void setMessageLength(int messageLength) 
	{
		this.messageLength = messageLength;
	}
	
	public byte[] getMessage() 
	{
		return message;
	}
	public void setMessage(byte[] message) 
	{
		this.message = message;
	}
	
	public int getAckLength() 
	{
		return ackLength;
	}
	public void setAckLength(int ackLength) 
	{
		this.ackLength = ackLength;
	}
	
	public byte[] getAckMsg() 
	{
		return ackMsg;
	}
	public void setAckMsg(byte[] ackMsg) 
	{
		this.ackMsg = ackMsg;
	}
	
	public int getSignatureLength() 
	{
		return signatureLength;
	}
	public void setSignatureLength(int signatureLength) 
	{
		this.signatureLength = signatureLength;
	}
	
	public byte[] getSignature() 
	{
		return signature;
	}
	public void setSignature(byte[] signature) 
	{
		this.signature = signature;
	}
}