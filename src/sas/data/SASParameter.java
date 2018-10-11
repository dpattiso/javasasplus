package sas.data;

import sas.util.PDDLConvertable;
import javaff.data.GroundProblem;
import javaff.data.Parameter;
import javaff.data.Type;
import javaff.data.strips.PDDLObject;

/**
 * SAS+ parameters are just plain text -- at least they are if they have gone through the translator.
 * @author David Pattison
 *
 */
public class SASParameter implements PDDLConvertable<Parameter>
{
	private String name;
	
	//cached for massive speed improvements
	private int hash;
	
	public SASParameter(String name)
	{
		this.name = name;
		
		this.updateHash();
	}
	
	/**
	 * Updates the cached hash code, rather than computing it every time.
	 */
	protected int updateHash()
	{
		this.hash = this.name.hashCode() ^ 31;
		return this.hash;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
		
		this.updateHash();
	}
	
	public Object clone()
	{
		return new SASParameter(this.name);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return this.name.equals(((SASParameter) obj).name);
	}
	
	@Override
	public int hashCode()
	{
		return this.hash;
	}
	
	@Override
	public String toString()
	{
		return name;
	}

	/**
	 * This method just returns a new PDDLObject with the name of the this parameter. As SAS+ has no type 
	 * information in it once its been through the translator, the object will be of type {@link Type#rootType}.
	 */
	@Override
	public Parameter convertToPDDL(SASProblem sproblem)
	{
		return new PDDLObject(this.name);
	}
	
	/**
	 * Returns a reference to the PDDL object in the GroundProblem if one exists.
	 * @throws NullPointerException if no object with the same name exists.
	 */
	@Override
	public Parameter convertToPDDL(SASProblem sproblem, GroundProblem pddlProblem)
	{
		for (Parameter o : pddlProblem.getObjects())
		{
			if (o.toString().equals(this.name))
			{
				return o;
			}
		}
		
		throw new NullPointerException("Cannot find object "+this.name+" in PDDL representation");
	}
}
