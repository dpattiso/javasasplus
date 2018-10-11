package sas.util;

public class UnsolveableProblemException extends Exception
{

	public UnsolveableProblemException()
	{
	}

	public UnsolveableProblemException(String message)
	{
		super(message);
	}

	public UnsolveableProblemException(Throwable cause)
	{
		super(cause);
	}

	public UnsolveableProblemException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
