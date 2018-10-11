package sas.data;

/**
 * Represents a variable/object/parameter which has been created during SAS+ translation.
 * @author David Pattison
 *
 */
public class SASDomainObject extends SASParameter
{
	private final int objectID; 
	
	public SASDomainObject(int objectID)
	{
		super("domainvar" + objectID);	
		
		this.objectID = objectID;
	}
	
	@Override
	public Object clone()
	{
		return new SASDomainObject(this.getObjectID());
	}
	
	public int getObjectID()
	{
		return objectID;
	}
	
	@Override
	public int hashCode()
	{
		return super.hashCode() ^ this.getObjectID() ^ 31;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
			return false;
		
		return super.equals(obj) && this.getObjectID() == ((SASDomainObject)obj).getObjectID();
	}
	
}
