package org.didelphis.genetics.alignment.operators.gap;

import org.didelphis.language.phonetic.sequences.Sequence;
import org.jetbrains.annotations.NotNull;

/**
 * @author Samantha Fiona McCabe
 * Created: 6/4/15
 */
public class ConvexGapPenalty<T> extends AbstractGapPenalty<T> {

	private final double openPenalty;
	private final double extensionPenalty;

	public ConvexGapPenalty(@NotNull Sequence<T> gap, double openPenalty, double extensionPenalty) {
		super(gap);
		this.openPenalty = openPenalty;
		this.extensionPenalty = extensionPenalty;
	}

	@Override
	public double evaluate(int currentGapLength) {
		return currentGapLength==0 ? openPenalty : extensionPenalty;
	}
}
