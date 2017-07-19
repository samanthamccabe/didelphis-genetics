package org.didelphis.genetic.data.generation;

import org.didelphis.io.DiskFileHandler;
import org.didelphis.io.FileHandler;
import org.didelphis.structures.maps.GeneralMultiMap;
import org.didelphis.structures.maps.GeneralTwoKeyMultiMap;
import org.didelphis.structures.maps.interfaces.MultiMap;
import org.didelphis.structures.maps.interfaces.TwoKeyMultiMap;
import org.didelphis.structures.tuples.Triple;
import org.didelphis.structures.tuples.Tuple;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.StreamSupport;

import static java.lang.Math.abs;
import static java.lang.Math.random;
import static java.lang.Math.round;
import static java.lang.Math.toIntExact;

/**
 * Created by samantha on 4/22/17.
 */
public final class BrownAlignmentGenerator {

	private static final Pattern NEWLINE = Pattern.compile("\n");
	private static final FileHandler HANDLER = new DiskFileHandler("UTF-8");
	private static final Random RANDOM = new Random();

	private final TwoKeyMultiMap<String, String, Correspondence> stkm;
	private final NavigableMap<Double, Correspondence> treeMap;

	public BrownAlignmentGenerator(String correspondencePath) {
		stkm = loadMap(correspondencePath);
		treeMap = new TreeMap<>();
		double sum = StreamSupport.stream(stkm.spliterator(), true)
				.mapToDouble(BrownAlignmentGenerator::sum)
				.sum();

		double last = 0.0;
		for (Triple<String, String, Collection<Correspondence>> triple : stkm) {
			Collection<Correspondence> collection = triple.getThirdElement();
			for (Correspondence correspondence : collection) {
				last = put(correspondence, sum, last);
			}
		}
	}

	public double put(Correspondence crs, double sum, double last) {
		double percentage = crs.getPercentage() / sum + last;
		treeMap.put(percentage, crs);
		return percentage;
	}

	public static void main(String... args) {
		int maxIterations = Integer.valueOf(args[0]);

		String dataFile = args[1];
		String outputFile = args[2];

//		String patternFile = args[3];

//		CharSequence input = HANDLER.read(patternFile);
//		List<Tuple<String, String>> patterns = NEWLINE.splitAsStream(input)
//				.filter(predicate -> !predicate.isEmpty())
//				.map(BrownAlignmentGenerator::splitTuple)
//				.collect(Collectors.toList());

		//		TwoKeyMultiMap<String, String, Correspondence> map = loadMap(dataFile);
		BrownAlignmentGenerator generator = new BrownAlignmentGenerator(dataFile);

//		String string = generator.generate(patterns, maxIterations);
		String string = generator.generate(3, 10, 1000);
		try (Writer fileWriter = new BufferedWriter(
				new FileWriter(outputFile))) {
			fileWriter.write(string);
		} catch (IOException ignored) {
		}
	}

	public String generate(int min, int max, int iterations) {
		StringBuilder stringBuilder = new StringBuilder(iterations * (min + max) / 2);

		stringBuilder.append("A\tB\n");

		for (int i = 0; i < iterations; i++) {
			int n = toIntExact(round(random() * (max - min) + min));

			StringBuilder left = new StringBuilder("# ");
			StringBuilder right = new StringBuilder("# ");

			for (int j = 0; j < n; j++) {
				Entry<Double, Correspondence> entry = treeMap.floorEntry(random());
				while (entry == null) {
					entry = treeMap.floorEntry(random());
				}
				Correspondence value = entry.getValue();
				left.append(value.getLeftSymbol()).append(' ');
				right.append(value.getRightSymbol()).append(' ');
			}
			stringBuilder.append(left).append('\t').append(right).append('\n');
		}
		return stringBuilder.toString();
	}

	public String generate(List<Tuple<String, String>> patterns, int iterations) {

		StringBuilder stringBuilder = new StringBuilder(iterations * 10);

		stringBuilder.append("A\tB\n");

		for (int i = 0; i < iterations; i++) {

			Tuple<String, String> tuple = patterns.get(randomInt(patterns.size()));

			String tupleLeft = tuple.getLeft();
			String tupleRight = tuple.getRight();

			StringBuilder left = new StringBuilder();
			StringBuilder right = new StringBuilder();

			int lengthL = tupleLeft.length();
			int lengthR = tupleRight.length();

			for (int j = 0; j < lengthL && j < lengthR; j++) {

				String k1 = tupleLeft.substring(j, j + 1);
				String k2 = tupleRight.substring(j, j + 1);

				List<Correspondence> list = new ArrayList<>(stkm.get(k1, k2));
				List<Double> percentages = list.stream()
						.map(Correspondence::getPercentage)
						.collect(Collectors.toList());

				int pIndex = rouletteSelect(percentages);

				Correspondence correspondence = list.get(pIndex);
				String leftSymbol = correspondence.getLeftSymbol();
				String rightSymbol = correspondence.getRightSymbol();

				left.append(leftSymbol).append(' ');
				right.append(rightSymbol).append(' ');
			}

			stringBuilder.append(left).append('\t').append(right).append('\n');
		}
		return stringBuilder.toString();
	}


	private static double sum(Triple<?, ?, Collection<Correspondence>> t) {
		return toDoubleStream(t).sum();
	}

	private static DoubleStream toDoubleStream(
			@NotNull Triple<?, ?, Collection<Correspondence>> triple
	) {
		return triple.getThirdElement().parallelStream()
				.mapToDouble(Correspondence::getPercentage);
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
		return abs(RANDOM.nextInt() % size);
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

					if (checkVC(classLeft) && checkVC(classRight) && count >= 10) {
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

	private static boolean checkVC(String classLeft) {
		return classLeft.equals("C") || classLeft.equals("V");
	}
}
