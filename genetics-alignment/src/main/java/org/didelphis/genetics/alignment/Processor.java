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
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.graph.Arc;
import org.didelphis.structures.graph.Graph;
import org.didelphis.structures.graph.GraphUtils;
import org.didelphis.structures.maps.GeneralMultiMap;
import org.didelphis.structures.maps.GeneralTwoKeyMap;
import org.didelphis.structures.maps.GeneralTwoKeyMultiMap;
import org.didelphis.structures.maps.interfaces.MultiMap;
import org.didelphis.structures.maps.interfaces.TwoKeyMap;
import org.didelphis.structures.maps.interfaces.TwoKeyMultiMap;
import org.didelphis.structures.tables.ColumnTable;
import org.didelphis.structures.tables.RectangularTable;
import org.didelphis.structures.tables.Table;
import org.didelphis.structures.tuples.Triple;
import org.didelphis.structures.tuples.Tuple;
import org.didelphis.structures.tuples.Twin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode
public final class Processor<T> {

	private static final Logger LOG = LogManager.getLogger(Processor.class);

	private static final DecimalFormat QUAD    = new DecimalFormat("0000");
	private static final DecimalFormat DECIMAL = new DecimalFormat("0.00");
	private static final FileHandler   HANDLER = new DiskFileHandler("UTF-8");

	private static final BiFunction<Segment<?>, Double, Double> UPDATE
			= (key, v) -> (v == null) ? 1 : v + 1;

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
		this.mode = mode;
		this.qQuantiles = qQuantiles;
		this.dataConfig = dataConfig;

		List<List<String>> tfList = dataConfig.getTransformations();
		transformer = new StringTransformer(tfList, mode);
		algorithm = buildAlgorithm(type, algorithmConfig);
		gapSymbol = algorithmConfig.getGapSymbol();
	}

	public void process() {
		SequenceFactory<T> factory = algorithm.getFactory();
		String destination = dataConfig.getDestinationPath();
		for (DataFile dataFile : dataConfig.getFiles()) {

			String outPath = destination + "/" + dataFile.getGroupName() + "/";
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
				if (column == null) continue;
				finder.countInSequences(column);
				LOG.debug("Including language key: {}", displayNames.get(headerKey));
			}

			List<String> langKeys = new ArrayList<>(displayNames.keySet());
			Table<Double> scores = new RectangularTable<>(
					0.0,
					langKeys.size(),
					langKeys.size()
			);

			// Alternative (multiple) alignment method
			data.rowIterator().forEachRemaining(row -> {
				List<Sequence<T>> sequences = new ArrayList<>(row);

				List<AlignmentResult<T>> results = new ArrayList<>();

				for (List<String> keys : fileKeys) {
					String k1 = keys.get(0);
					String k2 = keys.get(1);

					List<String> keyList = data.getKeys();

					Sequence<T> q1 = sequences.get(keyList.indexOf(k1));
					Sequence<T> q2 = sequences.get(keyList.indexOf(k2));

					AlignmentResult<T> result = algorithm.apply(q1, q2);
					results.add(result);
				}

				// FileKeys will still give the association for the result pairs
				int i = 0;

				List<Alignment<T>> alignments = results.stream()
						.map(AlignmentResult::getAlignments)
						.map(list -> list.get(0))
						.collect(Collectors.toList());



			});

			for (List<String> keys : fileKeys) {
				Collection<AlignmentResult<T>> results = align(algorithm, keys, data);

				// Put the score in the table
				if (createDistanceTable) {
					double score  = getWeightedScore(results);
					int    lIndex = langKeys.indexOf(keys.get(0));
					int    rIndex = langKeys.indexOf(keys.get(1));
					scores.set(lIndex, rIndex, score);
				}

				List<String> collect = keys.stream()
						.map(displayNames::get)
						.collect(Collectors.toList());
				writeResults(outPath, collect, results);

				List<?> something = processResults(outPath, keys, results);
			}

			if (createDistanceTable) {
				List<String> collect = langKeys.stream()
						.map(displayNames::get)
						.collect(Collectors.toList());
				String displayTable = formatDistanceTable(collect, scores);
				File tableFile = new File(outPath, "distances.table");
				try {
					String absolutePath = tableFile.getAbsolutePath();
					HANDLER.writeString(absolutePath, displayTable);
				} catch (IOException e) {
					LOG.error("Failed to write distance table", e);
				}
			}
		}
	}

	private List<?> processResults(String outPath, List<String> keys,
			Iterable<AlignmentResult<T>> results
	) {

		LOG.debug("Processing results for {}", String.join(":", keys));

		TwoKeyMap<Segment<T>, Segment<T>, Double> countMap = getCounts(results);
		TwoKeyMap<Segment<T>, Segment<T>, Double> freqMap = toLogFrequency(countMap, results);

		TwoKeyMultiMap<Segment<T>, Segment<T>, Alignment<T>> map
				= buildCorrespondences(results);

		// Populate multimaps to determine one-to-one and one-to-many relations
		MultiMap<Segment<T>, Segment<T>> leftRight = new GeneralMultiMap<>();
		MultiMap<Segment<T>, Segment<T>> rightLeft = new GeneralMultiMap<>();
		for (Tuple<Segment<T>, Segment<T>> tuple : map.keys()) {
			leftRight.add(tuple.getLeft(), tuple.getRight());
			rightLeft.add(tuple.getRight(), tuple.getLeft());
		}

		// List with canonical ordering from most to least frequent
		List<Twin<Segment<T>>> list = countMap.stream()
				.sorted((o1, o2) -> Double.compare(o2.third(), o1.third()))
				.map(t -> new Twin<>(t.first(), t.second()))
				.collect(Collectors.toList());

		for (Twin<Segment<T>> segments : list) {
			Segment<T> left = segments.getLeft();
			Segment<T> right = segments.getRight();

			if (leftRight.get(left).size() == 1 && rightLeft.get(right).size() == 1) {
				LOG.debug("1-to-1 correspondence pair: {} {}", left, right);
			}
		}

		// Write GML Data
		writeGMLData(outPath, keys, freqMap);

		// Generate printable score ranking
		String collect = countMap.stream().map(triple -> {
			String key1 = triple.first().toString();
			String key2 = triple.second().toString();
			Double third = triple.third();
			return key1 + "\t" + key2 + "\t" + third;
		}).collect(Collectors.joining("\n"));

		return Collections.emptyList();
	}

	private void writeGMLData(String outPath, List<String> keys,
			TwoKeyMap<Segment<T>, Segment<T>, Double> freqMap
	) {
		double firstQuantile = computeQuantile(qQuantiles, freqMap);
		String leftKey = keys.get(0);
		String rightKey = keys.get(1);
		String toGml = toGml(firstQuantile, leftKey, rightKey, freqMap);
		File file = new File(outPath + "/gml/");
		createFolderIfNotExists(file);
		String path = String.format("%s/%s-%s.gml", file.getPath(), leftKey, rightKey);
		try {
			HANDLER.writeString(path, toGml);
		} catch (IOException e) {
			LOG.error("Unable to write graph data to {}", path, e);
		}
	}


	private static <T> double getAverageScore(Collection<AlignmentResult<T>> results) {
		double sum = results.stream()
				.mapToDouble(Processor::getAverageDistance)
				.sum();
		return sum / results.size();
	}

	private static <T> double getWeightedScore(Collection<AlignmentResult<T>> results) {
		double sum = results.stream()
				.mapToDouble(Processor::getAverageDistance)
				.sum();
		return 10 * sum / Math.pow(results.size(), 3.0 / 2.0);
	}

	private static double getAverageDistance(AlignmentResult<?> r) {
		return r.getScore() / (r.getAlignments().get(0).columns());
	}

	private static <T> String toGml(double nTile, String lKey, String rKey,
			TwoKeyMap<Segment<T>, Segment<T>, Double> freqMap
	) {
		Graph<Double> graph = new Graph<>();
		for (Triple<Segment<T>, Segment<T>, Double> triple : freqMap) {
			String left = triple.first().toString();
			String right = triple.second().toString();
			Double count = triple.third();

			// Use greater than because these are inverse log values
			if (nTile > 0 && count > nTile) continue;

			String leftValue = escape(lKey + " [" + left + "]");
			String rightValue = escape(rKey + " [" + right + "]");
			graph.add(leftValue, new DoubleArc(count), rightValue);
		}
		return GraphUtils.graphToGML(graph, true);
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
					if (isAnchor(left, right)) continue;
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

	private static <T> boolean isAnchor(Segment<T> left, Segment<T> right) {
		return left.getSymbol().equals("#") || right.getSymbol().equals("#");
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
					if (isAnchor(left, right)) continue;
					corrMap.add(left, right, alignment);
				}
			}
		}
		return corrMap;
	}

	@NonNull
	private static <T> TwoKeyMap<Segment<T>, Segment<T>, Double> toLogFrequency(
			TwoKeyMap<Segment<T>, Segment<T>, Double> cMap,
			Iterable<AlignmentResult<T>> results
	) {

		// Count the # of times a segment appears in its original language
		Map<Segment<T>, Double> lFreq = new HashMap<>();
		Map<Segment<T>, Double> rFreq = new HashMap<>();
		for (AlignmentResult<T> result : results) {
			result.getLeft().forEach(g -> lFreq.compute(g, UPDATE));
			result.getRight().forEach(g -> rFreq.compute(g, UPDATE));
		}

		double lTotal = lFreq.values().stream().mapToDouble(x -> x).sum();
		double rTotal = rFreq.values().stream().mapToDouble(x -> x).sum();

		for (Segment<T> segment : lFreq.keySet()) {
			lFreq.computeIfPresent(segment, (key, value) -> value / lTotal);
		}

		for (Segment<T> segment : rFreq.keySet()) {
			rFreq.computeIfPresent(segment, (key, value) -> value / rTotal);
		}

		// Count the total number of correspondences
		double sum = cMap.stream().mapToDouble(Triple::third).sum();

		double leftDefault = 1.0 / lTotal;
		double rightDefault = 1.0 / rTotal;

		TwoKeyMap<Segment<T>, Segment<T>, Double> fMap =
				new GeneralTwoKeyMap<>(TreeMap.class);
		for (Triple<Segment<T>, Segment<T>, Double> t : cMap) {

			Segment<T> left = t.first();
			Segment<T> right = t.second();

			double fLeft = lFreq.getOrDefault(left, leftDefault);
			double fRight = rFreq.getOrDefault(right, rightDefault);

			double frequency = t.third() / sum;

//			double a = frequency * (1 - 1 / (1/fLeft + 1/fRight));

			@SuppressWarnings ("NonReproducibleMathCall")
			double value = -1 * Math.log(frequency);
			fMap.put(left, right, value);
		}
		return fMap;
	}

	private static <T> double computeQuantile(
			int q, TwoKeyMap<Segment<T>, Segment<T>, Double> fMap
	) {
		double max = fMap.stream()
				.mapToDouble(Triple::third)
				.max()
				.orElse(0.0);
		double min = fMap.stream()
				.mapToDouble(Triple::third)
				.min()
				.orElse(0.0);
		return (q == 0) ? 0.0 : max - (max - min) / q;
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
			DataFile data,
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
			String outputPath,
			List<String> keys,
			Collection<AlignmentResult<T>> results
	) {

		File outputFolder = new File(outputPath, "sdm");
		createFolderIfNotExists(outputFolder);

		DecimalFormat decimalFormat = new DecimalFormat("0.00");
		String fileName = String.join("-", keys).replaceAll("\\s+","") + ".sdm";
		try(BufferedWriter writer = openWriter(outputFolder, fileName)) {

			writer.write("% "+fileName+"\n");
			writer.write(keys.get(0) + "\n");
			writer.write(keys.get(1) + "\n");
			writer.write("\n\n");

			writer.write("%  Average Score: " + getAverageScore(results) + "\n");
			writer.write("% Weighted Score: " + getWeightedScore(results) + "\n\n");

			for (AlignmentResult<T> result : results) {
				Collection<String> metaList  = new ArrayList<>();
				Collection<String> leftList  = new ArrayList<>();
				Collection<String> rightList = new ArrayList<>();
				for (Alignment<T> alignment : result.getAlignments()) {
					List<String> charSequences = Alignment.buildPrettyAlignments(alignment);
					metaList.add(charSequences.get(0));
					leftList.add(charSequences.get(1));
					rightList.add(charSequences.get(2));
				}

				String metaGroup  = String.join(" | ", metaList);
				String leftGroup  = String.join(" | ", leftList);
				String rightGroup = String.join(" | ", rightList);

				Sequence<T> left  = result.getLeft();
				Sequence<T> right = result.getRight();

				writer.write(metaGroup + "\n");
				writer.write(leftGroup + "\n");
				writer.write(rightGroup + "\n");
				writer.write("\n");
				writer.flush();

				// Write alignment table in SDM comment block
				String collect1 = right.stream()
						.map(Segment::getSymbol)
						.map(FormatterMode.COMPOSITION::normalize)
						.map((String s) -> pad(s, 5))
						.collect(Collectors.joining(" "));
				writer.write("%%      " + collect1 + "\n");
				Table<Double> table = result.getTable();
				for (int i = 0; i < table.rows(); i++) {
					writer.write("%% ");
					writer.write(pad(left.get(i).getSymbol(), -5));
					List<Double> row = table.getRow(i);
					String collect = row.stream()
							.map(decimalFormat::format)
							.map(FormatterMode.COMPOSITION::normalize)
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

	private static void createFolderIfNotExists(File outputFolder) {
		if (!outputFolder.exists()) {
			boolean failed = !outputFolder.mkdirs();
			if (failed) {
				LOG.error("Failed to create folder {}", outputFolder);
			}
		}
	}

	@NonNull
	private static BufferedWriter openWriter(File outputFolder, String fileName)
			throws IOException {
		FileWriter writer = new FileWriter(new File(outputFolder, fileName));
		return new BufferedWriter(writer);
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

	@NonNull
	private static <T> Collection<AlignmentResult<T>> align(
			@NonNull AlignmentAlgorithm<T> algorithm,
			@NonNull List<String> keyList,
			@NonNull ColumnTable<Sequence<T>> data
	) {
		String k1 = keyList.get(0);
		String k2 = keyList.get(1);

		List<Sequence<T>> d1 = Objects.requireNonNull(data.getColumn(k1));
		List<Sequence<T>> d2 = Objects.requireNonNull(data.getColumn(k2));

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
