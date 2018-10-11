package sas.search;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Comparator;

import javaff.search.UnreachableGoalException;
import sas.data.CausalGraph;
import sas.data.DTGActionEdge;
import sas.data.DomainTransitionGraph;
import sas.data.NoneOfThoseProposition;
import sas.data.SASAction;
import sas.data.SASAxiom;
import sas.data.SASEffect;
import sas.data.SASLiteral;
import sas.data.SASPlan;
import sas.data.SASState;
import sas.data.SASVariable;
import sas.util.SASActionEdge;
import sas.util.SASNullAction;
import sas.util.UnreachableSASGoalException;

/**
 * Direct C-to-Java translation of the Fast Downward implementation of the context-enhanced additive heuristic,
 * by Geffner and Helmert, 2008.
 * @author David Pattison
 *
 */
public class CeaHeuristic implements SASHeuristic
{
	private CausalGraph cg;

	private PriorityQueue<LocalProblemNode> queue;
	private List<LocalProblem> local_problems;
	private LocalProblem[][] local_problem_index;
	private LocalProblem goal_problem;
	private LocalProblemNode goal_node;
	private Collection<SASLiteral> goalLiterals;
	
	private Map<DomainTransitionGraph, ValueNode[]> globalNodes;
	
	protected int numNodesExpanded;

	/**
	 * Map of DTGs to the variable indices of their parents.
	 */
	private Map<DomainTransitionGraph, List<Integer>> dtg_cea_parents;
	
	/**
	 * Map of global variable number to local variable number. Some nodes/variables in the range 0:N may be
	 * pruned, leaving only 1 <= M <= N nodes. This map stores the actual variable number mapped to the
	 * 0-indexed variable number encountered during parsing. TODO -- this should really just be in SASVariable
	 */
	private Map<Integer, Integer> global_to_local_var_map;

	/**
	 * Creates an instance of the hCEA heuristic. Performs problem setup processing, which need
	 * not be done again after instantiation, based upon the value of the flag passed in. Note
	 * that this setup is required and will be performed at the first call of 
	 * {@link #compute_heuristic(SASState)}.
	 * 
	 * @param cg The causal graph to use.
	 * @param doPreProcessing A flag indicating whether the initial setup processing should be performed.
	 * @see #clonePartial() Creates a clone of this object without performing any problem setup.
	 */
	public CeaHeuristic(CausalGraph cg, boolean doPreProcessing)
	{
		this.cg = cg;

		this.queue = new PriorityQueue<LocalProblemNode>();
		this.local_problems = new ArrayList<CeaHeuristic.LocalProblem>();
		this.goal_node = null;
		this.goal_problem = null;
		this.goalLiterals = Collections.EMPTY_LIST;
		
		this.dtg_cea_parents = new HashMap<DomainTransitionGraph, List<Integer>>();
		this.global_to_local_var_map = new HashMap<Integer, Integer>();
		
		this.globalNodes = new HashMap<DomainTransitionGraph, ValueNode[]>();
		
		this.numNodesExpanded = 0;

		this.local_problem_index = null;
		
		if (doPreProcessing)
		{
			this.createLocalProblems();
		
			for (DomainTransitionGraph dtg : cg.getDTGs())
			{
				ValueNode[] nodes = this.buildValueNodes(dtg);
				this.globalNodes.put(dtg, nodes);
			}
		}
	}
	
	/**
	 * Creates an instance of the hCEA heuristic. Also performs problem setup processing, which need
	 * not be done again after instantiation.
	 * 
	 * @param cg The causal graph to use.
	 * @see #clonePartial() Creates a clone of this object without performing any problem setup.
	 */
	public CeaHeuristic(CausalGraph cg)
	{
		this(cg, true);
	}
	
	public List<LocalProblem> getLocalProblems()
	{
		return Collections.unmodifiableList(local_problems);
	}
	
	
	/**
	 * Create the LocalProblems used during planning. In Fast Downward this is done inside the DTGs themselves,
	 * but this is a poor place to do this (even by the FD author's admission). So instead this method must be 
	 * called prior to any estimate being produced. It need only be called once in order to set up the 
	 * appropriate fields.
	 */
	protected void createLocalProblems()
	{
		int num_variables = this.cg.vertexSet().size();
		this.local_problem_index = new LocalProblem[num_variables][];
		
		//don't iterate through the DTGs using a counter -- some may have been pruned.
		//Use an iterator instead
//		for (int var_no = 0; var_no < num_variables; ++var_no)
		int c = 0;
		for (DomainTransitionGraph dtg : this.cg.getDTGs())
		{	
			//use the domain size, as the number of vertices in the graph may be 
			//less than this, as there may be values which have no transitions (this is really
			//for IGRAPH's benefit, but makes sense nonetheless).
			int num_values = dtg.getVariable().getDomain().size();
			
			 //num_values = dtg.vertexSet().size();
			
			this.global_to_local_var_map.put(dtg.getVariable().getId(), c);
			
			local_problem_index[c] = new LocalProblem[num_values];
//			dtg.generateDotGraph(new File("dtg.dot"));

			for (int i = 0; i < num_values; i++)
			{
				local_problem_index[c][i] = null;
			}
			
			++c;
		}
	}

	public Object clone()
	{
		CeaHeuristic clone = new CeaHeuristic((CausalGraph) this.cg.clone(), false);
		clone.local_problem_index = Arrays.copyOf(this.local_problem_index, this.local_problem_index.length);
		clone.global_to_local_var_map = this.global_to_local_var_map;
		clone.goal_node = this.goal_node;
		clone.goal_problem = this.goal_problem;
		clone.dtg_cea_parents = this.dtg_cea_parents;
		clone.goalLiterals = this.goalLiterals;
		clone.globalNodes = this.globalNodes;
		clone.local_problems = this.local_problems;
		clone.numNodesExpanded = this.numNodesExpanded;
		clone.queue = this.queue;
		
		return clone;
	}
	
	/**
	 * Computes a partial shallow clone of this object. In the partial clone, all setup which is required at
	 * class instantiation is skipped, as this process is deterministic and always produces the same
	 * output. A new instance of {@link CeaHeuristic} is returned, with all read-only fields containing
	 * shallow copies of this object's contents.
	 * @return
	 */
	public CeaHeuristic branch()
	{
		CeaHeuristic clone = new CeaHeuristic(this.cg, false);
		
		clone.global_to_local_var_map = this.global_to_local_var_map;
		clone.dtg_cea_parents = this.dtg_cea_parents;
		
		clone.local_problem_index = new LocalProblem[this.local_problem_index.length][];
		for (int i = 0; i < clone.local_problem_index.length; i++)
		{
			clone.local_problem_index[i] = new LocalProblem[this.local_problem_index[i].length];
			for (int j = 0; j < clone.local_problem_index[i].length; j++)
			{
				//may not have been constructed yet
				if (this.local_problem_index[i][j] != null)
				{
					clone.local_problem_index[i][j] = (LocalProblem) this.local_problem_index[i][j].clone();
				}
			}
		}
		
		clone.global_to_local_var_map = this.global_to_local_var_map;
		clone.globalNodes = new HashMap<DomainTransitionGraph, CeaHeuristic.ValueNode[]>(this.globalNodes);
		
		return clone;
	}

	/**
	 * Gets the estimate to the goals provided. This is the sum of individual
	 * estimates.
	 * 
	 * @see #getHcea(SASLiteral, SASState, int) Computes the distance to each
	 *      individual goal.
	 */
	@Override
	public double getEstimate(final SASState initial, Collection<SASLiteral> goals) throws UnreachableGoalException
	{
//		double d = 0;
//		for (SASLiteral g : goals)
//		{
//			ArrayList<SASLiteral> l = new ArrayList<SASLiteral>();
//			l.add(g);
//			
//			this.initialize(goals);
//	
//			double h = this.compute_heuristic(initial);
//	
//			if (h == CeaHeuristic.Unreachable)
//				throw new UnreachableSASGoalException(goals, "Cannot compute estimate for goals " + goals);
//
//			d += h;
//		}
//		return d;
		
		
		this.initialize(goals);

		double h = this.compute_heuristic(initial);

		if (h == CeaHeuristic.Unreachable)
			throw new UnreachableSASGoalException(goals, "Cannot compute estimate for goals " + goals);
		
		return h;
	}
	
//	/**
//	 * Reset all learnt knowledge in the heuristic.
//	 */
//	protected void resetFields()
//	{
//		this.goalLiterals = Collections.EMPTY_LIST;
//		this.dtg_cea_parents.clear();
//		this.global_to_local_var_map.clear();
//		
//		this.queue = new PriorityQueue<LocalProblemNode>();
//		this.local_problems = new ArrayList<CeaHeuristic.LocalProblem>();
//		this.local_problem_index = null;
//		this.goal_node = null;
//		this.goal_problem = null;
//		
//		this.dtg_cea_parents = new HashMap<DomainTransitionGraph, List<Integer>>();
//		this.global_to_local_var_map = new HashMap<Integer, Integer>();
//		
//		this.globalNodes = new HashMap<DomainTransitionGraph, ValueNode[]>();
//	}

	protected void initialize(Collection<SASLiteral> goalset)
	{
//		this.resetFields();
		
		this.goalLiterals = goalset;
		
		assert (goal_problem == null);
		

		goal_problem = build_problem_for_goal(goalset);
		goal_node = goal_problem.nodes[1]; // get the 1st node, which is the
											// result of a stub action
											// transition
		
		this.numNodesExpanded = 0;

	}

	protected LocalProblem build_problem_for_goal(Collection<SASLiteral> goalset)
	{
		LocalProblem problem = new LocalProblem(-1);

		problem.context_variables = new ArrayList<Integer>();
		// for (int i = 0; i < g_goal.size(); ++i)
		// problem.context_variables.add(g_goal[i].first);
		for (SASLiteral g : goalset)
			problem.context_variables.add(g.getVariableId());

		// create 2 nodes of equal value, which are used to generate a stub
		// transition node
		problem.nodes = new LocalProblemNode[2];
		for (int value = 0; value < 2; ++value)
			problem.nodes[value] = new LocalProblemNode(problem, goalset.size(), value);

		List<LocalAssignment> goals = new ArrayList<CeaHeuristic.LocalAssignment>();
		int g = 0;
		for (SASLiteral goal : goalset)
		{
//			int local_var = this.global_to_local_var_map.get(g.getVariableId());
//			int local_var = g.getVariableId();
			goals.add(new LocalAssignment(g++, goal.getValueId()));
		}
		List<LocalAssignment> no_effects = new ArrayList<CeaHeuristic.LocalAssignment>();
		ValueTransitionLabel label = new ValueTransitionLabel(new SASNullAction(0), goals, no_effects);
		LocalTransition trans = new LocalTransition(problem.nodes[0], problem.nodes[1], label, 0);
		problem.nodes[0].outgoing_transitions.add(trans);
		return problem;
	}

	protected double compute_heuristic(SASState state)
	{
		if (this.local_problem_index == null)
			this.createLocalProblems();
		
		initialize_heap();
		goal_problem.base_priority = -1;
		for (int i = 0; i < local_problems.size(); ++i)
			local_problems.get(i).base_priority = -1;

		set_up_local_problem(goal_problem, 0, 0, state);

		double heuristic = this.compute_costs(state);

		return heuristic;
	}

	protected double compute_costs(final SASState state)
	{
		while (!this.queue.isEmpty())
		{
			LocalProblemNode node = this.queue.remove();
			double curr_priority = get_priority(node);

			assert (is_local_problem_set_up(node.owner));
			//don't understand how this IF can ever be fired!
			if (get_priority(node) < curr_priority)
				continue;

//			if (node.equals(this.goal_node))
//			if (isGoalMet(node))
			// if (isGoalMetExpand(node, goal))
			 if (node == goal_node)
			{
				return node.cost;
			}

			assert (get_priority(node) == curr_priority);
			
//			System.out.println("Expanding node "+node.owner.variable+","+node.val+ " cost "+node.cost);
//			System.out.println("Queue is "+this.queue);
			expand_node(node);

			for (LocalTransition t : node.outgoing_transitions)
			{
				expand_transition(t, state);
			}
		}
		return Unreachable;
	}


	protected boolean isGoalMet(LocalProblemNode node)
	{
		int[] contextValues = node.getContext();
		List<Integer> contextVariables = node.owner.context_variables;

		
		for (SASLiteral g : this.goalLiterals)
		{
			int goalVar = g.getVariableId();
			int goalVal = g.getValueId();
	
////			System.out.println("Is goal met with context variables " + contextVariables + " values " + Arrays.toString(contextValues) + " goal " + goalVar + " = "+goalVal);
//					+ goalVal);
	
			int idx = contextVariables.indexOf(goalVar);
			if (idx < 0)
				return false;
	
			boolean goalMet = contextValues[idx] == goalVal;
			if (!goalMet)
				return false;
		}

		return true;
	}

	protected void expand_transition(LocalTransition trans, final SASState state)
	{
		/*
		 * Called when the source of trans is reached by Dijkstra exploration.
		 * Try to compute cost for the target of the transition from the source
		 * cost, action cost, and set-up costs for the conditions on the label.
		 * The latter may yet be unknown, in which case we "subscribe" to the
		 * waiting list of the node that will tell us the correct value.
		 */

		assert (trans.source.cost >= 0);
		assert (trans.source.cost < Integer.MAX_VALUE);

		trans.targetCost = trans.source.cost + trans.actionCost;

		if (trans.target.cost <= trans.targetCost)
		{
			// Transition cannot find a shorter path to target.
			return;
		}

		trans.unreachedConditions = 0;
		List<LocalAssignment> precond = trans.label.precond;

		// vector<LocalAssignment>::const_iterator
		// curr_precond = precond.begin(),
		// last_precond = precond.end();

		int[] context = trans.source.getContext();
		List<Integer> parent_vars = trans.source.owner.context_variables;

		for (LocalAssignment pc : precond)
		{
			int local_var = pc.local_var;
			int current_val = context[local_var];
			int precond_value = pc.value;
			int precond_var_no = parent_vars.get(local_var);

			if (current_val == precond_value)
			{
				continue;
			}

			LocalProblem subproblem = get_local_problem(precond_var_no, current_val);
			assert(subproblem != null);

			if (!is_local_problem_set_up(subproblem))
			{
				set_up_local_problem(subproblem, get_priority(trans.source), current_val, state);
			}

			LocalProblemNode cond_node = subproblem.nodes[precond_value];
			if (cond_node.expanded)
			{
//				System.out.println("Setting target cost from " + trans.targetCost + " to " + (trans.targetCost + cond_node.cost));
				trans.targetCost += cond_node.cost;
				if (trans.target.cost <= trans.targetCost)
				{
					// Transition cannot find a shorter path to target.
					return;
				}
			}
			else
			{
				assert(trans != null);
				cond_node.waitingList.add(trans);
				++trans.unreachedConditions;
			}
		}
		try_to_fire_transition(trans);
	}

	protected void set_up_local_problem(LocalProblem problem, double base_priority, int start_value, SASState state)
	{
		assert (problem.base_priority == -1);
		problem.base_priority = base_priority;

		LocalProblemNode[] nodes = problem.nodes;
		for (LocalProblemNode n : nodes)
		{
			if (n == null)
				continue;
			
			n.expanded = false;
			n.cost = Integer.MAX_VALUE;
			n.waitingList.clear();
			n.reached_by = null;
		}

		LocalProblemNode start = nodes[start_value];
		start.cost = 0;
		for (int i = 0; i < problem.context_variables.size(); ++i)
		{
			int contextVar = problem.context_variables.get(i);
			int val = state.getValueIndex(contextVar);

			// int val = problem.context_variables.get(i);
			start.getContext()[i] = val;
		}

		add_to_heap(start);
	}

	protected LocalProblem get_local_problem(int global_var_no, int value)
	{
		int local_var_no = this.global_to_local_var_map.get(global_var_no);
//		int local_var_no = global_var_no;
		
		LocalProblem table_entry = local_problem_index[local_var_no][value];
		if (table_entry == null)
		{
			table_entry = build_problem_for_variable(global_var_no);
			
			local_problem_index[local_var_no][value] = table_entry;
			this.local_problems.add(table_entry);
		}
		return table_entry;
	}

	protected LocalProblem build_problem_for_variable(int var_no)
	{
		LocalProblem problem = new LocalProblem(var_no);

		DomainTransitionGraph dtg = this.cg.getDTG(var_no);
		
		ValueNode[] nodes;
		if (this.globalNodes.containsKey(dtg))
			nodes = this.globalNodes.get(dtg);
		else
		{
			nodes = buildValueNodes(dtg);
			this.globalNodes.put(dtg, nodes);
		}

		problem.context_variables = this.dtg_cea_parents.get(dtg);

		int num_parents = problem.context_variables.size();
		int num_values = nodes.length;
		problem.nodes = new LocalProblemNode[num_values];
		for (SASLiteral actualLiteral : dtg.vertexSet())
		{
			problem.nodes[actualLiteral.getValueId()] = new LocalProblemNode(problem, num_parents, actualLiteral.getValueId());
		}

//		for (int v = 0; v < num_values; ++v)
		for (SASLiteral v : dtg.vertexSet())
		{
//			int value = dtg.getVariable().getDomain().get(v).getValueId();
			int value = v.getValueId();
			
			LocalProblemNode node = problem.nodes[value];
			ValueNode dtg_node = nodes[value];
			// for (int i = 0; i < dtg_node.transitions.length; ++i)
			for (ValueTransition dtg_trans : dtg_node.transitions)
			{
				// ValueTransition dtg_trans = dtg_node.transitions[i];
				int target_value = dtg_trans.target.value;
				LocalProblemNode target = problem.nodes[target_value];
				for (ValueTransitionLabel label : dtg_trans.cea_labels)
				{
					double action_cost = label.op.getCost();
					if (label.op instanceof SASAxiom)
					{
						assert(action_cost == 0);
					}
					
					LocalTransition trans = new LocalTransition(node, target, label, action_cost);
					node.outgoing_transitions.add(trans);
				}
			}
		}

		return problem;
	}
	
//	/**
//	 * Reset the heuristic for another goal, without destroying all the information built up for the problem.
//	 */
//	public void reset()
//	{
//		this.goalLiterals.clear();
//		
//		this.queue.clear();
//		
//		this.goal_node = null;
//		this.goal_problem = null;
//		
////		this.dtg_cea_parents = new HashMap<DomainTransitionGraph, List<Integer>>();
////		this.global_to_local_var_map = new HashMap<Integer, Integer>();
//	}

	protected ValueNode[] buildValueNodes(DomainTransitionGraph dtg)
	{

		List<Integer> cea_parents = new ArrayList<Integer>();

		Map<Integer, Integer> global_to_cea_parent = new HashMap<Integer, Integer>();

		ValueNode[] nodes = new ValueNode[dtg.vertexSet().size() + 1]; //FIXME + 1 for NOTP

		for (SASLiteral v : dtg.vertexSet())
		{
			ValueNode node = new ValueNode(dtg, v);
			nodes[v.getValueId()] = node;
		}

		final List<LocalAssignment> no_effect = new ArrayList<CeaHeuristic.LocalAssignment>();
		for (ValueNode source : nodes)
		{
//			if (dtg.outDegreeOf(source.literal) <= 0)
//				continue;
			if (source == null)
				continue;

			int c = 0;
			for (DTGActionEdge e : dtg.outgoingEdgesOf(source.literal))
			{

				ValueNode target = nodes[e.getEffect().getValueId()];
				
				if (source.transitions[c] == null)
				{
					source.transitions[c] = new ValueTransition(target);
				}

				// ValueTransition transition = new ValueTransition(target);
				ValueTransition transition = source.transitions[c];// TODO this
																	// sucks

				// build the LocalAssignment objects for CEA preconditions
				List<LocalAssignment> precond = new ArrayList<CeaHeuristic.LocalAssignment>();
				List<LocalAssignment> cea_precond = new ArrayList<CeaHeuristic.LocalAssignment>();
//				for (SASLiteral pc : e.getAllPreconditions())
				for (SASLiteral pc : e.getAssociatedPcs()) //only want PCs of variables other than this DTG's var
				{
					int global_var = pc.getVariableId();
					assert(global_var != e.getPc().getVariableId());

					if (global_to_cea_parent.containsKey(global_var) == false)
					{
						global_to_cea_parent.put(global_var, cea_parents.size());
						cea_parents.add(global_var);
					}
					int cea_parent = global_to_cea_parent.get(global_var);

					LocalAssignment ass = new LocalAssignment(cea_parent, pc.getValueId());
					precond.add(ass);
					cea_precond.add(ass);
				}

				// build up the CEA effect
				// note that this excludes all the trigger condition stuff in
				// the C++ code, as I have no idea what it does
				// and it doesn't seem to have any effect on anything
				List<LocalAssignment> cea_effects = new ArrayList<CeaHeuristic.LocalAssignment>();
				for (Entry<Integer, SASEffect> effect : e.getAction().getEffects().entrySet())
				{
					int var_no = effect.getKey();
					int pre = effect.getValue().precondition;
					int post = effect.getValue().effect;

					if (pre == -1)
					{
						continue;
					}

					//only interested in recording this effect if it is not part of this DTG's underlying variable
					//and it has an associated precondition 
					if (var_no == e.getPc().getVariableId() || !global_to_cea_parent.containsKey(var_no))
					{
						// This is either an effect on the variable we're
						// building the DTG for, or an effect on a variable we
						// don't need to track because it doesn't appear in
						// conditions of this DTG. Ignore it.
						continue;
					}

					int cea_parent = global_to_cea_parent.get(var_no);

					LocalAssignment eff = new LocalAssignment(cea_parent, post);
					cea_effects.add(eff);
				}

				transition.labels.add(new ValueTransitionLabel(e.getAction(), precond, no_effect));
				transition.cea_labels.add(new ValueTransitionLabel(e.getAction(), cea_precond, cea_effects));
				// source.transitions.add(transition);
				source.transitions[c++] = transition;
			}

		}

		dtg_cea_parents.put(dtg, cea_parents);

		return nodes;
	}

	// protected ValueTransitionLabel buildValueTransitionLabel(DTGActionEdge
	// edge)
	// {
	// ArrayList<LocalAssignment> pcs = new ArrayList<LocalAssignment>();
	// pcs.add(new LocalAssignment(edge.getPc().getVariableId(),
	// edge.getPc().getValueId()));
	// for (SASLiteral pc : edge.getAssociatedPcs())
	// {
	// pcs.add(new LocalAssignment(pc.getVariableId(),pc.getValueId()));
	// }
	//
	// ArrayList<LocalAssignment> effs = new ArrayList<LocalAssignment>();
	// effs.add(new LocalAssignment(edge.getEffect().getVariableId(),
	// edge.getEffect().getValueId()));
	//
	// ValueTransitionLabel label = new ValueTransitionLabel(edge.getAction(),
	// pcs, effs);
	// return label;
	// }

	protected boolean is_local_problem_set_up(LocalProblem problem)
	{
		return problem.base_priority != -1;
	}

	protected void expand_node(LocalProblemNode node)
	{
		node.expanded = true;
		// Set context unless this was an initial node.
		LocalTransition reached_by = node.reached_by;
		if (reached_by != null)
		{
			LocalProblemNode parent = reached_by.source;

			int[] context = parent.getContext();

			List<LocalAssignment> precond = reached_by.label.precond;
			for (LocalAssignment pc : precond)
			{
				assert(parent.owner.context_variables.get(pc.local_var) != node.owner.variable);
					
				context[pc.local_var] = pc.value;
			}

			List<LocalAssignment> effect = reached_by.label.effect;
			for (LocalAssignment eff : effect)
			{
				context[eff.local_var] = eff.value;
			}
			// context[reached_by.]

			if (parent.reached_by != null)
				node.reached_by = parent.reached_by;

			node.setContext(context); // need to explicitly re-set the context (if
									// only we had & references)
		}

		for (int i = 0; i < node.waitingList.size(); ++i)
		{
			LocalTransition trans = node.waitingList.get(i);
			assert(trans != null);
			assert (trans.unreachedConditions >= 0);
			--trans.unreachedConditions; // -1 unreached condition because we
											// have just set the PC to be true?

//			System.out.println("Waiting node "+trans.source+" cost increased from " + trans.targetCost + " to " + (trans.targetCost + node.cost) +" -- "+this.extractPlan(trans.source)+" "+trans.label.op.getOperatorName());
//			if (trans.label.op instanceof SASAxiom == false)
			trans.targetCost += node.cost;
			
			boolean fired = try_to_fire_transition(trans);
//			if(fired)
//				System.out.println("Waiting transition "+trans.label.op.getOperatorName()+" fired successfully");
//			else
//				System.out.println("Waiting transition "+trans.label.op.getOperatorName()+" did not fire");
		}
		node.waitingList.clear();
		++numNodesExpanded;
	}
	
	/**
	 * Return the number of nodes which were expanded in the last search.
	 * @return
	 */
	public int getNumNodesExpanded()
	{
		return numNodesExpanded;
	}

	protected boolean try_to_fire_transition(LocalTransition trans)
	{
		if (trans.unreachedConditions == 0)
		{
			LocalProblemNode target = trans.target;
			if (trans.targetCost < target.cost)
			{
//				System.out.println("Cost increased from " + target.cost + " to " + trans.targetCost +" -- "+this.extractPlan(target));
				target.cost = trans.targetCost;
				target.reached_by = trans;
				
				assert(trans.source.equals(trans.target) == false);
				add_to_heap(target);
				
				return true;
			}
		}
		
		return false;
	}
	
	protected SASPlan extractPlan(LocalProblemNode node)
	{
		SASPlan p = new SASPlan();
		
		LocalProblemNode curr = node;
		while (curr.reached_by != null)
		{
			p.prepend(curr.reached_by.label.op);
			curr = curr.reached_by.source;
		}
		
		return p;
	}

	protected void initialize_heap()
	{
		this.queue.clear();
	}

	protected void add_to_heap(LocalProblemNode node)
	{
		//don't want duplicates, and if LocalProblemNode.compareTo(x,y) == 0
		//the PriorityQueue just keeps both in some undetermined order
		//rather than ignoring duplicates.
		assert(node != null);
		if (!this.queue.contains(node))
		{
			this.queue.add(node);
		}
	}

	/**
	 * Builds a string of repeated tabs, for use in outputting debug info for
	 * recursion.
	 * 
	 * @param count
	 * @return
	 */
	protected String buildTabs(int count)
	{
		StringBuffer b = new StringBuffer();
		for (int i = 0; i < count; i++)
			b.append("\t");
		return b.toString();
	}

	protected double get_priority(LocalProblemNode node)
	{
		/*
		 * Nodes have both a "cost" and a "priority", which are related. The
		 * cost is an estimate of how expensive it is to reach this node. The
		 * "priority" is the lowest cost value in the overall cost computation
		 * for which this node will be important. It is essentially the sum of
		 * the cost and a local-problem-specific "base priority", which depends
		 * on where this local problem is needed for the overall computation.
		 */
		return node.owner.base_priority + node.cost;
		// return node.cost;
	}

	public class LocalProblemNode implements Comparable<LocalProblemNode>
	{
		// Attributes fixed during initialization.
		LocalProblem owner;
		List<LocalTransition> outgoing_transitions;

		// Dynamic attributes (modified during heuristic computation).
		double cost;
		boolean expanded;
		
		private int[] context;

		LocalTransition reached_by;
		/*
		 * Before a node is expanded, reached_by is the "current best"
		 * transition leading to this node. After a node is expanded, the
		 * reached_by value of the parent is copied (unless the parent is the
		 * initial node), so that reached_by is the *first* transition on the
		 * optimal path to this node. This is useful for preferred operators.
		 * (The two attributes used to be separate, but this was a bit
		 * wasteful.)
		 */

		List<LocalTransition> waitingList;
		
		int val;
		
		int hash;
		
		private LocalProblemNode()
		{
			
		}

		/**
		 * 
		 * @param owner
		 * @param contextSize
		 * @param val Purely used in debugging
		 */
		LocalProblemNode(LocalProblem owner, int contextSize, int val)
		{
			this.owner = owner;
			this.cost = -1;
			this.expanded = false;
			this.setContext(new int[contextSize]);
			for (int i = 0; i < contextSize; i++)
				getContext()[i] = -1;
			
			this.val = val;


			this.reached_by = null;
			this.outgoing_transitions = new ArrayList<CeaHeuristic.LocalTransition>();
			this.waitingList = new ArrayList<CeaHeuristic.LocalTransition>();
			
			this.updateHash();
		}
		
		public Object clone()
		{
			LocalProblemNode clone = new LocalProblemNode();
			clone.context = Arrays.copyOf(this.context, this.context.length);
			clone.cost = this.cost;
			clone.expanded = this.expanded;
			clone.hash = this.hash;
			clone.outgoing_transitions = this.outgoing_transitions;
			clone.owner = (LocalProblem) this.owner.clone();
			clone.reached_by = (LocalTransition) this.reached_by.clone();
			clone.val = this.val;
			clone.waitingList = new ArrayList<CeaHeuristic.LocalTransition>();
			for (LocalTransition t : this.waitingList)
			{
				clone.waitingList.add((LocalTransition) t.clone());
			}
			
			return clone;
		}
		
		public int[] getContext()
		{
			return context;
		}
		
		public void setContext(int[] context)
		{
			this.context = context;
			this.updateHash();
		}
		
		@Override
		public String toString()
		{
			return "Node v"+owner.variable+", val "+val;
		}
		
		protected int updateHash()
		{
			this.hash = this.getContext().hashCode() ^ this.val ^ this.owner.variable ^ 31;
			
			return this.hash;
		}
		
		@Override
		public int hashCode()
		{
			return this.hash;
		}
		

		@Override
		public int compareTo(LocalProblemNode o)
		{
			// return Double.compare(get_priority(this), get_priority(o));
			double tp = get_priority(this);
			double op = get_priority(o);

			// if this node has a lesser priority than the other,return -1 to
			// indicate that it
			// should precede it. Preferring a higher cost is equivalent to
			// finding a solution through always following the first transition
			// which leads to varying length solutions, if any.
			if (tp < op)
				return -1;
			// else return 1. We don't test for equality as this would eliminate
			// one of the nodes from the
			// global queue
			else if (tp > op)
				return +1;
			else
//				return 1;
				return Integer.compare(this.hashCode(), o.hashCode()); //TODO add another criteria here?
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj == null)
				return false;

//			LocalProblemNode other = (LocalProblemNode) obj;
//			if (Arrays.equals(this.getContext(), other.getContext()) == false)
//				return false;
//
//			if (this.owner.equals(other.owner) == false)
//				return false;
//			
//			if (this.val != other.val)
//				return false;
//
//			return true;
			
			return this.hashCode() == obj.hashCode();
		}
	}

	public class LocalTransition
	{
		LocalProblemNode source;
		LocalProblemNode target;
		ValueTransitionLabel label;

		double actionCost;

		double targetCost;
		int unreachedConditions;
		
		private LocalTransition()
		{
			
		}

		public LocalTransition(LocalProblemNode source, LocalProblemNode target, ValueTransitionLabel label, double actionCost)
		{
			this.source = source;
			this.target = target;
			this.label = label;

			this.actionCost = actionCost;
			targetCost = -1;
			this.unreachedConditions = -1;
		}
		
		public Object clone()
		{
			LocalTransition clone = new LocalTransition();
			
			clone.actionCost = this.actionCost;
			clone.label = (ValueTransitionLabel) this.label.clone();
			clone.source = (LocalProblemNode) this.source.clone();
			clone.target = (LocalProblemNode) this.target.clone();
			clone.targetCost = this.targetCost;
			clone.unreachedConditions = this.unreachedConditions;
			
			return clone;
		}
		
		@Override
		public String toString()
		{
			return this.source.toString()+" -> "+this.label.toString()+ " -> "+this.target.toString();
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj == null)
				return false;

			LocalTransition other = (LocalTransition) obj;
			if (this.source.equals(other.source) == false)
				return false;

			if (this.target.equals(other.target) == false)
				return false;

			if (this.label.equals(other.label) == false)
				return false;

			return true;
		}
	}

	public class LocalProblem
	{
		final int variable;
		double base_priority;
		LocalProblemNode[] nodes;
		/**
		 * Not created in any particular order
		 */
		List<Integer> context_variables;

		/**
		 * 
		 * @param variable
		 *            Purely for debugging purposes.
		 */
		public LocalProblem(int variable)
		{
			this.variable = variable;
			this.base_priority = -1;
			this.nodes = null;
			this.context_variables = new ArrayList<Integer>();
		}
		
		public Object clone()
		{
			LocalProblem clone = new LocalProblem(this.variable);
			clone.base_priority = this.base_priority;
			clone.context_variables = new ArrayList<Integer>(this.context_variables);

			clone.nodes = new LocalProblemNode[this.nodes.length];
			for (int i = 0; i < clone.nodes.length; i++)
			{
				clone.nodes[i] = (LocalProblemNode) this.nodes[i].clone();
			}
			
			return clone;
		}
		
		@Override
		public String toString()
		{
			return "LocalProblem (var" + variable + "), priority " + this.base_priority + ", context variables " + context_variables;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj == null)
				return false;

			LocalProblem other = (LocalProblem) obj;
			if (this.nodes.equals(other.nodes) == false)
				return false;

			if (this.context_variables.equals(other.context_variables) == false)
				return false;

			return true;
		}
	}

	public class LocalAssignment
	{
		final int local_var;
		final int value;

		LocalAssignment(int var, int val)
		{
			this.local_var = var;
			this.value = val;
		}

		@Override
		public String toString()
		{
			return local_var + " = " + value;
		}
		
		/**
		 * Deep copy.
		 */
		public Object clone()
		{
			return new LocalAssignment(this.local_var, this.value);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj == null)
				return false;

			LocalAssignment other = (LocalAssignment) obj;
			if (other.local_var != this.local_var)
				return false;

			if (this.value != other.value)
				return false;

			return true;
		}
	}

	public class ValueTransitionLabel
	{
		SASAction op;
		List<LocalAssignment> precond;
		List<LocalAssignment> effect;
		
		public ValueTransitionLabel()
		{
			
		}

		public ValueTransitionLabel(SASAction theOp, List<LocalAssignment> precond, List<LocalAssignment> effect)
		{
			this.op = theOp;
			this.precond = precond;
			this.effect = effect;
		}
		
		/**
		 * Shallow copy only.
		 */
		public Object clone()
		{
			ValueTransitionLabel clone = new ValueTransitionLabel();
			clone.op = this.op;
			clone.precond = this.precond;
			clone.effect = this.effect;
			
			return clone;
		}
		
		@Override
		public String toString()
		{
			return this.op.toString();
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj == null)
				return false;

			ValueTransitionLabel other = (ValueTransitionLabel) obj;
			if (this.op.equals(other.op) == false)
				return false;

			if (this.precond.equals(other.precond) == false)
				return false;

			if (this.effect.equals(other.effect) == false)
				return false;

			return true;
		}
	}

	public class ValueTransition
	{
		ValueNode target;
		List<ValueTransitionLabel> labels;
		List<ValueTransitionLabel> cea_labels; // labels for cea heuristic

		public ValueTransition()
		{
			
		}
		
		public ValueTransition(ValueNode targ)
		{
			this.target = targ;
			this.labels = new ArrayList<CeaHeuristic.ValueTransitionLabel>();
			this.cea_labels = new ArrayList<CeaHeuristic.ValueTransitionLabel>();
		}
		
		/**
		 * Shallow copy.
		 */
		public Object clone()
		{
			ValueTransition clone = new ValueTransition();
			clone.cea_labels = this.cea_labels;
			clone.labels = this.labels;
			clone.target = this.target;
			
			return clone;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj == null)
				return false;

			ValueTransition other = (ValueTransition) obj;
			if (this.target.equals(other.target) == false)
				return false;

			if (this.labels.equals(other.labels) == false)
				return false;

			return true;
		}
	}

	public class ValueNode
	{
		DomainTransitionGraph parent_graph;
		int value;
		ValueTransition[] transitions;

		ValueNode reached_from; // cg
		ValueTransitionLabel reached_by; // cg

		SASLiteral literal;
		
		private ValueNode()
		{
			
		}

		public ValueNode(DomainTransitionGraph parent, SASLiteral val)
		{
			this.parent_graph = parent;
			this.literal = val;

			this.value = literal.getValueId();
			this.reached_from = null;
			this.reached_by = null;

			int num_transitions = parent.outDegreeOf(val);
			this.transitions = new ValueTransition[num_transitions];

		}
		
		public Object clone()
		{
			ValueNode clone = new ValueNode();
			
			clone.literal = this.literal;
			clone.parent_graph = this.parent_graph;
			clone.reached_by = this.reached_by;
			clone.reached_from = this.reached_from;
			clone.transitions = this.transitions;
			clone.value = this.value;
			
			return clone;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj == null)
				return false;

			ValueNode other = (ValueNode) obj;
			if (this.parent_graph.equals(other.parent_graph) == false)
				return false;

			if (this.value != other.value)
				return false;

			return true;
		}
	}
}
