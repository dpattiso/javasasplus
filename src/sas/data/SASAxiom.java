package sas.data;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

//has to extend action for DTG transitions
public class SASAxiom extends SASAction implements Comparable<SASAxiom>
{
	private SASDerivedPredicate derivedPredicate; 
	
	//sas+ fields
	private Map<Integer, Integer> sasCondition;
	
	private SASAxiom(int id)
	{
		super(id, "Axiom_"+id+"_-1");
		this.sasCondition = new HashMap<Integer, Integer>();
		super.setCost(0);
	}	

	public SASAxiom(int id, SASDerivedPredicate associatedPredicate)
	{
		this(id);
		this.derivedPredicate = associatedPredicate;
	}
	
	
	
	@Override
	public boolean equals(Object obj)
	{
		boolean supeq = super.equals(obj);
		if (supeq == false)
			return false;
		
		SASAxiom other = (SASAxiom) obj;
		if (this.derivedPredicate.equals(other.derivedPredicate) == false)
			return false;
		if (this.sasCondition.equals(other.sasCondition) == false)
			return false;

		return true;
	}

	@Override
	public boolean isApplicable(SASState s)
	{
		boolean isActionApplicable = super.isApplicable(s);
		if (isActionApplicable == false)
			return false;
		
		for (Entry<Integer, Integer> e : this.sasCondition.entrySet())
		{
			if (s.containsVariable(e.getKey()) == false || s.getValueIndex(e.getKey()) != e.getValue())
				return false;
		}
		
		for (Entry<Integer, SASEffect> e : this.getEffects().entrySet())
		{
			if (s.containsVariable(e.getKey()) == false || s.getValueIndex(e.getKey()) != e.getValue().precondition)
				return false;
		}
		
		return true;
	}
	
	@Override
	public void apply(SASState s)
	{
		//does this state already contain a value for this derived predicate?
		if (s.containsVariable(this.derivedPredicate.getId()) == false)
		{
			//if it does not, add the derived predicate to it, else apply as normal
			s.addVariable(this.derivedPredicate);
		}
		else
		{
			super.apply(s);
		}
	}
	
	@Override
	public String toString()
	{
		return super.toString() + " ("+this.derivedPredicate.toString()+")";
	}
	
	/**
	 * Inner class for conditions and effect mappings.
	 * @author David Pattison
	 *
	 */
	public class SASPair
	{
		public SASProposition condition, effect;
		
		public SASPair(SASProposition condition, SASProposition effect)
		{
			this.condition = condition;
			this.effect = effect;
		}
	}
	
	public Object clone()
	{
		SASAxiom clone = new SASAxiom(this.getOperatorId(), (SASDerivedPredicate) this.derivedPredicate.clone());
		clone.sasCondition = new HashMap<Integer, Integer>(this.sasCondition);
		clone.setEffectConditions(new HashMap<Integer, Integer>(this.getEffectConditions()));
		clone.setEffects(new HashMap<Integer, SASEffect>(this.getEffects()));
		clone.setCost(super.getCost());
		clone.setOperatorName(new String(this.getOperatorName()));
		clone.setPrevails(new HashMap<Integer, Integer>());
		for (Entry<Integer, Integer> e : this.getPrevails().entrySet())
		{
			clone.getPrevails().put(e.getKey(), e.getValue());
		}
		
		assert(this.hashCode() == clone.hashCode());
				
		
		return clone;
	}

//	public void apply(SASState s)
//	{
//		SASState strips = (SASState) s.clone(); //SAS+ requires strips states!
//		
//		for (Entry<SASObject, SASProposition> e : condition.entrySet())
//		{
//			if (e.getValue().isTrue(strips) == false)
//				return;
//		}
//		
//		for (Entry<SASObject, SASPair> e : effect.entrySet())
//		{
//			if (e.getValue().condition.isTrue(s))
//			{
//				strips.removeProposition(e.getValue().condition);
//				strips.addProposition(e.getValue().effect);
//			}
//		}
//	}

//	public boolean isTrue(SASState s)
//	{
//		SASState strips = (SASState) s.clone(); //SAS+ requires strips states!
//		
//		for (Entry<SASObject, SASProposition> e : condition.entrySet())
//		{
//			if (e.getValue().isTrue(strips) == false)
//				return false;
//		}
//		
//		return true;
//	}

	@Override
	public int compareTo(SASAxiom o)
	{
		if (this.derivedPredicate.getAxiomLayer() < o.derivedPredicate.getAxiomLayer())
			return -1;
		else if (this.derivedPredicate.getAxiomLayer() == o.derivedPredicate.getAxiomLayer())
			return 0;
		else
			return 1;
	}

	public SASDerivedPredicate getDerivedPredicate()
	{
		return derivedPredicate;
	}

	public void setDerivedPredicate(SASDerivedPredicate derivedPredicate)
	{
		this.derivedPredicate = derivedPredicate;
	}

	public Map<Integer, Integer> getAxiomCondition()
	{
		return Collections.unmodifiableMap(sasCondition);
	}

	public void setAxiomCondition(Map<Integer, Integer> sasCondition)
	{
		this.sasCondition = sasCondition;
	}
}
