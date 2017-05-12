package org.didelphis.genetics.alignment.common;

import org.didelphis.common.io.ClassPathFileHandler;
import org.didelphis.common.io.DiskFileHandler;
import org.didelphis.common.io.FileHandler;
import org.didelphis.common.language.enums.FormatterMode;
import org.didelphis.common.language.exceptions.ParseException;
import org.didelphis.common.language.phonetic.SequenceFactory;
import org.didelphis.common.language.phonetic.model.doubles.DoubleFeatureMapping;
import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.common.language.phonetic.sequences.BasicSequence;
import org.didelphis.common.language.phonetic.sequences.Sequence;
import org.didelphis.common.structures.tables.ColumnTable;
import org.didelphis.common.structures.tables.DataTable;
import org.didelphis.common.structures.tables.Table;
import org.didelphis.common.structures.tuples.Tuple;
import org.didelphis.genetics.alignment.Expression;
import org.didelphis.genetics.alignment.correspondences.EnvironmentMap;
import org.didelphis.genetics.alignment.operators.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Samantha Fiona Morrigan McCabe
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

	public static SequenceFactory<Double> loadFactoryFromClassPath(String path,
			FormatterMode formatterMode) {
		DoubleFeatureMapping mapping =
				DoubleFeatureMapping.load(path, ClassPathFileHandler.INSTANCE,
						formatterMode);

		return new SequenceFactory<>(mapping, formatterMode);
	}

	public static ColumnTable<String> loadTableFromFile(File file)
			throws IOException {

		FileHandler diskFileHandler = new DiskFileHandler("UTF-8");
		CharSequence charSequence =
				diskFileHandler.read(file.getAbsolutePath());
//		List<String> lines = Split.splitToList(charSequence.toString(), null);

		List<String> lines = Arrays.stream(PATTERN.split(charSequence))
						.collect(Collectors.toList());

		if (!lines.isEmpty()) {
			
			String[] headers = lines.remove(0).split("\t", -1);

			int numCol = headers.length;

			Map<String, List<String>> map = new LinkedHashMap<>();
			for (String header : headers) {
				map.put(header, new ArrayList<>());
			}

			for (String line : lines) {
				String[] cells = line.split("\t", -1);

				for (int i = 0; i < numCol; i++) {
					String header = headers[i];
					String cell = cells[i];

					map.get(header).add(cell);
				}
			}
			return new DataTable<>(map);
		}
		throw new ParseException("Unable to read table, file was empty",
				file.getCanonicalPath());
	}

	/**
	 * Converts some columns from a {@link String}-based table into a {@link
	 * Sequence}-based one
	 *
	 * @param file the file to read data from
	 * @param keys the columns to be selected for conversion
	 * @param factory the factory needed to generate the {@link Sequence}s
	 * @param clex
	 *
	 * @return a new DataTable representing the columns with cognate data
	 */
	public static ColumnTable<Sequence<Double>> getPhoneticData(File file,
			Collection<String> keys, SequenceFactory<Double> factory,
			Iterable<Expression> clex) throws IOException {

		ColumnTable<String> table = loadTableFromFile(file);

		if (keys == null || keys.isEmpty()) {
			keys = table.getKeys();
		}

		Map<String, List<Sequence<Double>>> map = new LinkedHashMap<>();
		for (String key : keys) {
			Collection<String> column = table.getColumn(key);
			List<Sequence<Double>> list = new ArrayList<>(column.size());

			for (String string : column) {
				// Scrub inputs
				String s = string;
				if (clex != null) {
					for (Expression expression : clex) {
						s = expression.apply(s);
					}
				}

				// Attempt conversion
				try {
					list.add(factory.getSequence(s));
				} catch (IndexOutOfBoundsException e) {
					LOGGER.error("Failed to parse sequence {}", s, e);
					list.add(factory.getNewSequence());
				}
			}
			map.put(key, list);
		}
		return new DataTable<>(map);
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

	public static Map<String, EnvironmentMap> computeEnvironments(
			SequenceFactory<Double> factory,
			ColumnTable<Sequence<Double>> data) {
		Map<String, EnvironmentMap> environments = new HashMap<>();
		for (String key : data.getKeys()) {
			List<Sequence<Double>> column = data.getColumn(key);
			EnvironmentMap env = new EnvironmentMap(column, factory);
			environments.put(key, env);
		}
		return environments;
	}

	public static void getTupleDistances(Comparator<Segment<Double>> comparator,
			Segment<Double> gap,
			Iterable<Tuple<Sequence<Double>, Sequence<Double>>> tuples,
			Table<Double> distancesRight, Table<Double> distancesLeft) {
		int i = 0;
		for (Tuple<Sequence<Double>, Sequence<Double>> t1 : tuples) {
			int j = 0;
			for (Tuple<Sequence<Double>, Sequence<Double>> t2 : tuples) {

				if (distancesLeft.get(i, j) == null) {
					Sequence<Double> a = new BasicSequence<>(
							t1.getLeft()).getReverseSequence();
					Sequence<Double> b = new BasicSequence<>(
							t2.getLeft()).getReverseSequence();

					double d = 0.0;
					if (!a.isEmpty() && !b.isEmpty()) {
						d += comparator.apply(a.getFirst(), b.getFirst());
					} else if (a.isEmpty() && !b.isEmpty()) {
						d += comparator.apply(gap, b.getFirst());
					} else if (b.isEmpty() && !a.isEmpty()) {
						d += comparator.apply(a.getFirst(), gap);
					}
					distancesLeft.set(i, j, d);
				}

				if (distancesRight.get(i, j) == null) {
					Sequence<Double> a = t1.getRight();
					Sequence<Double> b = t2.getRight();

					double d = 0.0;
					if (!a.isEmpty() && !b.isEmpty()) {
						d += comparator.apply(a.getFirst(), b.getFirst());
					} else if (a.isEmpty() && !b.isEmpty()) {
						d += comparator.apply(gap, b.getFirst());
					} else if (b.isEmpty() && !a.isEmpty()) {
						d += comparator.apply(a.getFirst(), gap);
					}
					distancesRight.set(i, j, d);
				}

				j++;
			}
			i++;
		}
	}

	public static Map<String, Map<Segment<Double>, Integer>> computeSegmentCounts(
			ColumnTable<Sequence<Double>> data) {

		Map<String, Map<Segment<Double>, Integer>> map = new HashMap<>();

		for (String key : data.getKeys()) {
			Map<Segment<Double>, Integer> counts = new HashMap<>();
			for (Sequence<Double> sequence : data.getColumn(key)) {
				for (Segment<Double> segment : sequence) {
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
