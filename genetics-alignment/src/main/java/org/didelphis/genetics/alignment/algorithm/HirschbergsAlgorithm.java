package org.didelphis.genetics.alignment.algorithm;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.AlignmentResult;
import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.BasicSequence;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.Table;
import org.didelphis.structures.tuples.Tuple;
import org.didelphis.structures.tuples.Twin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @param <N>
 */
@ToString
@EqualsAndHashCode(callSuper = true)
public class HirschbergsAlgorithm<N>
		extends AbstractAlignmentAlgorithm<N> {

	private final NeedlemanWunschAlgorithm<N> nwAlgorithm;

	public HirschbergsAlgorithm(
			Comparator<N> comparator,
			GapPenalty<N> gapPenalty,
			SequenceFactory<N> factory
	) {
		super(comparator, BaseOptimization.MIN, gapPenalty, factory);
		nwAlgorithm = new NeedlemanWunschAlgorithm<>(comparator,
				BaseOptimization.MIN,
				gapPenalty,
				factory
		);
	}

	@Override
	public @NotNull AlignmentResult<N> apply(@NotNull List<? extends Sequence<N>> sequences) {

		if (sequences.size() != 2) {
			throw new IllegalArgumentException(getClass().getCanonicalName()
					+ " does not support aligning more than two sequences");
		}

		Sequence<N> left = sequences.get(0);
		Sequence<N> right = sequences.get(1);

		Twin<Sequence<N>> t = hirschberg(left, right);

		FeatureModel<N> model = getFactory().getFeatureMapping()
				.getFeatureModel();

		//		return new Alignment<>(
		//				new BasicSequence<>(t.getLeft(), model),
		//				new BasicSequence<>(t.getRight(), model));
		return null; // TODO:
	}
	
	private Twin<Sequence<N>> hirschberg(
			@NotNull Sequence<N> left,
			@NotNull Sequence<N> right
	) {
		int m = left.size();
		int n = right.size();

		Sequence<N> Z = new BasicSequence<>(left.getFeatureModel());
		Sequence<N> W = new BasicSequence<>(right.getFeatureModel());

		Sequence<N> gap = getGapPenalty().getGap();
		
		if (m == 0) {
			for (Segment<N> aRight : right) {
				Z.addAll(gap);
				W.add(aRight);
			}
		} else if (n == 0) {
			for (Segment<N> aLeft : left) {
				Z.add(aLeft);
				W.addAll(gap);
			}
		} else if (m == 1 || n == 1) {
			AlignmentResult<N> alignmentResult = nwAlgorithm.apply(Arrays.asList(left, right));
			List<Alignment<N>> alignments = alignmentResult.getAlignments();

			if (!alignments.isEmpty()) {
				Alignment<N> alignment = alignments.get(0);
				Z.addAll(alignment.getRow(0));
				W.addAll(alignment.getRow(1));
			}

		} else {
			int xMid = m / 2;

			Sequence<N> revLeft = new BasicSequence<>(left.subsequence(xMid, m));
			Sequence<N> revRight = new BasicSequence<>(right);

			Collections.reverse(revLeft);
			Collections.reverse(revRight);

			Sequence<N> leftHalf = left.subsequence(0, xMid);
			
			List<Double> scoresLeft = NWScore(leftHalf, right);
			List<Double> scoresRight = NWScore(revLeft, revRight);

			Collections.reverse(scoresRight);

			int yMid = getMid(scoresLeft, scoresRight);

			Sequence<N> r1 = right.subsequence(0, yMid);
			Sequence<N> l2 = left.subsequence(xMid, m);
			Sequence<N> r2 = right.subsequence(yMid, n);
			
			Tuple<Sequence<N>, Sequence<N>> A = hirschberg(leftHalf, r1);
			Tuple<Sequence<N>, Sequence<N>> B = hirschberg(l2, r2);

			W.addAll(A.getLeft());
			W.addAll(B.getLeft());
			
			Z.addAll(A.getRight());
			Z.addAll(B.getRight());
		}

		return new Twin<>(W, Z);
	}

	private static int getMid(List<Double> scoresLeft, List<Double> scoresRight) {
		int n = scoresLeft.size();
		double maxV = Double.MIN_VALUE;
		int   yMid = -1;
		for (int i = 0 ; i < n; i++) {
			double v = scoresLeft.get(i) + scoresRight.get(i);
			if (v > maxV) {
				maxV = v;
				yMid = i;
			}
		}
		return yMid;
	}

	private @NotNull List<Double> NWScore(Sequence<N> left, Sequence<N> right) {
		Table<Double> table = nwAlgorithm.align(left, right);
		return table.getRow(table.rows()-1);
	}
	
	private double ins(Sequence<N> sequence, int index) {
		Sequence<N> gap = getGapPenalty().getGap();
		return getComparator().apply(sequence, gap, index, 0);
	}

	private double del(Sequence<N> sequence, int index) {
		Sequence<N> gap = getGapPenalty().getGap();
		return getComparator().apply(sequence, gap, index, 0);
	}

	private double sub(Sequence<N> left, Sequence<N> right, int i, int j) {
		return getComparator().apply(left, right, i, j);
	}

}
