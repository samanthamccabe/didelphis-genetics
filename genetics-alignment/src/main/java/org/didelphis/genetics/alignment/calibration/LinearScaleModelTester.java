package org.didelphis.genetics.alignment.calibration;

import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.algorithm.SingleAlignmentAlgorithm;
import org.didelphis.genetics.alignment.common.Utilities;
import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.genetics.alignment.operators.comparators.LinearWeightComparator;
import org.didelphis.genetics.alignment.operators.comparators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.gap.ConstantGapPenalty;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.io.ClassPathFileHandler;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.features.IntegerFeature;
import org.didelphis.language.phonetic.model.FeatureMapping;
import org.didelphis.language.phonetic.model.FeatureModelLoader;
import org.didelphis.language.phonetic.sequences.Sequence;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Samantha Fiona McCabe
 * Created: 6/6/2015
 */
public final class LinearScaleModelTester extends BaseModelTester {

	private static final NumberFormat FORMAT_SHORT = new DecimalFormat("0.000");

	protected LinearScaleModelTester(SequenceFactory<Integer> factoryParam) {
		super(factoryParam);
	}

	@SuppressWarnings("MagicNumber")
	public static void main(String[] args) throws IOException {

		// LOAD MODEL 
		// ================================================================================

		String path = "AT_hybrid_reduced.model";

		FeatureType<Integer> featureType = IntegerFeature.INSTANCE;

		FeatureModelLoader<Integer> loader = new FeatureModelLoader<>(
				featureType, ClassPathFileHandler.INSTANCE, path);

		FeatureMapping<Integer> mapping = loader.getFeatureMapping();

		SequenceFactory<Integer> factory = new SequenceFactory<>(
				mapping, FormatterMode.INTELLIGENT);

		BaseModelTester runner = new LinearScaleModelTester(factory);

		File nakhDataFile = new File("../data/nakh.tsv");
//		ColumnTable<Sequence<Integer>> nakhData =
//				Utilities.toPhoneticTable(nakhDataFile, factory, new StringTransformer(), "CHE", "ING", "BCB");

		// LOAD CONSTRAINTS 
		// ==========================================================================
//		runner.loadLexicon(new File("../data/training/CHE_BCB.std"), nakhData);
//		runner.loadLexicon(new File("../data/training/CHE_ING.std"), nakhData);
//		runner.loadLexicon(new File("../data/training/ING_BCB.std"), nakhData);
		//		constraints.add(LexiconConstraint.loadFromPaths("ASP_SKT.std",
		// "ASP_SKT.lex", factory));


		// SET UP PARAMETERS 
		// =========================================================================

		Sequence<Integer> gap = factory.getSequence("_");

		// RUN ALGORITHM 
		// =============================================================================
		Writer writer = new BufferedWriter(
				new FileWriter(new File("linear_multiples.csv")));
		writer.write("Fitness\t" +
				//				"A\t" +
				//				"B\t" +
				"N\t" +
				"Sonorance\tVOT\tRelease\tNasal\tLateral\tLabial\tRound" +
				"\tLingual\tHeight\tFront\tBack\tATR\tRadical\tGlottalState" +
				"\tLength\n");
		double nPrime = 1.894;
		double bPrime = 2.120;
		double aPrime = 1.816;
		double[] array = {
				2.000,
				1.124,
				4.037,
				1.597,
				1.405,
				5.627,
				1.364,
				3.194,
				0.576,
				1.701,
				2.124,
				2.350,
				4.209,
				2.186,
				3.397
		};
		for (int i = 1; i < 100; i++) {

			double a = aPrime * i;
			double b = bPrime * i;
			double n = nPrime * i;

			List<Double> weights = new ArrayList<>();
			for (double v : array) {
				weights.add(v * i);
			}

			Comparator<Integer> segmentComparator =
					new LinearWeightComparator(featureType,weights);
			Comparator<Integer> sequenceComparator =
					new SequenceComparator(segmentComparator);

			//			GapPenalty gapPenalty = new ConvexGapPenalty(gap, a,
			// b);
			GapPenalty gapPenalty = new ConstantGapPenalty(gap, 0.0);

			//			AlignmentAlgorithm algorithm = new 
			// SingleAlignmentAlgorithm(gapPenalty, 1, sequenceComparator);
			AlignmentAlgorithm algorithm =
					new SingleAlignmentAlgorithm(sequenceComparator, gapPenalty,
							1, factory);

			double fitnessSum = 0.0;
			double strengthSum = 0.0;

			// TODO: broken??
			//		for (Constraint constraint : constraints) {
			//				fitnessSum  += constraint.apply(algorithm);
			//			strengthSum += constraint.getStrength();
			//		}

			writer.write(FORMAT_SHORT.format(fitnessSum / strengthSum) + '\t' +
					//				Utilities.FORMAT_SHORT.format(a) + "\t" +
					//				Utilities.FORMAT_SHORT.format(b) + "\t" +
					Utilities.FORMAT_SHORT.format(n) + '\t' +
					Utilities.format(weights) + '\n');
		}
		writer.close();
	}
}
