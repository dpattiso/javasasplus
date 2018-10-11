//package sas.data;
//
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.Set;
//
//import javaff.data.Fact;
//import javaff.data.GroundFact;
//import javaff.data.Literal;
//import javaff.data.Parameter;
//import javaff.data.strips.PredicateSymbol;
//import javaff.data.strips.Proposition;
//import javaff.data.strips.SingleLiteral;
//
//public class SASDerivedPredicate extends Literal implements MultiValuedVariable<Proposition>, PDDLConvertable<Proposition>
//{
//	private int varId;
//	private int axiomLayer;
//
//	public SASDerivedPredicate(int varId)
//	{
//		super();
//		super.setPredicateSymbol(new SASDerivedPredicateSymbol());
//		this.varId = varId;
//	}
//	
//	public SASDerivedPredicate(int varId, int axiomLayer)
//	{
//		this(varId);
//		this.axiomLayer = axiomLayer;
//	}
//	
//	public Proposition getValue(Object ... parameters)
//	{
//		return null;
//	}
//	
//	@Override
//	public String toString()
//	{
//		StringBuffer s =  new StringBuffer(this.name.toString()+varId+"(");
//		for (Parameter p : super.parameters)
//		{
//			s.append(p.toString()+" ");
//		}
//		s.append(")");
//		
//		return s.toString();
//	}
//
//	@Override
//	public Proposition convertToPDDL(Object... parameters)
//	{
//		Proposition p = new Proposition(this.name);
//		p.addParameters(super.parameters);
//		
//		return p;
//	}
//
//	@Override
//	public Object clone()
//	{
//		SASDerivedPredicate clone = new SASDerivedPredicate(this.varId);
//		clone.axiomLayer = this.axiomLayer;
//		return clone;
//	}
//
//	@Override
//	public Set<? extends Fact> getFacts()
//	{
//		Set<Fact> s = new HashSet<Fact>();
//		s.add(this);
//		return s;
//	}
//}

package sas.data;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import sas.util.SASException;

import javaff.data.GroundProblem;
import javaff.data.strips.Proposition;
import javaff.data.strips.SingleLiteral;

public class SASDerivedPredicate extends SASVariable
{
	private int axiomLayer;
	private SASDerivedPredicateSymbol symbol;
	private int defaultValue;
	private List<SASParameter> params;
	
	//cached for speedup
	private int hash;
	private boolean setup = false; //flag used for indicating that the hash can be computed
	//this is needed because the super constructor calls updateHash(), which is overridden in this class
	//and relies on all variables having been setup prior to calling. This should only be set
	//once, in the constructor. //TODO this is a truly terrible way to do this
	
	protected SASDerivedPredicate(int varId, int zeroId, int valueCount, String derivedPredicateName)
	{
		super(varId, zeroId, valueCount);
		super.setObject(new SASParameter(derivedPredicateName));
		this.symbol = new SASDerivedPredicateSymbol(derivedPredicateName);
		this.defaultValue = -1;
		this.params = new ArrayList<SASParameter>();
		this.axiomLayer = -1;
		
//		super.setObject(new SASParameter(this.toString()));
		super.setObject(new SASParameter(this.symbol.getName()));
		
		this.setup = true;
		
		this.updateHash();
	}
	
	public SASDerivedPredicate(int varId, int zeroId, int valueCount, int axiomLayer, String derivedPredicateName)
	{
		this(varId, zeroId, valueCount, derivedPredicateName);
		this.axiomLayer = axiomLayer;
		this.updateHash();
	}
	
	public SASDerivedPredicate(int varId, int zeroId, int valueCount, int axiomLayer, int defaultValue, String derivedPredicateName)
	{
		this(varId, zeroId, valueCount, derivedPredicateName);
		this.defaultValue = defaultValue;
		this.axiomLayer = axiomLayer;
		super.setCurrentValue(this.defaultValue);
		
		this.updateHash();
	}

	/**
	 * Sets this derived predicate's current value to its default value. Used in evaluating axioms.
	 */
	public void reset()
	{
//		super.setCurrentValue(1);
		super.setCurrentValue(this.defaultValue);
		
		this.updateHash();
	}
	
	@Override
	public Object clone()
	{
		SASDerivedPredicate clone = new SASDerivedPredicate(this.getId(), this.getZeroId(), super.getValues().size(), this.axiomLayer, this. defaultValue, new String(this.symbol.getName()));
				
		clone.setObject((SASParameter) this.getObject().clone());
		clone.setDomain((SASDomain) this.getDomain().clone());
		
		clone.setDefaultValue(this.getDefaultValue());
		clone.setCurrentValue(this.getCurrentIndex());
		clone.axiomLayer = this.axiomLayer;
		clone.symbol = (SASDerivedPredicateSymbol) this.symbol.clone();
		
		clone.updateHash();
		
		assert(this.hashCode() == clone.hashCode());
		
		return clone;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof SASDerivedPredicate == false)
			return false;
		
		if (super.equals(obj) == false)
			return false;
		SASDerivedPredicate p = (SASDerivedPredicate) obj;
		
		return this.axiomLayer == p.axiomLayer && this.symbol.equals(p.symbol);
	}
	
	@Override
	protected int updateHash()
	{
		if (this.setup)
		{
			this.hash = super.updateHash() ^ 
					this.axiomLayer ^ 
					this.defaultValue ^ 
					this.symbol.hashCode() ^ 
					this.params.hashCode() ^ 
					31;
		}
		return this.hash;
	}
	
	@Override
	public int hashCode()
	{
		return this.hash;
	}
	
	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer("["+super.getId()+"] - "+this.symbol.toString());
		if (this.params.isEmpty() == false)
		{
			buf.append("(");
			for (SASParameter p : this.params)
			{
				buf.append(p.toString()+", ");
				
			}
			buf.deleteCharAt(buf.length()-1);
			buf.deleteCharAt(buf.length()-1);
			buf.append(")");
		}
		
		buf.append(" - ID- "+super.getId());
		
		for (int i = 0; i < super.getValues().size(); i++)
		{
			buf.append("\n\t");
			if (i == super.getCurrentIndex())
				buf.append("*");
			
			buf.append(super.getValues().get(i).toString());
		}
		buf.append("\n");
		
		return buf.toString();
	}

	/**
	 * Derived Predicates do not translate literally into PDDL objects. Rather they are stubs
	 * which represent whether a set of axioms is true. Therefore, this method simply returns
	 * a stub proposition.
	 */
	@Override
	public Proposition convertToPDDL(SASProblem sproblem)
	{
		Proposition p = new Proposition(this.symbol.convertToPDDL(sproblem));
		for (SASParameter l : this.params)
		{
			p.addParameter(l.convertToPDDL(sproblem));
		}
		
		return p;
	}
	
	@Override
	public Proposition convertToPDDL(SASProblem sproblem, GroundProblem pddlProblem)
	{
		Proposition p = new Proposition(this.symbol.convertToPDDL(sproblem, pddlProblem));
		for (SASParameter l : this.params)
		{
			p.addParameter(l.convertToPDDL(sproblem, pddlProblem));
		}
		
		return p;
	}
	

	
	public void addParameter(SASParameter p)
	{
		this.params.add(p);
		
		this.updateHash();
	}


	public int getDefaultValue()
	{
		return defaultValue;
	}

	public void setDefaultValue(int defaultValue)
	{
		this.defaultValue = defaultValue;
		
		this.updateHash();
	}

	public int getAxiomLayer()
	{
		return this.axiomLayer;
	}
	
	public void setAxiomLayer(int layer)
	{
		this.axiomLayer = layer;
		
		this.updateHash();
	}
}
