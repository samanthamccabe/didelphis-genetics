package org.didelphis.genetics.alignment.algorithm;

import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Class NeedlemanWunsch
 *
 * @since 06/05/2017
 */
public
class NeedlemanWunschAlgorithm<N>
		extends AbstractAlignmentAlgorithm<N> {

	public NeedlemanWunschAlgorithm(Comparator<N, Double> comparator,
			GapPenalty<N> gapPenalty, SequenceFactory<N> factory) {
		super(comparator, gapPenalty, factory);
	}

	@NotNull
	@Override
	public Alignment<N> getAlignment(@NotNull List<Sequence<N>> list) {
		return null;
	}
}
