package org.didelphis.genetics.alignment.algorithm;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.AlignmentResult;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.sequences.BasicSequence;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.RectangularTable;
import org.didelphis.structures.tables.Table;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Class {@code NeedlemanWunsch}
 *
 * @author Samantha Fiona McCabe
 * @since 06/05/2017
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode(callSuper = true)
public class NeedlemanWunschAlgorithm<N> extends AbstractAlignmentAlgorithm<N> {

	public NeedlemanWunschAlgorithm(
			Optimization<Double> optimization,
			SequenceComparator<N> comparator,
			GapPenalty<N> gapPenalty,
			SequenceFactory<N> factory
	) {
		super(optimization, comparator, gapPenalty, factory);
	}

	@Override
	public @NotNull AlignmentResult<N> apply(@NotNull List<? extends Sequence<N>> sequences) {
		Sequence<N> left = sequences.get(0);
		Sequence<N> right = sequences.get(1);
		AlgorithmRunner runner = new AlgorithmRunner(left, right);
		FeatureModel<N> model = getFactory().getFeatureMapping()
				.getFeatureModel();
		int startI = left.size() - 1;
		int startJ = right.size() - 1;
		List<Alignment<N>> alignments = runner.trace(
				new BasicSequence<>(Collections.emptyList(), model),
				new BasicSequence<>(Collections.emptyList(), model),
				startI,
				startJ
		);
		return new AlignmentResult<>(left, right, runner.table, alignments);
	}

	public Table<Double> align(Sequence<N> left, Sequence<N> right) {
		AlgorithmRunner runner = new AlgorithmRunner(left, right);
		return runner.getTable();
	}

	private static boolean neq(double sub, double del) {
		return !Objects.equals(sub, del);
	}

	private final class AlgorithmRunner {
		private final Sequence<N> left;
		private final Sequence<N> right;
		private final Table<Double> table;
		private final FeatureModel<N> model = getFactory().getFeatureMapping()
				.getFeatureModel();
		private final SequenceComparator<N> comparator = getComparator();
		private final GapPenalty<N> penalty = getGapPenalty();
		private final Optimization<Double> optimization = getOptimization();

		private final Sequence<N> gap = penalty.getGap();


		private AlgorithmRunner(Sequence<N> left, Sequence<N> right) {
			this.left = left;
			this.right = right;
			table = new RectangularTable<>(0.0, left.size(), right.size());
			align();
		}

		private void align() {
			int m = left.size();
			int n = right.size();
			for (int j = 1; j < n; j++) {
				double v = get(table, 0, j - 1) + ins(right, j);
				table.set(0, j, v);
			}
			for (int i = 1; i < m; i++) {
				double v = get(table, i - 1, 0) + del(left, i);
				table.set(i, 0, v);
				for (int j = 1; j < n; j++) {
					Collection<Double> candidates = buildCandidates(i, j);
					Double score = candidates.parallelStream()
							.reduce(optimization)
							.orElse(optimization.defaultValue());
					table.set(i, j, score);
				}
			}
		}

		private @NotNull Collection<Double> buildCandidates(int i, int j) {
			return Arrays.asList(
					get(table, i - 1, j - 1) + sub(left, right, i, j),
					get(table, i - 1, j) + del(left, i),
					get(table, i, j - 1) + ins(right, j)
			);
		}

		private List<Alignment<N>> trace(
				Sequence<N> w, Sequence<N> z, int startI, int startJ
		) {

			if (startI < 0 || startJ < 0) {
				return Collections.emptyList();
			}

			Sequence<N> W = new BasicSequence<>(w, model);
			Sequence<N> Z = new BasicSequence<>(z, model);

			int i = startI;
			int j = startJ;
			while (i > 0 || j > 0) {
				double sub = get(table, i - 1, j - 1);
				double del = get(table, i - 1, j);
				double ins = get(table, i, j - 1);

				if (i > 0 && j > 0 && op(sub, del, ins)) {
					// Sub
					W.add(left.get(i));
					Z.add(right.get(j));
					i--;
					j--;
				} else if (i > 0 && op(del, ins, sub)) {
					// Del
					W.add(left.get(i));
					Z.add(gap);
					i--;
				} else /*if (j > 0 && op(ins, sub, del))*/ {
					// Ins
					W.add(gap);
					Z.add(right.get(j));
					j--;
				}
			}

			if (i >= 0) {
				W.add(left.get(i));
			}
			if (j >= 0) {
				Z.add(right.get(j));
			}

			Collections.reverse(W);
			Collections.reverse(Z);

			List<Alignment<N>> alignments = new ArrayList<>();
			alignments.add(new Alignment<>(Arrays.asList(W, Z), model));
			return alignments;
		}

		private boolean op(double v1, double v2, double v3) {
			return optimization.test(v1, v2) && optimization.test(v1, v3);
		}

		private double get(Table<Double> table, int i, int j) {
			return (i < 0 || j < 0 || i >= table.rows() || j >= table.columns())
					? optimization.defaultValue()
					: table.get(i, j);
		}

		private double ins(Sequence<N> right, int j) {
			return comparator.apply(gap, right, 0, j)
					+ penalty.applyAsDouble(0);
		}

		private double del(Sequence<N> left, int i) {
			return comparator.apply(left, gap, i, 0) + penalty.applyAsDouble(0);
		}

		private double sub(Sequence<N> left, Sequence<N> right, int i, int j) {
			return comparator.apply(left, right, i, j);
		}

		private Table<Double> getTable() {
			return table;
		}
	}
}
