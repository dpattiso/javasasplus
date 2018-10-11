package sas.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import sas.data.DomainTransitionGraph;
import sas.data.SASAction;
import sas.data.SASLiteral;
import sas.data.SASPlan;
import sas.data.SASProblem;
import sas.data.SASState;
import sas.parser.ParseException;
import sas.parser.SASTranslator;
import sas.parser.SASplusParser;
import sas.search.CausalGraphHeuristic;
import javaff.search.UnreachableGoalException;

/**
 * Simple planner which uses CG heuristic.
 * @author David Pattison
 *
 */
public class CGHPlanner
{
	private SASProblem problem;
	private Random rand;
	
	public CGHPlanner(SASProblem problem)
	{
		this.problem = problem;
		this.rand = new Random();
		
		for (Entry<Integer, DomainTransitionGraph> dtg : problem.causalGraph.getDTGsMap().entrySet())
		{
			dtg.getValue().decompileUniversalTransitions();
		}
	}
	
	public SASPlan plan() throws UnreachableGoalException
	{
		CausalGraphHeuristic heuristic = new CausalGraphHeuristic(this.problem, true);
		SASState initial = this.problem.getCurrentState();

		Set<SASLiteral> goal = this.problem.getGoalLiterals();
		
		SASPlan plan = new SASPlan();
		double bestH = heuristic.getEstimate(initial, goal);
//		int bestH = heuristic.getPlan(initial).getPlanLength();
		HashSet<SASState> closed = new HashSet<SASState>();
		LinkedList<SASState> open = new LinkedList<SASState>();
		open.add(initial);
		
		while (open.isEmpty() == false)
		{
			SASState current = open.remove(); //pop head state
			closed.add(current);
			
			Map<SASAction, SASState> succs = this.getSuccessors(current);
			Map<SASAction, Integer> hValues = new HashMap<SASAction, Integer>();
			double bestSeen = bestH; //change to Integer.MAX_VALUE to stop HC
			ArrayList<SASAction> bestActions = new ArrayList<SASAction>();
			out: for (Entry<SASAction, SASState> e : succs.entrySet())
			{
				for (SASState s : closed)
					if (s.equals(e.getValue()))
						continue out;
				
				double succH = heuristic.getEstimate(e.getValue(), goal);
				if(succH == 0)
					return plan;

				if (succH < bestSeen)
				{
					bestSeen = succH;
					bestActions.clear();
					bestActions.add(e.getKey());
				}
				else if (succH == bestSeen)
				{
					bestActions.add(e.getKey());
				}
			}
			
			if (bestActions.size() == 0)
				throw new NullPointerException("Planning failed: Dead end");
			else if (bestActions.size() == 1)
			{
				SASAction best = bestActions.iterator().next();
				plan.addAction(best);
				
				SASState next = current.apply(best);
				System.out.println("Chose "+best);
				
				open.add(next);			
			}
			else
			{
//				System.out.println("Multiple best actions. Selecting at random");
				SASAction best = bestActions.get(this.rand.nextInt(bestActions.size()));
				plan.addAction(best);
				
				SASState next = ((SASState) current.clone()).apply(best);
				System.out.println("Chose "+best);
				
				open.add(next);	
			}
			
			bestH = bestSeen;
			System.out.println("h(G) = "+bestSeen);
		}
		
		return plan;
	}
	
	private Map<SASAction, SASState> getSuccessors(SASState s)
	{
		Map<SASAction, SASState> succs = new HashMap<SASAction, SASState>();
		for (SASAction a : this.problem.actions.values())
		{
			if (a.isApplicable(s))
			{
				SASState successor = s.apply(a);
				succs.put(a, successor);
			}
			
		}
		
		return succs;
	}
	
	private boolean goalMet(SASState s)
	{
		for (Entry<Integer, Integer> g : this.problem.goal.entrySet())
		{
			if (s.getValue(g.getKey()).getValueId() != g.getValue())
				return false;
		}
		
		return true;
	}
	
	public static void main(String[] args)
	{
		File domain = new File(args[0]);
		File pfile = new File(args[1]);
		
		//translate and initialise SAS+ problem
		try
		{
			SASTranslator.translateToSAS(domain, pfile);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		
		File outputSas = new File("./output.sas");
		File allGroups = new File("./all.groups");
		File testGroups = new File("./test.groups");
		File output = new File("./output");

		
		try
		{
			SASplusParser.parse();
		}
		catch (FileNotFoundException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		catch (IOException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		catch (ParseException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		SASProblem spA = SASplusParser.sasProblemAll;
		SASProblem spO = SASplusParser.sasProblemOptimised;
		spO.setupInitialState();
		
		
		CGHPlanner planner = new CGHPlanner(spO);
		SASPlan plan = null;
		try
		{
			plan = planner.plan();
		}
		catch (UnreachableGoalException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		

		int counter = 1;
		System.out.println("Plan found");
		for (SASAction a : plan.getActions())
		{
			System.out.println(counter++ +": "+a.toString());
		}
	}
}
