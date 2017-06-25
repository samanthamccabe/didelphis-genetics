package org.didelphis.genetics.alignment.algorithm;

import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;

/**
 * Class AbstractAlignmentAlgorithm
 *
 * @since 06/05/2017
 */
public abstract class AbstractAlignmentAlgorithm<N>
		implements AlignmentAlgorithm<N> {

	private final Comparator<N, Double> comparator;
	private final GapPenalty<N> gapPenalty;
	private final SequenceFactory<N> factory;

	protected AbstractAlignmentAlgorithm(Comparator<N, Double> comparator,
			GapPenalty<N> gapPenalty, SequenceFactory<N> factory) {
		this.comparator = comparator;
		this.gapPenalty = gapPenalty;
		this.factory = factory;
	}

	protected GapPenalty<N> getGapPenalty() {
		return gapPenalty;
	}

	protected SequenceFactory<N> getFactory() {
		return factory;
	}

	protected Comparator<N, Double> getComparator() {
		return comparator;
	}
}
