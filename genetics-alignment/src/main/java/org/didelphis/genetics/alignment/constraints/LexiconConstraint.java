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

package org.didelphis.genetics.alignment.constraints;

import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.AlignmentSet;
import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.FeatureArray;
import org.didelphis.language.phonetic.model.FeatureMapping;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.segments.StandardSegment;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.ColumnTable;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class LexiconConstraint<T> implements Constraint<T> {

	private static final Pattern EXTENSION = Pattern.compile("\\.[^.]*");

	private final SequenceFactory<T> factory;
	private final ColumnTable<Sequence<T>> data;
	private final List<Alignment<T>> standard;
	private final String name;
	private final double strength;

	public LexiconConstraint(
			String name,
			SequenceFactory<T> factory,
			ColumnTable<Sequence<T>> dataParam,
			List<Alignment<T>> standard
	) {
		this.name = name;
		this.factory = factory;
		this.standard = standard;
		data = dataParam;
		strength = computeStrength();
	}

	public static <T> Constraint<T> loadFromPaths(
			String humanPath,
			String lexiconPath,
			SequenceFactory<T> factory,
			String... keys
	) throws IOException {
		AlignmentSet<T> alignments = AlignmentSet.loadFromFile(
				humanPath,
				factory
		);
		//
		//		ColumnTable<Sequence<T>> dataTable =
		//				Utilities.toPhoneticTable(lexiconPath, factory, new StringTransformer(),
		//						keys);

		String path = EXTENSION.matcher(lexiconPath).replaceAll("");
		return new LexiconConstraint<T>(path, factory, null, alignments);
	}

	@Override
	public double evaluate(AlignmentAlgorithm<T> algorithm) {

		Alignment<T> blank = new Alignment<>(factory.getFeatureMapping()
				.getFeatureModel());
		//		List<Alignment<T>> alignments = algorithm.getAlignments(data);
		//		Alignment<T> alignments = algorithm.apply();

		double sum = 0.0;

		//		int size = alignments.size();
		//		for (int i = 0; i < size; i++) {
		//			Alignment<T> a = alignments.get(i);
		//			Alignment<T> s = standard.get(i);
		//
		//			if (s.columns() != 0) {
		//				boolean equals = a.equals(s) && !a.equals(blank);
		//				sum += equals ? 1 : 0;
		//			}
		//		}

		return sum;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public double getStrength() {
		return strength;
	}

	@Override
	public String toString() {
		return "LexiconConstraint{" + "factory=" + factory + ", data=" + data +
				", standard=" + standard + ", name='" + name + '\'' +
				", strength=" + strength + '}';
	}

	private int computeStrength() {
		//noinspection NumericCastThatLosesPrecision
		return (int) standard.stream()
				.filter(lists -> lists.columns() > 0)
				.count();
	}

	private Sequence<T> normalizeSymbols(
			Iterable<Segment<T>> sequence
	) {
		FeatureMapping<T> featureMapping = factory.getFeatureMapping();

		Sequence<T> normalized = factory.toSequence("");
		for (Segment<T> segment : sequence) {
			FeatureArray<T> features = segment.getFeatures();
			String symbol = featureMapping.findBestSymbol(features);
			normalized.add(new StandardSegment<>(symbol, features));
		}
		return normalized;
	}
}
