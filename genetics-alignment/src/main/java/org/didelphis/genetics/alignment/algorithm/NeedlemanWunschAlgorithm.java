package org.didelphis.genetics.alignment.algorithm;

import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.BasicSequence;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.RectangularTable;
import org.didelphis.structures.tables.Table;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Class {@code NeedlemanWunsch}
 * @author Samantha Fiona McCabe
 * @since 06/05/2017
 */
public class NeedlemanWunschAlgorithm<N> extends AbstractAlignmentAlgorithm<N> {

	public NeedlemanWunschAlgorithm(Comparator<N, Double> comparator,
			GapPenalty<N> gapPenalty, SequenceFactory<N> factory) {
		super(comparator, gapPenalty, factory);
	}

	@NotNull
	@Override
	public Alignment<N> getAlignment(@NotNull List<Sequence<N>> sequences) {
		Sequence<N> left = sequences.get(0);
		Sequence<N> right = sequences.get(1);
		AlgorithmRunner runner = new AlgorithmRunner(left, right);
		FeatureModel<N> model = getFactory().getFeatureMapping().getFeatureModel();
		int startI = left.size() - 1;
		int startJ = right.size() - 1;
		Collection<Alignment<N>> alignments
				= runner.trace(new Alignment<>(2, model), startI, startJ);
		Alignment<N> alignment = alignments.iterator().next();
		alignment.setScore(runner.table.get(startI, startJ));
		return alignment;
	}

	public Table<Double> align(Sequence<N> left, Sequence<N> right) {
		AlgorithmRunner runner = new AlgorithmRunner(left, right);
		return runner.getTable();
	}

	private final class AlgorithmRunner {
		private final Sequence<N> left;
		private final Sequence<N> right;
		private final Table<Double> table;
		private final FeatureModel<N> model;
		private final Sequence<N> gap;

		private AlgorithmRunner(Sequence<N> left, Sequence<N> right) {
			this.left = left;
			this.right = right;

			gap = getGapPenalty().getGap();
			model = getFactory().getFeatureMapping().getFeatureModel();
			table = new RectangularTable<>(0.0, left.size(), right.size());
			align();
		}

		private void align() {
			int m = left.size();
			int n = right.size();
			for (int j = 1; j < n; j++) {
				double v = get(table,0, j - 1) + ins(right, j);
				table.set(0, j, v);
			}
			for (int i = 1; i < m; i++) {
				double v = get(table, i - 1, 0) + del(left, i);
				table.set(i, 0, v);
				for (int j = 1; j < n; j++) {
					Double score = buildCandidates(i, j).parallelStream()
							.min(Double::compare)
							.orElse(Double.POSITIVE_INFINITY);
					table.set(i, j, score);
				}
			}
		}

		@NotNull
		private Collection<Double> buildCandidates(int i, int j) {
			return Arrays.asList(
					get(table,i - 1, j - 1) + sub(left, right, i, j),
					get(table,i - 1, j) + del(left, i),
					get(table,i, j - 1) + ins(right, j)
			);
		}

		private Collection<Alignment<N>> trace(Alignment<N> alignment,
				int startI, int startJ) {

			Sequence<N> W = new BasicSequence<>(alignment.getRow(0), model);
			Sequence<N> Z = new BasicSequence<>(alignment.getRow(1), model);

			Collection<Alignment<N>> alignments = new ArrayList<>();

			int i = startI;
			int j = startJ;
			while (!(i == 0 && j == 0)) {
				double sub = get(table, i - 1, j - 1);
				double del = get(table, i - 1, j);
				double ins = get(table, i, j - 1);

				Segment<N> lI = left.get(i);
				Segment<N> rJ = right.get(j);

				if (del < ins && del < sub) {
					W.add(lI);
					Z.add(gap);
					i--;
				} else if (ins < sub && ins < del) {
					W.add(gap);
					Z.add(rJ);
					j--;
				} else if (sub < del && sub < ins) {
					W.add(lI);
					Z.add(rJ);
					i--;
					j--;
				}
				// Because of how the alignments are calculated, multiple paths
				// may not even be possible, but I leave it here anyway
				else {
					if (Objects.equals(sub, del) && Objects.equals(del, ins)) {
						alignments.addAll(trace(alignment, i - 1, j - 1));
						alignments.addAll(trace(alignment, i - 1, j));
						alignments.addAll(trace(alignment, i, j - 1));
					} else if (Objects.equals(sub, del)) {
						alignments.addAll(trace(alignment, i - 1, j - 1));
						alignments.addAll(trace(alignment, i - 1, j));
					} else if (Objects.equals(del, ins)) {
						alignments.addAll(trace(alignment, i - 1, j));
						alignments.addAll(trace(alignment, i, j - 1));
					} else if (Objects.equals(ins, sub)) {
						alignments.addAll(trace(alignment, i - 1, j - 1));
						alignments.addAll(trace(alignment, i, j - 1));
					}
					return alignments;
				}
			}

			W.add(left.get(i));
			Z.add(right.get(j));

			Collections.reverse(W);
			Collections.reverse(Z);

			alignments.add(new Alignment<>(Arrays.asList(W, Z), model));
			return alignments;
		}

		private double get(Table<Double> table, int i, int j) {
			if (i < 0 || j < 0 || i >= table.rows() || j >= table.columns()) {
				return Double.POSITIVE_INFINITY;
			}
			Double value = table.get(i, j);
			return value == null ? Double.POSITIVE_INFINITY : value;
		}

		private double ins(Sequence<N> sequence, int index) {
			return getComparator().apply(sequence, gap, index, 0);
		}

		private double del(Sequence<N> sequence, int index) {
			return getComparator().apply(sequence, gap, index, 0);
		}

		private double sub(Sequence<N> left, Sequence<N> right, int i, int j) {
			return getComparator().apply(left, right, i, j);
		}

		private Table<Double> getTable() {
			return table;
		}
	}
}
