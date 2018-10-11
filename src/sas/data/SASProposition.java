package sas.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sas.util.PDDLConvertable;

import javaff.data.GroundProblem;
import javaff.data.strips.Constant;
import javaff.data.strips.PredicateSymbol;
import javaff.data.strips.Proposition;
import javaff.data.strips.SingleLiteral;

public class SASProposition implements SASLiteral, PDDLConvertable<Proposition>
{
	private SASPredicateSymbol symbol;
	private List<SASParameter> params;
	private int variable, value;
	
	//I cannot express to you, dear unknown developer, just how much faster code executes when
	//we cache the hash for these facts. The CG heuristic goes from being terrifyingly slow, to 
	//(somewhat) lightning quick.
	private int hash;
	
	public SASProposition(SASPredicateSymbol p)
	{
		this.value = -1;
		this.variable = -1;
		this.symbol = p;
		this.params = new ArrayList<SASParameter>();
		
		this.updateHash();
	}
	
	public SASProposition(String predicateSymbol)
	{
		this(new SASPredicateSymbol(predicateSymbol));
		this.params = new ArrayList<SASParameter>();
	}
	
	protected int updateHash()
	{
		this.hash = symbol.hashCode() ^ params.hashCode() ^ this.variable ^ this.value ^ 31;
		return this.hash;
	}
	
	public void addParameter(SASParameter p)
	{
//		if (this.params.contains(p) == false)
		this.params.add(p);
		
		this.updateHash();
	}
	
	/**
	 * Compares the ordering of two {@link SASProposition}s. -1 is returned if this object has 
	 * a variable ID lower than the others ({@link #getVariableId()}, or +1 if the reverse is true.
	 * If both literals have the same variable ID, their values are compared instead, with -1 returned 
	 * if this proposition has a lower value, 0 if these are equal, and +1 if the other proposition has a 
	 * higher value.
	 */
	@Override
	public int compareTo(SASLiteral o)
	{
		int res = Integer.compare(this.getVariableId(), o.getVariableId());
		if (res == 0)
			res = Integer.compare(this.getValueId(), o.getValueId());
		
		return res;
	}
	
	public boolean removeParameter(SASParameter p)
	{
		boolean res = this.params.remove(p);
		this.updateHash();
		
		return res;
	}

	public SASPredicateSymbol getPredicateSymbol()
	{
		return symbol;
	}

	public void setPredicateSymbol(SASPredicateSymbol symbol)
	{
		this.symbol = symbol;

		this.updateHash();
	}

	/**
	 * Returns an unmodifiable list of {@link SASParameter}s linked to this proposition.
	 */
	public List<SASParameter> getParameters()
	{
		return Collections.unmodifiableList(params);
	}

	public void setParameters(List<SASParameter> params)
	{
		this.params = params;

		this.updateHash();
	}
	
	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer(this.symbol.toString());
		for (SASParameter p : this.params)
		{
			buf.append(" "+p.toString());
		}
		
		return buf.toString();
	}

	@Override
	public int hashCode()
	{
		return this.hash;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		SASProposition other = (SASProposition) obj;
//		if (this.symbol.equals(other.symbol) == false)
//			return false;
//		
//		return this.params.equals(other.params) && this.variable == other.variable && this.value == other.value;
		
		return this.variable == other.variable && this.value == other.value;
	}

	@Override
	public Object clone()
	{
		SASProposition clone = new SASProposition((SASPredicateSymbol) this.symbol.clone());
		for (SASParameter p : this.params)
		{
			clone.addParameter((SASParameter) p.clone());
		}
		clone.variable = this.variable;
		clone.value = this.value;
		
		clone.updateHash();
		
		assert(this.hashCode() == clone.hashCode());
		
		return clone;
	}

	@Override
	public Proposition convertToPDDL(SASProblem sproblem)
	{
		PredicateSymbol sym = symbol.convertToPDDL(sproblem);
		Proposition p = new Proposition(sym);
		
		for (SASParameter par : this.params)
		{
			p.addParameter(par.convertToPDDL(sproblem));
		}
		
		return p;
	}

	/**
	 * Uses the specified GroundProblem as a lookup for a predicate symbol which has the same
	 * signature at this SAS predicate. This conversion will include the correct types and static
	 * value.
	 * @throws NullPointerException if no such predicate exists.
	 * @return
	 */
	@Override
	public Proposition convertToPDDL(SASProblem sproblem, GroundProblem pddlProblem) //throws NullPointerException
	{
		Proposition converted = this.convertToPDDL(sproblem);
		
		String str = converted.toString();
		for (Proposition p : pddlProblem.getGroundedPropositions())
		{
			if (p.toString().equals(str)) //have to compare string because SAS+ strips out types
				return p;
		}
		
		//TODO investigate this. On pipeworld_tankage, the SAS+ translator produces literals which are not
		//produced by the JavaFF grounding process. I have a suspicion this is related to Constants, but
		//the JavaFF parameter generator is a bit of a mystery to me. Returning null and ignoring 
		//the literal if this happens seems to at least let AUTOGRAPH run
		return null;
		//throw new NullPointerException("No match found in PDDL problem for proposition "+converted);
	}

	@Override
	public int getValueId()
	{
		return this.value;
	}

	@Override
	public int getVariableId()
	{
		return this.variable;
	}

	@Override
	public void setValueId(int value)
	{
		this.value = value;
		
		this.updateHash();
	}

	@Override
	public void setVariableId(int var)
	{
		this.variable = var;

		this.updateHash();
	}
}
