package sas.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jws.Oneway;

import javaff.data.GroundProblem;
import javaff.data.strips.Proposition;
import sas.util.PDDLConvertable;

/**
 * The domain of a SAS variable.
 * @author David Pattison
 *
 */
public class SASDomain implements PDDLConvertable<List<Proposition>>, Iterable<SASLiteral>
{
	private int size;
	private List<SASLiteral> literals;
	
	//cached for speed improvements
	private int hash;
	
	public SASDomain(int domainSize)
	{
		this.size = domainSize;
		this.literals = new ArrayList<SASLiteral>(domainSize);
		
		this.intialise();
		
		this.updateHash();
	}
	
	@Override
	public Iterator<SASLiteral> iterator()
	{
		return this.literals.iterator();
	}
	
	protected int updateHash()
	{
		this.hash = this.literals.hashCode() ^ 31;
		return this.hash;
	}
	
	/**
	 * Sets all literals to a stub value. This is because the parser must directly access position x,
	 * but if there is nothing in x currently, an exception is thrown.
	 */
	private void intialise()
	{
		for (int i = 0; i < this.size; i++)
		{
			this.literals.add(new NoneOfThoseProposition());
		}
	}
	
	@Override
	public String toString()
	{
		return "D = {"+this.literals.toString()+"}";
	}

	public int size()
	{
		return this.size;
	}
	
	public SASLiteral get(int index)
	{
//		if (index < 0)
//			System.out.println("moose");
		return this.literals.get(index);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		SASDomain other = (SASDomain) obj;
		
		return this.literals.equals(other.literals);
	}
	
	@Override
	public int hashCode()
	{
		return this.hash;
	}
	
	@Override
	public Object clone()
	{
		SASDomain clone = new SASDomain(this.size());
		
		//should guarantee to iterate in-order
		int c = 0;
		for (SASLiteral l : this.literals)
		{
			clone.set(c++, (SASLiteral) l.clone());
		}
		
		return clone;
	}
	
	public int indexOf(SASLiteral l)
	{
		return this.literals.indexOf(l);
	}
	
	public boolean contains(SASLiteral l)
	{
		return this.literals.contains(l);
	}
	
	public boolean add(SASLiteral l)
	{
		boolean res = this.literals.add(l);
		this.updateHash();
		return res;
	}
	
	public SASLiteral set(int index, SASLiteral l)
	{
		SASLiteral s = this.literals.set(index, l);
		this.updateHash();
		return s;
	}
		
	
	

	@Override
	public List<Proposition> convertToPDDL(SASProblem sproblem, GroundProblem gp)
	{
		ArrayList<Proposition> props = new ArrayList<Proposition>();
		for (SASLiteral l : this.literals)
		{
			Proposition p = l.convertToPDDL(sproblem, gp);
			props.add(p);
		}
		
		return props;
	}

	@Override
	public List<Proposition> convertToPDDL(SASProblem sproblem)
	{
		ArrayList<Proposition> props = new ArrayList<Proposition>();
		for (SASLiteral l : this.literals)
		{
			Proposition p = l.convertToPDDL(sproblem);
			props.add(p);
		}
		
		return props;
	}
	
	public boolean remove(SASLiteral l)
	{
		return this.literals.remove(l);
	}
	
	public SASLiteral remove(int index)
	{
		return this.literals.remove(index);
	}

	/**
	 * Returns an unmodifiable list containing this domain's literals
	 * @return
	 */
	public List<SASLiteral> getLiterals()
	{
		return Collections.unmodifiableList(this.literals);
	}


	public void setLiterals(List<SASLiteral> literals)
	{
		if (literals.size() != this.size)
			throw new IllegalArgumentException("Number of literals must equal array length");
		
		this.literals = literals;
		
		this.updateHash();
	}
}
