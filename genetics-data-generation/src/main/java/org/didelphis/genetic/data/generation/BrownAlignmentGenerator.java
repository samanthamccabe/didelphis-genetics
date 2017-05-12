package org.didelphis.genetic.data.generation;

import org.didelphis.common.io.DiskFileHandler;
import org.didelphis.common.io.FileHandler;
import org.didelphis.common.structures.maps.GeneralMultiMap;
import org.didelphis.common.structures.maps.GeneralTwoKeyMultiMap;
import org.didelphis.common.structures.maps.interfaces.MultiMap;
import org.didelphis.common.structures.maps.interfaces.TwoKeyMultiMap;
import org.didelphis.common.structures.tuples.Tuple;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by samantha on 4/22/17.
 */
public final class BrownAlignmentGenerator {

	private static final Pattern NEWLINE = Pattern.compile("\n");
	private static final FileHandler HANDLER = new DiskFileHandler("UTF-8");
	private static final Random RANDOM = new Random();

	private BrownAlignmentGenerator() {
	}

	public static void main(String... args) {
		int maxIterations = Integer.valueOf(args[0]);
		String patternFile = args[1];
		String dataFile = args[2];
		String outputFile = args[3];

		StringBuilder stringBuilder = new StringBuilder(maxIterations * 10);
		CharSequence input = HANDLER.read(patternFile);
		List<Tuple<String, String>> patterns = NEWLINE.splitAsStream(input)
				.filter(predicate -> !predicate.isEmpty())
				.map(line -> splitTuple(line))
				.collect(Collectors.toList());

		TwoKeyMultiMap<String, String, Correspondence> map = loadMap(dataFile);
		for (int i = 0; i < maxIterations; i++) {

			Tuple<String, String> tuple = patterns.get(randomInt(patterns.size()));
			
			String tupleLeft = tuple.getLeft();
			String tupleRight = tuple.getRight();

			StringBuilder left = new StringBuilder();
			StringBuilder right = new StringBuilder();

			for (int j = 0; j < tupleLeft.length(); j++) {

				String k1 = tupleLeft.substring(j, j + 1);
				String k2 = tupleRight.substring(j, j + 1);

				List<Correspondence> list = new ArrayList<>(map.get(k1, k2));
				List<Double> percentages = list.stream()
						.map(co -> co.getPercentage())
						.collect(Collectors.toList());

				int pIndex = rouletteSelect(percentages);

				Correspondence correspondence = list.get(pIndex);
				String leftSymbol = correspondence.getLeftSymbol();
				String rightSymbol = correspondence.getRightSymbol();
				
				left.append(leftSymbol);
				right.append(rightSymbol);
			}

			stringBuilder.append(left).append('\t').append(right).append('\n');
		}

		try (Writer fileWriter = new BufferedWriter(
				new FileWriter(outputFile))) {
			fileWriter.write(stringBuilder.toString());
		} catch (IOException ignored) {
		}
	}

	private static Tuple<String, String> splitTuple(String line) {
		String[] strings = line.split("\t");
		return new Tuple<>(strings[0], strings[1]);
	}

	private static int rouletteSelect(List<Double> weight) {
		// calculate the total weight
		double sum = weight.stream().mapToDouble(aDouble -> aDouble).sum();
		// get a random value
		double value = randUniformPositive() * sum;
		// locate the random value based on the weights
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
		return Math.abs(RANDOM.nextInt() % size);
	}

	private static TwoKeyMultiMap<String, String, Correspondence> loadMap(
			String dataFile) {

		MultiMap<String, String> classToValue = new GeneralMultiMap<>();
		TwoKeyMultiMap<String, String, Correspondence> map =
				new GeneralTwoKeyMultiMap<>();
		NEWLINE.splitAsStream(HANDLER.read(dataFile))
				.skip(1)
				.filter(predicate -> !predicate.isEmpty())
				.forEach(item -> {
					String[] strings = item.split("\t");

					String classLeft = strings[0];
					String classRight = strings[1];

					String left = strings[2];
					String right = strings[3];

					int count = Integer.valueOf(strings[4]);
					double percent = Double.valueOf(strings[5]);

					if ((classLeft.equals("C") || classLeft.equals("V")) &&
					    (classRight.equals("V") || classRight.equals("C")) &&
					    count >= 10) {
						classToValue.add(classLeft, left);
						classToValue.add(classRight, right);
					}

					map.add(classLeft, classRight,
					        new Correspondence(left, right, count, percent));
					map.add(classRight, classLeft,
					        new Correspondence(right, left, count, percent));
				});

		classToValue.iterator().forEachRemaining(tuple -> {
			Iterable<String> symbols = tuple.getRight();
			String classLabel = tuple.getLeft();
			symbols.forEach(symbol -> {
				Correspondence correspondence =
						new Correspondence(symbol, symbol, 100, 100);
				map.add(classLabel, classLabel, correspondence);
			});
		});
		return map;
	}
}
