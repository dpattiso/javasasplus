package sas.data;

public class DTGState
{
	public int sasVariable, sasValue;
	public SASLiteral literal;
	
	public DTGState(int sasVariable, int sasValue, SASLiteral literal)
	{
		this.sasVariable = sasVariable;
		this.sasValue = sasValue;
		this.literal = literal;
	}
}
