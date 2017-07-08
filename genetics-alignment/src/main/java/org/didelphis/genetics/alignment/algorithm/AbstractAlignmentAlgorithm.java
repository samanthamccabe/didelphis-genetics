package org.didelphis.genetics.alignment.algorithm;

import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.jetbrains.annotations.NotNull;

/**
 * Class AbstractAlignmentAlgorithm
 *
 * @since 06/05/2017
 */
public abstract class AbstractAlignmentAlgorithm<N>
		implements AlignmentAlgorithm<N> {

	private final Comparator<N> comparator;
	private final Optimization optimization;
	private final GapPenalty<N> gapPenalty;
	private final SequenceFactory<N> factory;

	protected AbstractAlignmentAlgorithm(Comparator<N> comparator,
			Optimization optimization, GapPenalty<N> gapPenalty,
			SequenceFactory<N> factory) {
		this.comparator = comparator;
		this.optimization = optimization;
		this.gapPenalty = gapPenalty;
		this.factory = factory;
	}

	@NotNull
	@Override
	public GapPenalty<N> getGapPenalty() {
		return gapPenalty;
	}

	@NotNull
	@Override
	public SequenceFactory<N> getFactory() {
		return factory;
	}

	@NotNull
	@Override
	public Comparator<N> getComparator() {
		return comparator;
	}

	@Override
	public Optimization getOptimization() {
		return optimization;
	}
}
