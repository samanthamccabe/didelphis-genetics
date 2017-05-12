package org.didelphis.genetics.alignment.operators.gap;


import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.common.language.phonetic.sequences.Sequence;
import org.didelphis.genetics.alignment.Alignment;

import java.util.List;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 6/3/15
 */
public class AffineGapPenalty extends AbstractGapPenalty {

	private final double openPenalty;
	private final double extensionCoefficient;

	public AffineGapPenalty(Segment<Double> gap, double a, double b) {
		super(gap);
		openPenalty = a;
		extensionCoefficient = b;
	}

	@Override
	public double evaluate(Alignment<Double> alignment) {
		int length = 0;
		for (List<Sequence<Double>> sequences : alignment) {
			length += countGaps(sequences);
		}
		return length == 1 ? openPenalty : extensionCoefficient * length;
	}
}
