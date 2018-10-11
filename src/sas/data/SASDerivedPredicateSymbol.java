package sas.data;

import javaff.data.GroundProblem;
import javaff.data.strips.PredicateSymbol;


public class SASDerivedPredicateSymbol extends SASPredicateSymbol
{
	public SASDerivedPredicateSymbol(String name)
	{
		super(name);
	}
	
	@Override
	public int hashCode()
	{
		int hash = super.hashCode() ^ 31;
		return hash;
	}
	
	public Object clone()
	{
		return new SASDerivedPredicateSymbol(this.getName());
	}
	
	@Override
	public boolean equals(Object obj)
	{
		SASDerivedPredicateSymbol sym = (SASDerivedPredicateSymbol) obj;
		return super.equals(sym);
	}
	
	@Override
	public PredicateSymbol convertToPDDL(SASProblem sproblem)
	{
		return super.convertToPDDL(sproblem);
	}
	
	@Override
	public PredicateSymbol convertToPDDL(SASProblem sproblem,
			GroundProblem pddlProblem)
	{
		return this.convertToPDDL(sproblem);
	}
	
	@Override
	public String toString()
	{
		return super.toString();
	}
}
