package org.didelphis.genetics.alignment.algorithm.single;

import org.didelphis.common.language.phonetic.SequenceFactory;
import org.didelphis.common.language.phonetic.model.interfaces.FeatureModel;
import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.common.language.phonetic.sequences.BasicSequence;
import org.didelphis.common.language.phonetic.sequences.Sequence;
import org.didelphis.common.structures.tables.ColumnTable;
import org.didelphis.common.structures.tables.RectangularTable;
import org.didelphis.common.structures.tables.Table;
import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 4/12/2016
 */
public class SingleAlignmentAlgorithm implements AlignmentAlgorithm {

	private final Comparator<Sequence<Double>> comparator;

	private final GapPenalty gapPenalty;
	private final Segment<Double> boundary;
	private final FeatureModel<Double> model;

	private final int arity;

	public SingleAlignmentAlgorithm(Comparator<Sequence<Double>> comparator,
			GapPenalty gapPenalty, int arity, SequenceFactory<Double> factory) {
		this.arity = arity;
		this.comparator = comparator;
		this.gapPenalty = gapPenalty;

		model = factory.getFeatureMapping().getFeatureModel();
		boundary = factory.getSegment("#");
	}

	@Override
	public Alignment<Double> getAlignment(List<Sequence<Double>> sequences) {

		Sequence<Double> left = sequences.get(0);
		Sequence<Double> right = sequences.get(1);

		left.addFirst(boundary);
		right.addFirst(boundary);

		int m = left.size();
		int n = right.size();

		Table<Alignment<Double>> matrix =
				new RectangularTable<>((Alignment<Double>) null, m, n);
		Collection<Segment<Double>> list = new ArrayList<>();
		Collections.addAll(list, left.getFirst(), right.getFirst());

		Alignment<Double> start = new Alignment<>(2, model);

		matrix.set(0, 0, start);

		Segment<Double> gap = gapPenalty.getGapSegment();

		for (int i = 1; i < m; i++) {   // Fill Left
			Alignment<Double> tail = matrix.get(i - 1, 0);
			Alignment<Double> body = new Alignment<>(tail);
			Alignment<Double> head = new Alignment<>(left.get(i), gap);
			body.add(head);
			matrix.set(i, 0, body);
			body.setScore(0);
		}
		for (int j = 1; j < n; j++) {   // Fill Right
			Alignment<Double> tail = matrix.get(0, j - 1);
			Alignment<Double> body = new Alignment<>(tail);
			Alignment<Double> head = new Alignment<>(gap, right.get(j));
			body.add(head);
			matrix.set(0, j, body);
			body.setScore(0);
		}
		for (int i = 1; i < m; i++) {
			for (int j = 1; j < n; j++) {
				matrix.set(i, j, min(matrix, left, right, i, j));
			}
		}
		return matrix.get(m - 1, n - 1);
	}

	@Override
	public List<Alignment<Double>> align(ColumnTable<Sequence<Double>> data) {

		List<String> keys = data.getKeys();

		List<Sequence<Double>> leftColumn = data.getColumn(keys.get(0));
		List<Sequence<Double>> rightColumn = data.getColumn(keys.get(1));

		if (leftColumn.size() != rightColumn.size()) {
			throw new RuntimeException(
					"Mismatch in right and left column sizes! " +
							leftColumn.size() + " vs " + rightColumn.size());
		} else {
			Alignment<Double> emptyAlignment = new Alignment<>(model);

			List<Alignment<Double>> alignments =
					new ArrayList<>(leftColumn.size());
			for (int i = 0; i < leftColumn.size(); i++) {
				Sequence<Double> left = new BasicSequence<>(leftColumn.get(i));
				Sequence<Double> right =
						new BasicSequence<>(rightColumn.get(i));

				Alignment<Double> alignment;
				if (!left.isEmpty() && !right.isEmpty()) {
					List<Sequence<Double>> list = new ArrayList<>();
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

	private Alignment<Double> minimum(Table<Alignment<Double>> matrix,
			Sequence<Double> left, Sequence<Double> right, int i, int j) {
		// We could probably just use the current segments and not need indices
		// matrix isn't needed

		Sequence<Double> g = new BasicSequence<>(gapPenalty.getGapSegment());

		// Length-1 sequences
		Sequence<Double> l = new BasicSequence<>(left.get(i));
		Sequence<Double> r = new BasicSequence<>(right.get(j));

		Alignment<Double> insert = build(matrix.get(i - 1, j), l, g);
		Alignment<Double> delete = build(matrix.get(i, j - 1), g, r);
		Alignment<Double> change = build(matrix.get(i - 1, j - 1), l, r);

		NavigableMap<Double, Alignment<Double>> map = new TreeMap<>();
		map.put(insert.getScore(), insert);
		map.put(delete.getScore(), delete);
		map.put(change.getScore(), change);

		return map.firstEntry().getValue();
	}

	private Alignment<Double> min(Table<Alignment<Double>> matrix,
			Sequence<Double> left, Sequence<Double> right, int i, int j) {

		NavigableMap<Double, Alignment<Double>> candidates = new TreeMap<>();

		Sequence<Double> gap = new BasicSequence<>(gapPenalty.getGapSegment());

		for (int g = 0; g <= arity; g++) {
			for (int h = 0; h <= arity; h++) {

				int indexL = i - g;
				int indexR = j - h;

				if (indexL >= 0 && indexR >= 0 && (g != 0 || h != 0)) {

					Sequence<Double> subL = left.subsequence(indexL + 1, i + 1);
					Sequence<Double> subR =
							right.subsequence(indexR + 1, j + 1);

					List<Sequence<Double>> sequenceList = new ArrayList<>();
					sequenceList.add(g == 0 ? gap : subL);
					sequenceList.add(h == 0 ? gap : subR);

					List<Integer> indexList = new ArrayList<>();
					indexList.add(indexL * (g == 0 ? -1 : 1));
					indexList.add(indexR * (h == 0 ? -1 : 1));

					Alignment<Double> head = matrix.get(indexL, indexR);
					Alignment<Double> alignment = new Alignment<>(head);
					alignment.add(sequenceList, indexList);

					double score = evaluate(alignment);
					alignment.setScore(score);

					candidates.put(score, alignment);
				}
			}
		}
		return candidates.firstEntry().getValue();
	}

	private Alignment<Double> build(Table<Alignment<Double>> matrix,
			Sequence<Double> a, Sequence<Double> b, int i, int j, int g,
			int h) {

		Sequence<Double> gap = new BasicSequence<>(gapPenalty.getGapSegment());

		List<Sequence<Double>> sequenceList = new ArrayList<>();
		int ixL = i - g;
		int ixR = j - h;
		sequenceList.add(g == 0 ? gap : a.subsequence(ixL, i + 1));
		sequenceList.add(h == 0 ? gap : b.subsequence(ixR, j + 1));

		List<Integer> indexList = new ArrayList<>();
		indexList.add(ixL * (g == 0 ? -1 : 1));
		indexList.add(ixR * (h == 0 ? -1 : 1));

		Alignment<Double> alignment = new Alignment<>(matrix.get(ixL, ixR));
		alignment.add(sequenceList, indexList);

		double score = evaluate(alignment);
		alignment.setScore(score);

		return alignment;
	}

	private Alignment<Double> build(Alignment<Double> tail, Sequence<Double> a,
			Sequence<Double> b) {

		Alignment<Double> head = new Alignment<>(tail.getSpecification());
		Alignment<Double> body = new Alignment<>(tail);
		body.add(head);
		double score = evaluate(body);
		body.setScore(score);
		return body;
	}

	private double evaluate(Alignment<Double> alignment) {
		double score = 0.0;
		for (int i = 0; i < alignment.getNumberColumns(); i++) {
			Sequence<Double> a = alignment.getRow(0).get(i);
			Sequence<Double> b = alignment.getRow(1).get(i);
			score += comparator.apply(a, b);
		}
		return score + gapPenalty.evaluate(alignment);
	}
}
