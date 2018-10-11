package sas.util;

import javaff.data.GroundProblem;
import javaff.data.PDDLPrintable;
import sas.data.SASProblem;


/**
 * This interface provides a convenient means of getting the PDDL equivalent representation of the
 * implementing SAS object.
 * @author David Pattison
 *
 * @param <T>
 */
public interface PDDLConvertable<T>
{
	/**
	 * Convert the implementing object into a PDDL representation, using the specified PDDL ground
	 * problem as a lookup for the equivalent type (if such an equivalent exists). This prevents
	 * hashCode() lookups failing to find equality between the SAS+ and PDDL representations.
	 * @param sproblem
	 * @return
	 */
	T convertToPDDL(SASProblem sproblem, GroundProblem gp);
	
	/**
	 * Convert the implementing object into a PDDL representation.
	 * @param sproblem
	 * @return
	 */
	T convertToPDDL(SASProblem sproblem);
}
