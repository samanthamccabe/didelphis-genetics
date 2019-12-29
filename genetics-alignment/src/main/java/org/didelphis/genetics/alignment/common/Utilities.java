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
import org.didelphis.language.automata.Regex;
import org.didelphis.language.parsing.Formatter;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.parsing.ParseException;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.PhoneticSequence;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.maps.SymmetricalTwoKeyMap;
import org.didelphis.structures.tables.ColumnTable;
import org.didelphis.structures.tables.DataTable;
import org.didelphis.structures.tables.Table;
import org.didelphis.structures.tuples.Tuple;
import org.didelphis.utilities.Splitter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

@UtilityClass
public final class Utilities {

	public final NumberFormat FORMAT_SHORT = new DecimalFormat("0.000");
	public final NumberFormat FORMAT_LONG = new DecimalFormat("0.00000");

	private static final Logger LOG = LogManager.getLogger(Utilities.class);
	private static final Regex PIPE     = new Regex("\\s+\\|\\s+");
	private static final Regex NEWLINES = new Regex("\r\n|\n|\r");
	private static final Regex BLOCK    = new Regex("(\r\n\r\n)|(\n\n)|(\r\r)");
	private static final Regex COMMENT  = new Regex("%.*(\\r|\\r?\\n)");

	private final Pattern SPACE = Pattern.compile("\\s+");
	private final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.withFirstRecordAsHeader();
	private final CSVFormat TSV_FORMAT = CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter('\t');
	private final CSVFormat DSV_FORMAT = CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter('|').withQuote(null);

	@NonNull
	public ColumnTable<String> dsvToTable(@NonNull String path) {
		return loadTable(path, FormatterMode.NONE, Collections.emptyList(), DSV_FORMAT);
	}

	@NonNull
	public ColumnTable<String> csvToTable(@NonNull String path) {
		return loadTable(path, FormatterMode.NONE, Collections.emptyList(), CSV_FORMAT);
	}

	@NonNull
	public ColumnTable<String> tsvToTable(@NonNull String path) {
		return loadTable(path, FormatterMode.NONE, Collections.emptyList(), TSV_FORMAT);
	}

	@NonNull
	public ColumnTable<String> dsvToTable(@NonNull String path, Formatter form, Collection<String> keys) {
		return loadTable(path, form, keys, DSV_FORMAT);
	}

	@NonNull
	public ColumnTable<String> csvToTable(@NonNull String path, Formatter form, Collection<String> keys) {
		return loadTable(path, form, keys, CSV_FORMAT);
	}

	@NonNull
	public ColumnTable<String> tsvToTable(@NonNull String path, Formatter form, Collection<String> keys) {
		return loadTable(path, form, keys, TSV_FORMAT);
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
					Sequence<T> segments = new PhoneticSequence<>(model);
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
			Table<Sequence<T>> table,
			SequenceFactory<T> factory
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

	@NonNull
	public <T> List<List<Alignment<T>>> loadSDM(
			@NonNull String fileData,
			@NonNull SequenceFactory<T> factory
	) {
		List<List<Alignment<T>>> alignmentSet = new ArrayList<>();

		int size = 0;

		// Each block should represent a single alignment or set of equivalent
		// alignments:
		// 0 0 0   0 0 0
		// a a b | a a b
		// a _ b | _ a b
		// These are not *necessarily* equivalent in all cases - under a global
		// alignment they could be equivalent, but would not be so under a
		// local alignment where gaps at the beginning and end of a sequence do
		// not incur a cost
		for (String block : BLOCK.split(fileData)) {
			block = COMMENT.replace(block,"$1").trim();
//			block = block.replaceAll("%.*(\n|\n?\n)","");

			if (block.isEmpty()) continue;

			// The first block should contain the language headers and however
			// many headers there are should be set as the correct number later.
			// Any subsequence block with too many or too few headers will be
			// skipped and logged.
			if (size == 0) {
				size = NEWLINES.split(block).size();
				continue;
			}

			int blockWidth = 0;
			List<List<String>> lists = new ArrayList<>();
			for (String line : NEWLINES.split(block)) {
				List<String> list = PIPE.split(line);
				if (blockWidth == 0) {
					blockWidth = list.size();
				}
				lists.add(list);
			}

			List<Alignment<T>> alignments = new ArrayList<>();
			for (int i = 0; i < blockWidth; i++) {
				Collection<String> strings = new ArrayList<>();

				// TODO: add annotations to the alignment
				String annotations = lists.get(0).get(i).trim();

				for (int j = 1; j <  lists.size(); j++) {
					String item = lists.get(j).get(i).trim();
					if (item.startsWith("#")) {
						strings.add(item);
					} else {
						strings.add("# " + item);
					}
				}
				alignments.add(toAlignment(strings, factory));
			}
			alignmentSet.add(alignments);
		}
		return alignmentSet;
	}

	public static @NonNull <T> Alignment<T> toAlignment(
			@NonNull Iterable<String> list,
			@NonNull SequenceFactory<T> factory
	) {
		FeatureModel<T> model = factory.getFeatureMapping().getFeatureModel();
		List<Sequence<T>> sequences = toSequences(list, factory);
		return new Alignment<>(sequences, model);
	}

	public static @NonNull <T> List<Sequence<T>> toSequences(
			@NonNull Iterable<String> list,
			@NonNull SequenceFactory<T> factory
	) {
		List<Sequence<T>> sequences = new ArrayList<>();
		FeatureModel<T> model = factory.getFeatureMapping().getFeatureModel();
		for (String string : list) {
			Sequence<T> sequence = new PhoneticSequence<>(model);
			for (String s : string.split("\\s+")) {
				sequence.add(factory.toSegment(s));
			}
			sequences.add(sequence);
		}
		return sequences;
	}

	@NonNull
	private static ColumnTable<String> loadTable(
			@NonNull String path,
			@NonNull Formatter norm,
			@NonNull Collection<String> keys,
			@NonNull CSVFormat format
	) {
		FileHandler handler = new DiskFileHandler("UTF-8");
		String payload;
		try {
			payload = handler.read(path);
		} catch (IOException e) {
			throw new ParseException("Unable to read table", e);
		}
		List<List<String>> table = new ArrayList<>();
		try (CSVParser parser = CSVParser.parse(payload, format)){
			List<String> headerNames = parser.getHeaderNames();
			List<Integer> collect = keys.stream()
					.filter(headerNames::contains)
					.map(headerNames::indexOf)
					.collect(Collectors.toList());
			for (CSVRecord csvRecord : parser) {
				List<String> row = new ArrayList<>();
				for (int i = 0; i < csvRecord.size(); i++) {
					if (keys.isEmpty() || collect.contains(i)) {
						String entry = csvRecord.get(i);
						row.add(norm.normalize(entry));
					}
				}
				table.add(row);
			}
			List<String> strings = new ArrayList<>(headerNames);
			strings.retainAll(keys);
			return new DataTable<>(strings, table);
		} catch (IOException e) {
			LOG.error("Failed to read CSV from payload", e);
		}
		throw new ParseException("Unable to read table");
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
}
