package org.didelphis.genetics.alignment;

import org.didelphis.language.phonetic.ModelBearer;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.model.FeatureSpecification;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.RectangularTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Created by samantha on 1/9/17.
 */
public class Alignment<T> extends RectangularTable<Segment<T>>
		implements ModelBearer<T> {

	private final FeatureModel<T> featureModel;

	public Alignment(FeatureModel<T> featureModel) {
		super((Segment<T>) null, 0,0);
		this.featureModel = featureModel;
	}

	public Alignment(int n, FeatureModel<T> featureModel) {
		super((Segment<T>) null, n,0);
		this.featureModel = featureModel;
	}

	public Alignment(Alignment<T> alignment) {
		super(alignment);
		featureModel = alignment.featureModel;
	}

	public Alignment(List<Sequence<T>> list, FeatureModel<T> featureModel) {
		super(list, list.size(), list.isEmpty() ? 0 : list.get(0).size() );
		this.featureModel = featureModel;
	}

	public Alignment(Sequence<T> left, Sequence<T> right) {
		super(Arrays.asList(left, right), 2, left.size());
		featureModel = left.getFeatureModel();
	}

	public void add(Collection<Segment<T>> list) {
		insertColumn(rows(), list);
	}

	@Deprecated
	public String getPrettyTable() {

		StringBuilder stringBuilder = new StringBuilder();
		for (CharSequence charSequence : buildPrettyAlignments()) {
			stringBuilder.append(charSequence);
			stringBuilder.append('\n');
		}
		return stringBuilder.toString();
	}

	@Deprecated
	public Collection<CharSequence> buildPrettyAlignments() {

		Collection<CharSequence> builders = new ArrayList<>(rows());

		int n = rows();

		List<Integer> maxima = new ArrayList<>(Collections.nCopies(n, 0));

		for (int j = 0; j < columns(); j++) {
			for (int i = 0; i < n; i++) {
				int v = maxima.get(i);
				String s = Objects.toString(get(i, j));
				int size = getPrintableLength(s);
				if (v < size) {
					maxima.set(i, size);
				}
			}
		}

		for (int j = 0; j < columns(); j++) {

			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < n; i++) {
				String s = Objects.toString(get(i,j));
				int maximum = maxima.get(i);
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

	@Override
	public FeatureModel<T> getFeatureModel() {
		return featureModel;
	}

	@Override
	public FeatureSpecification getSpecification() {
		return featureModel.getSpecification();
	}

	private static int getPrintableLength(String l) {
		int leftVisible = 0;
		for (char c : l.toCharArray()) {
			if (Character.getType(c) != Character.NON_SPACING_MARK) {
				leftVisible++;
			}
		}
		return leftVisible;
	}

	@Override
	public
	String toString() {
		return "Alignment{" + "featureModel=" + featureModel + "} " +
				super.toString();
	}
}
