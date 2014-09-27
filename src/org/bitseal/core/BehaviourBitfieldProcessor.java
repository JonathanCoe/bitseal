package org.bitseal.core;

/**
 * A class that offers methods to handle tasks based on the behaviour bitfield used
 * in Bitmessage pubkeys. <br><br>
 * 
 * See https://bitmessage.org/wiki/Protocol_specification#Pubkey_bitfield_features
 * 
 * @author Jonathan Coe
 */
public class BehaviourBitfieldProcessor
{
	/** The client that generated this pubkey will send acknowledgments for
	 * messages it receives. */
	private static final int SENDS_ACKS = 1;
	
	/** The client that generated this pubkey requires that any messages
	 * sent to it have a tag derived from the destination address prepended to them */
	private static final int INCLUDE_DESTINATION_TAG = 2;
	
	/**
	 * Returns an int representing the behaviour bitfield that
	 * should be used in pubkeys that Bitseal produces. (i.e. pubkeys
	 * for addresses owned by the user of the app)
	 * 
	 * @return An int representing the behaviour bitfield to use for
	 * my pubkeys
	 */
	public static int getBitfieldForMyPubkeys()
	{
		int behaviourBitfield = 0;
    	behaviourBitfield |= SENDS_ACKS;
    	return behaviourBitfield;
	}
	
	/**
	 * Reads a given behaviour bitfield and checks whether its
	 * flag for "sends acknowledgments" is set to true or false.
	 * 
	 * @param behaviourBitfield - An int representing the behaviour
	 * bitfield to check
	 * 
	 * @return A boolean indicating whether or not the node which
	 * generated the pubkey containing the given behaviour bitfield
	 * will send acknowledgments for messages it receives. 
	 */
	public static boolean checkSendsAcks(int behaviourBitfield)
	{
		if ((behaviourBitfield & SENDS_ACKS) == SENDS_ACKS)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Reads a given behaviour bitfield and checks whether its
	 * flag for "include destination tag" is set to true or false.
	 * 
	 * @param behaviourBitfield - An int representing the behaviour
	 * bitfield to check
	 * 
	 * @return A boolean indicating whether or not the node which
	 * generated the pubkey requires that messages sent to it have
	 * a tag derived from the address that the message has been sent to 
	 * included unencrypted at the start of the message. 
	 */
	public static boolean checkIncludeDestinationTag(int behaviourBitfield)
	{
		if ((behaviourBitfield & INCLUDE_DESTINATION_TAG) == INCLUDE_DESTINATION_TAG)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
}