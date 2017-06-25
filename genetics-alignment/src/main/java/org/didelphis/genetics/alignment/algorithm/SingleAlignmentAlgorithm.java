package org.didelphis.genetics.alignment.algorithm;

import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.BasicSequence;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.ColumnTable;
import org.didelphis.structures.tables.RectangularTable;
import org.didelphis.structures.tables.Table;
import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * @author Samantha Fiona McCabe
 * Created: 4/12/2016
 */
public class SingleAlignmentAlgorithm<T> implements AlignmentAlgorithm<T> {

	private final Comparator<T, Double> comparator;

	private final GapPenalty<T> gapPenalty;
	private final Segment<T> boundary;
	private final FeatureModel<T> model;

	private final int arity;

	public SingleAlignmentAlgorithm(Comparator<T, Double> comparator,
			GapPenalty<T> gapPenalty, int arity, SequenceFactory<T> factory) {
		this.arity = arity;
		this.comparator = comparator;
		this.gapPenalty = gapPenalty;

		model = factory.getFeatureMapping().getFeatureModel();
		boundary = factory.getSegment("#");
	}

	@NotNull
	@Override
	public Alignment<T> getAlignment(@NotNull List<Sequence<T>> sequences) {

		Sequence<T> left = sequences.get(0);
		Sequence<T> right = sequences.get(1);

		left.add(0, boundary);
		right.add(0,boundary);

		int m = left.size();
		int n = right.size();

		Table<Alignment<T>> matrix =
				new RectangularTable<>((Alignment<T>) null, m, n);
//
//		Alignment<T> start = new Alignment<>(2, model);
//
//		matrix.set(0, 0, start);
//
//		Segment<T> gap = gapPenalty.getGap();
//
//		for (int i = 1; i < m; i++) {   // Fill Left
//			Alignment<T> tail = matrix.get(i - 1, 0);
//			Alignment<T> body = new Alignment<>(tail);
//			Alignment<T> head = new Alignment<>(left.get(i), gap);
//			body.add(head);
//			matrix.set(i, 0, body);
//			body.setScore(0);
//		}
//		for (int j = 1; j < n; j++) {   // Fill Right
//			Alignment<T> tail = matrix.get(0, j - 1);
//			Alignment<T> body = new Alignment<>(tail);
//			Alignment<T> head = new Alignment<>(gap, right.get(j));
//			body.add(head);
//			matrix.set(0, j, body);
//			body.setScore(0);
//		}
//		for (int i = 1; i < m; i++) {
//			for (int j = 1; j < n; j++) {
//				matrix.set(i, j, min(matrix, left, right, i, j));
//			}
//		}
		return matrix.get(m - 1, n - 1);
	}

	public List<Alignment<T>> align(ColumnTable<Sequence<T>> data) {

		List<String> keys = data.getKeys();

		List<Sequence<T>> leftColumn = data.getColumn(keys.get(0));
		List<Sequence<T>> rightColumn = data.getColumn(keys.get(1));

		if (leftColumn.size() != rightColumn.size()) {
			throw new RuntimeException(
					"Mismatch in right and left column sizes! " +
							leftColumn.size() + " vs " + rightColumn.size());
		} else {
			Alignment<T> emptyAlignment = new Alignment<>(model);

			List<Alignment<T>> alignments =
					new ArrayList<>(leftColumn.size());
			for (int i = 0; i < leftColumn.size(); i++) {
				Sequence<T> left = new BasicSequence<>(leftColumn.get(i));
				Sequence<T> right =
						new BasicSequence<>(rightColumn.get(i));

				Alignment<T> alignment;
				if (!left.isEmpty() && !right.isEmpty()) {
					List<Sequence<T>> list = new ArrayList<>();
					list.add(left);
					list.add(right);

					alignment = getAlignment(list);
				} else {
					alignment = emptyAlignment;
				}
				alignments.add(alignment);
			}
			return alignments;
		}
	}

	@Override
	public String toString() {
		return "SingleAlignmentAlgorithm{" + "comparator=" + comparator +
				", gapPenalty=" + gapPenalty + ", boundary=" + boundary +
				", model=" + model + ", arity=" + arity + '}';
	}

	private Alignment<T> minimum(Table<Alignment<T>> matrix,
			Sequence<T> left, Sequence<T> right, int i, int j) {
		// We could probably just use the current segments and not need indices
		// matrix isn't needed

		Sequence<T> g = new BasicSequence<>(gapPenalty.getGap());

		// Length-1 sequences
		Sequence<T> l = new BasicSequence<>(left.get(i));
		Sequence<T> r = new BasicSequence<>(right.get(j));

		Alignment<T> insert = build(matrix.get(i - 1, j), l, g);
		Alignment<T> delete = build(matrix.get(i, j - 1), g, r);
		Alignment<T> change = build(matrix.get(i - 1, j - 1), l, r);

		NavigableMap<Double, Alignment<T>> map = new TreeMap<>();
//		map.put(insert.getScore(), insert);
//		map.put(delete.getScore(), delete);
//		map.put(change.getScore(), change);

		return map.firstEntry().getValue();
	}

	private Alignment<T> min(Table<Alignment<T>> matrix,
			Sequence<T> left, Sequence<T> right, int i, int j) {

		NavigableMap<Double, Alignment<T>> candidates = new TreeMap<>();

		Sequence<T> gap = new BasicSequence<>(gapPenalty.getGap());

		for (int g = 0; g <= arity; g++) {
			for (int h = 0; h <= arity; h++) {

				int indexL = i - g;
				int indexR = j - h;

				if (indexL >= 0 && indexR >= 0 && (g != 0 || h != 0)) {

					Sequence<T> subL = left.subsequence(indexL + 1, i + 1);
					Sequence<T> subR =
							right.subsequence(indexR + 1, j + 1);

					List<Sequence<T>> sequenceList = new ArrayList<>();
					sequenceList.add(g == 0 ? gap : subL);
					sequenceList.add(h == 0 ? gap : subR);

					List<Integer> indexList = new ArrayList<>();
					indexList.add(indexL * (g == 0 ? -1 : 1));
					indexList.add(indexR * (h == 0 ? -1 : 1));

					Alignment<T> head = matrix.get(indexL, indexR);
					Alignment<T> alignment = new Alignment<>(head);
//					alignment.add(sequenceList, indexList);

					double score = evaluate(alignment);
//					alignment.setScore(score);

					candidates.put(score, alignment);
				}
			}
		}
		return candidates.firstEntry().getValue();
	}

	private Alignment<T> build(Table<Alignment<T>> matrix,
			Sequence<T> a, Sequence<T> b, int i, int j, int g,
			int h) {

		Sequence<T> gap = new BasicSequence<>(gapPenalty.getGap());

		List<Sequence<T>> sequenceList = new ArrayList<>();
		int ixL = i - g;
		int ixR = j - h;
		sequenceList.add(g == 0 ? gap : a.subsequence(ixL, i + 1));
		sequenceList.add(h == 0 ? gap : b.subsequence(ixR, j + 1));

		List<Integer> indexList = new ArrayList<>();
		indexList.add(ixL * (g == 0 ? -1 : 1));
		indexList.add(ixR * (h == 0 ? -1 : 1));

		Alignment<T> alignment = new Alignment<>(matrix.get(ixL, ixR));
//		alignment.add(sequenceList, indexList);

		double score = evaluate(alignment);
//		alignment.setScore(score);

		return alignment;
	}

	private Alignment<T> build(Alignment<T> tail, Sequence<T> a,
			Sequence<T> b) {

//		Alignment<T> head = new Alignment<>(tail.getSpecification());
		Alignment<T> body = new Alignment<>(tail);
//		body.add(head);
		double score = evaluate(body);
//		body.setScore(score);
		return body;
	}

	private double evaluate(Alignment<T> alignment) {
		double score = 0.0;
//		for (int i = 0; i < alignment.getNumberColumns(); i++) {
//			Sequence<T> a = alignment.getRow(0).get(i);
//			Sequence<T> b = alignment.getRow(1).get(i);
//			score += comparator.apply(a, b);
//		}
		return score + gapPenalty.evaluate(alignment);
	}
}
