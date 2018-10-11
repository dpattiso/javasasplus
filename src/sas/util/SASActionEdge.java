package sas.util;

import org.jgrapht.graph.DefaultEdge;

import sas.data.SASAction;

public class SASActionEdge extends DefaultEdge
{

	private SASAction action;
	
	public SASActionEdge(SASAction a)
	{
		this.action = a;
	}

	public SASAction getAction()
	{
		return action;
	}

	public void setAction(SASAction action)
	{
		this.action = action;
	}
}
