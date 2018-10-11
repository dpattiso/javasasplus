package sas.util;

import sas.data.SASAction;

/**
 * Action with no preconditions or effects.
 * 
 * @author David Pattison
 *
 */
public class SASNullAction extends SASAction
{
	private int nullId;

	public SASNullAction(int nullId)
	{
		super(nullId, "NullAction_"+nullId);
		
		this.nullId = nullId;
	}

	public int getNullId()
	{
		return nullId;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
			return false;
		
		SASNullAction a = (SASNullAction) obj;
		if (super.equals(a) == false)
			return false;
		
		if (this.getNullId() != a.getNullId())
			return false;
		
		return true;
	}
	
	@Override
	public int hashCode()
	{
		return super.hashCode() ^ this.getNullId() ^ 31;
	}
	
	@Override
	public String toString()
	{
		return "NullAction"+this.getNullId();
	}
	
	
	@Override
	public Object clone()
	{
		SASNullAction clone = new SASNullAction(this.getNullId());
		
		assert(this.hashCode() == clone.hashCode());
		
		return clone;
	}
}
