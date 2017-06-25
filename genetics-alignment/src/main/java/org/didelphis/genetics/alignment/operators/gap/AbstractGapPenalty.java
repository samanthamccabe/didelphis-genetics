package org.didelphis.genetics.alignment.operators.gap;

import org.didelphis.language.phonetic.model.FeatureSpecification;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;

import java.util.List;

/**
 * @author Samantha Fiona McCabe
 * Created: 6/3/2015
 */
public abstract class AbstractGapPenalty<N>
		implements GapPenalty<N> {

	private final Sequence<N> gap;

	protected AbstractGapPenalty(Sequence<N> gap) {
		this.gap = gap;
	}

	@Override
	public FeatureSpecification getSpecification() {
		return gap.getSpecification();
	}

	@Override
	public Sequence<N> getGap() {
		return gap;
	}

	protected int countGaps(List<Sequence<N>> sequences) {

		int size = sequences.size();
		Sequence<N> sequence = sequences.get(size - 1);
		if (!sequence.isEmpty()) {
			Segment<N> tail = sequence.get(0);
			if (tail.equals(gap)) {
				int length = 1;
				for (int i = size - 2; i >= 0; i--) {
					if (sequences.get(i).get(0).equals(gap)) {
						length++;
					} else {
						i = -1;
					}
				}
				return length;
			} else {
				return 0;
			}
		}
		return 0;
	}
}
