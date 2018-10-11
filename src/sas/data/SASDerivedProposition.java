package sas.data;

import java.util.Collections;
import java.util.List;

import javaff.data.GroundProblem;
import javaff.data.strips.Proposition;

public class SASDerivedProposition extends SASProposition
{

	public SASDerivedProposition(SASPredicateSymbol p)
	{
		super(p);
	}

	public SASDerivedProposition(String predicateSymbol)
	{
		super(predicateSymbol);
	}

	
	@Override
	public Object clone()
	{
		SASDerivedProposition clone = new SASDerivedProposition((SASPredicateSymbol) super.getPredicateSymbol().clone());
		clone.setValueId(this.getValueId());
		clone.setVariableId(this.getVariableId());
				
		assert(this.hashCode() == clone.hashCode());
		return clone;
	}
	
	@Override
	public int hashCode()
	{
		return super.hashCode() ^ 31;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof SASDerivedPredicate == false)
			return false;
		
		return super.equals(obj);
	}
	
	@Override
	public Proposition convertToPDDL(SASProblem sproblem)
	{
		return super.convertToPDDL(sproblem);
	}
	
	@Override
	public Proposition convertToPDDL(SASProblem sproblem,
			GroundProblem pddlProblem) throws NullPointerException
	{
		return this.convertToPDDL(sproblem);
	}
	
	@Override
	public void addParameter(SASParameter p)
	{
		
	}
	
	@Override
	public List<SASParameter> getParameters()
	{
		return Collections.EMPTY_LIST;
	}
	
	@Override
	public boolean removeParameter(SASParameter p)
	{
		return false;
	}
	
	@Override
	public void setParameters(List<SASParameter> params)
	{
		
	}
	
	
}
