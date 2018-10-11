package sas.data;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Directed edge in a DTG. An edge is associated with a variable (As it is in a DTG which is itself associated
 * with a variable). Therefore, the edge start node is {@link SASLiteral} precondition of the variable, 
 * while the edge represents a {@link SASAction}, and the end of the edge is the effect/postcondition of the variable.
 * However, the action this edge encapsulates may have preconditions related to other variables, which are
 * captured as "associated" preconditions. 
 * @author David Pattison
 *
 */
public class DTGActionEdge
{
	private double weight;
	private SASLiteral pc, effect;
	private SortedSet<SASLiteral> associatedPcs;
	private SASAction action;
	
	protected int hash;
	
	public DTGActionEdge(SASLiteral pc, SASAction action, SASLiteral effect)
	{
		this.weight = 1; //TODO non uniform costs -- delegate to Action?
		this.pc = pc;
		this.associatedPcs = new TreeSet<SASLiteral>();
		this.action = action;
		this.effect = effect;
		
		this.updateHash();
	}
	
	public DTGActionEdge(SASLiteral pc, SASAction action, SASLiteral effect, Collection<SASLiteral> associatedPCs)
	{
		this(pc, action, effect);
		this.associatedPcs = new TreeSet<SASLiteral>(associatedPCs);
		
		this.updateHash();
	}
	
	protected int updateHash()
	{
		this.hash = this.pc.hashCode() ^ 
				this.action.hashCode() ^ 
				this.effect.hashCode() ^ 
				this.associatedPcs.hashCode() ^ 
				((Double)this.getWeight()).hashCode() ^
				31;
		return this.hash;
	}
	
	public Object clone()
	{
		DTGActionEdge clone = new DTGActionEdge((SASLiteral) pc.clone(), (SASAction) action.clone(), (SASLiteral) effect.clone());
		clone.weight = this.weight;
		clone.associatedPcs.clear();
		for (SASLiteral l : this.associatedPcs)
		{
			clone.associatedPcs.add((SASLiteral) l.clone());
		}
		this.updateHash();
		clone.updateHash();
				
		assert(this.hashCode() == clone.hashCode());
		return clone;
	}
	
	public boolean isApplicable(SASState state)
	{
		if ((state.vars.get(this.pc.getVariableId()).getCurrentIndex() == this.pc.getValueId()) == false)
			return false;
		
		for (SASLiteral apc : this.associatedPcs)
		{
			if ((state.vars.get(apc.getVariableId()).getCurrentIndex() == apc.getValueId()) == false)
				return false;
		}
		
		return true;
	}
	
	/**
	 * Gets an ordered set of all preconditions associated with the action this edge encapsulates.
	 * Note that this is the precondition associated with this edge's variable, and all other preconditions
	 * on other variables.
	 * @see {@link #getAssociatedPcs()} - The preconditions of the action which are not related to this edge's variable.
	 * @see {@link #getPc()} - The precondition of this edge's {@link SASVariable}.
	 * 
	 * @return
	 */
	public SortedSet<SASLiteral> getAllPreconditions()
	{
		TreeSet<SASLiteral> set = new TreeSet<SASLiteral>(this.associatedPcs);
		set.add(this.pc);
		
		return Collections.unmodifiableSortedSet(set);
	}
	
	public void addAssociatedPrecondition(SASLiteral pc)
	{
		this.associatedPcs.add(pc);
		this.updateHash();
	}
	
	public boolean removeAssociatedPrecondition(SASLiteral pc)
	{
		boolean res = this.associatedPcs.remove(pc);
		this.updateHash();
		return res;
	}

	public SASLiteral getPc()
	{
		return pc;
	}

	public void setPc(SASLiteral pc)
	{
		this.pc = pc;
		this.updateHash();
	}

	public SASLiteral getEffect()
	{
		return effect;
	}

	public void setEffect(SASLiteral effect)
	{
		this.effect = effect;
		this.updateHash();
	}

	public SASAction getAction()
	{
		return action;
	}

	public void setAction(SASAction action)
	{
		this.action = action;
		this.updateHash();
	}
	
	@Override
	public int hashCode()
	{
		return this.hash;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		DTGActionEdge other = (DTGActionEdge) obj;
		return this.getWeight() == other.getWeight() && 
				this.pc.equals(other.pc) && 
				this.action.equals(other.action) && 
				this.effect.equals(other.effect) && 
				this.associatedPcs.equals(other.associatedPcs);
	}
	
	@Override
	public String toString()
	{
//		return this.pc.toString() + " -> "+this.action.toString()+ " -> "+this.effect.toString();
		return this.action.toString();
	}

	/**
	 * Get all preconditions which are NOT associated with this edge's main {@link SASVariable}.
	 * These are returned in a sorted order, based up the natural ordering of {@link SASLiteral}.
	 * The returned set is not modifiable.
	 * @return
	 * @see #addAssociatedPrecondition(SASLiteral)
	 * @see #setAssociatedPcs(SortedSet) 
	 *
	 */
	public SortedSet<SASLiteral> getAssociatedPcs()
	{
		return Collections.unmodifiableSortedSet(associatedPcs);
	}

	public void setAssociatedPcs(SortedSet<SASLiteral> associatedPcs)
	{
		this.associatedPcs = associatedPcs;
	}

	public double getWeight()
	{
		return weight;
	}

	public void setWeight(double weight)
	{
		this.weight = weight;
		this.updateHash();
	}
}
