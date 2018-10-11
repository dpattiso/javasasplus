package sas.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.TreeMap;

import sas.util.PDDLConvertable;

import javaff.data.Action;
import javaff.data.GroundProblem;
import javaff.data.Parameter;
import javaff.data.strips.And;
import javaff.data.strips.Not;
import javaff.data.strips.OperatorName;
import javaff.data.strips.PDDLObject;
import javaff.data.strips.STRIPSInstantAction;
import javaff.data.strips.SingleLiteral;


public class SASAction implements PDDLConvertable<STRIPSInstantAction>//, Comparable<SASAction>
{
	//sas+ fields
	private String sasOperatorName;
	private int sasOperatorId;
	private Map<Integer, Integer> sasPrevails;
	private Map<Integer, SASEffect> sasEffects;
	private Map<Integer, Integer> sasEffectConditions;
	
	private Map<Integer, Integer> allPreconditions; //cache this here -- massive speedups 
	
	//common
	/**
	 * Defaults to 1.
	 */
	private double cost;

	public SASAction(int id, String name)
	{
		this.sasOperatorId = id;
		this.sasOperatorName = name;
		this.cost = 1d;
		
		this.sasPrevails = new TreeMap<Integer, Integer>();
		this.sasEffects = new TreeMap<Integer, SASEffect>();
		this.sasEffectConditions = new HashMap<Integer, Integer>(1);
		
		this.allPreconditions = new HashMap<Integer, Integer>();
		this.setupAllPreconditions();
	}

	/**
	 * Returns an unmodifiable map which is the union of the normal PCs and prevails.
	 * @return
	 */
	public Map<Integer, Integer> getPreconditions()
	{
		return this.allPreconditions;
//		Map<Integer, Integer> pcs = new HashMap<Integer, Integer>(this.sasPrevails); //add prevails here
//		
//		for (Entry<Integer, SASEffect> eff : this.sasEffects.entrySet())
//		{
//			pcs.put(eff.getKey(), eff.getValue().precondition);
//		}
//		
//		pcs.putAll(this.sasEffectConditions);
//		
//		return Collections.unmodifiableMap(pcs);
	}
	
	/**
	 * Previously, calling {@link #getPreconditions()} would construct the returned set explicitely
	 * on every call. In the case of things like the causal graph heuristic, this is a massive overhead,
	 * so this list is instead cached as a field, as it will almost never change after action
	 * initialisation. This method performs that setup, by putting the Prevail, preconditions of the 
	 * {@link SASEffect}s and any SAS effect conditions into a single Map.
	 */
	protected void setupAllPreconditions()
	{
		Map<Integer, Integer> pcs = new HashMap<Integer, Integer>(this.sasPrevails); //add prevails here
		
		for (Entry<Integer, SASEffect> eff : this.sasEffects.entrySet())
		{
			pcs.put(eff.getKey(), eff.getValue().precondition);
		}
		
		pcs.putAll(this.sasEffectConditions);
		
		this.allPreconditions = pcs;
	}
	
	
	/**
	 * Returns an unmodifiable map of effects, var -> value. Constructed at execution time.
	 * @see #getEffects() Used to construct the map returned by this method.
	 * @return
	 */
	public Map<Integer, Integer> getEffectsMap()
	{
		//TODO this might as well be cached
		Map<Integer, Integer> effects = new HashMap<Integer, Integer>();
		
		for (Entry<Integer, SASEffect> eff : this.sasEffects.entrySet())
		{
			effects.put(eff.getKey(), eff.getValue().effect);
		}
		
		return Collections.unmodifiableMap(effects);
	}
	
	@Override
	public int hashCode()
	{
		int hash = this.allPreconditions.hashCode() ^ 
				((Double) this.cost).hashCode() ^
				this.sasEffectConditions.hashCode() ^
				this.sasEffects.hashCode() ^
				this.sasOperatorId ^
				this.sasOperatorName.hashCode() ^
				this.sasPrevails.hashCode();
		
		return hash;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		SASAction other = (SASAction) obj;
		if (this.sasOperatorId != other.sasOperatorId)
			return false;
		if (this.getOperatorName().equals(other.getOperatorName()) == false)
			return false;
		if (this.sasPrevails.equals(other.sasPrevails) == false)
			return false;
		if (this.sasEffects.equals(other.sasEffects) == false)
			return false;
		if (this.sasEffectConditions.equals(other.sasEffectConditions) == false)
			return false;
		
		return true;
	}
	
	public boolean isApplicable(SASState s)
	{
		for (Entry<Integer, Integer> e : sasPrevails.entrySet())
		{
			if (s.containsVariable(e.getKey()) == false || s.getValueIndex(e.getKey()) != e.getValue())
				return false;
		}
		
		for (Entry<Integer, SASEffect> e : sasEffects.entrySet())
		{
			if (s.containsVariable(e.getKey()) == false || s.getValueIndex(e.getKey()) != e.getValue().precondition)
				return false;
		}
		
		//effect conditions?
		for (Entry<Integer, Integer> e : sasEffectConditions.entrySet())
		{
			if (s.containsVariable(e.getKey()) == false || s.getValueIndex(e.getKey()) != e.getValue())
				return false;
		}
		
		
		return true;
	}
	
	public void apply(SASState s)
	{
		for (Entry<Integer, SASEffect> e : sasEffects.entrySet())
		{
			s.getVariable(e.getKey()).setCurrentValue(e.getValue().effect);
		}
	}

	@Override
	public STRIPSInstantAction convertToPDDL(SASProblem sproblem)
	{
		STRIPSInstantAction a = new STRIPSInstantAction();
		a.setCost(new BigDecimal(this.cost));
		
		Scanner sc = new Scanner(this.getOperatorName());
		a.setName(new OperatorName(sc.next()));
		a.setParameters(new ArrayList<Parameter>());
		while (sc.hasNext())
		{
			a.getParameters().add(new PDDLObject(sc.next()));
		}

		And pcs = new And();
		And effects = new And();
		//form prevails as PCs
		for (Entry<Integer, Integer> e : this.sasPrevails.entrySet())
		{
			SingleLiteral prev = sproblem.variables.get(e.getKey()).getValue(e.getValue()).convertToPDDL(sproblem);
			pcs.add(prev);
		}
		//get normal PCs and their effects
		for (Entry<Integer, SASEffect> e : this.sasEffects.entrySet())
		{
			SingleLiteral pc = sproblem.variables.get(e.getKey()).getValue(e.getValue().precondition).convertToPDDL(sproblem);
			pcs.add(pc);

			SingleLiteral eff = sproblem.variables.get(e.getKey()).getValue(e.getValue().effect).convertToPDDL(sproblem);
			effects.add(eff);
		}
		//effect conditions
		for (Entry<Integer, Integer> e : this.sasEffectConditions.entrySet())
		{
			SingleLiteral prev = sproblem.variables.get(e.getKey()).getValue(e.getValue()).convertToPDDL(sproblem);
			pcs.add(prev);
		}
		
		//set final PCs and Effects
		a.setCondition(pcs);
		a.setEffect(effects);
		
		return a;
	}

	@Override
	public STRIPSInstantAction convertToPDDL(SASProblem sproblem, GroundProblem pddlProblem)
													throws NullPointerException
	{
		
		String aStr = this.getOperatorName();
		
		for (Action pddlAction : pddlProblem.getActions())
		{
			if (pddlAction.toString().equals(aStr))
				return (STRIPSInstantAction) pddlAction;
		}
		
		throw new NullPointerException("Action with signature "+this.getOperatorName()+" could not be found in PDDL problem");
	}
	
	
	public Object clone()
	{
		SASAction clone = new SASAction(this.sasOperatorId, this.getOperatorName());
		clone.sasPrevails = new TreeMap<Integer, Integer>(this.sasPrevails);
		clone.sasEffects = new TreeMap<Integer, SASEffect>(this.sasEffects);
		clone.sasEffectConditions = new HashMap<Integer, Integer>(this.sasEffectConditions);
		clone.cost = this.cost;
		clone.setupAllPreconditions();
		
		assert(this.hashCode() == clone.hashCode());
		
		return clone;
	}
	
	@Override
	public String toString()
	{
		return this.getOperatorName();
	}

	public String getOperatorName()
	{
		return sasOperatorName;
	}

	public void setOperatorName(String sasOperatorName)
	{
		this.sasOperatorName = sasOperatorName;
	}

	public int getOperatorId()
	{
		return sasOperatorId;
	}

	public void setOperatorId(int sasOperatorId)
	{
		this.sasOperatorId = sasOperatorId;
	}

	/**
	 * Returns an unmodifiable map containing all conditions which must prevail
	 * throughout this action's execution.
	 * @return
	 */
	public Map<Integer, Integer> getPrevails()
	{
		return Collections.unmodifiableMap(sasPrevails);
	}
	
	/**
	 * Adds or sets the specified prevails condition. 
	 * @param var The variable
	 * @param value The value which should hold true across execution of this action
	 * @return The previous prevail value associated with the variable, or null if this is
	 * the first addition.
	 */
	public Integer setPrevail(Integer var, Integer value)
	{
		Integer prev = this.sasPrevails.put(var, value);
		this.setupAllPreconditions();
		return prev;
	}

	/**
	 * Sets all conditions which must prevail
	 * throughout this action's execution.
	 * @return
	 */
	public void setPrevails(Map<Integer, Integer> sasPrevails)
	{
		this.sasPrevails = sasPrevails;
		this.setupAllPreconditions();
	}

	public void setEffects(Map<Integer, SASEffect> sasEffects)
	{
		this.sasEffects = sasEffects;
		this.setupAllPreconditions();
	}
	
	/**
	 * Add or set the specified variable's value as an effect condition. 
	 * @param var
	 * @param value
	 * @return The variable's previous value, or null if this is an addition.
	 */
	public Integer setEffectCondition(Integer var, Integer value)
	{
		Integer res = this.sasEffectConditions.put(var, value);
		this.setupAllPreconditions();
		return res;
	}

	/**
	 * Returns an unmodifiable map containing all effect conditions.
	 * @return
	 * @see #setEffectCondition(Integer, Integer)
	 * @see #setEffectConditions(Map)
	 */
	public Map<Integer, Integer> getEffectConditions()
	{
		return Collections.unmodifiableMap(sasEffectConditions);
	}


	/**
	 * Sets all effect conditions.
	 * @return
	 */
	public void setEffectConditions(Map<Integer, Integer> sasEffectConditions)
	{
		this.sasEffectConditions = sasEffectConditions;
		this.setupAllPreconditions();
	}

	/**
	 * Add or set the specified variable's effect.
	 * @param var The variable
	 * @param effect The effect
	 * @return The variable's previous effect, or null if this is an addition.
	 */
	public SASEffect setEffect(Integer var, SASEffect effect)
	{
		SASEffect res = this.sasEffects.put(var, effect);
		this.setupAllPreconditions();
		return res;
	}

	/**
	 * Returns an unmodifiable map containing all {@link SASEffect}s of this action.
	 * @return 
	 * @see #setEffect(Integer, SASEffect)
	 * @see #setEffects(Map)
	 */
	public Map<Integer, SASEffect> getEffects()
	{
		return Collections.unmodifiableMap(sasEffects);
	}

	public double getCost()
	{
		return cost;
	}

	public void setCost(double cost)
	{
		this.cost = cost;
	}

	//unused because SASAxiom needs a compareTo method which conflicts, and I'm too lazy to fix it.
//	@Override
//	public int compareTo(SASAction other)
//	{
//		return Integer.compareTo(this.sasOperatorId, other.sasOperatorId);
//	}
	
}
