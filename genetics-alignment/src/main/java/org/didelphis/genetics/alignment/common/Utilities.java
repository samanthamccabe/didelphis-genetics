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

package org.didelphis.genetics.alignment.common;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.correspondences.EnvironmentMap;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.comparators.BrownEtAlComparator;
import org.didelphis.io.DiskFileHandler;
import org.didelphis.io.FileHandler;
import org.didelphis.language.parsing.ParseException;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.BasicSequence;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.maps.SymmetricalTwoKeyMap;
import org.didelphis.structures.tables.ColumnTable;
import org.didelphis.structures.tables.DataTable;
import org.didelphis.structures.tables.Table;
import org.didelphis.structures.tuples.Tuple;
import org.didelphis.utilities.Splitter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

/**
 * @author Samantha Fiona McCabe Created: 6/6/2015
 */
@UtilityClass
public final class Utilities {

	private final Pattern SPACE = Pattern.compile("\\s+");
	private final Pattern PATTERN = Pattern.compile("\n|\r?\n");

	public final NumberFormat FORMAT_SHORT = new DecimalFormat("0.000");
	public final NumberFormat FORMAT_LONG = new DecimalFormat("0.00000");

	@NonNull
	public ColumnTable<String> loadTable(String path) {
		return loadTable(path, Function.identity());
	}

	@NonNull
	public ColumnTable<String> toTable(
			CharSequence payload, Function<? super String, String> transformer
	) throws ParseException {
		List<String> lines
				= stream(PATTERN.split(payload)).collect(Collectors.toList());
		if (!lines.isEmpty()) {
			List<String> keys = asList(lines.remove(0).split("\t", -1));
			int numCol = keys.size();
			List<List<String>> table = new ArrayList<>();
			for (String line: lines) {
				String[] cells = line.split("\t", -1);
				List<String> list = asList(cells).subList(0, numCol);
				List<String> collected = list.stream()
						.map(transformer)
						.collect(Collectors.toList());
				table.add(collected);
			}
			return new DataTable<>(keys, table);
		}
		throw new ParseException("Unable to read table, payload was empty"+
				payload);
	}

	@NonNull
	public ColumnTable<String> loadTable(
			String path, Function<String, String> transformer
	) {
		FileHandler handler = new DiskFileHandler("UTF-8");
		CharSequence chars = null;
		try {
			chars = handler.read(path);
		} catch (IOException e) {
			throw new ParseException("Unable to read table", e);
		}
		if (chars.length() == 0) {
			throw new ParseException("Unable to read table, file was empty: " + path);
		} else {
			return toTable(chars, transformer);
		}
	}

	public <T> ColumnTable<Sequence<T>> toPhoneticTable(
			ColumnTable<String> table,
			SequenceFactory<T> factory,
			Function<String, String> transformer
	) {
		return toPhoneticTable(table,
				factory,
				transformer,
				Collections.emptyList()
		);
	}

	/**
	 * Converts some columns from a {@link String}-based table into a {@link
	 * Sequence}-based one
	 *
	 * @param factory a {@link SequenceFactory}
	 * @param transformer a {@link StringTransformer} to process the data and
	 * 		clean or apply it.
	 * @param keys the columns to be selected for conversion
	 *
	 * @return a new DataTable representing the columns with cognate data
	 */
	public <T> ColumnTable<Sequence<T>> toPhoneticTable(
			ColumnTable<String> table,
			SequenceFactory<T> factory,
			Function<String, String> transformer,
			List<String> keys
	) {
		List<String> keyList = (keys.isEmpty()) ? table.getKeys() : keys;
		Collection<Integer> indices = new HashSet<>();
		int k = 0;
		for (String key: table.getKeys()) {
			if (keyList.contains(key)) {
				indices.add(k);
			}
			k++;
		}

		FeatureModel<T> model = factory.getFeatureMapping().getFeatureModel();
		List<List<Sequence<T>>> lists = new ArrayList<>();
		for (int i = 0; i < table.rows(); i++) {
			List<Sequence<T>> list = new ArrayList<>();
			for (int j = 0; j < table.columns(); j++) {
				if (indices.contains(j)) {
					String word = table.get(i, j);
					String s = transformer.apply(word);
					Sequence<T> segments = new BasicSequence<>(model);
					for (String s1: SPACE.split(s)) {
						segments.add(factory.toSequence(s1));
					}
					list.add(segments);
				}
			}
			lists.add(list);
		}
		return new DataTable<>(keyList, lists);
	}

	public <T> SequenceComparator<T> loadMatrixComparator(
			FileHandler handler,
			SequenceFactory<T> factory,
			Function<String, String> transformer,
			String matrixPath
	) throws IOException {
		SymmetricalTwoKeyMap<Segment<T>, Double> map
				= new SymmetricalTwoKeyMap<>();
		String lines = handler.read(matrixPath);
		
		for (String line: Splitter.lines(lines)) {
			String[] matcher = line.split("\t");
			String s1 = matcher[0];
			String s2 = matcher[1];
			map.put(
					factory.toSegment(s1 == null ? "" : transformer.apply(s1)),
					factory.toSegment(s2 == null ? "" : transformer.apply(s2)),
					Double.parseDouble(matcher[2])
			);
		}
		return new BrownEtAlComparator<>(map);
	}

	public <T> List<Alignment<T>> toAlignments(
			Table<Sequence<T>> table, SequenceFactory<T> factory
	) {
		FeatureModel<T> featureModel = factory.getFeatureMapping()
				.getFeatureModel();
		List<Alignment<T>> alignments = new ArrayList<>();
		for (int i = 0; i < table.rows(); i++) {
			List<Sequence<T>> row = table.getRow(i);
			alignments.add(new Alignment<>(row, featureModel));
		}
		return alignments;
	}

	public String formatStrings(Iterable<String> strings) {
		StringBuilder sb = new StringBuilder();
		for (String string: strings) {
			sb.append(string);
			sb.append('\t');
		}
		return sb.toString();
	}

	public String format(Iterable<Double> weights) {
		StringBuilder sb = new StringBuilder();

		for (Double value: weights) {
			String format = FORMAT_SHORT.format(value);
			if (format.startsWith("-")) {
				sb.append(format);
			} else {
				sb.append(' ').append(format);
			}
			sb.append(' ');
		}

		return sb.toString();
	}

	public <T> Map<String, EnvironmentMap<T>> computeEnvironments(
			SequenceFactory<T> factory, ColumnTable<Sequence<T>> data
	) {
		Map<String, EnvironmentMap<T>> environments = new HashMap<>();
		for (String key: data.getKeys()) {
			List<Sequence<T>> column = data.getColumn(key);
			EnvironmentMap<T> env = new EnvironmentMap<>(column, factory);
			environments.put(key, env);
		}
		return environments;
	}

	public <T> void getTupleDistances(
			SequenceComparator<T> comparator,
			Sequence<T> gap,
			Iterable<Tuple<Sequence<T>, Sequence<T>>> tuples,
			Table<Double> distancesRight,
			Table<Double> distancesLeft
	) {
		int i = 0;
		for (Tuple<Sequence<T>, Sequence<T>> t1: tuples) {
			int j = 0;
			for (Tuple<Sequence<T>, Sequence<T>> t2: tuples) {
				j++;
			}
			i++;
		}
	}

	private <T> double getD(
			SequenceComparator<T> comparator,
			Sequence<T> gap,
			Sequence<T> q1,
			Sequence<T> q2
	) {
		double d = 0.0;
		if (!(q1.isEmpty() || q2.isEmpty())) {
			d += comparator.apply(q1, q2, 0, 0);
		} else if (q1.isEmpty() && !q2.isEmpty()) {
			d += comparator.apply(gap, q2, 0, 0);
		} else if (!q1.isEmpty()) {
			d += comparator.apply(q1, gap, 0, 0);
		}
		return d;
	}

	public <T> Map<String, Map<Segment<T>, Integer>> computeSegmentCounts(
			ColumnTable<Sequence<T>> data
	) {

		Map<String, Map<Segment<T>, Integer>> map = new HashMap<>();

		for (String key: data.getKeys()) {
			Map<Segment<T>, Integer> counts = new HashMap<>();
			for (Sequence<T> sequence: data.getColumn(key)) {
				for (Segment<T> segment: sequence) {
					if (counts.containsKey(segment)) {
						Integer integer = counts.get(segment);
						counts.put(segment, integer + 1);
					} else {
						counts.put(segment, 1);
					}
				}
			}
			map.put(key, counts);
		}

		return map;
	}
}
