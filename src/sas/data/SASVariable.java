package sas.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import sas.util.PDDLConvertable;

import javaff.data.GroundProblem;
import javaff.data.strips.Proposition;
import javaff.data.strips.SingleLiteral;

public class SASVariable implements Comparable<SASVariable>, PDDLConvertable<Proposition>
{
	//untranslated fields
	private int varId, zeroId;
	
	//translated fields
	private SASParameter object;
	private SASDomain domain;
	
	//common fields
	private int currentValue;
	
	//cached for speedups
	private int hash;
	
	
	public SASVariable(int id, int zeroId, int valueCount)
	{
		this.varId = id;
		this.zeroId = zeroId;
		this.domain = new SASDomain(valueCount);
		
		this.object = new SASParameter("StubObject");
		
		this.currentValue = -1;
		
		this.updateHash();
	}
	
	/**
	 * Get the index of this variable as it was encountered during SAS+ parsing.
	 * @return
	 */
	public int getZeroId()
	{
		return zeroId;
	}
	
	public void setZeroId(int zeroId)
	{
		this.zeroId = zeroId;
	}
	
	@Override
	public int compareTo(SASVariable other)
	{
		if (this.getId() < other.getId())
			return -1;
		else if (this.getId() == other.getId())
			return 0;
		else
			return +1;
	}
	
	public SASLiteral getValue(int index)
	{
		return this.domain.get(index);
	}
	
	/**
	 */
	@Override
	public Proposition convertToPDDL(SASProblem sproblem)
	{
		SASLiteral l = this.getValue();
		
		Proposition pddl = l.convertToPDDL(sproblem);
		
		return pddl;
	}
	
	/**
	 */
	@Override
	public Proposition convertToPDDL(SASProblem sproblem, GroundProblem pddlProblem)
	{
		SASLiteral l = this.getValue();
//		if (l instanceof SASDerivedPredicate)
//		{
//			l = sproblem.evaluate((SASDerivedPredicate) l);
//		}
		
		Proposition pddl = l.convertToPDDL(sproblem, pddlProblem);
		
		return pddl;
	}
	
	
	public Object clone()
	{
		SASVariable clone = new SASVariable(this.getId(), this.getZeroId(), this.getValues().size());
		
		clone.object = (SASParameter) this.getObject().clone();
		clone.domain = (SASDomain) this.getDomain().clone();
		
		clone.setCurrentValue(this.getCurrentIndex());
		clone.updateHash();
		
		return clone;
	}
	
	@Override
	public int hashCode()
	{
		return this.hash;	
	}
	
	protected int updateHash()
	{
		this.hash = this.varId ^ this.zeroId ^ this.currentValue ^ this.domain.hashCode() ^ 
				this.object.hashCode() ^ this.domain.hashCode() ^ 31;
		
		return this.hash;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		SASVariable other = (SASVariable) obj;
		if (this.currentValue != other.currentValue)
			return false;
		if (this.domain.equals(other.domain) == false)
			return false;
		if (this.object.equals(other.object) == false)
			return false;
		if (this.varId != other.varId)
			return false;
		if (this.getZeroId() != other.getZeroId())
			return false;
		
		return true;
	}

	public List<SASLiteral> getValues()
	{
		return this.domain.getLiterals();
	}

	public SASLiteral getValue()
	{
		SASLiteral l = this.domain.get(this.currentValue);
		
		return l;
	}
	
	public int getIndexOf(SASLiteral l)
	{
		return this.domain.indexOf(l);
	}		
	

	public void setObject(SASParameter variable)
	{
		this.object = variable;
		
		this.updateHash();
	}

	public SASParameter getObject()
	{
		return object;
	}

	public void setCurrentValue(int currentValue)
	{
		this.currentValue = currentValue;
		
		this.updateHash();
	}

	public int getCurrentIndex()
	{
		return currentValue;
	}

	public void setValues(List<SASLiteral> values)
	{
		this.domain.setLiterals(values);
		
		this.updateHash();
	}

	/**
	 * Adds the specified proposition to this variables list of possible domain. Note that
	 * no error checking is performed other than checking for duplicates.
	 * @param p
	 */
	public void addValue(SASLiteral p, int valueId)
	{
//		if (this.domain.contains(p) == false)
//		this.domain.remove(valueId);
			this.domain.set(valueId, p);
			
			this.updateHash();
	}

	/**
	 * Set the ID of this variable. Also propagates this new ID to each
	 * of the SASLiterals which are part of this variable.
	 * @param operatorId
	 */
	public void setId(int operatorId)
	{
		this.varId = operatorId;
		for (SASLiteral l : this.domain.getLiterals())
		{
			l.setValueId(this.varId);
		}
		this.updateHash();
	}

	public int getId()
	{
		return varId;
	}
	
	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		if (this.object != null)
			buf.append("["+this.object.toString()+"]");
		else
			buf.append("[null]");
		
		buf.append(" - ID- "+this.varId + " ("+this.getZeroId()+")");
		
		for (int i = 0; i < this.domain.size(); i++)
		{
			buf.append("\n\t");
			if (i == this.currentValue)
				buf.append("*");
			
			buf.append(this.domain.get(i).toString());
		}
		buf.append("\n");
		
		return buf.toString();
	}

	public SASDomain getDomain()
	{
		return domain;
	}

	public void setDomain(SASDomain domain)
	{
		this.domain = domain;
		
		this.updateHash();
	}
}
