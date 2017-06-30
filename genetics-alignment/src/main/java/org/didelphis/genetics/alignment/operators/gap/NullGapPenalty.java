package org.didelphis.genetics.alignment.operators.gap;

import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.language.phonetic.sequences.Sequence;

/**
 * @author Samantha Fiona McCabe
 * Created: 6/3/2015
 */
public class NullGapPenalty<N> extends AbstractGapPenalty<N> {

	public NullGapPenalty(Sequence<N> gap) {
		super(gap);
	}

	@Override
	public double evaluate(Alignment<N> alignment) {
		return 0.0;
	}
}
