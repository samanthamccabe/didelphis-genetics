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

package org.didelphis.genetics.alignment;

import lombok.NonNull;
import org.didelphis.language.phonetic.ModelBearer;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.model.FeatureSpecification;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.RectangularTable;
import org.didelphis.utilities.Templates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by samantha on 1/9/17.
 */
public final class  Alignment<T> extends RectangularTable<Segment<T>>
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
				String message =  Templates.create().add(
						"Sequence {} in {} is not the correct number",
						"of elements: {} vs {}"
				).with(sequence, list, sequence.size(), columns()).build();
				throw new IllegalArgumentException(message);
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

	@NonNull
	@Override
	public String toString() {
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
