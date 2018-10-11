package sas.test;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import javaff.data.GroundProblem;
import javaff.planning.STRIPSState;

import sas.data.*;
import sas.parser.ParseException;
import sas.parser.SASTranslator;
import sas.parser.SASplusParser;
import sas.util.SASException;



public class SASParserTester
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		File domain = new File("../Domains/ipc5/trucksPropositional/domain_p01.pddl");
		File pfile = new File("../Domains/ipc5/trucksPropositional/p01.pddl");
		
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
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (ParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		SASProblem spA = SASplusParser.sasProblemAll;
		SASProblem spO = SASplusParser.sasProblemOptimised;
		spO.setupInitialState();
		
		SASState state = spO.getCurrentState();
		STRIPSState strips = state.convertToPDDL(spO);
		
		GroundProblem gp = spO.convertToPDDL(spO);
		System.out.println(spA);
	}
	

//	public static void processPDDL(SASProblem SAS, GroundProblem GP)
//	{
//		SAS.name = GP.name;
//		
//		// setup initial state and current state
//		SAS.initial = new SASState(SAS.variables);
//		SAS.current = SAS.initial;
//		
//		//setup goal
//		SAS.goal = new HashMap<SASVariable, Integer>(SASplusParser.goal.size());
//		for (Entry<Integer, Integer> e : SASplusParser.goal.entrySet())
//		{
//			SASVariable v = SASplusParser.sasVariables.get(e.getKey());
//			SAS.goal.put(v, e.getValue());
//		}
//		
//		// SAS.actions needs to have grounded props added to the SAS+ reps
//		for (SASAction a : SAS.actions)
//		{
//			And pc = new And();
//			for (Entry<Integer, Integer> e : a.sasPrevails.entrySet())
//			{
//				//add prevails to PCs
//				pc.add(SASplusParser.sasVariables.get(e.getKey()).getValue(e.getValue()));
//			}
//			
//			And eff = new And();
//			for (Entry<Integer, SASEffect> e : a.sasEffects.entrySet())
//			{
//				//Add PC of effect
//				pc.add(SASplusParser.sasVariables.get(e.getKey()).getValue(e.getValue().precondition));
//				
//				//Add effect of effect
//				eff.add(SASplusParser.sasVariables.get(e.getKey()).getValue(e.getValue().effect));
//			}
//			
//			//effect preconditions- I'm still not sure how these work in PDDL...
//			for (Entry<Integer, Integer> e : a.sasEffectConditions.entrySet())
//			{
//				//Add PC of effect
//				pc.add(SASplusParser.sasVariables.get(e.getKey()).getValue(e.getValue()));
//			}
//		}
//		
//		
//		// SAS.reachableFacts should already be set up
//
//		// try to detect the names of variables of the form (varX) by
//		// extrapolating common
//		// parameters from their mutexes. If none exist, just use a default name
//		int defaultCount = 0;
//		for (SASMutexGroup m : SAS.mutexes)
//		{
//			HashSet<Parameter> common = null;
//			for (SingleLiteral p : m.getMutexes())
//			{
//				if (common == null)
//				{
//					common = new HashSet<Parameter>();
//					common.addAll(p.getParameters());
//				}
//				else
//				{
//					common.retainAll(p.getParameters());
//				}
//			}
//
//			Parameter finalVar;
//			if (m.getMutexes().size() > 1 && common.size() == 1)
//				finalVar = common.iterator().next();
//			else if (m.getMutexes().size() == 1)
//				finalVar = new PDDLObject(m.getMutexes().iterator().next()
//						.toString(), Type.rootType);
//			else
//				finalVar = new PDDLObject("sasvar" + defaultCount++,
//						Type.rootType);
//
//			m.setVariable((PDDLObject) finalVar);
//		}
//
//		// only set size of DTG array, because DTG transitions are in preprocess
//		// file
//		SAS.dtgs = new HashMap<Integer, DomainTransitionGraph>(SAS.variables
//				.size());
//
//		// now need to set up names for the variables in the DTGs. These are
//		// extrapolated in the same manner as 'all.groups' mutexes, but instead
//		// use test.groups variable domains.
//		defaultCount = 0; // reset for test.groups variable domains
//		for (SASVariable v : SAS.variables)
//		{
//			// PDDLObject var = null;
//			HashSet<Parameter> common = null;
//			for (SingleLiteral p : v.getValues())
//			{
//				if (common == null)
//				{
//					common = new HashSet<Parameter>();
//					common.addAll(p.getPredicateSymbol().getParameters());
//				}
//				else
//				{
//					common.retainAll(p.getPredicateSymbol().getParameters());
//				}
//			}
//
//			PDDLObject finalVar;
//			if (v.getValues().size() > 1 && common.size() == 1)
//				// && SAS.varDomains.containsKey(common.iterator().next()) ==
//				// false)
//				finalVar = (PDDLObject) common.iterator().next();
//			else if (v.getValues().size() == 1)
//				finalVar = new PDDLObject(v.getValues().get(0).toString(),
//						Type.rootType);
//			else
//				finalVar = new PDDLObject("domainvar" + defaultCount++,
//						Type.rootType);
//
//			// put the variable and the domain into the SAS problem
//			v.setVariable(finalVar);
//		}
//
//		// Causal sas.graph created in preprocess
//	}

}
