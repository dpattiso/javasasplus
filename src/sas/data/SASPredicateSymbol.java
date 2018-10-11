package sas.data;

import sas.util.PDDLConvertable;
import javaff.data.GroundProblem;
import javaff.data.strips.PredicateSymbol;
import javaff.data.strips.Proposition;

public class SASPredicateSymbol implements PDDLConvertable<PredicateSymbol>
{
	private String name;
	
	public SASPredicateSymbol(String name)
	{
//		super(name);
		this.name = name;
	}
	
	public Object clone()
	{
		return new SASPredicateSymbol(this.name);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return this.name.equals(((SASPredicateSymbol) obj).name);
	}
	
	@Override
	public int hashCode()
	{
		return name.hashCode() ^ 31;
	}
	
	@Override
	public String toString()
	{
		return this.name;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Converts to a PredicateSymbol. NOTE- no parameters are held in SAS+, so the returned variable
	 * will not have them either, as is usually the case in PDDL. Note that the value of "static" will 
	 * always be false, as SAS+ ignores these.
	 * @return
	 */
	@Override
	public PredicateSymbol convertToPDDL(SASProblem sproblem)
	{
		PredicateSymbol s = new PredicateSymbol(this.name);
		
		return s;
	}

	/**
	 * Uses the specified GroundProblem as a lookup for a predicate symbol which has the same
	 * signature at this SAS predicate. This conversion will include the correct types and static
	 * value.
	 * @throws NullPointerException if no such predicate exists.
	 * @return
	 */
	@Override
	public PredicateSymbol convertToPDDL(SASProblem sproblem, GroundProblem pddlProblem)
	{
		for (Proposition p : pddlProblem.getGroundedPropositions())
		{
			if (p.getPredicateSymbol().toString().equals(this.name))
				return p.getPredicateSymbol();
		}
		
		throw new NullPointerException("No match found in PDDL problem for predicate "+this.name);
	}
}
