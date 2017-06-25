package org.didelphis.genetics.alignment.operators.gap;


import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.language.phonetic.sequences.Sequence;

/**
 * @author Samantha Fiona McCabe
 * Created: 6/3/2015
 */
public class ConstantGapPenalty<N> extends AbstractGapPenalty<N> {

	private final double penalty;

	public ConstantGapPenalty(Sequence<N> gap, double penalty) {
		super(gap);
		this.penalty = penalty;
	}

	@Override
	public double evaluate(Alignment<N> alignment) {
		return penalty;
	}
}
