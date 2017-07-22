package org.didelphis.genetics.alignment.correspondences;

import org.didelphis.structures.tuples.Twin;

/**
 * @author Samantha Fiona McCabe
 * Created: 4/15/2016
 */
public class ContextPair<T> extends Twin<Context<T>> {

	public ContextPair(Context<T> left, Context<T> right) {
		super(left, right);
	}
}
