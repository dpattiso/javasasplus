package sas.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import javaff.data.Action;
import javaff.data.TotalOrderPlan;
import javaff.parser.PDDL21parser;
import javaff.parser.SolutionParser;
import javaff.search.UnreachableGoalException;
import sas.data.DomainTransitionGraph;
import sas.data.SASLiteral;
import sas.data.SASProblem;
import sas.data.SASState;
import sas.parser.ParseException;
import sas.parser.SASTranslator;
import sas.parser.SASplusParser;
import sas.search.CausalGraphHeuristic;
import sas.search.CeaHeuristic;
import sas.search.CeaHeuristic;

public class CEATester
{

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException
	{
		File domain = new File(args[0]);
		File pfile = new File(args[1]);
		if (domain.exists() == false || pfile.exists() == false)
			throw new FileNotFoundException("Domain and/or pfile not found");
		
		File solutionFile = null;
		if (args.length > 2)
		{
			solutionFile= new File(args[2]);
		}
		
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
//			SASProblem sp = new SASProblem();
//			sp = SASplusParser.parsePreprocess(output, sp);
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
		
		spO.causalGraph.generateDotGraph(new java.io.File("cg.dot"));
		HashMap<Integer, DomainTransitionGraph> newDTGs = new HashMap<Integer, DomainTransitionGraph>();
		for (Entry<Integer, DomainTransitionGraph> dtg : spO.causalGraph.getDTGsMap().entrySet())
		{
//			dtg.getValue().decompileUniversalTransitions();
			dtg.getValue().generateDotGraph(new File("DTGs/dtg_"+dtg.getValue().getVariable().getId()+".dot"));
		}
		
		TotalOrderPlan soln = null;
		if (solutionFile != null)
		{
			try {
				soln = SolutionParser.parse(PDDL21parser.parseFiles(domain, pfile), solutionFile);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (javaff.parser.ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		TreeSet<SASLiteral> sortedGoal = new TreeSet<SASLiteral>();
		sortedGoal.addAll(spO.getGoalLiterals());
//		sortedGoal.add(spO.getGoalLiterals().iterator().next());

		double hcg = Double.MAX_VALUE;
		double hcea = Double.MAX_VALUE;
//		for (Action a : soln)
//		{
			try
			{
				CausalGraphHeuristic cgh = new CausalGraphHeuristic(spO, true);
				
				System.out.println("Computing CG");
				hcg = cgh.getEstimate(spO.getCurrentState(), sortedGoal);
				
			}
			catch (UnreachableGoalException e)
			{
				e.printStackTrace();
			}
			
			int nodes = 0;
			try
			{
				CeaHeuristic cea = new CeaHeuristic(spO.causalGraph);
//				CeaHeuristic cea = new CeaHeuristic(spO.causalGraph);
				System.out.println("Computing CEA");
				hcea = cea.getEstimate(spO.getCurrentState(), sortedGoal);
				nodes = cea.getNumNodesExpanded();
	
			}
			catch (UnreachableGoalException e)
			{
				e.printStackTrace();
			}

			System.out.println("h_cg(G) = "+hcg);
			System.out.println("h_cea(G) = "+hcea+", "+nodes+" nodes expanded");
			if (soln != null)
				System.out.println("solution = "+soln.getPlanLength());
//		}

	}
	
//
}
