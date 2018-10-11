package sas.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import sas.util.SASException;
import sas.util.UnsolveableProblemException;

public abstract class SASTranslator
{
	
	/**
	 * Translate the specified PDDL domain and problem into a SAS+ representation. SAS+ output is passed to 
	 * System.out.
	 * @param domain
	 * @param pfileNoGoal
	 * @throws Exception
	 * @throws UnsolveableProblemException
	 */
	public static void translateToSAS(File domain, File pfileNoGoal) throws Exception, UnsolveableProblemException
	{
		SASTranslator.translateToSAS(domain, pfileNoGoal, System.out);
	}
	
	/**
	 * Translate the specified PDDL domain and problem into a SAS+ representation. SAS+ output is printed to
	 * the specified print stream.
	 * @param domain
	 * @param pfileNoGoal
	 * @throws Exception
	 * @throws UnsolveableProblemException
	 */
	public static void translateToSAS(File domain, File pfileNoGoal, PrintStream sasOutputStream) throws Exception, UnsolveableProblemException
	{
		String sasDirectory = ".";
		if (System.getProperties().containsKey("downward.home"))
		{
			sasDirectory = System.getProperty("downward.home");
		}
		System.out.println("SAS+ directory is " + sasDirectory);
		try
		{
			String translate, hack;
			
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("windows"))
			{
				//FIXME this will probably break on most systems! 
				translate = "C:/Python27/python.exe " + sasDirectory
						+ "/lama/translate/translate.py \"" + domain.getAbsolutePath() + "\" \""
						+ pfileNoGoal.getAbsolutePath() + "\"";
				hack = "perl " + sasDirectory + "/sas_hack_new_windows.pl";
			}
			else
			{
				translate = "python " + sasDirectory
						+ "/lama/translate/translate.py \"" + domain.getAbsolutePath() + "\" \""
						+ pfileNoGoal.getAbsolutePath() + "\"";
				hack = "perl " + sasDirectory + "/sas_hack_new_linux.pl";
			}

			String[] commands = {translate, hack};
			Process p;
			BufferedReader reader;
			int result;
			String line;
			
			for (int i = 0; i < commands.length; i++)
			{
				System.out.println("\nexecuting: " + commands[i] + "\n");
				p = Runtime.getRuntime().exec(commands[i]);
				
				StreamGobbler errorGobbler = new StdErrStreamGobbler(p.getErrorStream());

				// any output?
				StreamGobbler outputGobbler = new StdOutStreamGobbler(p.getInputStream());

				// start gobblers
				outputGobbler.start();
				errorGobbler.start();
				
				result = p.waitFor();
				if (result != 0)
					throw new IOException(
							"Process terminated with non 0 exit code.");

//				reader = new BufferedReader(new InputStreamReader(p
//						.getInputStream()));
//				line = reader.readLine();
//				while (line != null)
//				{
//					if (line.contains("No relaxed solution!"))
//						throw new UnsolveableProblemException("Problem has no relaxed solution!");
//					
//					sasOutputStream.println(line);
//					line = reader.readLine();
//				}
//				reader.close();
			}
			System.out.println("Finished perl translation");
		}
		catch (IOException e)
		{
			System.err.println("IO Exception occurred: " + e.getMessage());
			throw e;
		}
//		catch (UnsolveableProblemException e)
//		{
//			throw e;
//		}
		catch (InterruptedException e)
		{
			System.err.println("Translation process was interrupted: "
					+ e.getMessage());
			throw e;
		}
		catch (Exception e)
		{
			System.err.println("Exception thrown during SAS translation: "
					+ e.getMessage());
			e.printStackTrace();
			throw e;
		}	
	}
	
	public static void errorReceived(Object sender, String output) throws SASException
	{
		System.err.println(output);
		throw new SASException("SAS+ parsing failed: "+ output);
	}
	
	public static void outputReceived(Object sender, String output)
	{
		System.out.println(output);
	}
	
	public static abstract class StreamGobbler extends Thread {
	    InputStream is;
	    String type;

	    private StreamGobbler(InputStream is, String type) {
	        this.is = is;
	        this.type = type;
	    }

	    @Override
	    public void run() {
	        try {
	            InputStreamReader isr = new InputStreamReader(is);
	            BufferedReader br = new BufferedReader(isr);
	            String line = null;
	            while ((line = br.readLine()) != null)
					try
					{
						handleOutput(line);
					}
					catch (SASException e)
					{
						e.printStackTrace();
					}
	        }
	        catch (IOException ioe) {
	            ioe.printStackTrace();
	        }
	    }
	    
	    protected abstract void handleOutput(String text) throws SASException;
	}
	
	public static class StdErrStreamGobbler extends StreamGobbler {

	    private StdErrStreamGobbler(InputStream is) {
	        super(is, "ERROR");
	    }

		@Override
		protected void handleOutput(String text) throws SASException
		{
			errorReceived(this, this.type+": "+ text);
		}
	}	
	
	public static class StdOutStreamGobbler extends StreamGobbler {

	    private StdOutStreamGobbler(InputStream is) {
	        super(is, "OUT");
	    }

		@Override
		protected void handleOutput(String text) throws SASException
		{
			outputReceived(this, this.type+": "+ text);
		}
	}
}
