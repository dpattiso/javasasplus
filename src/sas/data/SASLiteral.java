package sas.data;

import java.util.List;

import sas.util.PDDLConvertable;

import javaff.data.strips.Proposition;
import javaff.data.strips.SingleLiteral;

/**
 * A fact in SAS+ form.
 * @author David Pattison
 *
 */
public interface SASLiteral extends PDDLConvertable<Proposition>, Comparable<SASLiteral>
{
	/**
	 * Get the predicate symbol of this literal.
	 * @return
	 */
	public SASPredicateSymbol getPredicateSymbol();

	/**
	 * Set the predicate symbol of this literal.
	 * @param n
	 */
	public void setPredicateSymbol(SASPredicateSymbol n);

	/**
	 * Set the parameters of this literal.
	 * @return
	 */
	public List<SASParameter> getParameters();

	/**
	 * Get the parameters of this literal.
	 * @param params
	 */
	public void setParameters(List<SASParameter> params);
	
	/**
	 * Clone this literal.
	 * @return
	 */
	public Object clone();
	
	@Override
	public int hashCode();
	
	@Override
	public boolean equals(Object obj);

	/**
	 * Gets the variable ID which this SASLiteral is a value of.
	 * @return
	 */
	public int getVariableId();
	
	/**
	 * Gets the value associated with this literal within the variable.
	 * @return
	 */
	public int getValueId();
	
	/**
	 * Sets the variable ID which this SASLiteral is a value of.
	 * @return
	 */
	public void setVariableId(int var);
	
	/**
	 * Sets the value associated with this literal within the variable.
	 * @return
	 */
	public void setValueId(int value);
}
