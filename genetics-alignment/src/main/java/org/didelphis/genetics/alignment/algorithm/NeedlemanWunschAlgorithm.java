/******************************************************************************
 * General components for language modeling and analysis                      *
 *                                                                            *
 * Copyright (C) 2014-2019 Samantha F McCabe                                  *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.     *
 *                                                                            *
 ******************************************************************************/

package org.didelphis.genetics.alignment.algorithm;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.AlignmentResult;
import org.didelphis.genetics.alignment.algorithm.optimization.Optimization;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.BasicSequence;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tuples.Twin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.didelphis.genetics.alignment.algorithm.Operation.DEL;
import static org.didelphis.genetics.alignment.algorithm.Operation.INS;
import static org.didelphis.genetics.alignment.algorithm.Operation.SUB;

/**
 * Class {@code NeedlemanWunsch}
 */
@ToString
@EqualsAndHashCode
public class NeedlemanWunschAlgorithm<T> implements AlignmentAlgorithm<T> {

	private final AlignmentMode alignmentMode;
	private final SequenceComparator<T> comparator;
	private final Optimization optimization;
	private final GapPenalty<T> gapPenalty;
	private final SequenceFactory<T> factory;
	private final Segment<T> anchor;

	public NeedlemanWunschAlgorithm(
			Optimization optimization,
			AlignmentMode alignmentMode,
			SequenceComparator<T> comparator,
			GapPenalty<T> gapPenalty,
			SequenceFactory<T> factory
	) {
		this.optimization = optimization;
		this.alignmentMode = alignmentMode;
		this.comparator = comparator;
		this.gapPenalty = gapPenalty;
		this.factory = factory;

		anchor = factory.toSegment("#");
	}

	@NonNull
	@Override
	public AlignmentResult<T> apply(
			@NonNull Sequence<T> left, @NonNull Sequence<T> right
	) {
		if (!left.startsWith(anchor)) left.add(0, anchor);
		if (!right.startsWith(anchor)) right.add(0, anchor);

		AlignmentTable<T> table = new AlignmentTable<>(left, right);
		FeatureModel<T> model = factory.getFeatureMapping().getFeatureModel();
		Sequence<T> w = new BasicSequence<>(model);
		Sequence<T> z = new BasicSequence<>(model);

		populateTable(table);
		trace(table, w, z);

		List<Alignment<T>> alignments = new ArrayList<>();
		Alignment<T> alignment = new Alignment<>(new Twin<>(w, z), model);
		alignments.add(alignment);

		return new AlignmentResult<>(
				left,
				right,
				table.getScores(),
				alignments
		);
	}

	@NonNull
	@Override
	public GapPenalty<T> getGapPenalty() {
		return gapPenalty;
	}

	@NonNull
	@Override
	public SequenceFactory<T> getFactory() {
		return factory;
	}

	@NonNull
	@Override
	public SequenceComparator<T> getComparator() {
		return comparator;
	}

	@NonNull
	@Override
	public Optimization getOptimization() {
		return optimization;
	}

	private void trace(AlignmentTable<T> table, Sequence<T> w, Sequence<T> z) {

		Sequence<T> gap = gapPenalty.getGap();

		int i = table.rows() - 1;
		int j = table.cols() - 1;

		// Leaving these in for now; Recursive tracing is probably needed
		// in order to find multiple paths
		Sequence<T> right = table.getRight();
		Sequence<T> left = table.getLeft();
		while (i > 0 || j > 0) {
			double sub = get(table, i - 1, j - 1);
			double del = get(table, i - 1, j);
			double ins = get(table, i, j - 1);
			if (i > 0 && check(del, ins, sub)) {
				// Del
				w.add(left.get(i));
				z.add(gap);
				i--;
			} else if (j > 0 && check(ins, sub, del)) {
				// Ins
				w.add(gap);
				z.add(right.get(j));
				j--;
			} else {
				// Sub
				w.add(left.get(i));
				z.add(right.get(j));
				i--;
				j--;
			}
		}

		if (i >= 0) w.add(left.get(i));
		if (j >= 0) z.add(right.get(j));

		Collections.reverse(w);
		Collections.reverse(z);
	}

	private void populateTable(AlignmentTable<T> table) {
		int m = table.rows();
		int n = table.cols();
		for (int j = 1; j < n; j++) {
			double v = ins(table, 0, j);
			table.setScore(0, j, v);
			table.setOperations(0, j, Collections.singleton(INS));
		}

		for (int i = 1; i < m; i++) {
			double v = del(table, i, 0);
			table.setScore(i, 0, v);
			table.setOperations(i, 0, Collections.singleton(DEL));
		}

		for (int i = 1; i < m; i++) {
			for (int j = 1; j < n; j++) {
				Map<Operation, Double> ops = new EnumMap<>(Operation.class);
				ops.put(SUB, sub(table, i, j));
				ops.put(DEL, del(table, i, j));
				ops.put(INS, ins(table, i, j));

				double bestValue = ops.values()
						.stream()
						.reduce(optimization)
						.orElse(optimization.defaultValue());

				Set<Operation> bestOps = ops.entrySet()
						.stream()
						.filter(e -> e.getValue().equals(bestValue))
						.map(Map.Entry::getKey)
						.collect(Collectors.toSet());

				table.setScore(i, j, bestValue);
				table.setOperations(i, j, bestOps);
			}
		}
	}

	private boolean check(double v1, double v2, double v3) {
		return optimization.test(v1, v2) && optimization.test(v1, v3);
	}

	private double get(AlignmentTable<T> table, int i, int j) {
		if (i < 0 || j < 0) {
			return optimization.defaultValue();
		}
		if (i >= table.rows() || j >= table.cols()) {
			return optimization.defaultValue();
		}
		return table.getScore(i, j);
	}

	private double ins(AlignmentTable<T> table, int i, int j) {
		Sequence<T> gap = gapPenalty.getGap();
		Set<Operation> ops = table.getOperations(i, j - 1);
		int gapSize = ops.contains(INS) ? 1 : 0;
		double value = gapPenalty.applyAsDouble(gapSize);
		double score = comparator.apply(gap, table.getRight(), 0, j);
		return get(table, i, j - 1) + score + value;
	}

	private double del(AlignmentTable<T> table, int i, int j) {
		Sequence<T> gap = gapPenalty.getGap();
		Set<Operation> ops = table.getOperations(i - 1, j);
		int gapSize = ops.contains(DEL) ? 1 : 0;
		double value = gapPenalty.applyAsDouble(gapSize);
		double score = comparator.apply(table.getLeft(), gap, i, 0);
		return get(table, i - 1, j) + score + value;
	}

	private double sub(AlignmentTable<T> table, int i, int j) {
		Sequence<T> left = table.getLeft();
		Sequence<T> right = table.getRight();
		return get(table, i - 1, j - 1) + comparator.apply(left, right, i, j);
	}
}
