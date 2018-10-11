package sas.search;

import java.util.Collection;

import javaff.search.UnreachableGoalException;
import sas.data.SASLiteral;
import sas.data.SASState;

/**
 * A heuristic in the SAS+ framework.
 * @author David Pattison
 *
 */
public interface SASHeuristic
{
	/**
	 * The value which is considered as "Unreachable" in SAS+ heuristic estimates.
	 */
	public static double Unreachable = Double.MAX_VALUE;
	
	/**
	 * Get the heuristic estimate from the initial state to the specified goals.
	 * @param initial The initial state.
	 * @param goals The goals.
	 * @return The estimate to the goals.
	 * @throws UnreachableGoalException Thrown if any of the goals are unreachable.
	 */
	public double getEstimate(SASState initial, Collection<SASLiteral> goals) throws UnreachableGoalException;

}
