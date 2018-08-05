package org.didelphis.genetics.alignment.algorithm;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.language.phonetic.SequenceFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Class AbstractAlignmentAlgorithm
 *
 * @since 06/05/2017
 */
@ToString
@EqualsAndHashCode
public abstract class AbstractAlignmentAlgorithm<N>
		implements AlignmentAlgorithm<N> {

	private final SequenceComparator<N> comparator;
	private final Optimization<Double> optimization;
	private final GapPenalty<N> gapPenalty;
	private final SequenceFactory<N> factory;

	protected AbstractAlignmentAlgorithm(
			Optimization<Double> optimization,
			SequenceComparator<N> comparator,
			GapPenalty<N> gapPenalty,
			SequenceFactory<N> factory
	) {
		this.comparator = comparator;
		this.optimization = optimization;
		this.gapPenalty = gapPenalty;
		this.factory = factory;
	}

	@Override
	public @NotNull GapPenalty<N> getGapPenalty() {
		return gapPenalty;
	}

	@Override
	public @NotNull SequenceFactory<N> getFactory() {
		return factory;
	}

	@Override
	public @NotNull SequenceComparator<N> getComparator() {
		return comparator;
	}

	@Override
	public @NotNull Optimization<Double> getOptimization() {
		return optimization;
	}
}
