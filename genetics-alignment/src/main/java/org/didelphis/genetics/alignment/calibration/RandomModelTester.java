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

package org.didelphis.genetics.alignment.calibration;

import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.algorithm.SingleAlignmentAlgorithm;
import org.didelphis.genetics.alignment.common.Utilities;
import org.didelphis.genetics.alignment.constraints.Constraint;
import org.didelphis.genetics.alignment.constraints.LexiconConstraint;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.comparators.LinearWeightComparator;
import org.didelphis.genetics.alignment.operators.gap.ConstantGapPenalty;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.io.ClassPathFileHandler;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.features.IntegerFeature;
import org.didelphis.language.phonetic.model.FeatureMapping;
import org.didelphis.language.phonetic.model.FeatureModelLoader;
import org.didelphis.language.phonetic.model.FeatureSpecification;

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
import java.util.Map.Entry;
import java.util.regex.Pattern;

public final class RandomModelTester<T> extends BaseModelTester<T> {

	private static final Pattern SPACE = Pattern.compile("\\s+");

	public RandomModelTester(SequenceFactory<T> factoryParam) {
		super(factoryParam);
	}

	public static void main(String... args) throws IOException {

		String path = "AT_hybrid_reduced.model";

		FeatureType<Integer> featureType = IntegerFeature.INSTANCE;

		FeatureModelLoader<Integer> loader = new FeatureModelLoader<>(
				featureType, ClassPathFileHandler.INSTANCE, path);

		FeatureMapping<Integer> mapping = loader.getFeatureMapping();

		SequenceFactory<Integer> factory = new SequenceFactory<>(
				mapping, FormatterMode.INTELLIGENT);

		Map<String, String> constraintPaths = new HashMap<>();
		constraintPaths.put("CHE_BCB.std", "CHE_BCB.lex");
		constraintPaths.put("CHE_ING.std", "CHE_ING.lex");
		constraintPaths.put("ING_BCB.std", "ING_BCB.lex");
		//		constraintPaths.put("ASP_SKT.std", "ASP_SKT.lex");

		Collection<Constraint<Integer>> constraints = new HashSet<>();
		String trainingPath = "E:/git/data/training/";
		for (Entry<String, String> entry : constraintPaths.entrySet()) {
			Constraint<Integer> constraint = LexiconConstraint.loadFromPaths(
					trainingPath + entry.getKey(),
					trainingPath + entry.getValue(), factory);
			constraints.add(constraint);
		}

		double n = 10000;
		double max = 14.0;
		int bMax = 10;

		FeatureSpecification spec = factory.getFeatureMapping().getFeatureModel().getSpecification();
		int features = spec.size();

		String suffix = "max(" + max + ")_n(" + n + ").csv";
		String pathname = trainingPath + path + suffix;
		Writer writer = new BufferedWriter(new FileWriter(new File(pathname)));

		// =====================================================================
		// GENERATE HEADER
		// =====================================================================
		Collection<String> labels = new ArrayList<>(5 + features);
		labels.add("F");
		labels.add("A");
		labels.addAll(spec.getFeatureNames());
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

			SequenceComparator<Integer> sequenceComparator =
					new LinearWeightComparator<>(featureType,weights);

			GapPenalty<Integer> gapPenalty =
					new ConstantGapPenalty<>(factory.toSequence("_"), a);

			AlignmentAlgorithm<Integer> algorithm =
					new SingleAlignmentAlgorithm<>(sequenceComparator, gapPenalty,
							1, factory);

			for (Constraint<Integer> constraint : constraints) {
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
