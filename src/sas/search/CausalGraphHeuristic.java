package sas.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Map.Entry;

import org.jgrapht.alg.DijkstraShortestPath;

import sas.data.*;
import javaff.search.UnreachableGoalException;

/**
 * An implementation of the causal graph heuristic as described in "A Planning Heuristic Based on Causal Graph Analysis" by Malte Helmert,
 * 2007. This implementation has a caching functionality to speed up computing estimates, as many 
 * facts can have their estimates computed by the side-effect of computing another goal's estimate. This
 * caching functionality is disabled by default, but can be enabled by {@link #setUseCache(boolean)}.
 * 
 * @author David Pattison
 *
 */
public class CausalGraphHeuristic implements SASHeuristic
{	
	/**
	 * Whether the CG heuristic should cache results for faster lookups.
	 */
	private boolean useCache;
	
	private int[] levels; //levels used in breaking cycles between vertices in CG
	private CausalGraph acyclicCG; //the CG (once cycles have been broken)
	private HashMap<DomainTransitionGraph, Map<Integer, DTGNode>> lookup; //Mapping of variables/nodes inside each DTG
	private HashMap<CGHLookupTuple, Double> cache; //lookup cache for accessing previously computed estimates

	/**
	 * Initialises all fields. Sets the useCache flag to false.
	 */
	protected CausalGraphHeuristic()
	{
		this.levels = null;
		this.acyclicCG = null;
		this.lookup = new HashMap<DomainTransitionGraph, Map<Integer,DTGNode>>();
		this.cache = new HashMap<CausalGraphHeuristic.CGHLookupTuple, Double>();
		
		this.setUseCache(false);
	}
	
	/**
	 * Create a CG heuristic and initialise it based upon the problem specified. If 
	 * the flag is set, the causal graph used in heuristic estimation has any cycles
	 * broken. If not, the causal graph is assumed to already be acyclic. Note that
	 * if the flag is set to true, the {@link SASProblem}'s CG will be overwritten
	 * with the new acyclic CG.
	 * @param problem The SAS problem to use during estimation.
	 * @param breakCycles If true, the CG has it's cycles broken and the resulting CG
	 * is used to produce estimates.
	 */
	public CausalGraphHeuristic(SASProblem problem, boolean breakCycles)
	{
		this.lookup = new HashMap<DomainTransitionGraph, Map<Integer, DTGNode>>();
		this.cache = new HashMap<CausalGraphHeuristic.CGHLookupTuple, Double>();
		
		this.setupVariableLevels(problem);

		if (breakCycles)
			problem.causalGraph = this.detectAndBreakCycles(problem.causalGraph);
		
		this.acyclicCG = problem.causalGraph;
	}
	
	/**
	 * If true, the distances to side-effect goals found during computing estimates
	 * for other goals are cached locally and returned if these are later asked for a 
	 * heuristic estimate.
	 * @see #resetCache() Clears the cache. Should be called after each state change.
	 * @see #isUsingCache()
	 * @param useCache
	 */
	public void setUseCache(boolean useCache)
	{
		this.useCache = useCache;
	}
	
	/**
	 * Returns whether the estimates to a goal are returned based upon a previously derived heuristic
	 * estimate. This may be computed as a side-effect of computing another literals estimate.
	 * @see #setUseCache(boolean)
	 * @see #resetCache()
	 * @return
	 */
	public boolean isUsingCache()
	{
		return useCache;
	}
	
	public Object clone()
	{
		CausalGraphHeuristic clone = new CausalGraphHeuristic();
		
		clone.levels = this.levels.clone();
		clone.acyclicCG = (CausalGraph) this.acyclicCG.clone();
		clone.lookup = new HashMap<DomainTransitionGraph, Map<Integer,DTGNode>>(this.lookup);
		clone.cache = new HashMap<CausalGraphHeuristic.CGHLookupTuple, Double>(this.cache);
		
		return clone;
	}
	
	public void resetCache()
	{
		this.cache.clear();
	}
	
	/**
	 * C++ allows for maps with X elements as they key -- Java does not, so this is needed to emulate the original CG Heuristic. I'm sure there is a more elegant way,
	 * but I won't be looking for it.
	 * 
	 * @author David Pattison
	 *
	 */
	protected class CGHLookupTuple
	{
		public int variableId;
		public SASState state;
		public int startVal;
		public int val;
		//public double dist;
		
		public CGHLookupTuple(int varId, SASState state, int startVal, int val)//, double dist)
		{
			this.variableId = varId;
			this.state = state;
			this.startVal = startVal;
			this.val = val;
			//this.dist = dist;
		}
		
		@Override
		public int hashCode()
		{
			//sweet baby jesus this is bad
			return new String("V"+this.variableId+"S"+this.state.hashCode()+"SV"+this.startVal+"V"+this.val).hashCode();
		}
		
		@Override
		public boolean equals(Object obj)
		{
			CGHLookupTuple other = (CGHLookupTuple) obj;
			return this.variableId == other.variableId && this.startVal == other.startVal &&
					this.state.equals(other.state) && this.val == other.val;
		}
	}
		
	
	protected void setupDTGs() 
	{
	    for (DomainTransitionGraph dtg : this.acyclicCG.getDTGs()) 
	    {
	    	HashMap<Integer, DTGNode> map = new HashMap<Integer, DTGNode>();
	    	for (SASLiteral l : dtg.getVariable().getValues()) 
	        {
	    		DTGNode node = new DTGNode(l.getValueId());
	    			
	            map.put(l.getValueId(), node);
	        }
	    	lookup.put(dtg, map);
	    }
	
	}
	

	public double getEstimate(SASState state, SASLiteral goal) throws UnreachableGoalException
	{
		HashSet<SASLiteral> goalSet = new HashSet<SASLiteral>();
		goalSet.add(goal);
		
		return this.getEstimate(state, goalSet);
	}
	
	@Override
	public double getEstimate(SASState state, Collection<SASLiteral> goals) throws UnreachableGoalException
	{
		double cost = 0;
		for (SASLiteral g : goals)
		{
			double h = this.solveProblem(state, new VarValuePair(g.getVariableId(), g.getValueId()));
			
			if (h == Unreachable)
				return Unreachable;
			
			cost += h;
		}
		
		return cost;
	}
	
	public double solveProblem(SASState initial, VarValuePair goal) throws UnreachableGoalException
	{
		this.setupDTGs();
		
		DomainTransitionGraph dtg = this.acyclicCG.getDTG(goal.variable);
		double h = solveProblem(initial, dtg, initial.getValueIndex(goal.variable), goal.value);
		
		return h;
	}
	
	protected double solveProblem(SASState state, DomainTransitionGraph dtg, int startVal, int goalVal) 
	{
	    int varId = dtg.getVariable().getId();
	    
	    if (startVal == goalVal || state.getValueIndex(varId) == goalVal)
	        return 0d;
	
	    DTGNode start = this.lookup.get(dtg).get(startVal);
	    assert(start != null);

	    //check cache first
	    if (this.isUsingCache())
	    {
	    	CGHLookupTuple toCheck = new CGHLookupTuple(varId, state, startVal, goalVal);
	    	
	    	Double dist = this.cache.get(toCheck);
	    	if (dist != null)
	    		return dist;
	    }

	    if (start.dists.isEmpty())
	    {
		    for (SASLiteral l : dtg.getVariable().getDomain().getLiterals())
		    {
		    	start.dists.put(l.getValueId(), Unreachable);
		    }
		    
		    start.dists.put(startVal, 0d);
		    
		    PriorityQueue<DTGQueueNode> queue = new PriorityQueue<DTGQueueNode>();
		    queue.add(new DTGQueueNode(start, 0d));
		    while (queue.isEmpty() == false)
		    {
		    	DTGQueueNode best = queue.remove();
		    	double sourceDist = best.dist;
		    	DTGNode source = best.node;
		    	
		    	if (start.dists.get(source.value) < sourceDist)
		    	{
		    		continue;
		    	}
		    	

		    	for (DTGActionEdge e : dtg.outgoingEdgesOf(dtg.getVariable().getValue(source.value)))
		    	{
		    		SASLiteral targetLiteral = e.getEffect();
		    		DTGNode target = this.lookup.get(dtg).get(targetLiteral.getValueId());
		    		
		    		double targetDist = start.dists.get(target.value);
		    		
		    		double newDist = sourceDist + e.getAction().getCost();
		    		for (SASLiteral pc : e.getAssociatedPcs())
		    		{
		    			if (newDist >= targetDist)
		    				break;
		    			
		    			SASState childState = (SASState) state;//.clone();
		    			childState.setValue(varId, source.value);
		    			
		    			int pcCurrentVal = childState.getValueIndex(pc.getVariableId());
		    			int pcTargetVal = pc.getValueId();
		    			DomainTransitionGraph pcDTG = this.acyclicCG.getDTG(pc.getVariableId());
		    			
		    			double recursiveCost = this.solveProblem(childState, pcDTG, pcCurrentVal, pcTargetVal);
		    			
		    			if (recursiveCost == Unreachable)
		    				newDist = Unreachable;
		    			else
		    				newDist = newDist + recursiveCost;
		    		}
		    		
		    		if (targetDist > newDist)
		    		{
		    			targetDist = newDist;

		    			start.dists.put(targetLiteral.getValueId(), newDist);
		    			
		    			queue.add(new DTGQueueNode(target, newDist));
		    		}
		    	}
		    			
		    }
	    }
	    
	    if (this.isUsingCache())
	    {

	    	for (Entry<Integer, Double> e : start.dists.entrySet())
	    	{
	    		int val = e.getKey();
	    		Double distance = e.getValue();
	    		
	    		if (val == startVal)
	    			continue;
	    		
	    		CGHLookupTuple newTuple = new CGHLookupTuple(varId, state, startVal, val);

	    		this.cache.put(newTuple, distance);
	    	}
	    	
	    	
	    }
	    
	    double h = start.dists.get(goalVal);
	    return h;
	}
	
	//TODO this is a messy way of getting the priority queue to store each node's value- could use the internal hashmap
	protected class DTGQueueNode implements Comparable<DTGQueueNode>
	{
		public DTGNode node;
		public double dist;
		
		public DTGQueueNode(DTGNode node, double dist)
		{
			this.node = node;
			this.dist = dist;
		}

		@Override
		public int compareTo(DTGQueueNode other)
		{
			if (this.dist < other.dist)
				return -1;
			else if (this.dist > other.dist)
				return +1;
			else
				return 0;
		}		
		
		
		@Override
		public String toString()
		{
			return this.node.value + ": " + this.dist;
		}
	}
	
	/**
	 * Finds a path to the goal value specified in the DTG specified. The start value is determined by the
	 * state provided. Dijkstra is used to find the path.
	 * @param dtg The DTG to search through.
	 * @param state The state to start at.
	 * @param goalValue The goal value of the DTG's variable
	 * @return The shortest path between the current value and goal value in the DTG.
	 */
	protected SASPlan findSingleDTGPath(DomainTransitionGraph dtg, SASState state, int goalValue)
	{
		if (state.containsVariable(dtg.getDTGIndex()) && state.getValue(dtg.getDTGIndex()).getValueId() == goalValue)
			return new SASPlan();
		
		SASLiteral lowCurrentLiteral =  dtg.getVariable().getValue();
		SASLiteral lowGoalLiteral = dtg.getVariable().getValue(goalValue);

		List<DTGActionEdge> plan = null;
		try
		{
			plan = DijkstraShortestPath.findPathBetween(dtg, lowCurrentLiteral, lowGoalLiteral);
		}
		catch (IllegalArgumentException e)
		{
			System.out.println("No path from "+lowCurrentLiteral+" to "+lowGoalLiteral);
		}


		if (plan != null)
		{
			SASPlan best = new SASPlan();
			for (DTGActionEdge a : plan)
				best.addAction(a.getAction());

			
			return best;
		}
		
		return null;
	}
	
	/**
	 * Breaks any cycles which exist between 2 vertices in the causal graph.
	 * @see #breakCycle(CausalGraph, DomainTransitionGraph, DomainTransitionGraph, boolean)
	 * @param cg
	 * @return
	 */
	public CausalGraph detectAndBreakCycles(final CausalGraph cg)
	{
		CausalGraph clone = (CausalGraph) cg.clone();
		for (DomainTransitionGraph v : clone.vertexSet())
		{
			Collection<DomainTransitionGraph> conn = clone.getIncomingVertices(v);
			for (DomainTransitionGraph vPrime : conn)
			{
				if (hasCyclicLink(clone, v, vPrime))
				{
					breakCycle(clone, v, vPrime, true);
				}
			}
			
		}
		
		return clone;
	}
	
	/**
	 * Determines whether the specified DTGs have a cyclic link in the causal graph. Note that
	 * this only covers order-1 links, i.e. A is connected to B and B is connected to A -- intermediate
	 * connections are not considered.
	 * @param cg The causal graph
	 * @param high The first DTG
	 * @param low The second DTG
	 * @return True if a cycle exists, false otherwise.
	 */
	protected boolean hasCyclicLink(CausalGraph cg, DomainTransitionGraph high, DomainTransitionGraph low)
	{
		Collection<DomainTransitionGraph> highConn = cg.getOutgoingVertices(high);
		Collection<DomainTransitionGraph> lowConn = cg.getOutgoingVertices(low);
		return lowConn.contains(high) && highConn.contains(low);
	}
	
	/**
	 * Breaks the cycle between two vertices in the CG, based upon the criteria set forth in 
	 * Helmert's CG paper.
	 * @param cg
	 * @param v
	 * @param vPrime
	 * @param removePCs
	 * @return
	 */
	protected Set<SASLiteral> breakCycle(CausalGraph cg, DomainTransitionGraph v, DomainTransitionGraph vPrime, boolean removePCs)
	{
		//find out which dtg is "higher" -- the higher var appears in fewer PCs than the other
		//
		boolean vHigher = this.isHigher(v.getVariable(), vPrime.getVariable());
		if (vHigher == false)
		{
			return null;
		}

		HashSet<SASLiteral> toIgnore = new HashSet<SASLiteral>();
		//detect if v appears as a PC
		for (DTGActionEdge e : vPrime.edgeSet())
		{
			//no point in looking at e.getPC because it will be for a value of vPrime
			for (SASLiteral ass : e.getAssociatedPcs())
			{
				if (ass.getVariableId() == v.getDTGIndex())
				{
					toIgnore.add(ass);
					break;
				}
			}
			
			if (removePCs)
			{
				for (SASLiteral pc : toIgnore)
					e.removeAssociatedPrecondition(pc);
			}
		}

		cg.removeEdge(vPrime, v);
//		cg.removeEdge(v, vPrime);
		
		return toIgnore;
		
		//sanity check
//		for (SASLiteral ig : toIgnore)
//		{
//			if (ig.getVariableId() != v.index)
//				throw new NullPointerException("Error in breaking cycles");
//		}
	}
	
	
	/**
	 * Is A higher than B.
	 * @param a
	 * @param b
	 * @return
	 */
	protected boolean isHigher(SASVariable a, SASVariable b)
	{
		int aLevel = this.levels[a.getId()];
		int bLevel = this.levels[b.getId()];
		return aLevel < bLevel;
	}
	
	protected void setupVariableLevels(SASProblem problem)
	{
		this.levels = new int[problem.variables.size()];
		for (int i = 0; i < levels.length; i++)
			levels[i] = 0;
		
		for (SASAction a : problem.actions.values())
		{
			for (Entry<Integer, Integer> pc : a.getPreconditions().entrySet())
			{
				levels[pc.getKey()] = levels[pc.getKey()] + 1;
			}			
		}
		
	}
	
	protected class VarValuePair implements Comparable<VarValuePair>
	{
		public int variable, value;
		
		public VarValuePair(int variable, int value)
		{
			this.variable = variable;
			this.value = value;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			return this.variable == ((VarValuePair) obj).variable && this.value == ((VarValuePair) obj).value;
		}
		
		@Override
		public String toString()
		{
			return this.variable+" -> "+ this.value;
		}

		@Override
		public int compareTo(VarValuePair o)
		{
			return ((Integer) variable).compareTo((Integer) o.variable);
		}
	}
}
