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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.analysis.UnmappedSymbolFinder;
import org.didelphis.genetics.alignment.common.StringTransformer;
import org.didelphis.genetics.alignment.common.Utilities;
import org.didelphis.genetics.alignment.configuration.AlgorithmConfig;
import org.didelphis.genetics.alignment.configuration.ConfigObject;
import org.didelphis.genetics.alignment.configuration.DataFile;
import org.didelphis.io.DiskFileHandler;
import org.didelphis.io.FileHandler;
import org.didelphis.language.parsing.Formatter;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.model.FeatureModelLoader;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.BasicSequence;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.graph.Arc;
import org.didelphis.structures.graph.Graph;
import org.didelphis.structures.graph.GraphUtils;
import org.didelphis.structures.maps.GeneralTwoKeyMap;
import org.didelphis.structures.maps.GeneralTwoKeyMultiMap;
import org.didelphis.structures.maps.interfaces.TwoKeyMap;
import org.didelphis.structures.maps.interfaces.TwoKeyMultiMap;
import org.didelphis.structures.tables.ColumnTable;
import org.didelphis.structures.tables.RectangularTable;
import org.didelphis.structures.tables.Table;
import org.didelphis.structures.tuples.Triple;
import org.didelphis.structures.tuples.Tuple;
import org.didelphis.structures.tuples.Twin;
import org.didelphis.utilities.Logger;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.regex.Pattern.*;

@ToString
@EqualsAndHashCode
public final class Processor<T> {

	private static final Logger LOG = Logger.create(Processor.class);

	private static final DecimalFormat QUAD = new DecimalFormat("0000");
	private static final DecimalFormat DECIMAL = new DecimalFormat("0.00");


	private static final FileHandler HANDLER = new DiskFileHandler("UTF-8");
	private static final Pattern     HYPHEN  = compile("-");
	private static final Pattern     HASH    = compile("#", LITERAL);

	@Getter
	private final AlignmentAlgorithm<T>    algorithm;
	private final Function<String, String> transformer;

	private final FormatterMode mode;
	private final ConfigObject  dataConfig;
	private final String        gapSymbol;
	private final int           qQuantiles;

	public Processor(
			int qQuantiles,
			FeatureType<T> type,
			FormatterMode mode,
			ConfigObject dataConfig,
			AlgorithmConfig algorithmConfig
	) {
		this.qQuantiles = qQuantiles;
		this.mode = mode;
		this.dataConfig = dataConfig;

		List<List<String>> tfList = dataConfig.getTransformations();
		transformer = new StringTransformer(tfList, mode);
		algorithm = buildAlgorithm(type, algorithmConfig);
		gapSymbol = algorithmConfig.getGapSymbol();
	}

	public void process() {
		SequenceFactory<T> factory = algorithm.getFactory();
		File outputFolder = new File(dataConfig.getDestinationPath());
		for (DataFile dataFile : dataConfig.getFiles()) {

			Map<String, String> displayNames = dataFile.getDisplayNames();

			boolean createDistanceTable;

			// Set up which languages to process
			Collection<String> allKeys = new HashSet<>();
			List<List<String>> fileKeys = dataFile.getKeys();
			if (fileKeys.isEmpty()) {
				// If nothing is specified, each will be compared to each other
				List<String> list = new ArrayList<>(displayNames.keySet());
				for (int i = 1; i < list.size(); i++) {
					for (int j = 0; j < i; j++) {
						fileKeys.add(new Twin<>(list.get(i), list.get(j)));
					}
				}
				allKeys.addAll(displayNames.keySet());
				createDistanceTable = true;
			} else {
				fileKeys.forEach(allKeys::addAll);
				createDistanceTable = false;
			}

			ColumnTable<String> table = parseDataFile(dataFile, mode, allKeys);

			if (table == null) return;

			ColumnTable<Sequence<T>> data = Utilities.toPhoneticTable(
					table,
					factory,
					transformer
			);

			for (String headerKey : data.getKeys()) {
				UnmappedSymbolFinder<T> finder = new UnmappedSymbolFinder<>(gapSymbol, factory, true);
				List<Sequence<T>> column = data.getColumn(headerKey);
				finder.countInSequences(column);
				System.out.println(displayNames.get(headerKey));
				finder.write(System.out);
			}


			List<String> langKeys = new ArrayList<>(displayNames.keySet());

			RectangularTable<Double> scores = new RectangularTable<>(
					0.0,
					langKeys.size(),
					langKeys.size()
			);

			for (List<String> keys : fileKeys) {
				Collection<AlignmentResult<T>> results = align(algorithm,
						keys,
						data
				);

				// Put the score in the table
				if (createDistanceTable) {
					double sum = results.stream()
							.mapToDouble(AlignmentResult::getScore)
							.sum();
					double score = sum / results.size();
					String leftKey = keys.get(0);
					String rightKey = keys.get(1);
					int lIndex = langKeys.indexOf(leftKey);
					int rIndex = langKeys.indexOf(rightKey);
					scores.set(lIndex, rIndex, score);
				}

				writeResults(outputFolder, keys, results);

				List<?> something = processResults(keys, results);
			}

			List<String> collect = langKeys.stream()
					.map(displayNames::get)
					.collect(Collectors.toList());

			String displayTable = formatDistanceTable(collect, scores);

			int x = 0;
		}
	}

	private List<?> processResults(
			List<String> keys,
			Iterable<AlignmentResult<T>> results
	) {
		TwoKeyMap<Segment<T>, Segment<T>, Double> countMap = getCounts(results);
		TwoKeyMap<Segment<T>, Segment<T>, Double> freqMap = toFrequency(countMap);

		TwoKeyMultiMap<Segment<T>, Segment<T>, Alignment<T>> corrMap = buildCorrespondences(results);

		double firstQuantile = computeQuantile(freqMap, qQuantiles);

		String leftKey = keys.get(0);
		String rightKey = keys.get(1);

		String toGml = toGml(leftKey, rightKey, freqMap, firstQuantile);

		String collect = countMap.stream().map(triple -> {
			String key1 = triple.first().toString();
			String key2 = triple.second().toString();
			Double third = triple.third();
			return key1 + "\t" + key2 + "\t" + third;
		}).collect(Collectors.joining("\n"));

		return Collections.emptyList();
	}

	@NonNull
	private static <T> TwoKeyMap<Segment<T>, Segment<T>, Double>
	getCounts(Iterable<AlignmentResult<T>> results) {
		TwoKeyMap<Segment<T>, Segment<T>, Double> countMap =
				new GeneralTwoKeyMap<>(TreeMap.class);
		for (AlignmentResult<T> result : results) {
			List<Alignment<T>> alignments = result.getAlignments();
			int num = alignments.size();
			for (Alignment<T> alignment : alignments) {
				for (int i = 0; i < alignment.columns(); i++) {
					List<Segment<T>> column = alignment.getColumn(i);
					Segment<T> left = column.get(0);
					Segment<T> right = column.get(1);
					if (countMap.contains(left, right)) {
						double value = countMap.getOrDefault(left, right, 0.0);
						countMap.put(left, right, value + (1.0 / num));
					} else {
						countMap.put(left, right, (1.0 / num));
					}
				}
			}
		}
		return countMap;
	}

	@NonNull
	private static <T> TwoKeyMultiMap<Segment<T>, Segment<T>, Alignment<T>>
	buildCorrespondences(Iterable<AlignmentResult<T>> results) {
		TwoKeyMultiMap<Segment<T>, Segment<T>, Alignment<T>> corrMap =
				new GeneralTwoKeyMultiMap<>(TreeMap.class, ArrayList.class);
		for (AlignmentResult<T> result : results) {
			for (Alignment<T> alignment : result.getAlignments()) {
				for (int i = 0; i < alignment.columns(); i++) {
					List<Segment<T>> column = alignment.getColumn(i);
					Segment<T> left = column.get(0);
					Segment<T> right = column.get(1);
					corrMap.add(left, right, alignment);
				}
			}
		}
		return corrMap;
	}

	@NonNull
	private static <T> TwoKeyMap<Segment<T>, Segment<T>, Double> toFrequency(
			TwoKeyMap<Segment<T>, Segment<T>, Double> cMap
	) {
		double sum = cMap.stream().mapToDouble(Triple::third).sum();

		TwoKeyMap<Segment<T>, Segment<T>, Double> fMap =
				new GeneralTwoKeyMap<>(TreeMap.class);

		for (Triple<Segment<T>, Segment<T>, Double> t : cMap) {
			fMap.put(t.first(), t.second(), -1 * Math.log(t.third() / sum));
		}
		return fMap;
	}

	private static <T> double computeQuantile(
			TwoKeyMap<Segment<T>, Segment<T>, Double> fMap, int q) {
		double max = fMap.stream()
				.mapToDouble(Triple::third)
				.max()
				.orElse(0.0);

		double min = fMap.stream()
				.mapToDouble(Triple::third)
				.min()
				.orElse(0.0);

		return (q == 0)
				? 0.0
				: max - (max - min) / q;
	}

	private String toGml(
			String leftKey,
			String rightKey,
			TwoKeyMap<Segment<T>, Segment<T>, Double> freqMap,
			double nTile
	) {
		Graph<Double> graph = new Graph<>();
		for (Triple<Segment<T>, Segment<T>, Double> triple : freqMap) {
			String left = triple.first().toString();
			String right = triple.second().toString();
			Double count = triple.third();

			// Use greater than because these are inverse log values
			if (count > nTile) continue;

			String leftValue = escape(leftKey + " [" + left + "]");
			String rightValue = escape(rightKey + " [" + right + "]");
			graph.add(leftValue, new DoubleArc(count), rightValue);
		}
		return GraphUtils.graphToGML(graph, true);
	}

	private static String formatDistanceTable(List<String> labels, Table<Double> table) {
		StringBuilder sb = new StringBuilder();

		int size;
		if (labels.size() == table.rows()) {
			size = labels.size();
		} else {
			LOG.error("Number of labels ({}) does not match the number of " +
					"rows ({}); proceeding with the smaller value.",
					labels.size(), table.rows());
			size = Math.min(labels.size(), table.rows());
		}


		for (int i = 0; i < labels.size() - 1; i++) {
			String label = labels.get(i);
			sb.append("\t");
			sb.append(label);
		}
		sb.append("\n");

		for (int i = 1; i < size; i++) {
			sb.append(labels.get(i));
			for (double value : table.getRow(i)) {
				sb.append("\t");
				if (value != 0.0) {
					sb.append(DECIMAL.format(value));
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	private static String escape(String string) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < string.length(); i++) {
			int codePoint = Character.codePointAt(string, i);
			if (128 < codePoint) {
				sb.append("&#");
				sb.append(QUAD.format(codePoint));
				sb.append(";");
			} else {
				sb.append(string.charAt(i));
			}
		}
		return sb.toString();
	}

	@NonNull
	private static <T> AlignmentAlgorithm<T> buildAlgorithm(
			FeatureType<T> type,
			AlgorithmConfig config
	) {
		FeatureModelLoader<T> loader = new FeatureModelLoader<>(
				type,
				HANDLER,
				config.getModelPath()
		);
		SequenceFactory<T> factory = new SequenceFactory<>(
				loader.getFeatureMapping(),
				FormatterMode.INTELLIGENT
		);
		return config.buildAlgorithm(factory);
	}

	@Nullable
	private static ColumnTable<String> parseDataFile(
			@NonNull DataFile data,
			Formatter form,
			Collection<String> allKeys
	) {
		String path = data.getPath();
		String format = data.getType();
		ColumnTable<String> table;
		if (format.equals("csv")) {
			table = Utilities.csvToTable(path, form, allKeys);
		} else if (format.equals("tsv")) {
			table = Utilities.tsvToTable(path, form, allKeys);
		} else if (format.equals("dsv")) {
			table = Utilities.dsvToTable(path, form, allKeys);
		} else {
			LOG.error("Unsupported file type {}", format);
			return null;
		}
		return table;
	}

	private static <T> void writeResults(
			File outputFolder,
			List<String> keys,
			Iterable<AlignmentResult<T>> results
	) {

		if (!outputFolder.exists()) {
			boolean failed = !outputFolder.mkdirs();
			if (failed) {
				LOG.error("Failed to create folder {}", outputFolder);
			}
		}

		DecimalFormat decimalFormat = new DecimalFormat("0.00");
		String fileName = String.join("-", keys) + ".sdm";
		try(BufferedWriter writer = openWriter(outputFolder, fileName)) {

			writer.write("%% "+fileName+"\n");
			writer.write(keys.get(0) + "\n");
			writer.write(keys.get(1) + "\n");
			writer.write("\n");

			for (AlignmentResult<T> result : results) {
				Collection<String> leftList = new ArrayList<>();
				Collection<String> rightList = new ArrayList<>();
				for (Alignment<T> alignment : result.getAlignments()) {
					List<String> charSequences = Alignment.buildPrettyAlignments(alignment);
					leftList.add(charSequences.get(0));
					rightList.add(charSequences.get(1));
				}

				String leftGroup = String.join(" | ", leftList);
				String rightGroup = String.join(" | ", rightList);

				Sequence<T> left  = result.getLeft();
				Sequence<T> right = result.getRight();

				writer.write(leftGroup + "\n");
				writer.write(rightGroup + "\n");
				writer.write("\n");
				writer.flush();

				// Write alignment table in SDM comment block
				String collect1 = right.stream()
						.map(Segment::getSymbol)
						.map((String s) -> pad(s, 5))
						.collect(Collectors.joining(" "));
				writer.write("%      " + collect1 + "\n");
				Table<Double> table = result.getTable();
				for (int i = 0; i < table.rows(); i++) {
					writer.write("% ");
					writer.write(pad(left.get(i).getSymbol(), -5));
					List<Double> row = table.getRow(i);
					String collect = row.stream()
							.map(decimalFormat::format)
							.map((String string) -> pad(string, 5))
							.collect(Collectors.joining(" "));
					writer.write(collect);
					writer.write("\n");
				}
				writer.write("\n");
				writer.write("\n");
				writer.flush();
			}
		} catch (IOException e) {
			LOG.error("Unable to write outputs", e);
		}
	}


	@NonNull
	private static BufferedWriter openWriter(File outputFolder, String fileName)
			throws IOException {
		return new BufferedWriter(new FileWriter(new File(outputFolder, fileName)));
	}

	@NonNull
	private static String pad(String string, int width) {
		StringBuilder stringBuilder = new StringBuilder(string);
		if (width < 0) {
			while (stringBuilder.length() < -width) {
				stringBuilder.append(" ");
			}
		} else {
			while (stringBuilder.length() < width) {
				stringBuilder.insert(0, " ");
			}
		}
		return stringBuilder.toString();
	}



	private static <T> void writeAlignments(String rootPath,
			Iterable<Tuple<String, Collection<AlignmentResult<T>>>> alignments) {
		for (Tuple<String, Collection<AlignmentResult<T>>> entry : alignments) {
			String key = entry.getLeft();
			StringBuilder sb1 = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			sb1.append(HYPHEN.matcher(key).replaceAll("\t"));
			sb1.append('\n');
			for (AlignmentResult<T> result : entry.getRight()) {
				Iterator<Alignment<T>> list = result.getAlignments().iterator();
				List<String> charSequences = list.hasNext()
						? Alignment.buildPrettyAlignments(list.next())
						: Collections.emptyList();
				for (CharSequence sequence : charSequences) {
					String normal = Normalizer.normalize(sequence, Normalizer.Form.NFC);
					String str = HASH.matcher(normal)
							.replaceAll("").trim();
					sb1.append(str);
					sb1.append('\t');
				}
				String stackedSequences = charSequences.stream()
						.map(q -> Normalizer.normalize(q, Normalizer.Form.NFC))
						.map(q -> HASH.matcher(q).replaceAll("").trim())
						.collect(Collectors.joining("\r", "\"", "\""));
				sb1.append(stackedSequences);
				sb1.append('\n');

				//				ObjectNode node = new ObjectNode(OM.getNodeFactory());
				//				node.put("left",result.getLeft().toString());
				//				node.put("right",result.getRight().toString());
				//				List<Object> objects = new ArrayList<>();
				//				for (Alignment<T> alignment : result.getAlignments()) {
				//					objects.add(alignment.getPrettyTable().split("\n"));
				//				}
				//				List<Object> table = new ArrayList<>();
				//				Iterator<Collection<Double>> it = result.getTable().rowIterator();
				//				while (it.hasNext()) {
				//					table.add(it.next());
				//				}
				//				node.putPOJO("alignments",objects);
				//				node.putPOJO("table", table);
				//				try {
				//					String value = OM
				//							.writerWithDefaultPrettyPrinter()
				//							.writeValueAsString(node);
				//					sb2.append(value);
				//				} catch (JsonProcessingException e) {
				//					LOG.error("{}", e);
				//				}
			}

			File file1 = new File(rootPath + "alignments_" + key + ".csv");
			//			File file2 = new File(rootPath + "alignments_" + key + ".json");
			Path path = file1.toPath();
			try {
				Files.createDirectories(path.getParent());
			} catch (IOException e) {
				LOG.error("{}", e);
			}

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file1))) {
				writer.write(sb1.toString());
			} catch (IOException e) {
				LOG.error("{}", e);
			}

			//			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file2))) {
			//				writer.write(sb2.toString());
			//			} catch (IOException e) {
			//				LOG.error("{}", e);
			//			}
		}
	}

	@NonNull
	private static <T> Collection<AlignmentResult<T>> align(
			@NonNull AlignmentAlgorithm<T> algorithm,
			@NonNull List<String> keyList,
			@NonNull ColumnTable<Sequence<T>> data
	) {
		String k1 = keyList.get(0);
		String k2 = keyList.get(1);

		List<Sequence<T>> d1 = data.getColumn(k1);
		List<Sequence<T>> d2 = data.getColumn(k2);

		Collection<AlignmentResult<T>> alignments = new ArrayList<>();
		Iterator<Sequence<T>> it1 = d1.iterator();
		Iterator<Sequence<T>> it2 = d2.iterator();
		while (it1.hasNext() && it2.hasNext()) {
			Sequence<T> e1 = it1.next();
			Sequence<T> e2 = it2.next();

			if (e1.isEmpty() || e2.isEmpty()) {
				continue;
			}

			AlignmentResult<T> result = algorithm.apply(e1, e2);
			alignments.add(result);
		}
		return alignments;
	}

	private static <E> void add(TwoKeyMap<E, E, Integer> map, E s1, E s2) {
		if (map.contains(s1, s2)) {
			Integer value = map.get(s1, s2);
			if (value != null) {
				map.put(s1, s2, value + 1);
				return;
			}
		}
		map.put(s1, s2, 1);
	}

	private static <T> Sequence<T> lookBack(List<Segment<T>> segments, int i, Segment<T> gap) {
		List<Segment<T>> collect = segments.subList(0, i)
				.stream()
				.filter(segment -> !segment.equals(gap))
				.collect(Collectors.toList());
		return new BasicSequence<>(collect, gap.getFeatureModel());
	}

	private static <T> Sequence<T> lookForward(List<Segment<T>> segments, int i, Segment<T> gap) {
		List<Segment<T>> collect = segments.subList(i+1, segments.size())
				.stream()
				.filter(segment -> !segment.equals(gap))
				.collect(Collectors.toList());
		return new BasicSequence<>(collect, gap.getFeatureModel());
	}

	public static class DoubleArc implements Arc<Double> {


		private final DecimalFormat format = new DecimalFormat("0.000");
		private final Double value;

		public DoubleArc(Double value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return format.format(value);
		}

		@Override
		public int match(Double sequence, int index) {
			return 0;
		}
	}
}
