package org.didelphis.genetics.alignment.operators.comparators;

import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.contracts.Streamable;
import org.didelphis.structures.maps.SymmetricalTwoKeyMap;
import org.didelphis.structures.tuples.Triple;
import org.jetbrains.annotations.NotNull;

/**
 * Class {@code MatrixComparator}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0 Date: 2017-07-04
 */
public class BrownEtAlComparator<T> implements Comparator<T> {

	private final SymmetricalTwoKeyMap<Segment<T>, Double> map;
	private final double max;

	public BrownEtAlComparator(
			Streamable<Triple<Segment<T>, Segment<T>, Double>> streamable
	) {
		map = new SymmetricalTwoKeyMap<>();
		max = streamable.stream()
				.map(Triple::getThirdElement)
				.max(Double::compare)
				.orElse(100.0);
		streamable.stream().forEach(t -> map.put(
				t.getFirstElement(),
				t.getSecondElement(),
				(max - t.getThirdElement()) / 10.0));
	}

	@Override
	public double apply(@NotNull Sequence<T> left, @NotNull Sequence<T> right,
			int i, int j) {
		Segment<T> sL = left.get(i);
		Segment<T> sR = right.get(j);
		Double value = sL.equals(sR)
				? 0.0
				: (map.contains(sL, sR) ? map.get(sL, sR) : max);
		return value;
	}
}
