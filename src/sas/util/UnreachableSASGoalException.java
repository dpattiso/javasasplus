package sas.util;

import java.util.Collection;
import java.util.HashSet;

import sas.data.SASLiteral;

import javaff.data.Fact;
import javaff.search.UnreachableGoalException;

/**
 * Specialisation of {@link UnreachableGoalException} for SAS+ problems. Do not use
 * {@link UnreachableGoalException#getUnreachables()}, as it always returns an empty set - use
 * {@link #getSASUnreachables()} instead.
 * @author David Pattison
 *
 */
public class UnreachableSASGoalException extends UnreachableGoalException
{
	private Collection<SASLiteral> unreachable;

	public UnreachableSASGoalException()
	{
		super(new HashSet<Fact>());
		this.unreachable = new HashSet<SASLiteral>();
	}

	public UnreachableSASGoalException(Collection<SASLiteral> unreachables)
	{
		this();
		this.unreachable = unreachables;
	}

	public UnreachableSASGoalException(SASLiteral unreachable)
	{
		this();
		this.unreachable.add(unreachable);
	}

	public UnreachableSASGoalException(SASLiteral unreachable, String message)
	{
		super(new HashSet<Fact>(), message);
		this.unreachable = new HashSet<SASLiteral>();
		this.unreachable.add(unreachable);
	}

	public UnreachableSASGoalException(Collection<SASLiteral> unreachables,
			String message)
	{
		super(new HashSet<Fact>(), message);
	}

	public UnreachableSASGoalException(Throwable cause)
	{
		super(cause);
	}

	public UnreachableSASGoalException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public Collection<SASLiteral> getSASUnreachables()
	{
		return this.unreachable;
	}

}
