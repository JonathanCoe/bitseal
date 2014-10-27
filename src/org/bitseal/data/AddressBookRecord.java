package org.bitseal.data;

/**
 * An object representing an record in our address book.
 * 
 * @author Jonathan Coe
 */
public class AddressBookRecord implements Comparable<AddressBookRecord>
{
	private long id;
	
	private int colourR; 
	private int colourG;
	private int colourB;
	
	private String label;
	private String address;
	
	/**
	 * Used to sort AddressBookRecords by their label, in alphabetical order.
	 */
	@Override
	public int compareTo(AddressBookRecord a)
	{
		return this.getLabel().compareToIgnoreCase(a.getLabel());
	}
	
	public long getId()
	{
		return id;
	}
	public void setId(long id)
	{
		this.id = id;
	}
	
	public int getColourR()
	{
		return colourR;
	}
	public void setColourR(int r)
	{
		this.colourR = r;
	}

	public int getColourG()
	{
		return colourG;
	}
	public void setColourG(int g)
	{
		this.colourG = g;
	}

	public int getColourB()
	{
		return colourB;
	}
	public void setColourB(int b)
	{
		this.colourB = b;
	}

	public String getLabel()
	{
		return label;
	}
	public void setLabel(String label)
	{
		this.label = label;
	}

	public String getAddress()
	{
		return address;
	}
	public void setAddress(String address)
	{
		this.address = address;
	}
}