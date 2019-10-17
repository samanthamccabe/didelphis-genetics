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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.AlignmentResult;
import org.didelphis.genetics.alignment.algorithm.optimization.Optimization;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.model.FeatureMapping;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.BasicSequence;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.RectangularTable;
import org.didelphis.structures.tables.Table;
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
 *
 */
@ToString
@EqualsAndHashCode
public class NeedlemanWunschAlgorithm<T> implements AlignmentAlgorithm<T> {

	private final AlignmentMode         alignmentMode;
	private final SequenceComparator<T> comparator;
	private final Optimization          optimization;
	private final GapPenalty<T>         gapPenalty;
	private final SequenceFactory<T>    factory;
	private final Segment<T>            anchor;

	public NeedlemanWunschAlgorithm(
			Optimization optimization,
			AlignmentMode alignmentMode,
			SequenceComparator<T> comparator,
			GapPenalty<T> gapPenalty,
			SequenceFactory<T> factory
	) {
		this.optimization = optimization;
		this.alignmentMode = alignmentMode;
		this.comparator   = comparator;
		this.gapPenalty   = gapPenalty;
		this.factory      = factory;

		anchor = factory.toSegment("#");
	}

	@NonNull
	@Override
	public AlignmentResult<T> apply(
			@NonNull Sequence<T> left, @NonNull Sequence<T> right
	) {
		if (!left.startsWith(anchor)) left.add(0, anchor);
		if (!right.startsWith(anchor)) right.add(0, anchor);

		AlgorithmRunner<T> runner = new AlgorithmRunner<>(left, right, this);
		List<Alignment<T>> alignments = runner.align();
		Table<Double> table = runner.getTable();
		return new AlignmentResult<>(left, right, table, alignments);
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

	@ToString
	@EqualsAndHashCode
	@FieldDefaults (makeFinal = true, level = AccessLevel.PRIVATE)
	private static final class AlgorithmRunner<T> {
		Sequence<T>   left;
		Sequence<T>   right;
		Table<Double> table;

		Table<Set<Operation>> operations;

		FeatureModel<T>       model;
		SequenceComparator<T> comparator;
		GapPenalty<T>         penalty;
		Optimization          optimization;
		private final Sequence<T> gap;

		private AlgorithmRunner(
				Sequence<T> left,
				Sequence<T> right,
				AlignmentAlgorithm<T> algorithm
		) {
			this.left = left;
			this.right = right;

			int m = left.size();
			int n = right.size();
			table      = new RectangularTable<>(0.0, m, n);
			operations = new RectangularTable<>(Collections.emptySet(), m, n);

			SequenceFactory<T> factory = algorithm.getFactory();
			FeatureMapping<T> featureMapping = factory.getFeatureMapping();
			model        = featureMapping.getFeatureModel();
			comparator   = algorithm.getComparator();
			penalty      = algorithm.getGapPenalty();
			optimization = algorithm.getOptimization();
			gap = penalty.getGap();
		}

		@NonNull
		private List<Alignment<T>> align() {
			populateTable();

			Sequence<T> w = new BasicSequence<>(model);
			Sequence<T> z = new BasicSequence<>(model);

			trace(w, z);

			List<Alignment<T>> alignments = new ArrayList<>();
			alignments.add(new Alignment<>(new Twin<>(w, z), model));
			return alignments;
		}

		private void trace(Sequence<T> w, Sequence<T> z) {

			int i = left.size()  - 1;
			int j = right.size() - 1;

			// Leaving these in for now; Recursive tracing is probably needed
			// in order to find multiple paths
			while (i > 0 || j > 0) {
				double sub = get(i - 1, j - 1);
				double del = get(i - 1, j    );
				double ins = get(i,     j - 1);
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

		private void populateTable() {
			int m = left.size();
			int n = right.size();
			for (int j = 1; j < n; j++) {
				double v = ins(0, j);
				table.set(0, j, v);
				operations.set(0, j, Collections.singleton(INS));
			}

			for (int i = 1; i < m; i++) {
				double v = del(i, 0);
				table.set(i, 0, v);
				operations.set(i, 0, Collections.singleton(DEL));

				for (int j = 1; j < n; j++) {
					Map<Operation, Double> ops = new EnumMap<>(Operation.class);
					ops.put(SUB, sub(i, j));
					ops.put(DEL, del(i, j));
					ops.put(INS, ins(i, j));

					double bestValue = ops.values()
							.stream()
							.reduce(optimization)
							.orElse(optimization.defaultValue());

					Set<Operation> bestOps = ops.entrySet()
							.stream()
							.filter(e -> e.getValue().equals(bestValue))
							.map(Map.Entry::getKey)
							.collect(Collectors.toSet());

					table.set(i, j, bestValue);
					operations.set(i, j, bestOps);
				}
			}
		}

		private boolean check(double v1, double v2, double v3) {
			return optimization.test(v1, v2) && optimization.test(v1, v3);
		}

		private double get(int i, int j) {
			if (i < 0 || j < 0) {
				return optimization.defaultValue();
			}
			if (i >= table.rows() || j >= table.columns()) {
				return optimization.defaultValue();
			}
			return table.get(i, j);
		}

		private double ins(int i, int j) {
			Set<Operation> ops = operations.get(i, j - 1);
			int gapSize = ops.contains(INS) ? 1 : 0;
			double value = penalty.applyAsDouble(gapSize);
			double score = comparator.apply(gap, right, 0, j);
			return get(i, j - 1) + score + value;
		}

		private double del(int i, int j) {
			Set<Operation> ops = operations.get(i - 1, j);
			int gapSize = ops.contains(DEL) ? 1 : 0;
			double value = penalty.applyAsDouble(gapSize);
			double score = comparator.apply(left, gap, i, 0);
			return get(i - 1, j) + score + value;
		}

		private double sub(int i, int j) {
			return get(i - 1, j - 1) + comparator.apply(left, right, i, j);
		}

		private Table<Double> getTable() {
			return table;
		}

	}
}
