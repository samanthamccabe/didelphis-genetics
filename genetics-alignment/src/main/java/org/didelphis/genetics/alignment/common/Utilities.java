package org.didelphis.genetics.alignment.common;

import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.correspondences.EnvironmentMap;
import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.io.DiskFileHandler;
import org.didelphis.io.FileHandler;
import org.didelphis.language.parsing.ParseException;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.BasicSequence;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.ColumnTable;
import org.didelphis.structures.tables.DataTable;
import org.didelphis.structures.tables.Table;
import org.didelphis.structures.tuples.Tuple;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.*;

/**
 * @author Samantha Fiona McCabe
 * Created: 6/6/2015
 */
public final class Utilities {

	//
	public static final NumberFormat FORMAT_SHORT = new DecimalFormat("0.000");
	public static final NumberFormat FORMAT_LONG = new DecimalFormat("0.00000");
	private static final transient Logger LOGGER =
			LoggerFactory.getLogger(Utilities.class);
	public static final Pattern PATTERN = Pattern.compile("\n|\r?\n");

	private Utilities() {
	}

	@NotNull
	public static ColumnTable<String> loadTable(String path) {
		return loadTable(path, Function.identity());
	}

	@NotNull
	public static ColumnTable<String> loadTable(String path,
			Function<String, String> transformer) {

		FileHandler handler = new DiskFileHandler("UTF-8");
		CharSequence chars = handler.read(path);

		List<String> lines = stream(PATTERN.split(chars))
				.collect(Collectors.toList());
		if (!lines.isEmpty()) {
			List<String> keys = asList(lines.remove(0).split("\t", -1));
			int numCol = keys.size();
			List<List<String>> table = new ArrayList<>();
			for (String line : lines) {
				String[] cells = line.split("\t", -1);
				List<String> list = asList(cells).subList(0, numCol);
				List<String> collected = list.stream()
						.map(transformer)
						.collect(Collectors.toList());
				table.add(collected);
			}
			return new DataTable<>(keys, table);
		}
		throw new ParseException("Unable to read table, file was empty", path);
	}

	/**
	 * Converts some columns from a {@link String}-based table into a {@link
	 * Sequence}-based one
	 *
	 * @param file the {@link File} to read data from
	 * @param factory a {@link SequenceFactory}
	 * @param transformer a {@link StringTransformer} to process the data and
	 * clean or apply it.
	 * @param keys the columns to be selected for conversion
	 * @return a new DataTable representing the columns with cognate data
	 */
	public static <T> ColumnTable<Sequence<T>> toPhoneticTable(
			ColumnTable<String> table,
			SequenceFactory<T> factory,
			Function<String, String> transformer,
			String... keys) {

		List<String> keyList = (keys.length >= 1)
		                       ? asList(keys)
		                       : table.getKeys();
		Collection<Integer> indices = new HashSet<>();
		int k = 0;
		for (String key : table.getKeys()) {
			if (keyList.contains(key)) {
				indices.add(k);
			}
			k++;
		}
		List<List<Sequence<T>>> lists = new ArrayList<>();
		for (int i = 0; i < table.rows(); i++) {
			List<Sequence<T>> list = new ArrayList<>();
			for (int j = 0; j < table.columns(); j++) {
				if (indices.contains(j)) {
					String word = table.get(i, j);
					String s = word == null ? "" : transformer.apply(word);
					list.add(factory.getSequence(s));
				}
			}
			lists.add(list);
		}
		return new DataTable<>(keyList, lists);
	}

	public static <T> List<Alignment<T>> toAlignments(
			Table<Sequence<T>> table, FeatureModel<T> model) {
		List<Alignment<T>> alignments = new ArrayList<>();
		for (int i = 0; i < table.rows(); i++) {
			List<Sequence<T>> row = table.getRow(i);
			alignments.add(new Alignment<>(row, model));
		}
		return alignments;
	}


	public static String formatStrings(Iterable<String> strings) {
		StringBuilder sb = new StringBuilder();
		for (String string : strings) {
			sb.append(string);
			sb.append('\t');
		}
		return sb.toString();
	}

	public static String format(Iterable<Double> weights) {
		StringBuilder sb = new StringBuilder();

		for (Double value : weights) {
			String format = FORMAT_SHORT.format(value);
			if (!format.startsWith("-")) {
				sb.append(' ').append(format);
			} else {
				sb.append(format);
			}
			sb.append(' ');
		}

		return sb.toString();
	}

	public static <T> Map<String, EnvironmentMap<T>> computeEnvironments(
			SequenceFactory<T> factory,
			ColumnTable<Sequence<T>> data) {
		Map<String, EnvironmentMap<T>> environments = new HashMap<>();
		for (String key : data.getKeys()) {
			List<Sequence<T>> column = data.getColumn(key);
			EnvironmentMap<T> env = new EnvironmentMap<>(column, factory);
			environments.put(key, env);
		}
		return environments;
	}

	public static <T> void getTupleDistances(Comparator<T, Double> comparator,
			Sequence<T> gap,
			Iterable<Tuple<Sequence<T>, Sequence<T>>> tuples,
			Table<Double> distancesRight, Table<Double> distancesLeft) {
		int i = 0;
		for (Tuple<Sequence<T>, Sequence<T>> t1 : tuples) {
			int j = 0;
			for (Tuple<Sequence<T>, Sequence<T>> t2 : tuples) {

				if (distancesLeft.get(i, j) == null) {
					Sequence<T> a = new BasicSequence<>(
							t1.getLeft()).getReverseSequence();
					Sequence<T> b = new BasicSequence<>(
							t2.getLeft()).getReverseSequence();

					double d = getD(comparator, gap, a, b);
					distancesLeft.set(i, j, d);
				}

				if (distancesRight.get(i, j) == null) {
					Sequence<T> a = t1.getRight();
					Sequence<T> b = t2.getRight();

					double d = getD(comparator, gap, a, b);
					distancesRight.set(i, j, d);
				}

				j++;
			}
			i++;
		}
	}

	private static <T> double getD(Comparator<T, Double> comparator, Sequence<T> gap, Sequence<T> a, Sequence<T> b) {
		double d = 0.0;
		if (!(a.isEmpty() || b.isEmpty())) {
			d += comparator.apply(a, b, 0,0);
		} else if (a.isEmpty() && !b.isEmpty()) {
			d += comparator.apply(gap, b, 0, 0);
		} else if (!a.isEmpty()) {
			d += comparator.apply(a, gap, 0, 0);
		}
		return d;
	}

	public static <T> Map<String, Map<Segment<T>, Integer>> computeSegmentCounts(
			ColumnTable<Sequence<T>> data) {

		Map<String, Map<Segment<T>, Integer>> map = new HashMap<>();

		for (String key : data.getKeys()) {
			Map<Segment<T>, Integer> counts = new HashMap<>();
			for (Sequence<T> sequence : data.getColumn(key)) {
				for (Segment<T> segment : sequence) {
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
