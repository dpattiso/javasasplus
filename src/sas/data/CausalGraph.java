package sas.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.jgrapht.graph.DefaultEdge;

import javaff.graph.*;

import sas.util.CachedStandardGraph;
import sas.util.CausalGraphLink;
import sas.util.SASPrintable;



//public class CausalGraph extends StandardGraph<DomainTransitionGraph, CausalGraphLink> implements SASPrintable
public class CausalGraph extends CachedStandardGraph<DomainTransitionGraph, CausalGraphLink> implements SASPrintable
{
	private Map<Integer, DomainTransitionGraph> dtgs;
	private Set<DomainTransitionGraph> leaves;
	private HashSet<DomainTransitionGraph> roots;	
	
	public CausalGraph()//Map<Integer, DomainTransitionGraph> dtgs)
	{
		super(CausalGraphLink.class);
		
		this.dtgs = new TreeMap<Integer, DomainTransitionGraph>();
//		this.dtgs = new TreeMap<Integer, DomainTransitionGraph>(dtgs);
		this.leaves = new HashSet<DomainTransitionGraph>();
		this.roots = new HashSet<DomainTransitionGraph>();
		
		
//		initCG(dtgs);
//		findLeavesAndRoots();
	}
	
	@Override
	public boolean addVertex(DomainTransitionGraph v)
	{
		this.dtgs.put(v.getDTGIndex(), v);
		return super.addVertex(v);
	}
	
	@Override
	public Object clone()
	{
		Map<Integer, DomainTransitionGraph> dtgs = new TreeMap<Integer, DomainTransitionGraph>();
		for (Entry<Integer, DomainTransitionGraph> dtg : this.dtgs.entrySet())
		{
			dtgs.put(dtg.getKey(), (DomainTransitionGraph) dtg.getValue().clone());
		}	
		
		CausalGraph clone = new CausalGraph();
		for (DomainTransitionGraph v : super.vertexSet())
			clone.addVertex(v);
		
		for (CausalGraphLink e : super.edgeSet())
			clone.addEdge(super.getEdgeSource(e), super.getEdgeTarget(e), e);
		
		return clone;
	}

	public Collection<DomainTransitionGraph> getDTGs()
	{
		return this.dtgs.values();
	}
	
	public Map<Integer, DomainTransitionGraph> getDTGsMap()
	{
		return this.dtgs;
	}
	
	public DomainTransitionGraph getDTG(int varIndex)
	{
		return this.dtgs.get(varIndex);
	}

	public boolean isRoot(int varIndex)
	{
		return this.isRoot(this.dtgs.get(varIndex));
	}
	
	public boolean isRoot(DomainTransitionGraph dtg)
	{
		return this.roots.contains(dtg);
	}
	
	public boolean isLeaf(int varIndex)
	{
		return this.isRoot(this.dtgs.get(varIndex));
	}
	
	public boolean isLeaf(DomainTransitionGraph dtg)
	{
		return this.leaves.contains(dtg);
	}
	
	public CausalGraphLink addEdge(int dtgFrom, int dtgTo)
	{
		if (super.containsVertex(this.dtgs.get(dtgFrom)) == false)
			super.addVertex(this.dtgs.get(dtgFrom));

		if (super.containsVertex(this.dtgs.get(dtgTo)) == false)
			super.addVertex(this.dtgs.get(dtgTo));
		
		return super.addEdge(this.dtgs.get(dtgFrom), this.dtgs.get(dtgTo));
	}
	
	public void findLeavesAndRoots()
	{
		this.leaves.clear();
		this.roots.clear();
		
		for (DomainTransitionGraph v : super.vertexSet())
		{
//			System.out.println("DTG "+v.getObject().getVariableName()+", has "+super.getOutDegree(v)+" out edgeSet and "+
//					super.getInDegree(v)+" in edgeSet");
			if (super.outDegreeOf(v) == 0 && super.inDegreeOf(v) >= 1)// && this.leaves.contains(v.getObject()) == false)
			{
//				System.out.println("Added leaf "+v.getObject());
				this.leaves.add(v);
			}
			else if (super.inDegreeOf(v) == 0 && super.outDegreeOf(v) >= 1)// && this.leaves.contains(v.getObject()) == false)
			{
//				System.out.println("Added leaf "+v.getObject());
				this.roots.add(v);
			}
//			else
//			{
//				System.out.println("SKipped node "+v.getObject());	
//			}
		}
	}

	/**
	 * Returns all domain transition graphs which have no out edgeSet- meaning they are only ever used by other objects, and
	 * do not affect anything else.
	 * @return
	 */
	public Set<DomainTransitionGraph> getLeaves()
	{
		return leaves;
	}
	
	/**
	 * Returns all domain transition graphs which have no in edgeSet- meaning they are only ever used to modify
	 * other objects.
	 * @return
	 */
	public Set<DomainTransitionGraph> getRoots()
	{
		return roots;
	}
	
	/**
	 * Does this graph have any root nodes, i.e. no incoming edges.
	 * @return
	 */
	public boolean hasRoots()
	{
		return this.getRoots().isEmpty() == false;
	}

	/**
	 * Does this graph have any leaf nodes, i.e. no outgoing edges.
	 * @return
	 */
	public boolean hasLeaves()
	{
		return this.getLeaves().isEmpty() == false;
	}
	
	/**
	 * Returns all the variables represented within this causal sas.graph.
	 * @return
	 */
	public Set<SASParameter> getVariables()
	{
		Set<SASParameter> vars = new HashSet<SASParameter>();
		
		Collection<DomainTransitionGraph> dtgs = this.vertexSet();
		for (DomainTransitionGraph dtg : dtgs)
			vars.add(dtg.getVariable().getObject());
		
		return vars;
	}
	
	
	/**
	 * Returns all the variables represented within this causal sas.graph.
	 * @return
	 */
	public Set<SASVariable> getSASVariables()
	{
		Set<SASVariable> vars = new HashSet<SASVariable>();
		
		Collection<DomainTransitionGraph> dtgs = this.vertexSet();
		for (DomainTransitionGraph dtg : dtgs)
			vars.add(dtg.getVariable());
		
		return vars;
	}
	
	

//	/**
//	 * Generates a dot-parseable sas.graph.
//	 * @param sas.graph
//	 * @param dotFile
//	 */
//    public void generateDotSubGraph(File dotFile)
//    {
//    	FileWriter writer;
//    	BufferedWriter bufWriter;
//    	try
//    	{
//	    	writer = new FileWriter(dotFile);
//	    	bufWriter =  new BufferedWriter(writer);
//	    	
//	    	bufWriter.write("digraph Tree {\n\tnode [shape=circle, fontsize=14, color=black, fillcolor=white, fontcolor=black];\n\t edge [style=solid, color=black];\n");
//	    	
//	    	System.out.println("vertex size is "+this.vertexSet().size());
//	    	System.out.println("edge size is "+this.edgeSet().size());
//	    	
//
//	    	for (DomainTransitionGraph dtg : super.vertexSet())
//	    	{
//		    	bufWriter.write("subgraph DTG("+dtg.sasVariable.getObject()+")\n");
//		    	
//		    	System.out.println("vertex size is "+this.vertexSet().size());
//		    	System.out.println("edge size is "+this.edgeSet().size());
//		    	for (SASLiteral l : dtg.vertexSet())
//		    	{
//		    		String vert = l.toString().replace(' ', '_');
//		    		vert = vert.replace('-', '_');  
//		    		vert = vert.replace('#', '_');
//		    		vert = vert.replace("@", "AT");
//		    		
//	    			bufWriter.write(vert +" [label=\""+vert+"\"];\n");
//		    	}
//		    	
//		    	Collection<DTGActionEdge> edges = dtg.edgeSet();
//		    	for (DTGActionEdge e : edges)
//		    	{
//		    		String startVert = dtg.getEdgeSource(e).toString().replace(' ', '_');
//		    		String endVert = dtg.getEdgeTarget(e).toString().replace(' ', '_');
//		    		startVert = startVert.replace('-', '_');
//		    		endVert = endVert.replace('-', '_');	    
//		    		startVert = startVert.replace('#', '_');
//		    		endVert = endVert.replace('#', '_');	     
//		    		startVert = startVert.replace("@", "AT");
//		    		endVert = endVert.replace("@", "AT");	    				
//		    		bufWriter.write(startVert+" -> "+endVert+" [label=\""+e.toString()+"\"];\n");
////		    		}
//		    	}
//		    	
//	    		
//	    	}
//	    	
//	    	for (CausalGraphLink e : super.edgeSet())
//	    	{
//
//	    		String startVert = super.getEdgeSource(e).getVariableName().replace(' ', '_');
//	    		String endVert =super.getEdgeTarget(e).getVariableName().toString().replace(' ', '_');
//	    		startVert = startVert.replace('-', '_');
//	    		endVert = endVert.replace('-', '_');	    		
//	    		startVert = startVert.replace('#', '_');
//	    		endVert = endVert.replace('#', '_');	    		
//	    		startVert = startVert.replace("@", "AT");
//	    		endVert = endVert.replace("@", "AT");	    		
//	    		bufWriter.write("\"var "+super.getEdgeSource(e).index + " "+startVert+"\" -> "+
//	    				"\"var "+super.getEdgeTarget(e).index+" "+endVert+"\";\n");
////	    		}
//	    	}
//	    	
//	    	bufWriter.write("}\n");
//    		bufWriter.close();
//    		
//    		System.out.println("writing file "+dotFile.getAbsolutePath());
//    		Process p = Runtime.getRuntime().exec("dot -Tpng \'"+dotFile.getAbsolutePath()+"_dot\' > \'./test.png/'");
//    		p.waitFor();
//    	}
//    	catch (IOException ioe)
//    	{
//    		System.out.println("Cannot create file: "+ioe.getMessage());
//    		ioe.printStackTrace();
//    	}
//		catch (InterruptedException e)
//		{
//    		System.out.println("Cannot create file: "+e.getMessage());
//			e.printStackTrace();
//		}
//    	finally
//    	{
//    	}
//    }

	

	/**
	 * Generates a dot-parseable sas.graph.
	 * @param sas.graph
	 * @param dotFile
	 */
    public void generateDotGraph(File dotFile)
    {
    	FileWriter writer;
    	BufferedWriter bufWriter;
    	try
    	{
	    	writer = new FileWriter(dotFile);
	    	bufWriter =  new BufferedWriter(writer);
	    	
	    	bufWriter.write("digraph Tree {\n\tnode [shape=circle, fontsize=14, color=black, fillcolor=white, fontcolor=black];\n\t edge [style=solid, color=black];\n");
	    	
	    	System.out.println("vertex size is "+this.vertexSet().size());
	    	System.out.println("edge size is "+this.edgeSet().size());
//	    	
//	    	int counter = 0;
//	    	for (Object v : sas.graph.vertexSet())
//	    	{
//	    		String vert = ((Vertex)v).getObject().toString().replace(' ', '_');
//
//    			bufWriter.write(counter++ +" [label=\""+vert+"\"];\n");
//	    	}
	    	
	    	for (CausalGraphLink e : super.edgeSet())
	    	{
//    			bufWriter.write(counter++ +" [label=\""+e+"\"];\n");
	    		//System.out.println("Vertex: "+p);
//	    		Iterator<RelationshipEdge> outedgeSetIter = this.graph.outgoingedgeSetOf(p).iterator();
//	    		while (outedgeSetIter.hasNext())
//	    		{
//	    			RelationshipEdge e = outedgeSetIter.next();
	    			//System.out.println("Outgoing edge: "+e);

	    		String startVert = super.getEdgeSource(e).getVariableName().replace(' ', '_');
	    		String endVert =super.getEdgeTarget(e).getVariableName().toString().replace(' ', '_');
	    		startVert = startVert.replace('-', '_');
	    		endVert = endVert.replace('-', '_');	    		
	    		startVert = startVert.replace('#', '_');
	    		endVert = endVert.replace('#', '_');	    		
	    		startVert = startVert.replace("@", "AT");
	    		endVert = endVert.replace("@", "AT");	    		
	    		bufWriter.write("\"var "+super.getEdgeSource(e).getDTGIndex() + " "+startVert+"\" -> "+
	    				"\"var "+super.getEdgeTarget(e).getDTGIndex()+" "+endVert+"\";\n");
//	    		}
	    	}
	    	
	    	bufWriter.write("}\n");
	    	
    		//writer.close();
    		bufWriter.close();
    		
    		System.out.println("writing file "+dotFile.getAbsolutePath());
    		Process p = Runtime.getRuntime().exec("dot -Tpng \'"+dotFile.getAbsolutePath()+"_dot\' > \'./test.png/'");
    		p.waitFor();
    	}
    	catch (IOException ioe)
    	{
    		System.out.println("Cannot create file: "+ioe.getMessage());
    		ioe.printStackTrace();
    	}
		catch (InterruptedException e)
		{
    		System.out.println("Cannot create file: "+e.getMessage());
			e.printStackTrace();
		}
    	finally
    	{
    	}
    }

	@Override
	public void SASPrint(PrintStream p, int indent)
	{
		p.println("CG(V"+this.vertexSet().size()+", E"+this.edgeSet().size()+")");
	}
	
}
