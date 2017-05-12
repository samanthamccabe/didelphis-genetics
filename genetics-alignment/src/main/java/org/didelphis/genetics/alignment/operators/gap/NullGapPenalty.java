package org.didelphis.genetics.alignment.operators.gap;

import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.genetics.alignment.Alignment;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 6/3/2015
 */
public class NullGapPenalty extends AbstractGapPenalty {

	public NullGapPenalty(Segment<Double> gap) {
		super(gap);
	}

	@Override
	public double evaluate(Alignment<Double> alignment) {
		return 0.0;
	}
}
