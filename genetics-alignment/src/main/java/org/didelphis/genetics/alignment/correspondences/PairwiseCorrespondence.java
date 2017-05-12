package org.didelphis.genetics.alignment.correspondences;

import org.didelphis.common.language.phonetic.segments.Segment;

import java.util.ArrayList;
import java.util.List;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 4/10/2016
 */
public class PairwiseCorrespondence<N extends Number> {

	private final Segment<N> leftSegment;
	private final Segment<N> rightSegment;

	private final List<Context> leftContexts;
	private final List<Context> rightContexts;

	public PairwiseCorrespondence(Segment<N> leftSegment,
			Segment<N> rightSegment) {
		this.leftSegment = leftSegment;
		this.rightSegment = rightSegment;

		leftContexts = new ArrayList<>();
		rightContexts = new ArrayList<>();
	}

	public void addConext(List<Context> leftContext,
			List<Context> rightContext) {
	}


}
