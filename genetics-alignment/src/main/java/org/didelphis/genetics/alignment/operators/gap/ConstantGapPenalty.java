package org.didelphis.genetics.alignment.operators.gap;


import org.didelphis.language.phonetic.sequences.Sequence;
import org.jetbrains.annotations.NotNull;

/**
 * @author Samantha Fiona McCabe
 * Created: 6/3/2015
 */
public class ConstantGapPenalty<T> extends AbstractGapPenalty<T> {

	private final double penalty;

	public ConstantGapPenalty(@NotNull Sequence<T> gap, double penalty) {
		super(gap);
		this.penalty = penalty;
	}

	@Override
	public double evaluate(int currentGapLength) {
		return penalty;
	}
}
