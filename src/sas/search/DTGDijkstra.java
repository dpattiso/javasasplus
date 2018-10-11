package sas.search;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javaff.data.Action;

import sas.data.DTGActionEdge;
import sas.data.DomainTransitionGraph;
import sas.data.SASLiteral;

public class DTGDijkstra
{

	private final List<SASLiteral> nodes;
	private final List<DTGActionEdge> edges;
	private Set<SASLiteral> settledNodes;
	private Set<SASLiteral> unSettledNodes;
	private Map<SASLiteral, SASLiteral> predecessors;
	private Map<SASLiteral, Double> distance;
	private DomainTransitionGraph graph;

	public DTGDijkstra(DomainTransitionGraph graph)
	{
		// Create a copy of the array so that we can operate on this array
		this.graph = graph;
		this.nodes = new ArrayList<SASLiteral>(this.graph.vertexSet());
		this.edges = new ArrayList<DTGActionEdge>(this.graph.edgeSet());
	}

	public void execute(SASLiteral source)
	{
		this.settledNodes = new HashSet<SASLiteral>();
		this.unSettledNodes = new HashSet<SASLiteral>();
		this.distance = new HashMap<SASLiteral, Double>();
		this.predecessors = new HashMap<SASLiteral, SASLiteral>();
		this.distance.put(source, 0d);
		this.unSettledNodes.add(source);
		while (this.unSettledNodes.size() > 0)
		{
			SASLiteral node = getMinimum(this.unSettledNodes);
			this.settledNodes.add(node);
			this.unSettledNodes.remove(node);
			findMinimalDistances(node);
		}
	}

	private void findMinimalDistances(SASLiteral node)
	{
		List<SASLiteral> adjacentNodes = getNeighbors(node);
		for (SASLiteral target : adjacentNodes)
		{
			if (getShortestDistance(target) > getShortestDistance(node)
					+ getDistance(node, target))
			{
				this.distance.put(target,
						getShortestDistance(node) + getDistance(node, target));
				this.predecessors.put(target, node);
				this.unSettledNodes.add(target);
			}
		}

	}

	private double getDistance(SASLiteral node, SASLiteral target)
	{
		for (DTGActionEdge edge : this.edges)
		{
			if (this.graph.getEdgeSource(edge).equals(node)
					&& this.graph.getEdgeTarget(edge).equals(target))
			{
				return edge.getWeight();
			}
		}
		throw new RuntimeException("Should not happen");
	}

	private List<SASLiteral> getNeighbors(SASLiteral node)
	{
		List<SASLiteral> neighbors = new ArrayList<SASLiteral>();
		for (DTGActionEdge edge : this.edges)
		{
			if (this.graph.getEdgeSource(edge).equals(node)
					&& !isSettled(this.graph.getEdgeTarget(edge)))
			{
				neighbors.add(this.graph.getEdgeTarget(edge));
			}
		}
		return neighbors;
	}

	private SASLiteral getMinimum(Set<SASLiteral> vertexes)
	{
		SASLiteral minimum = null;
		for (SASLiteral vertex : vertexes)
		{
			if (minimum == null)
			{
				minimum = vertex;
			}
			else
			{
				if (this.getShortestDistance(vertex) < this.getShortestDistance(minimum))
				{
					minimum = vertex;
				}
			}
		}
		return minimum;
	}

	private boolean isSettled(SASLiteral vertex)
	{
		return this.settledNodes.contains(vertex);
	}

	private double getShortestDistance(SASLiteral destination)
	{
		Double d = this.distance.get(destination);
		if (d == null)
		{
			return Integer.MAX_VALUE;
		}
		else
		{
			return d;
		}
	}

	/*
	 * This method returns the path from the source to the selected target and
	 * NULL if no path exists
	 */
	public LinkedList<SASLiteral> getPath(SASLiteral target)
	{
		LinkedList<SASLiteral> path = new LinkedList<SASLiteral>();
		SASLiteral step = target;
		// Check if a path exists
		if (this.predecessors.get(step) == null)
		{
			return null;
		}
		path.add(step);
		while (this.predecessors.get(step) != null)
		{
			step = this.predecessors.get(step);
			path.add(step);
		}
		// Put it into the correct order
		Collections.reverse(path);
		return path;
	}

}
