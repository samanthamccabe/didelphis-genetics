package org.didelphis.genetics.alignment.operators.comparators;

import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.maps.SymmetricalTwoKeyMap;
import org.jetbrains.annotations.NotNull;

/**
 * Class {@code MatrixComparator}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0 Date: 2017-07-04
 */
public class BrownEtAlComparator<E> implements Comparator<E> {

	private final SymmetricalTwoKeyMap<Segment<E>, Double> map;

	public BrownEtAlComparator(SymmetricalTwoKeyMap<Segment<E>, Double> map) {
		this.map = map;
	}

	@Override
	public double apply(@NotNull Sequence<E> left, @NotNull Sequence<E> right,
			int i, int j) {
		Segment<E> sL = left.get(i);
		Segment<E> sR = right.get(j);
		if (sL.equals(sR)) {
			return 0;
		}
		Double aDouble = map.get(sL, sR);
		return aDouble == null ? 100 : 100-aDouble;
	}
}
