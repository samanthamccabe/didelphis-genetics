package org.didelphis.genetics.alignment.operators.gap;

import org.didelphis.common.language.phonetic.model.interfaces.FeatureSpecification;
import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.common.language.phonetic.sequences.Sequence;

import java.util.List;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 6/3/2015
 */
public abstract class AbstractGapPenalty implements GapPenalty {

	private final Segment<Double> gap;

	protected AbstractGapPenalty(Segment<Double> gap) {
		this.gap = gap;
	}

	@Override
	public FeatureSpecification getSpecification() {
		return gap.getSpecification();
	}

	@Override
	public Segment<Double> getGapSegment() {
		return gap;
	}

	protected int countGaps(List<Sequence<Double>> sequences) {

		int size = sequences.size();
		Sequence<Double> sequence = sequences.get(size - 1);
		if (!sequence.isEmpty()) {
			Segment<Double> tail = sequence.get(0);
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
