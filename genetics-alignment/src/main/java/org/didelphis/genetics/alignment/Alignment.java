package org.didelphis.genetics.alignment;

import org.didelphis.language.phonetic.ModelBearer;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.model.FeatureSpecification;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.RectangularTable;
import org.didelphis.utilities.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by samantha on 1/9/17.
 */
public class Alignment<T> extends RectangularTable<Segment<T>>
		implements ModelBearer<T> {

	private final FeatureModel<T> featureModel;

	public Alignment(FeatureModel<T> featureModel) {
		super((Segment<T>) null, 0, 0);
		this.featureModel = featureModel;
	}

	public Alignment(int n, FeatureModel<T> featureModel) {
		super((Segment<T>) null, n, 0);
		this.featureModel = featureModel;
	}

	public Alignment(Alignment<T> alignment) {
		super(alignment);
		featureModel = alignment.featureModel;
	}

	public Alignment(List<Sequence<T>> list, FeatureModel<T> featureModel) {
		super(list, list.size(), list.isEmpty() ? 0 : list.get(0).size());
		for (Sequence<T> sequence: list) {
			if (sequence.size() != columns()) {
				throw Exceptions.create(IllegalArgumentException.class).add(
						"Sequence {} in {} is not the correct number",
						"of elements: {} vs {}"
				).with(sequence, list, sequence.size(), columns()).build();
			}
		}
		this.featureModel = featureModel;
	}

	public Alignment(Sequence<T> left, Sequence<T> right) {
		super(Arrays.asList(left, right), 2, left.size());
		featureModel = left.getFeatureModel();
	}

	@Override
	public FeatureModel<T> getFeatureModel() {
		return featureModel;
	}

	@Override
	public FeatureSpecification getSpecification() {
		return featureModel.getSpecification();
	}

	@Override
	public @NotNull String toString() {
		return getPrettyTable().replaceAll("\n", "\t");
	}

	public void add(Collection<Segment<T>> list) {
		insertColumn(rows(), list);
	}

	@Deprecated
	public String getPrettyTable() {

		StringBuilder stringBuilder = new StringBuilder();
		for (CharSequence charSequence: buildPrettyAlignments()) {
			stringBuilder.append(charSequence);
			stringBuilder.append('\n');
		}
		return stringBuilder.toString();
	}

	/**
	 * Generates a human-readable alignments so that aligned segments of
	 * different sizes will still group into columns by adding padding on the
	 * shorter of a group:
	 * {@code
	 *  th a l  a n _
	 *  d  a lh a n a
	 * }
	 *
	 * @return a {@link List} where each entry is a single row of the
	 * 		alignment
	 */
	public List<CharSequence> buildPrettyAlignments() {

		List<CharSequence> builders = new ArrayList<>(rows());
		int rows = rows();
		int columns = columns();

		List<Integer> maxima = new ArrayList<>(Collections.nCopies(columns, 0));
		for (int j = 0; j < columns(); j++) {
			for (int i = 0; i < rows(); i++) {
				int v = maxima.get(j);
				Segment<T> segment = get(i, j);
				String s = segment.getSymbol();
				int size = getPrintableLength(s);
				if (v < size) {
					maxima.set(j, size);
				}
			}
		}

		for (int i = 0; i < rows; i++) {
			StringBuilder builder = new StringBuilder();
			for (int j = 0; j < columns; j++) {
				Segment<T> segment = get(i, j);
				String s = segment == null ? "null" : segment.getSymbol();
				int maximum = maxima.get(j);
				int visible = getPrintableLength(s);
				builder.append(s).append(' ');
				while (maximum > visible) {
					builder.append(' ');
					visible++;
				}
			}
			builders.add(builder);
		}
		return builders;
	}

	private static int getPrintableLength(String string) {
		int visible = 0;
		for (char c: string.toCharArray()) {
			if (Character.getType(c) != Character.NON_SPACING_MARK) {
				visible++;
			}
		}
		return visible;
	}
}
