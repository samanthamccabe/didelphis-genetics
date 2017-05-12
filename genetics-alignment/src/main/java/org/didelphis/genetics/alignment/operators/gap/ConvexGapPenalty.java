package org.didelphis.genetics.alignment.operators.gap;

import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.common.language.phonetic.sequences.Sequence;
import org.didelphis.genetics.alignment.Alignment;

import java.util.List;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 6/4/15
 */
public class ConvexGapPenalty extends AbstractGapPenalty {

	private final double openPenalty;
	private final double extensionCoefficient;

	public ConvexGapPenalty(Segment<Double> gap, double a, double b) {
		super(gap);
		openPenalty = a;
		extensionCoefficient = b;
	}

	@Override
	public double evaluate(Alignment<Double> alignment) {
		int length = 0;

		for (List<Sequence<Double>> sequences : alignment) {
			if (!sequences.isEmpty()) {
				length += countGaps(sequences);
			}
		}

		if (length == 0) {
			return 0.0;
		} else if (length == 1) {
			return openPenalty;
		} else {
			return extensionCoefficient * Math.log((double) length);
		}
	}
}
