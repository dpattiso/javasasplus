package sas.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.EdgeFactory;

import javaff.graph.StandardGraph;

/**
 * This class caches the incoming and outgoing edges and vertices of every vertex added to the graph.
 * This is done to speed up access to these, as JgraphT is quite slow at doing this. Not all 
 * methods which could have their result cached are overriden. In particular {@link #getEdgeSource(Object)} and
 * {@link #getEdgeTarget(Object)} are not currently cached.
 * @author David Pattison
 *
 * @param <V> Type of vertices.
 * @param <E> Type of edges.
 */
public class CachedStandardGraph<V, E> extends StandardGraph<V, E>
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5251179809603498897L;
	private HashMap<V, HashSet<E>> outEdgeSets, inEdgeSets;
	private HashMap<V, HashSet<V>> outVertexSets, inVertexSets;
	
	@SuppressWarnings("rawtypes")
	private static final HashSet EmptySet = new HashSet(0);

	public CachedStandardGraph(Class<E> edgeFactory)
	{
		super(edgeFactory);
		
		this.initialiseLookups();
	}

	public CachedStandardGraph(EdgeFactory<V,E> edgeFactory)
	{
		super(edgeFactory);
		
		this.initialiseLookups();
	}
	
	public CachedStandardGraph(StandardGraph<V, E> existing)
	{
		super(existing);
		
		this.initialiseLookups();
	}
	
	protected CachedStandardGraph(CachedStandardGraph<V, E> existing)
	{
		super(existing);
		
		this.outEdgeSets = existing.outEdgeSets;
		this.inEdgeSets = existing.inEdgeSets;
		this.outVertexSets = existing.outVertexSets;
		this.inVertexSets = existing.inVertexSets;
	}
	
	private void initialiseLookups()
	{
		this.outEdgeSets = new HashMap<V, HashSet<E>>();
		this.inEdgeSets = new HashMap<V, HashSet<E>>();
		
		this.outVertexSets = new HashMap<V, HashSet<V>>();
		this.inVertexSets = new HashMap<V, HashSet<V>>();
	}
	
	@Override
	public boolean isAllowingMultipleEdges()
	{
//		return super.isAllowingMultipleEdges();
		return false;
	}
	
	@Override
	public Object clone()
	{
		CachedStandardGraph<V, E> clone = new CachedStandardGraph<V, E>((StandardGraph<V,E>) super.clone());
		
		clone.inEdgeSets = this.inEdgeSets;
		clone.outEdgeSets = this.outEdgeSets;
		clone.inVertexSets = this.inVertexSets;
		clone.outVertexSets = this.outVertexSets;
		
		return clone;
	}
	
	@Override
	public E addEdge(V sourceVertex, V targetVertex)
	{
		if (super.containsEdge(sourceVertex, targetVertex) == true)
			return null;
			
		E edge = super.addEdge(sourceVertex, targetVertex);
		if (edge != null)
		{
			this.addLookup(sourceVertex, targetVertex, edge);
		}
		
		return edge;
	}	
	
	@Override
	public boolean addEdge(V sourceVertex, V targetVertex, E e)
	{
//		if (super.containsEdge(sourceVertex, targetVertex) == true)
//			return false;
		
		boolean edge = super.addEdge(sourceVertex, targetVertex, e);
		if (edge == true)
		{
			this.addLookup(sourceVertex, targetVertex, e);
		}
		
		return edge;
	}
	
	@Override
	public void clear()
	{
		this.clearCache();
		
		super.clear();
	}
	
	@Override
	public Set<E> edgesOf(V vertex)
	{
		HashSet<E> edges = new HashSet<E>();
		edges.addAll(this.outgoingEdgesOf(vertex));
		edges.addAll(this.incomingEdgesOf(vertex));
		return edges;
	}

	@Override
	public int degreeOf(V vertex)
	{
		return this.edgesOf(vertex).size();
	}
	
	@Override
	public Collection<V> getConnectedVertices(V v)
	{
		HashSet<V> verts = new HashSet<V>();
		verts.addAll(this.getIncomingVertices(v));
		verts.addAll(this.getOutgoingVertices(v));
		return verts;
	}
	
	@Override
	public int inDegreeOf(V vertex)
	{
		return this.incomingEdgesOf(vertex).size();
	}
	
	@Override
	public int outDegreeOf(V vertex)
	{
		return this.outgoingEdgesOf(vertex).size();
	}
	

	@Override
	public boolean containsEdge(V sourceVertex, V targetVertex)
	{
		Collection<V> targets = this.outVertexSets.get(sourceVertex);
		if (targets != null)
			return targets.contains(targetVertex);
		else
			return false;
	}
	
	@Override
	public Set<E> outgoingEdgesOf(V vertex)
	{
		Set<E> out = this.outEdgeSets.get(vertex);
		if (out != null)
			return out;
		else
			return EmptySet;
	}	
	
	@Override
	public Set<E> incomingEdgesOf(V vertex)
	{
		Set<E> in = this.inEdgeSets.get(vertex);
		if (in != null)
			return in;
		else
			return EmptySet;
	}
	
	@Override
	public Collection<V> getOutgoingVertices(V v)
	{
		Collection<V> out = this.outVertexSets.get(v);
		if (out != null)
			return out;
		else
			return EmptySet;
	}
	
	@Override
	public Collection<V> getIncomingVertices(V v)
	{
		Collection<V> in = this.inVertexSets.get(v);
		if (in != null)
			return in;
		else
			return EmptySet;
	}
	
	/**
	 * Remove all cached lookups.
	 */
	protected void clearCache()
	{
		this.outEdgeSets.clear();
		this.inEdgeSets.clear();
		this.inVertexSets.clear();
		this.outVertexSets.clear();
	}

	@Override
	protected boolean removeAllEdges(E[] edges)
	{
		this.clearCache();
		
		return super.removeAllEdges(edges);
	}
	
	@Override
	public boolean removeAllEdges(Collection<? extends E> edges)
	{
		this.clearCache();
		
		return super.removeAllEdges(edges);
	}
	
	@Override
	public Set<E> removeAllEdges(V sourceVertex, V targetVertex)
	{
		Set<E> removed = super.removeAllEdges(sourceVertex, targetVertex);
		for (E e : removed)
		{
			this.removeLookup(sourceVertex, targetVertex, e);
		}
		
		return removed;
	}
		
	@Override
	public boolean removeAllVertices(Collection<? extends V> vertices)
	{
		this.clearCache();
		
		return super.removeAllVertices(vertices);
	}


	protected void removeLookup(V sourceVertex, V targetVertex, E edge)
	{
		if (this.outEdgeSets.containsKey(sourceVertex))
		{
			this.outEdgeSets.get(sourceVertex).add(edge);
		}
		
		if (this.inEdgeSets.containsKey(targetVertex))
		{
			this.inEdgeSets.get(targetVertex).add(edge);
		}
		

		//vertex lookups
		if (this.outVertexSets.containsKey(sourceVertex))
		{
			this.outVertexSets.get(sourceVertex).add(targetVertex);
		}
		
		if (this.inVertexSets.containsKey(targetVertex))
		{
			this.inVertexSets.get(targetVertex).add(sourceVertex);
		}
	}
	
	@Override
	public boolean removeEdge(E e)
	{
		V s = super.getEdgeSource(e);
		V t = super.getEdgeTarget(e);
		
		boolean res = super.removeEdge(e);
		if (res)
		{
			this.removeLookup(s, t, e);
		}
		
		return res;
	}
	
	@Override
	public E removeEdge(V sourceVertex, V targetVertex)
	{
		E e = super.removeEdge(sourceVertex, targetVertex);
		if (e != null)
		{
			this.removeLookup(sourceVertex, targetVertex, e);
		}
		
		return e;
	}
	
	@Override
	public boolean removeVertex(V v)
	{
		Collection<V> out = super.getOutgoingVertices(v);
		Collection<V> in = super.getIncomingVertices(v);
		Collection<E> edges = super.edgesOf(v);
		
		boolean res = super.removeVertex(v);
		if (res)
		{
			this.outVertexSets.remove(v);
			this.inVertexSets.remove(v);
			
			for (E e : edges)
			{
				this.outEdgeSets.remove(e);
				this.inEdgeSets.remove(e);
			}
		}
		
		return res;
	}
	
	
	protected void addLookup(V sourceVertex, V targetVertex, E edge)
	{
		//edge lookups
		if (!this.outEdgeSets.containsKey(sourceVertex))
		{
			this.outEdgeSets.put(sourceVertex, new HashSet<E>());
		}
		this.outEdgeSets.get(sourceVertex).add(edge);
		
		if (!this.inEdgeSets.containsKey(targetVertex))
		{
			this.inEdgeSets.put(targetVertex, new HashSet<E>());
		}
		this.inEdgeSets.get(targetVertex).add(edge);
		

		//vertex lookups
		if (!this.outVertexSets.containsKey(sourceVertex))
		{
			this.outVertexSets.put(sourceVertex, new HashSet<V>());
		}
		this.outVertexSets.get(sourceVertex).add(targetVertex);
		
		if (!this.inVertexSets.containsKey(targetVertex))
		{
			this.inVertexSets.put(targetVertex, new HashSet<V>());
		}
		this.inVertexSets.get(targetVertex).add(sourceVertex);
	}
}
