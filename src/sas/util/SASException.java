package sas.util;

public class SASException extends Exception
{
	public SASException()
	{
		super();
	}

	public SASException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public SASException(String message)
	{
		super(message);
	}

	public SASException(Throwable cause)
	{
		super(cause);
	}
}
