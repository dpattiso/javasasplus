package sas.search;

import java.util.HashMap;

import sas.data.DTGActionEdge;
import sas.data.SASAction;

public class DTGNode
{
	public int value;
	public final HashMap<Integer, Double> dists;
	public DTGNode parent;
	
	public DTGNode(int value)
	{
		this.dists = new HashMap<Integer, Double>();
		this.parent = null;
		this.value = value;
	}
	
	@Override
	public String toString()
	{
		return this.value + ": " + this.dists.toString();
	}
}