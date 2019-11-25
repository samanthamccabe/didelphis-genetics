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

package org.didelphis.genetic.data.generation;

import lombok.AccessLevel;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.didelphis.io.DiskFileHandler;
import org.didelphis.io.FileHandler;
import org.didelphis.structures.maps.GeneralMultiMap;
import org.didelphis.structures.maps.GeneralTwoKeyMultiMap;
import org.didelphis.structures.maps.SymmetricalTwoKeyMap;
import org.didelphis.structures.maps.interfaces.MultiMap;
import org.didelphis.structures.maps.interfaces.TwoKeyMultiMap;
import org.didelphis.structures.tuples.Triple;
import org.didelphis.structures.tuples.Tuple;
import org.didelphis.structures.tuples.Twin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.lang.Math.abs;
import static java.lang.Math.random;
import static java.lang.Math.round;
import static java.lang.Math.toIntExact;

@ToString
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class BrownAlignmentGenerator {

	private static final Logger LOG = LogManager.getLogger(BrownAlignmentGenerator.class);

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private static final Pattern     NEWLINE = Pattern.compile("\n");
	private static final FileHandler HANDLER = new DiskFileHandler("UTF-8");
	private static final Random      RANDOM  = new Random();

	double generaBias;
	double gapBias;

	TwoKeyMultiMap<String, String, Correspondence> stkm;
	NavigableMap<Double, Correspondence> treeMap;
	SymmetricalTwoKeyMap<String, Double> scores;

	public BrownAlignmentGenerator(String correspondencePath) {
		this(correspondencePath, 1.0, 1.0);
	}

	public BrownAlignmentGenerator(
			String correspondencePath, double generaBias, double gapBias
	) {
		this.generaBias = generaBias;
		this.gapBias = gapBias;

		stkm = load(correspondencePath);
		treeMap = new TreeMap<>();
		scores = new SymmetricalTwoKeyMap<>();

		double sum = StreamSupport.stream(stkm.spliterator(), true)
				.mapToDouble(BrownAlignmentGenerator::sum)
				.sum();

		double last = 0.0;
		for (Triple<String, String, Collection<Correspondence>> triple : stkm) {
			Collection<Correspondence> collection = triple.third();
			for (Correspondence correspondence : collection) {
				last = put(correspondence, sum, last);
				scores.put(correspondence.getLeftSymbol(),
						correspondence.getRightSymbol(),
						correspondence.getScore()
				);
			}
		}
	}

	public static void main(String... args) {
		int maxIterations = Integer.parseInt(args[0]);

		String dataFile = args[1];
		String outputFile = args[2];

		BrownAlignmentGenerator generator = new BrownAlignmentGenerator(
				dataFile, 1.0, 3.0);

		String generatedString = generator.generate(3, 10, maxIterations);
		try (Writer fileWriter = new BufferedWriter(
				new FileWriter(outputFile))) {
			fileWriter.write(generatedString);
		} catch (IOException ignored) {
		}
	}

	public SymmetricalTwoKeyMap<String, Double> getScores() {
		return scores;
	}

	private Supplier<Twin<String>> supplier(int min, int max) {
		int n = randomInt(min, max);
		return () -> {
			StringBuilder left = new StringBuilder("# ");
			StringBuilder right = new StringBuilder("# ");
			for (int j = 0; j < n; j++) {
				Entry<Double, Correspondence> entry = treeMap.floorEntry(
						random());
				while (entry == null) {
					entry = treeMap.floorEntry(random());
				}
				Correspondence value = entry.getValue();
				left.append(value.getLeftSymbol()).append(' ');
				right.append(value.getRightSymbol()).append(' ');
			}
			return new Twin<>(left.toString(), right.toString());
		};
	}

	public String generate(int min, int max, int iterations) {
		StringBuilder stringBuilder = new StringBuilder(
				iterations * (min + max) / 2);
		Supplier<Twin<String>> supplier = supplier(min, max);
		stringBuilder.append("A\tB\n");
		for (int i = 0; i < iterations; i++) {
			Tuple<String, String> tuple = supplier.get();
			stringBuilder.append(tuple.getLeft())
					.append('\t')
					.append(tuple.getRight())
					.append('\n');
		}
		return stringBuilder.toString();
	}

	public String generate(List<? extends Tuple<String, String>> patterns, int n) {
		StringBuilder stringBuilder = new StringBuilder(n * 10);

		stringBuilder.append("A\tB\n");

		int size = patterns.size();

		for (int i = 0; i < n; i++) {
			Tuple<String, String> tuple = patterns.get(randomInt(size));

			String tupleLeft = tuple.getLeft();
			String tupleRight = tuple.getRight();

			StringBuilder left = new StringBuilder();
			StringBuilder right = new StringBuilder();

			int lengthL = tupleLeft  == null ? 0 : tupleLeft.length();
			int lengthR = tupleRight == null ? 0 : tupleRight.length();

			for (int j = 0; j < lengthL && j < lengthR; j++) {

				String k1 = tupleLeft.substring(j, j + 1);
				String k2 = tupleRight.substring(j, j + 1);

				Collection<Correspondence> correspondences = stkm.get(k1, k2);

				List<Correspondence> list = (correspondences == null)
						? Collections.emptyList()
						: new ArrayList<>(correspondences);

				List<Double> percentages = list.stream()
						.map(Correspondence::getScore)
						.collect(Collectors.toList());

				int pIndex = rouletteSelect(percentages);

				Correspondence correspondence = list.get(pIndex);
				if (correspondence != null) {
					String leftSymbol  = correspondence.getLeftSymbol();
					String rightSymbol = correspondence.getRightSymbol();

					left.append(leftSymbol).append(' ');
					right.append(rightSymbol).append(' ');
				}
			}

			stringBuilder.append(left).append('\t').append(right).append('\n');
		}
		return stringBuilder.toString();
	}

	public TwoKeyMultiMap<String, String, Correspondence> getCorrespondenceByClasses() {
		return stkm;
	}

	public NavigableMap<Double, Correspondence> getTreeMap() {
		return treeMap;
	}

	private double put(Correspondence crs, double sum, double last) {
		double percentage = crs.getScore() / sum + last;
		treeMap.put(percentage, crs);
		return percentage;
	}

	private TwoKeyMultiMap<String, String, Correspondence> load(String path) {
		MultiMap<String, String> classToValue = new GeneralMultiMap<>();
		TwoKeyMultiMap<String, String, Correspondence> map
				= new GeneralTwoKeyMultiMap<>();
		String read = null;
		try {
			read = HANDLER.read(path);
		} catch (IOException e) {
			LOG.error("Failed to read from path {}", path, e);
			return map;
		}
		NEWLINE.splitAsStream(read)
				.skip(1)
				.filter(predicate -> !predicate.isEmpty())
				.forEach(item -> {
					String[] strings = item.split("\t");

					String typeLeft = strings[0];
					String typeRight = strings[1];

					String left = strings[2];
					String right = strings[3];

					int count      = Integer.parseInt(strings[4]);
					double percent = Double.parseDouble(strings[5]);

					if (check(typeLeft, typeRight)) {
						classToValue.add(typeLeft, left);
						classToValue.add(typeRight, right);
					}

					boolean isGap = left.equals("Ø") || right.equals("Ø");
					double adjust = (generaBias > 0 ? count * generaBias : 1.0);
					double score = adjust * (isGap ? gapBias : 1.0) * percent;

					map.add(typeLeft, typeRight,
							new Correspondence(left, right, score)
					);
					map.add(typeRight, typeLeft,
							new Correspondence(right, left, score)
					);
				});
		return map;
	}

	private static int randomInt(int min, int max) {
		double random = SECURE_RANDOM.nextDouble();
		return toIntExact(round(random * (max - min) + min));
	}

	private static double sum(Triple<?, ?, Collection<Correspondence>> t) {
		return t.third()
				.parallelStream()
				.mapToDouble(Correspondence::getScore).sum();
	}

	private static int rouletteSelect(List<Double> weight) {
		// calculate the total weight
		double sum = weight.stream().mapToDouble(aDouble -> aDouble).sum();
		// get a secureRandom value
		double value = randUniformPositive() * sum;
		// locate the secureRandom value based on the weights
		for (int i = 0; i < weight.size(); i++) {
			value -= weight.get(i);
			if (value <= 0) {
				return i;
			}
		}
		// when rounding errors occur, we return the last item's index
		return weight.size() - 1;
	}

	// Returns a uniformly distributed double value between 0.0 and 1.0
	private static double randUniformPositive() {
		// easiest implementation
		return RANDOM.nextDouble();
	}

	private static int randomInt(int size) {
		return abs(RANDOM.nextInt() % size);
	}

	private static boolean check(String left, String right) {
		return (left.equals("C") || left.equals("V")) &&
				(right.equals("C") || right.equals("V"));
	}
}
