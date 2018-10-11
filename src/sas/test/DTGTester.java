package sas.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javaff.planning.STRIPSState;
import sas.data.DomainTransitionGraph;
import sas.data.SASProblem;
import sas.data.SASState;
import sas.parser.ParseException;
import sas.parser.SASTranslator;
import sas.parser.SASplusParser;

public class DTGTester
{

	/**
	 * @param args
	 */
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

		spO.causalGraph.generateDotGraph(new File("cg.dot"));
		for (DomainTransitionGraph dtg : spO.causalGraph.getDTGs())
		{
			dtg.generateDotGraph(new File("DTGs/"+dtg.getVariableName()+".dot"));
		}
	}

}
