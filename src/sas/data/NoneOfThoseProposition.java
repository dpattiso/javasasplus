package sas.data;

import java.util.ArrayList;
import java.util.List;

import javaff.data.GroundProblem;
import javaff.data.strips.NullFact;
import javaff.data.strips.Proposition;


public class NoneOfThoseProposition extends SASProposition
{
	private final List<SASParameter> EmptyList = new ArrayList<SASParameter>();

	public NoneOfThoseProposition()
	{
		super(new SASPredicateSymbol("NoneOfThose"));
	}
	
	@Override
	public int hashCode()
	{
		return super.getPredicateSymbol().hashCode() ^ 31;
	}
	
	
	public Object clone()
	{
		NoneOfThoseProposition clone = new NoneOfThoseProposition();
		clone.setValueId(super.getValueId());
		clone.setVariableId(super.getVariableId());
		
		assert(this.hashCode() == clone.hashCode());
		
		return clone;
	}
	
	@Override
	public List<SASParameter> getParameters()
	{
		return EmptyList ;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return (obj instanceof NoneOfThoseProposition);
	}
	
	@Override
	public void setParameters(List<SASParameter> params)
	{

	}
	
	
	@Override
	public Proposition convertToPDDL(SASProblem sproblem)
	{
		return new NullFact();
	}
	
	@Override
	public Proposition convertToPDDL(SASProblem sproblem,
			GroundProblem pddlProblem)
	{
		return new NullFact();
	}
	
	@Override
	public boolean removeParameter(SASParameter p)
	{
		return false;
	}
	
	@Override
	public void addParameter(SASParameter p)
	{
		
	}
	
	
}
