package sas.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javaff.data.Action;
import javaff.data.GroundProblem;
import javaff.data.TotalOrderPlan;
import javaff.parser.PDDL21parser;
import javaff.parser.SolutionParser;
import javaff.search.UnreachableGoalException;
import sas.data.DomainTransitionGraph;
import sas.data.SASAction;
import sas.data.SASProblem;
import sas.data.SASState;
import sas.parser.ParseException;
import sas.parser.SASTranslator;
import sas.parser.SASplusParser;
import sas.search.CausalGraphHeuristic;
import sas.search.CeaHeuristic;
import sas.search.SASHeuristic;

/**
 * Instead of planning, this class just parses in a plan and then iterates through it, outputting the
 * distance to the goal after each action. USeful for seeing what is happening with heuristic outputs.
 * @author David Pattison
 *
 */
public class CGHExecutor
{

	/**
	 * @param args
	 * @throws javaff.parser.ParseException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, javaff.parser.ParseException
	{
		File domain = new File(args[0]);
		File pfile = new File(args[1]);
		if (domain.exists() == false || pfile.exists() == false)
			throw new FileNotFoundException("Domain and/or pfile not found");

		File soln = new File(args[2]);
		if (soln.exists() == false)
			throw new FileNotFoundException("Solution file not found");
		
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
		
		//parse solution -- needs to use the JavaFF versions of the problem
		TotalOrderPlan plan = SolutionParser.parse(PDDL21parser.parseFiles(domain, pfile), soln);

		SASProblem spA = SASplusParser.sasProblemAll;
		SASProblem spO = SASplusParser.sasProblemOptimised;
		spO.setupInitialState();
		
//		spO.causalGraph.generateDotGraph(new java.io.File("cg.dot"));
//		HashMap<Integer, DomainTransitionGraph> newDTGs = new HashMap<Integer, DomainTransitionGraph>();
//		for (Entry<Integer, DomainTransitionGraph> dtg : spO.causalGraph.getDTGsMap().entrySet())
//		{
////			dtg.getValue().decompileUniversalTransitions();
//			dtg.getValue().generateDotGraph(new File("DTGs/dtg_"+dtg.getValue().sasVariable.getId()+".dot"));
//		}
		
		//optimise hashCode() and equals() lookups for actions and facts;
//		spO.bake();
		
		
//		SASHeuristic h = new CausalGraphHeuristic(spO, true);
		SASHeuristic h = new CeaHeuristic(spO.causalGraph);
		System.out.println("Computing CG heuristic estimates for plan of length "+plan.getPlanLength());
		ArrayList<Double> estimates = new ArrayList<Double>();
		for (Action a : plan.getActions())
		{
			double estimate = Double.MAX_VALUE;
			try
			{
				estimate = h.getEstimate(spO.getCurrentState(), spO.getGoalLiterals());
	
				estimates.add(estimate);
				
				SASAction chosen = null;
				for (SASAction sasa : spO.actions.values())
				{
					if (sasa.toString().equals(a.toString()))
					{
						chosen = sasa;
						break;
					}
				}
				
				if (chosen == null)
					throw new NullPointerException("Cannot find SAS+ representation of PDDL plan action");
				
				SASState next = spO.getCurrentState().apply(chosen);
				spO.setCurrentState(next);
			}
			catch (UnreachableGoalException e)
			{
				e.printStackTrace();
			}
		}

		int c = 1;
		for (Double e : estimates)
		{
			System.out.println(c+ " - h(G) = "+e);
			++c;
		}
	}

}
