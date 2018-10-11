package sas.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import sas.data.DomainTransitionGraph;
import sas.data.SASAction;
import sas.data.SASAxiom;
import sas.data.SASPlan;
import sas.data.SASProblem;
import sas.parser.ParseException;
import sas.parser.SASTranslator;
import sas.parser.SASplusParser;
import sas.search.CausalGraphHeuristic;
import javaff.search.UnreachableGoalException;

public class CGHTester
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
		
		spO.causalGraph.generateDotGraph(new java.io.File("cg.dot"));
		HashMap<Integer, DomainTransitionGraph> newDTGs = new HashMap<Integer, DomainTransitionGraph>();
		for (Entry<Integer, DomainTransitionGraph> dtg : spO.causalGraph.getDTGsMap().entrySet())
		{
//			dtg.getValue().decompileUniversalTransitions();
			dtg.getValue().generateDotGraph(new File("DTGs/dtg_"+dtg.getValue().getVariable().getId()+".dot"));
		}
		
		CausalGraphHeuristic h = new CausalGraphHeuristic(spO, true);
		double estimate = Double.MAX_VALUE;
		try
		{
			estimate = h.getEstimate(spO.getCurrentState(), spO.getGoalLiterals());

			System.out.println("h(G) = "+estimate);
		}
		catch (UnreachableGoalException e)
		{
			e.printStackTrace();
		}

	}
	
//
//	/**
//	 * @param args
//	 * @throws FileNotFoundException 
//	 */
//	public static void main(String[] args) throws FileNotFoundException
//	{
//		File domain = new File(args[0]);
//		File pfile = new File(args[1]);
//		if (domain.exists() == false || pfile.exists() == false)
//			throw new FileNotFoundException("Domain and/or pfile not found");
//		
//		//translate and initialise SAS+ problem
//		try
//		{
//			SASTranslator.translateToSAS(domain, pfile);
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//			System.exit(1);
//		}
//		
//		File outputSas = new File("./output.sas");
//		File allGroups = new File("./all.groups");
//		File testGroups = new File("./test.groups");
//		File output = new File("./output");
//
//		
//		try
//		{
//			SASplusParser.parse();
//		}
//		catch (FileNotFoundException e1)
//		{
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		catch (IOException e1)
//		{
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		catch (ParseException e1)
//		{
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//
//		SASProblem spA = SASplusParser.sasProblemAll;
//		SASProblem spO = SASplusParser.sasProblemOptimised;
//		spO.setupInitialState();
//		
//		spO.causalGraph.generateDotGraph(new java.io.File("cg.dot"));
//		HashMap<Integer, DomainTransitionGraph> newDTGs = new HashMap<Integer, DomainTransitionGraph>();
//		for (Entry<Integer, DomainTransitionGraph> dtg : spO.causalGraph.getDTGsMap().entrySet())
//		{
////			dtg.getValue().decompileUniversalTransitions();
//			dtg.getValue().generateDotGraph(new File("DTGs/dtg_"+dtg.getValue().sasVariable.getId()+".dot"));
//		}
//		
//		CausalGraphHeuristic h = new CausalGraphHeuristic(spO);
//		SASPlan plan = null;
//		try
//		{
//			plan = h.getPlan(spO.getCurrentState());
//			SASPlan newPlan = new SASPlan();
//			for (SASAction a : plan.getActions())
//			{
//				if (a instanceof SASAxiom == false)
//					newPlan.addAction(a);
//					
//			}
//			System.out.println(newPlan.getActions());
//			System.out.println("h(G) = "+newPlan.getActions().size());
//		}
//		catch (UnreachableGoalException e)
//		{
//			e.printStackTrace();
//		}
//
//	}

}
