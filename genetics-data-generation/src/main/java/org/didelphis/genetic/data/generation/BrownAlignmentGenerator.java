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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.lang.Math.abs;
import static java.lang.Math.random;
import static java.lang.Math.round;
import static java.lang.Math.toIntExact;

/**
 * Created by samantha on 4/22/17.
 */
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class BrownAlignmentGenerator {

	static Pattern NEWLINE = Pattern.compile("\n");
	static FileHandler HANDLER = new DiskFileHandler("UTF-8");
	static Random RANDOM = new Random();

	double generaBias;
	double gapBias;

	TwoKeyMultiMap<String, String, Correspondence> stkm;
	NavigableMap<Double, Correspondence> treeMap;
	SymmetricalTwoKeyMap<String, Double> scores;

	public BrownAlignmentGenerator(String correspondencePath) {
		this(correspondencePath, 1.0, 1.0);
	}

	public BrownAlignmentGenerator(String correspondencePath, double generaBias,
			double gapBias
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
			Collection<Correspondence> collection = triple.getThirdElement();
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
		int maxIterations = Integer.valueOf(args[0]);

		String dataFile = args[1];
		String outputFile = args[2];

		BrownAlignmentGenerator generator = new BrownAlignmentGenerator(
				dataFile, 1.0, 3.0);

		//		String string = generator.generate(patterns, maxIterations);
		String string = generator.generate(3, 10, maxIterations);
		try (Writer fileWriter = new BufferedWriter(
				new FileWriter(outputFile))) {
			fileWriter.write(string);
		} catch (IOException ignored) {
		}
	}

	public SymmetricalTwoKeyMap<String, Double> getScores() {
		return scores;
	}

	public Supplier<Twin<String>> supplier(int min, int max) {
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

	public String generate(List<Tuple<String, String>> patterns, int n) {
		StringBuilder stringBuilder = new StringBuilder(n * 10);

		stringBuilder.append("A\tB\n");

		int size = patterns.size();

		for (int i = 0; i < n; i++) {
			Tuple<String, String> tuple = patterns.get(randomInt(size));

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
						.map(Correspondence::getScore)
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
		NEWLINE.splitAsStream(HANDLER.read(path))
				.skip(1)
				.filter(predicate -> !predicate.isEmpty())
				.forEach(item -> {
					String[] strings = item.split("\t");

					String typeLeft = strings[0];
					String typeRight = strings[1];

					String left = strings[2];
					String right = strings[3];

					int count = Integer.valueOf(strings[4]);
					double percent = Double.valueOf(strings[5]);

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
		return toIntExact(round(random() * (max - min) + min));
	}

	private static double sum(Triple<?, ?, Collection<Correspondence>> t) {
		return t.getThirdElement()
				.parallelStream()
				.mapToDouble(Correspondence::getScore).sum();
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

	private static boolean check(String left, String right) {
		return (left.equals("C") || left.equals("V")) &&
				(right.equals("C") || right.equals("V"));
	}
}
