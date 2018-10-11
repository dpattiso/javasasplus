package sas.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import sas.util.PDDLConvertable;
import sas.util.SASException;

import javaff.data.Action;
import javaff.data.Fact;
import javaff.data.GroundProblem;
import javaff.data.strips.And;
import javaff.data.strips.Proposition;
import javaff.planning.STRIPSState;


public class SASState implements PDDLConvertable<STRIPSState>
{
	public SortedMap<Integer, SASVariable> vars;
	
	public SASState()
	{
		this.vars = new TreeMap<Integer, SASVariable>();
	}
	
	public SASState(Collection<SASVariable> vars)
	{
		this();
		this.vars = new TreeMap<Integer, SASVariable>();
		for (SASVariable v : vars)
		{
			this.addVariable(v);
		}
	}
	
	public SASState(SASVariable v)
	{
		this();
		this.addVariable(v);
	}
	
	/**
	 * Sets the value of the specified variable.
	 * @param variable
	 * @param value
	 * @return True if the variable was set, false if not (i.e. it does not exist in the state).
	 */
	public boolean setValue(int variable, int value)
	{
		if (this.vars.containsKey(variable) == false)
		{
			return false;
		}
		
		this.vars.get(variable).setCurrentValue(value);
		return true;
	}
	
	public boolean isTrue(Map<Integer, Integer> varValueMap)
	{
		for (Entry<Integer, Integer> e : varValueMap.entrySet())
		{
			if (this.isTrue(e.getKey(), e.getValue()) == false)
				return false;			
		}
		
		return true;
	}
	
	public boolean isTrue(int variable, int value)
	{
		return this.vars.get(variable).getCurrentIndex() == value;
	}
	
	public boolean isTrue(SASLiteral l)
	{
		return this.isTrue(l.getVariableId(), l.getValueId());
	}
	
	/**
	 * Applies the action to this state, regardless of whether it is actually applicable. A new state is returned, this state is
	 * not modified at all.
	 * @param a
	 * @return
	 */
	public SASState apply(SASAction a)
	{
		SASState succ = (SASState) this.clone();
		
		for (Entry<Integer, SASEffect> eff : a.getEffects().entrySet())
		{
			succ.vars.get(eff.getKey()).setCurrentValue(eff.getValue().effect);
		}
		
		return succ;
	}
	
	public Map<Integer, Integer> getMap()
	{
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		for (Entry<Integer, SASVariable> v : this.vars.entrySet())
		{
			map.put(v.getKey(), v.getValue().getCurrentIndex());
		}
		
		return map;
	}
	
	public SASVariable getVariable(int id)
	{
		return this.vars.get(id);
	}
	
	public void addVariable(SASVariable v)
	{
		this.vars.put(v.getId(), v);
	}
	
	public boolean containsVariable(int id)
	{
		return this.vars.containsKey(id);
	}
	
//	public Set<SASProposition> getValue(SortedSet<SASAxiom> axioms)
//	{
//		HashSet<SASProposition> state = new HashSet<SASProposition>();
//		for (SASVariable v : this.vars.values())
//		{
//			SASLiteral l = v.getValue();
//			if (l instanceof SASDerivedPredicate)
//			{
//				l = ((SASDerivedPredicate)l).getValue(this, axioms);
//			}
//			
//			state.add((SASProposition) l);
//		}
//		
//		return state;
//	}

	public SASLiteral getValue(int varIndex)
	{
		return this.vars.get(varIndex).getValue();
	}
	
	public int getValueIndex(int varIndex)
	{
		return this.vars.get(varIndex).getCurrentIndex();
	}

	
	/**
	 * Get a STRIPSState which contains the current value of each variable in this SAS state.
	 * 
	 */
	@Override
	public STRIPSState convertToPDDL(SASProblem sproblem)
	{
		SASState eva = sproblem.getCurrentState();
		
		Set<Fact> pddl = new HashSet<Fact>();
		for (SASVariable p : eva.vars.values())
		{
			pddl.add(p.getValue().convertToPDDL(sproblem));
		}
		
		STRIPSState s = new STRIPSState(new HashSet<Action>(), pddl, new And());
		
		return s;
	}
	

	
	/**
	 * Get a STRIPSState which contains the current value of each variable in this SAS state. Goal is empty.
	 * 
	 */
	@Override
	public STRIPSState convertToPDDL(SASProblem sproblem, GroundProblem pddlProblem)
	{
		SASState eva = sproblem.getCurrentState();
		
		Set<Fact> pddl = new HashSet<Fact>();
		for (SASVariable p : eva.vars.values())
		{
			pddl.add(p.getValue().convertToPDDL(sproblem, pddlProblem));
		}
		
		STRIPSState s = new STRIPSState(new HashSet<Action>(), pddl, new And());
		
		return s;
	}
	
	@Override
	public int hashCode()
	{
		int hash = this.vars.hashCode() ^ 31;
		return hash;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		SASState other = (SASState) obj;
		if (this.vars.size() != other.vars.size())
			return false;
		
		for (Integer v : this.vars.keySet())
		{
			int thisValue = this.vars.get(v).getCurrentIndex();
			int otherValue = other.vars.get(v).getCurrentIndex();
			
			if (thisValue != otherValue)
				return false;
		}
		return true;
	}
	
	public Object clone()
	{
		SASState clone = new SASState();
		for (SASVariable v : this.vars.values())
		{
			clone.addVariable((SASVariable) v.clone());
		}
		
		return clone;
	}
	
	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		for (SASVariable v : this.vars.values())
		{
			buf.append(v.toString()+"\n");
		}
		
		return buf.toString();
	}
	
}
