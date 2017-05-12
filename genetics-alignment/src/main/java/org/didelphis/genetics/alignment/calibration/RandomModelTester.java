package org.didelphis.genetics.alignment.calibration;

import org.didelphis.common.language.enums.FormatterMode;
import org.didelphis.common.language.phonetic.SequenceFactory;
import org.didelphis.common.language.phonetic.model.interfaces.FeatureSpecification;
import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.common.language.phonetic.sequences.Sequence;
import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.algorithm.single.SingleAlignmentAlgorithm;
import org.didelphis.genetics.alignment.common.Utilities;
import org.didelphis.genetics.alignment.constraints.Constraint;
import org.didelphis.genetics.alignment.constraints.LexiconConstraint;
import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.genetics.alignment.operators.comparators.LinearWeightComparator;
import org.didelphis.genetics.alignment.operators.comparators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.gap.ConstantGapPenalty;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 5/6/2015
 */
public final class RandomModelTester extends BaseModelTester {

	private static final Pattern SPACE = Pattern.compile("\\s+");

	public RandomModelTester(SequenceFactory<Double> factoryParam) {
		super(factoryParam);
	}

	public static void main(String[] args) throws IOException {

		String path = "AT_hybrid_reduced.model";

		SequenceFactory<Double> factory =
				Utilities.loadFactoryFromClassPath(path,
						FormatterMode.INTELLIGENT);

		Map<String, String> constraintPaths = new HashMap<>();
		constraintPaths.put("CHE_BCB.std", "CHE_BCB.lex");
		constraintPaths.put("CHE_ING.std", "CHE_ING.lex");
		constraintPaths.put("ING_BCB.std", "ING_BCB.lex");
		//		constraintPaths.put("ASP_SKT.std", "ASP_SKT.lex");

		Collection<Constraint> constraints = new HashSet<>();
		String trainingPath = "E:/git/data/training/";
		for (Map.Entry<String, String> entry : constraintPaths.entrySet()) {
			Constraint constraint = LexiconConstraint.loadFromPaths(
					trainingPath + entry.getKey(),
					trainingPath + entry.getValue(), factory);
			constraints.add(constraint);
		}

		double n = 10000;
		double max = 14.0;
		int bMax = 10;

		FeatureSpecification featureModel =
				factory.getFeatureMapping().getFeatureModel();
		int features = featureModel.size();

		String suffix = "max(" + max + ")_n(" + n + ").csv";
		String pathname = trainingPath + path + suffix;
		Writer writer = new BufferedWriter(new FileWriter(new File(pathname)));

		// =====================================================================
		// GENERATE HEADER
		// =====================================================================
		Collection<String> labels = new ArrayList<>(5 + features);
		labels.add("F");
		labels.add("A");
		labels.addAll(featureModel.getFeatureNames());
		writer.write(Utilities.formatStrings(labels));
		writer.write("\n");

		// =====================================================================
		// RUN SIMULATION
		// =====================================================================
		long start = System.nanoTime();
		int aMax = 10;
		for (int j = 0; j < n; j++) {

			double fitnessSum = 0.0;
			double strengthSum = 0.0;

			List<Double> weights = generateWeights(max, features);

			double a = (Math.random() * aMax);
			//			double b = (Math.random() * 20) - 10;

			Comparator<Segment<Double>> segmentComparator =
					new LinearWeightComparator(weights);
			Comparator<Sequence<Double>> sequenceComparator =
					new SequenceComparator(segmentComparator);

			GapPenalty gapPenalty =
					new ConstantGapPenalty(factory.getSegment("_"), a);

			AlignmentAlgorithm algorithm =
					new SingleAlignmentAlgorithm(sequenceComparator, gapPenalty,
							1, factory);

			for (Constraint constraint : constraints) {
				double fitness = constraint.evaluate(algorithm);
				double strength = constraint.getStrength();
				fitnessSum += fitness;
				strengthSum += strength;
			}

			String aStr = Utilities.FORMAT_LONG.format(a);

			double percent = (fitnessSum / strengthSum) * 100;
			String weightsFormatted = SPACE.matcher(Utilities.format(weights))
					.replaceAll("\t")
					.trim() + '\n';
			String string =
					Utilities.FORMAT_LONG.format(percent) + '\t' + aStr + '\t' +
							weightsFormatted;
			writer.write(string);
		}

		long end = System.nanoTime();

		System.out.println("Finished " + n + " runs in in " +
				Utilities.FORMAT_LONG.format((end - start) / 1000000000) +
				" s");

		writer.close();
	}

	private static List<Double> generateWeights(double max, int features) {
		List<Double> weights = new ArrayList<>();
		weights.add(5.0);
		for (int k = 1; k < features; k++) {
			double value = Math.random() * max;
			weights.add(value);
		}
		return weights;
	}

}
