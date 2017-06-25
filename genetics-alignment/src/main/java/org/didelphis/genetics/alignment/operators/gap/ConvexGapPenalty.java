package org.didelphis.genetics.alignment.operators.gap;

import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.genetics.alignment.Alignment;

import java.util.List;

/**
 * @author Samantha Fiona McCabe
 * Created: 6/4/15
 */
public class ConvexGapPenalty<N> extends AbstractGapPenalty<N> {

	private final double openPenalty;
	private final double extensionCoefficient;

	public ConvexGapPenalty(Sequence<N> gap, double a, double b) {
		super(gap);
		openPenalty = a;
		extensionCoefficient = b;
	}

	@Override
	public double evaluate(Alignment<N> alignment) {
		int length = 0;

//		TODO:
//		for (List<Sequence<N>> sequences : alignment) {
//			if (!sequences.isEmpty()) {
//				length += countGaps(sequences);
//			}
//		}

		if (length == 0) {
			return 0.0;
		} else if (length == 1) {
			return openPenalty;
		} else {
			//noinspection NonReproducibleMathCall
			return extensionCoefficient * Math.log(length);
		}
	}
}
