package org.didelphis.genetics.alignment.correspondences;

import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tuples.Twin;

/**
 * @author Samantha Fiona McCabe
 * Created: 4/10/2016
 */
public class Context<T> extends Twin<Sequence<T>> {

	public Context(Sequence<T> ante, Sequence<T> post) {
		super(ante, post);
	}
}
