package sas.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import javaff.graph.*;
import sas.util.CachedStandardGraph;
import sas.util.SASNullAction;
import sas.util.SASActionEdge;
import sas.util.SASPrintable;

//public class DomainTransitionGraph extends StandardGraph<SASLiteral, DTGActionEdge> implements SASPrintable
public class DomainTransitionGraph extends CachedStandardGraph<SASLiteral, DTGActionEdge> implements SASPrintable
{
	private int index;
	private SASVariable sasVariable;
	
	public DomainTransitionGraph(int index, SASVariable variable)
	{
		super(DTGActionEdge.class);
		this.index = index;
		this.sasVariable = variable;
	
	}
	
	public static DomainTransitionGraph decompileUniveralTransitions(DomainTransitionGraph dtg)
	{
		DomainTransitionGraph clone = (DomainTransitionGraph) dtg.clone();
		
		clone.decompileUniversalTransitions();
		
		return clone;
	}
	
	/**
	 * If this DTG contains a Universal transition, calling this method will return a copy
	 * of the DTG with the universal transition explicitly enumerated. The major drawback
	 * to this is that the action edges will be stubs and have no real meaning. The original 
	 * universal state is retained for Dijkstra searches. The universal state means that
	 * the successor states can be transitioned into regardless of their initial value. The 
	 * universal state only has outgoing edges, so can only be re-entered by explicit invocation.
	 * @return
	 */
	public void decompileUniversalTransitions()
	{		
		DomainTransitionGraph compiled = this;// (DomainTransitionGraph) this.clone();
		if (compiled.containsNoneOfThoseState())
		{
			//if there are only 2 states and one of those is a NOTP state, this is the same as saying that
			//the value of the variable is either true or false. Compiling it out would have no effect
			//and would break Dijkstra searches, so return it unmodified.
			if (this.vertexSet().size() == 2)
				return;
				
			SASLiteral notpVert = null;
			for (SASLiteral v : this.vertexSet())
			{
				if (v instanceof NoneOfThoseProposition)
				{
					notpVert = v;
					break; //can break because there should be only 1
				}
			}
			
			//get the outgoing edges from the NOTP node
			Collection<DTGActionEdge> outgoingEdges = this.outgoingEdgesOf(notpVert);
		
			
			//loop over every non-NOTP vertex in the DTG and create a new edge to/from it and everything else
			
			//add a transition from V to the edge's end state
			for (DTGActionEdge e : outgoingEdges)
			{
				SASLiteral w = e.getEffect();
				for (SASLiteral v : this.vertexSet())
				{
					if (v == w || v instanceof NoneOfThoseProposition)
						continue;
				
					compiled.addTransition(v, e.getAction(), w, e.getAssociatedPcs());
				}
			}
//			compiled.removeVertex(notpVert); //delete the NOTP node and associated edges from the compiled DTG
		}
		
		
//		return compiled;
	}
	
	public boolean isDomainGeneratedVariable()
	{
		return sasVariable.getObject() instanceof SASDomainObject; 
	}

	public int getDTGIndex()
	{
		return index;
	}

	public String getVariableName()
	{
		return sasVariable.getObject().getName();
	}

//	public Type getVariableType()
//	{
//		return sasVariable.getVariable().getType();
//	}
	

	public SASLiteral containsProposition(SASProposition p)
	{
		for (SASLiteral v : super.vertexSet())
		{
			if (v.equals(p))
				return v;
		}
		
		return null;
	}

	public boolean containsNoneOfThoseState()
	{
		for (SASLiteral p : this.vertexSet())
		{
			if (p instanceof NoneOfThoseProposition)
				return true;
		}
		
		return false;
	}

	/**
	 * Add the specified edge. If the edge already exists it is not added. Note that this means
	 * that 2 vertices can have multiple edges, so long as the action in each edge is different.
	 * 
	 */
	public void addTransition(SASLiteral fromProposition, SASAction action, 
			SASLiteral toProposition, Collection<SASLiteral> associatedPCs)
	{
		this.addTransition(new DTGActionEdge(fromProposition, action, toProposition, associatedPCs));
	}
	
	/**
	 * Add the specified edge. If the edge already exists it is not added. Note that this means
	 * that 2 vertices can have multiple edges, so long as the action in each edge is different.
	 * @param edge
	 */
	public void addTransition(DTGActionEdge edge)
	{
//		if (super.containsEdge(edge.getPc(), edge.getEffect()) == false ||
//			super.getEdge(edge.getPc(), edge.getEffect()).getAction().equals(edge.getAction()) == false)
		{
			super.addEdge(edge.getPc(), edge.getEffect(), edge);
		}
	}
	
	@Override
	public String toString()
	{
		return "DTG for "+this.sasVariable+"\n\t"+super.toString();
	}
	
	@Override
	public Object clone()
	{
		DomainTransitionGraph clone = 
			new DomainTransitionGraph(this.index, (SASVariable) this.sasVariable.clone());
		
		for (SASLiteral v : super.vertexSet())
			clone.addVertex((SASLiteral) v.clone());
		
		for (DTGActionEdge e : super.edgeSet())
			clone.addTransition((DTGActionEdge) e.clone());
				
		return clone;
	}
	
	@Override
	public int hashCode()
	{
		return super.hashCode() ^ this.index ^ this.sasVariable.hashCode() ^ 31;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (super.equals(obj) == false)
			return false;
		
		DomainTransitionGraph other = (DomainTransitionGraph) obj;
		return this.index == other.index && this.sasVariable.equals(other.sasVariable);
	}
	
	/**
	 * Generates a dot-parseable sas.graph.
	 * @param sas.graph
	 * @param dotFile
	 */
	@Override
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
	    	for (SASLiteral l : this.vertexSet())
	    	{
	    		String vert = l.toString().replace(' ', '_');
	    		vert = vert.replace('-', '_');  
	    		vert = vert.replace('#', '_');
	    		vert = vert.replace("@", "AT");
	    		
    			bufWriter.write(vert +" [label=\""+vert+"\"];\n");
	    	}
	    	
	    	Collection<DTGActionEdge> edges = this.edgeSet();
	    	for (DTGActionEdge e : edges)
	    	{
	    		String startVert = this.getEdgeSource(e).toString().replace(' ', '_');
	    		String endVert = this.getEdgeTarget(e).toString().replace(' ', '_');
	    		startVert = startVert.replace('-', '_');
	    		endVert = endVert.replace('-', '_');	    
	    		startVert = startVert.replace('#', '_');
	    		endVert = endVert.replace('#', '_');	     
	    		startVert = startVert.replace("@", "AT");
	    		endVert = endVert.replace("@", "AT");	    		
	    		
//	    		String label = e.toString();//+","+e.getAssociatedPcs().toString()+"";
	    		String label = e.getAction().getOperatorName();
	    			
	    		label = label.replace('-', '_');   
	    		label = label.replace('#', '_');   
	    		label = label.replace("@", "AT");  
	    		label = label.replace(")", "");  
	    		label = label.replace("(", ""); 
	    		label = label.replace("[", "");  
	    		label = label.replace("]", "");
	    		
	    		
	    		bufWriter.write(startVert+" -> "+endVert+" [label=\""+label+"\"];\n");
//	    		}
	    	}
	    	
	    	bufWriter.write("}\n");
	    	
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
		p.println("DTG("+this.sasVariable.toString()+" = (V"+this.vertexSet().size()+", E"+this.edgeSet().size()+")");
	}

	public void setDTGIndex(int index)
	{
		this.index = index;
	}

	public SASVariable getVariable()
	{
		return sasVariable;
	}

	public void setVariable(SASVariable sasVariable)
	{
		this.sasVariable = sasVariable;
	}
	
}
