package sas.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import sas.util.PDDLConvertable;
import sas.util.SASException;

import javaff.data.Action;
import javaff.data.Fact;
import javaff.data.GroundProblem;
import javaff.data.Parameter;
import javaff.data.metric.Metric;
import javaff.data.metric.MetricType;
import javaff.data.metric.NamedFunction;
import javaff.data.metric.NumberFunction;
import javaff.data.strips.And;
import javaff.data.strips.Proposition;
import javaff.data.strips.SingleLiteral;


public class SASProblem implements Cloneable, PDDLConvertable<GroundProblem>
{
	/**
	 * If true, flag for optimise plan length was set, if false, then minimising actions costs was 
	 * the metric.
	 */
	public boolean optimisePlanLength; 
	public String name;
	
	public CausalGraph causalGraph;

	public Map<Integer, SASAction> actions;
	public Map<Integer, Integer> initial, goal;
	public SASState state;
	
	public Set<SASProposition> reachableFacts;
	public Map<Integer, SASDerivedPredicate> derivedPredicates;
	
	public Map<Integer, SASVariable> variables;
	public Map<Integer, Map<Integer, SASAxiom>> axioms;
	
	public Map<Integer, SASMutexGroup> mutexes;
	
	public SASProblem()
	{
		this.reset();
	}
	

	
	/**
	 * Get the value of the specified variable index.
	 * @param varId
	 * @param varIndex
	 * @return
	 */
	public SASLiteral getValue(int varId, int varIndex)
	{
		return this.variables.get(varId).getValue(varIndex);
	}

	/**
	 * Get the current value of the specified variable.
	 * @param varId
	 * @return
	 */
	public SASLiteral getValue(int varId)
	{
		return this.variables.get(varId).getValue();
	}
	
	
	protected void reset()
	{
		this.optimisePlanLength = true;
		this.name = "";
		this.actions = new TreeMap<Integer, SASAction>();
//		this.dtgs = new TreeMap<Integer, DomainTransitionGraph>();
		this.causalGraph = null;
		this.goal = new TreeMap<Integer, Integer>();
		this.initial = new HashMap<Integer, Integer>();
		this.reachableFacts = new HashSet<SASProposition>();
		this.derivedPredicates = new TreeMap<Integer, SASDerivedPredicate>();
		this.variables = new TreeMap<Integer, SASVariable>();
		this.axioms = new TreeMap<Integer, Map<Integer, SASAxiom>>();
		this.mutexes = new TreeMap<Integer, SASMutexGroup>();
		
		this.setupInitialState();
	}
	
	/**
	 * This must be called after all files have been parsed. Calling this method creates
	 * the initial state, based on the initial state of variables and axioms.
	 */
	public void setupInitialState()
	{
		this.state = new SASState();
		for (Entry<Integer, Integer> e : this.initial.entrySet())
		{
			SASVariable v = this.variables.get(e.getKey());
//			if (v instanceof SASDerivedPredicate) //ignore derived predicates - they are added when their value is non default
//				continue;
			
			v.setCurrentValue(e.getValue());
			this.state.addVariable(v);
		}
		
		this.state = this.getAxiomState(this.state);
	}
	
	@Override
	public Object clone()
	{
		SASProblem sas = new SASProblem();
		sas.name = this.name;
		sas.actions = new TreeMap<Integer, SASAction>();
		for (Entry<Integer, SASAction> e : this.actions.entrySet())
		{
			sas.actions.put(e.getKey(), (SASAction) e.getValue().clone());
		}
		
//		sas.dtgs = new TreeMap<Integer, DomainTransitionGraph>();
//		for (Entry<Integer, DomainTransitionGraph> e : sas.dtgs.entrySet())
//		{
//			sas.dtgs.put(e.getKey(), (DomainTransitionGraph) e.getValue().clone());
//		}
			
		sas.causalGraph = (CausalGraph) this.causalGraph.clone();
		
		sas.goal = new TreeMap<Integer, Integer>();
		for (Entry<Integer, Integer> e : this.goal.entrySet())
		{
			sas.goal.put(e.getKey(), e.getValue());
		}

		sas.initial = new TreeMap<Integer, Integer>();
		for (Entry<Integer, Integer> e : this.initial.entrySet())
		{
			sas.initial.put(e.getKey(), e.getValue());
		}
		
		sas.reachableFacts = new HashSet<SASProposition>();
		for (SASProposition p : this.reachableFacts)
		{
			sas.reachableFacts.add((SASProposition) p.clone());
		}
		
		sas.state = (SASState) this.state.clone();
		
		sas.variables = new TreeMap<Integer, SASVariable>();
		for (Entry<Integer, SASVariable> e : this.variables.entrySet())
		{
			sas.variables.put(e.getKey(), (SASVariable) e.getValue().clone());
		}
		
		sas.mutexes = new TreeMap<Integer, SASMutexGroup>();
		for (Entry<Integer, SASMutexGroup> e : this.mutexes.entrySet())
		{
			sas.mutexes.put(e.getKey(), (SASMutexGroup) e.getValue().clone());
		}
		
		sas.axioms = new TreeMap<Integer, Map<Integer, SASAxiom>>();
		for (Entry<Integer, Map<Integer, SASAxiom>> e : this.axioms.entrySet())
		{
			sas.axioms.put(e.getKey(), new HashMap<Integer, SASAxiom>());
			for (Entry<Integer, SASAxiom> axiom : e.getValue().entrySet())
			{
				sas.axioms.get(e.getKey()).put(axiom.getKey(), (SASAxiom) axiom.getValue().clone());
			}
		}
		
		sas.derivedPredicates = new TreeMap<Integer, SASDerivedPredicate>();
		for (Entry<Integer, SASDerivedPredicate> e : this.derivedPredicates.entrySet())
		{
			sas.derivedPredicates.put(e.getKey(), (SASDerivedPredicate) e.getValue().clone());
		}
		
//		sas.setupInitialState();
		
		return sas;
	}
	
	public Set<SASLiteral> getGoalLiterals()
	{
		HashSet<SASLiteral> set = new HashSet<SASLiteral>();
		for (Entry<Integer, Integer> g : this.goal.entrySet())
		{
			SASLiteral l = this.variables.get(g.getKey()).getValue(g.getValue());
			
			set.add(l);
		}
		
		return set;
	}
	

	/**
	 * Evaluate the initial state. This must be done manually rather than held because
	 * of derived predicates. All derived predicates are evaluated against all current axioms 
	 * to derive a concrete value. Simple facts are added in their standard form.
	 * @return
	 */
	public SASState getInitialState()
	{
		SASState init = new SASState();
		for (Entry<Integer, Integer> e : this.initial.entrySet())
		{
			SASVariable var = this.variables.get(e.getKey());
			var.setCurrentValue(e.getValue());
			
			if (var instanceof SASDerivedPredicate)
			{
				var.setCurrentValue(((SASDerivedPredicate) var).getDefaultValue());
			}
			
			init.addVariable(var);			
		}
		
//		//should be sorted according to layer
//		int currentLayer = 0;
//		SASState previousState = (SASState) init.clone();
//		do
//		{
//			previousState = (SASState) current.clone();
//			
//			for (Entry<Integer,SASAxiom> ax : this.axioms.entrySet())
//			{
//				if (currentLayer == ax.getKey())
//				{
//					if (ax.getValue().isApplicable(current) == false)
//					{
//						break;
//		//				throw new SASException("Axiom "+ax.ruleNumber+" is not applicable in specified state. This should" +
//		//						"be impossible!");
//					}
//					else
//					{
//						ax.getValue().apply(current);
//					}
//				}
//			}
//		}
//		while (previousState.equals(current) == false);
		
		return init;
	}
	

	/**
	 * Evaluate the current value of any axioms in the problem starting from the specified state.
	 * This must be done manually rather than held because
	 * of derived predicates. All derived predicates are evaluated against all current axioms 
	 * to derive a concrete value. Simple facts are added in their standard form.
	 * @return
	 */
	public SASState getAxiomState(SASState s)
	{
		SASState current = (SASState) s.clone();
		for (SASVariable v : current.vars.values())
		{
			if (v instanceof SASDerivedPredicate)
			{
				((SASDerivedPredicate)v).reset();//setCurrentValue(((SASDerivedPredicate) v).getDefaultValue());
			}
		}
		
		//should be sorted according to layer
		SASState previousState = null;
		do
		{
			previousState = (SASState) current.clone();
			
			for (Entry<Integer, Map<Integer, SASAxiom>> layerSet : this.axioms.entrySet())
			{
				for (Entry<Integer, SASAxiom> axiom : layerSet.getValue().entrySet())
				{
					if (axiom.getValue().isApplicable(current) == true)
					{
						axiom.getValue().apply(current);
					}
				}
			}
		}
		while (previousState.equals(current) == false);
		
		return current;
	}

	/**
	 * Evaluate the current state. This must be done manually rather than held because
	 * of derived predicates. All derived predicates are evaluated against all current axioms 
	 * to derive a concrete value. Simple facts are added in their standard form.
	 * @return
	 * @see #getAxiomState(SASState)
	 */
	public SASState getCurrentState()
	{
		return this.getAxiomState(this.state);
	}
	
	/**
	 * Directly accesses axioms by their ID, rather than the layer on which they operate.
	 * @param axiomId
	 * @return The axiom, or null if it does not exist.
	 */
	public SASAxiom getAxiom(int axiomId)
	{
		for (Map<Integer, SASAxiom> axSet : this.axioms.values())
		{
			if (axSet.containsKey(axiomId))
				return axSet.get(axiomId);
		}
		
		return null;
	}

	@Override
	public GroundProblem convertToPDDL(SASProblem sproblem)
	{
		Set<Action> actions = new HashSet<Action>();
		for (SASAction a : this.actions.values())
		{
			actions.add(a.convertToPDDL(sproblem));
		}
		
		Set<Fact> init = new HashSet<Fact>();
		for (Entry<Integer, Integer> e : this.initial.entrySet())
		{
			SASLiteral sl = sproblem.variables.get(e.getKey()).getValue(e.getValue());
			if (sl instanceof NoneOfThoseProposition)
				continue; //skip things which have not had their value set yet
			
			SingleLiteral i = sl.convertToPDDL(sproblem);
			
			init.add((Proposition) i);
		}
		
		And goal = new And();
		for (Entry<Integer, Integer> e : this.goal.entrySet())
		{
			SASLiteral sl = sproblem.variables.get(e.getKey()).getValue(e.getValue());
			SingleLiteral g = sl.convertToPDDL(sproblem);
			
			goal.add(g);
		}
		
		Map<NamedFunction, BigDecimal> stub = new HashMap<NamedFunction, BigDecimal>();
		Metric metric = new Metric(MetricType.Minimize, new NumberFunction(-1));
		GroundProblem gp = new GroundProblem(actions, init, goal, stub, metric);
		
		HashSet<Fact> reachable = new HashSet<Fact>();
		HashSet<Proposition> grounded = new HashSet<Proposition>(); //easier to just make second set than go through annoying generic-related loops/casts
		for (SASProposition sp : sproblem.reachableFacts)
		{
			Proposition pddl = sp.convertToPDDL(sproblem);
			reachable.add(pddl);
			grounded.add(pddl);
		}
		gp.setReachableFacts(reachable);
		gp.setGroundedPropositions(grounded);
		
		HashSet<Parameter> objects = new HashSet<Parameter>();
		for (SASVariable v : sproblem.variables.values())
		{
			objects.add(v.getObject().convertToPDDL(sproblem));
		}
		
		gp.setObjects(objects);
		gp.getSTRIPSInitialState();
		gp.getMetricInitialState();
		gp.getTemporalMetricInitialState();
		
		return gp;
	}

	@Override
	public GroundProblem convertToPDDL(SASProblem sproblem, GroundProblem pddlProblem)
	{
		Set<Action> actions = new HashSet<Action>();
		for (SASAction a : this.actions.values())
		{
			actions.add(a.convertToPDDL(sproblem, pddlProblem));
		}
		
		Set<Fact> init = new HashSet<Fact>();
		for (Entry<Integer, Integer> e : this.initial.entrySet())
		{
			SASLiteral sl = sproblem.variables.get(e.getKey()).getValue(e.getValue());
			if (sl instanceof NoneOfThoseProposition)
				continue; //skip things which have not had their value set yet
			
			SingleLiteral i = sl.convertToPDDL(sproblem, pddlProblem);
			
			init.add((Proposition) i);
		}
		
		And goal = new And();
		for (Entry<Integer, Integer> e : this.goal.entrySet())
		{
			SASLiteral sl = sproblem.variables.get(e.getKey()).getValue(e.getValue());
			SingleLiteral g = sl.convertToPDDL(sproblem, pddlProblem);
			
			goal.add(g);
		}
		
		Map<NamedFunction, BigDecimal> stub = new HashMap<NamedFunction, BigDecimal>();
		Metric metric = new Metric(MetricType.Minimize, new NumberFunction(-1));
		GroundProblem gp = new GroundProblem(actions, init, goal, stub, metric);
		
		HashSet<Fact> reachable = new HashSet<Fact>();
		HashSet<Proposition> grounded = new HashSet<Proposition>(); //easier to just make second set than go through annoying generic-related loops/casts
		for (SASProposition sp : sproblem.reachableFacts)
		{
			Proposition pddl = sp.convertToPDDL(sproblem, pddlProblem);
			reachable.add(pddl);
			grounded.add(pddl);
		}
		gp.setReachableFacts(reachable);
		gp.setGroundedPropositions(grounded);
		
		HashSet<Parameter> objects = new HashSet<Parameter>();
		for (SASVariable v : sproblem.variables.values())
		{
			objects.add(v.getObject().convertToPDDL(sproblem, pddlProblem));
		}
		
		gp.setObjects(objects);
		gp.getSTRIPSInitialState();
		gp.getMetricInitialState();
		gp.getTemporalMetricInitialState();
		
		return gp;
	}

	public void setCurrentState(SASState next)
	{
		this.state = next;
	}
}
