package sas.data;

/**
 * A SAS effect is the transition from value X to value X' on an unassociated variable.
 * @author David Pattison
 *
 */
public class SASEffect
{
	public int precondition, effect;
	
	public SASEffect(int precondition, int effect)
	{
		this.precondition = precondition;
		this.effect = effect;
	}

//	@Override
//	public STRIPSInstantAction convertToPDDL()
//	{
//		STRIPSInstantAction a = new STRIPSInstantAction();
//		a.setCondition(condition)
//		
//		return null;
//	}
	
	@Override
	public boolean equals(Object obj)
	{
		SASEffect other = (SASEffect) obj;
		return this.precondition == other.precondition && this.effect == other.effect;
	}
	
	public Object clone()
	{
		return new SASEffect(this.precondition, this.effect);
	}
	
	@Override
	public String toString()
	{
		return this.precondition + "-> "+this.effect;
	}
}
