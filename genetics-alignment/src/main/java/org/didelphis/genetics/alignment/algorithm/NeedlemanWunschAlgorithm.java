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
import org.didelphis.language.phonetic.sequences.BasicSequence;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.RectangularTable;
import org.didelphis.structures.tables.Table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code NeedlemanWunsch}
 *
 * @author Samantha Fiona McCabe
 * @since 06/05/2017
 */
@ToString
@EqualsAndHashCode
public class NeedlemanWunschAlgorithm<T> implements AlignmentAlgorithm<T> {

	private final SequenceComparator<T> comparator;
	private final Optimization          optimization;
	private final GapPenalty<T>         gapPenalty;
	private final SequenceFactory<T>    factory;

	public NeedlemanWunschAlgorithm(
			Optimization optimization,
			SequenceComparator<T> comparator,
			GapPenalty<T> gapPenalty,
			SequenceFactory<T> factory
	) {
		this.optimization = optimization;
		this.comparator   = comparator;
		this.gapPenalty   = gapPenalty;
		this.factory      = factory;
	}

	@NonNull
	@Override
	public AlignmentResult<T> apply(@NonNull Sequence<T>left, @NonNull Sequence<T> right) {
		AlgorithmRunner<T> runner = new AlgorithmRunner<>(left, right, this);
		List<Alignment<T>> alignments = runner.trace(
				left.size() - 1,
				right.size() - 1
		);
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

	@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
	@ToString
	private static final class AlgorithmRunner<T> {
		Sequence<T>           left;
		Sequence<T>           right;
		Table<Double>         table;
		FeatureModel<T>       model;
		SequenceComparator<T> comparator;
		GapPenalty<T>         penalty;
		Optimization          optimization;

		private AlgorithmRunner(
				Sequence<T> left,
				Sequence<T> right,
				AlignmentAlgorithm<T> algorithm
		) {
			this.left  = left;
			this.right = right;

			SequenceFactory<T> factory = algorithm.getFactory();
			FeatureMapping<T> featureMapping = factory.getFeatureMapping();

			table        = new RectangularTable<>(0.0, left.size(), right.size());
			model        = featureMapping.getFeatureModel();
			comparator   = algorithm.getComparator();
			penalty      = algorithm.getGapPenalty();
			optimization = algorithm.getOptimization();

			align();
		}

		private void align() {
			int m = left.size();
			int n = right.size();
			for (int j = 1; j < n; j++) {
				double v = get(0, j - 1) + ins(j, j);
				table.set(0, j, v);
			}
			for (int i = 1; i < m; i++) {
				double v = get(i - 1, 0) + del(i, i);
				table.set(i, 0, v);
				for (int j = 1; j < n; j++) {
					Collection<Double> candidates = buildCandidates(i, j);
					Double score = candidates.stream()
							.reduce(optimization)
							.orElse(optimization.defaultValue());
					table.set(i, j, score);
				}
			}
		}

		@NonNull
	private Collection<Double> buildCandidates(int i, int j) {
			return Arrays.asList(
					get(i - 1, j - 1) + sub(i, j),
					get(i - 1, j    ) + del(i, 0),
					get(i,     j - 1) + ins(j, 0)
			);
		}

		private List<Alignment<T>> trace(int startI, int startJ) {

			if (startI < 0 || startJ < 0) {
				return Collections.emptyList();
			}

			Sequence<T> w = new BasicSequence<>(model);
			Sequence<T> z = new BasicSequence<>(model);

			int i = startI;
			int j = startJ;
			while (i > 0 || j > 0) {
				double sub = get(i - 1, j - 1);
				double del = get(i - 1, j    );
				double ins = get(i,     j - 1);

				if (i > 0 && j > 0 && check(sub, del, ins)) {
					// Sub
					w.add(left.get(i));
					z.add(right.get(j));
					i--;
					j--;
				} else if (i > 0 && check(del, ins, sub)) {
					// Del
					w.add(left.get(i));
					z.add(penalty.getGap());
					i--;
				} else /*if (j > 0 && op(ins, sub, del))*/ {
					// Ins
					w.add(penalty.getGap());
					z.add(right.get(j));
					j--;
				}
			}

			if (i >= 0) {
				w.add(left.get(i));
			}
			if (j >= 0) {
				z.add(right.get(j));
			}

			Collections.reverse(w);
			Collections.reverse(z);

			List<Alignment<T>> alignments = new ArrayList<>();
			alignments.add(new Alignment<>(Arrays.asList(w, z), model));
			return alignments;
		}

		private boolean check(double v1, double v2, double v3) {
			return optimization.test(v1, v2) && optimization.test(v1, v3);
		}

		private double get(int i, int j) {
			if (i < 0 || j < 0) {
				return optimization.defaultValue();
			}
			if ( i >= table.rows() || j >= table.columns()) {
				return optimization.defaultValue();
			}
			return table.get(i, j);
		}

		private double ins(int j, int gapSize) {
			double v = penalty.applyAsDouble(gapSize);
			return comparator.apply(penalty.getGap(), right, 0, j) + v;
		}

		private double del(int i, int gapSize) {
			double v = penalty.applyAsDouble(gapSize);
			return comparator.apply(left, penalty.getGap(), i, 0) + v;
		}

		private double sub(int i, int j) {
			return comparator.apply(left, right, i, j);
		}

		private Table<Double> getTable() {
			return table;
		}
	}
}
