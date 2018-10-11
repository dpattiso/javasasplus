package sas.data;

import java.util.HashSet;
import java.util.Set;

import sas.util.PDDLConvertable;

import javaff.data.Fact;
import javaff.data.GroundProblem;
import javaff.data.MutexSet;
import javaff.data.MutexSpace;
import javaff.data.strips.Proposition;
import javaff.data.strips.SingleLiteral;
import javaff.graph.FactMutex;

public class SASMutexGroup implements PDDLConvertable<MutexSet>
{
	private SASParameter object;
	private Set<SASLiteral> mutexes;
	
	public SASMutexGroup()
	{
		this.object = null;
		this.mutexes = new HashSet<SASLiteral>();
	}
	
	public SASMutexGroup(SASParameter v)
	{
		this();
		this.object = v;
	}
	
	public void addMutex(SASLiteral p)
	{
		this.mutexes.add(p);
	}
	
	/**
	 * Stub method which forwards to convertToPDDL().
	 */
	@Override
	public MutexSet convertToPDDL(SASProblem sproblem)
	{
		HashSet<SingleLiteral> mutexes = new HashSet<SingleLiteral>();
		for (SASLiteral l : this.mutexes)
		{
			mutexes.add(l.convertToPDDL(sproblem));
		}

		MutexSet pddl = new MutexSet();
		for (SingleLiteral l : mutexes)
		{
			pddl.addFact(l);
		}
		
		return pddl;
	}
	
	/**
	 * Stub method which forwards to convertToPDDL().
	 */
	@Override
	public MutexSet convertToPDDL(SASProblem sproblem, GroundProblem pddlproblem)
	{
		HashSet<SingleLiteral> mutexes = new HashSet<SingleLiteral>();
		for (SASLiteral l : this.mutexes)
		{
			Proposition p = l.convertToPDDL(sproblem, pddlproblem);
			if (p != null)
				mutexes.add(p);
		}

		MutexSet pddl = new MutexSet();
		for (SingleLiteral l : mutexes)
		{
			pddl.addFact(l);
		}
		
		return pddl;
	}

	public SASParameter getObject()
	{
		return this.object;
	}

	public void setObject(SASParameter variable)
	{
		this.object = variable;
	}

	public Set<SASLiteral> getMutexes()
	{
		return mutexes;
	}

	public void setMutexes(Set<SASLiteral> mutexes)
	{
		this.mutexes = mutexes;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		SASMutexGroup other = (SASMutexGroup) obj;
		if (this.object.equals(other.object) == false)
			return false;
		return this.mutexes.equals(other.mutexes);
	}
	
	public Object clone()
	{
		SASMutexGroup clone = new SASMutexGroup((SASParameter) this.object.clone());
		clone.mutexes = new HashSet<SASLiteral>();
		for (SASLiteral p : this.mutexes)
		{
			clone.mutexes.add((SASLiteral) p.clone());
		}
		return clone;
	}
	
	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer(this.object.toString()+" - |"+this.mutexes.size()+"|\n");
		for (SASLiteral l : this.mutexes)
		{
			buf.append("\t"+l.toString());
		}
		buf.append("\n");
		return buf.toString();
		
	}

}
