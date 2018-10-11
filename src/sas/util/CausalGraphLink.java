package sas.util;

/**
 * Stub class
 * @author David Pattison
 *
 */
public class CausalGraphLink
{
	private static int counter = 1;
	
	private int id; //graph package wont allow the same edge multiple times, so this is a hack to make edges unique
	private float weight;
	
	public CausalGraphLink()
	{
		weight = 0;
		id = CausalGraphLink.getId();
	}
	
	protected static int getId()
	{
		return CausalGraphLink.counter++;
	}
	
	public CausalGraphLink(float weight)
	{
		this.weight = weight;
	}

	public float getWeight()
	{
		return weight;
	}

	public void setWeight(float weight)
	{
		this.weight = weight;
	}
}