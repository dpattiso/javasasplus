package sas.parser;

import sas.util.SASException;

public interface SASOutputCallback
{
	public void errorReceived(Object sender, String output) throws SASException;
	
	public void outputReceived(Object sender, String output) throws SASException;
}
