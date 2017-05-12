package org.didelphis.genetics.alignment.operators.gap;


import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.genetics.alignment.Alignment;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 6/3/2015
 */
public class ConstantGapPenalty extends AbstractGapPenalty {

	private final double penalty;

	public ConstantGapPenalty(Segment<Double> gap, double penalty) {
		super(gap);
		this.penalty = penalty;
	}

	@Override
	public double evaluate(Alignment<Double> alignment) {
		return penalty;
	}
}
