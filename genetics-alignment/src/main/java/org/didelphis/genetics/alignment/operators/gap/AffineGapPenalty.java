package org.didelphis.genetics.alignment.operators.gap;


import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.genetics.alignment.Alignment;

import java.util.List;

/**
 * @author Samantha Fiona McCabe
 * Created: 6/3/15
 */
public class AffineGapPenalty<N> extends AbstractGapPenalty<N> {

	private final double openPenalty;
	private final double extensionCoefficient;

	public AffineGapPenalty(Sequence<N> gap, double a, double b) {
		super(gap);
		openPenalty = a;
		extensionCoefficient = b;
	}

	@Override
	public double evaluate(Alignment<N> alignment) {
		int length = 0;
		// TODO:
//		for (List<Sequence<N>> sequences : alignment) {
//			length += countGaps(sequences);
//		}
		return length == 1 ? openPenalty : extensionCoefficient * length;
	}
}
