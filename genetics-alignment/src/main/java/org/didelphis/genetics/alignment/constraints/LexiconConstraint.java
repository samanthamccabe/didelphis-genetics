package org.didelphis.genetics.alignment.constraints;

import org.didelphis.common.language.phonetic.SequenceFactory;
import org.didelphis.common.language.phonetic.features.FeatureArray;
import org.didelphis.common.language.phonetic.model.interfaces.FeatureMapping;
import org.didelphis.common.language.phonetic.model.interfaces.FeatureModel;
import org.didelphis.common.language.phonetic.model.interfaces.FeatureSpecification;
import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.common.language.phonetic.segments.StandardSegment;
import org.didelphis.common.language.phonetic.sequences.Sequence;
import org.didelphis.common.structures.tables.ColumnTable;
import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.algorithm.AlignmentSet;
import org.didelphis.genetics.alignment.common.Utilities;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 5/3/2015
 */
public class LexiconConstraint implements Constraint {

	//	public LexiconConstraint(String nameParam, SequenceFactory 
	// factoryParam, List<String> lexiconParam, List<Alignment> 
	// standardParam) {
	//					
	private static final Pattern EXTENSION = Pattern.compile("\\.[^.]*");
	//		name = nameParam;
	private final SequenceFactory<Double> factory;
	//		factory  = factoryParam;
	//		standard = standardParam;
	private final ColumnTable<Sequence<Double>> data;
	//		strength = parseLexicon(lexiconParam);
	//	}

	//	private double parseLexicon(List<String> lexicon) {
	//		Sequence sequence = factory.getNewSequence();
	private final List<Alignment<Double>> standard;
	//		Segment border = factory.getSegment("#");
	private final String name;
	//		double numberValid = 0.0;
	private final double strength;

	//		// And then process the file
	//		for (String l : lexicon) {
	//			String[] line = l.split("\t");
	public LexiconConstraint(String nameParam,
			SequenceFactory<Double> factoryParam,
			ColumnTable<Sequence<Double>> dataParam,
			List<Alignment<Double>> standardParam) {
		name = nameParam;
		factory = factoryParam;
		standard = standardParam;
		data = dataParam;
		strength = computeStrength();
	}
	//			if (line.length == 2) {
	//				String wordL = line[0].trim();
	//				String wordR = line[1].trim();

	public static Constraint loadFromPaths(String humanPath, String lexiconPath,
			SequenceFactory<Double> factory) throws IOException {
		return loadFromPaths(humanPath, lexiconPath, null, factory);
	}
	//				if (!wordL.isEmpty() && !wordR.isEmpty()) {
	//					Sequence left  = factory.getSequence(wordL);
	//					Sequence right = factory.getSequence(wordR);

	public static LexiconConstraint loadFromPaths(String humanPath,
			String lexiconPath, Collection<String> keys,
			SequenceFactory<Double> factory) throws IOException {
		AlignmentSet alignments = AlignmentSet.loadFromFile(humanPath, factory);

		ColumnTable<Sequence<Double>> dataTable =
				Utilities.getPhoneticData(new File(lexiconPath), keys, factory,
						null);

		String path = EXTENSION.matcher(lexiconPath).replaceAll("");
		return new LexiconConstraint(path, factory, dataTable, alignments);
	}
	//					left.addFirst(border);
	//					right.addFirst(border);

	@Override
	public double evaluate(AlignmentAlgorithm algorithm) {

		//		List<Alignment> alignments = new ArrayList<>();
		//
		FeatureSpecification specification =
				factory.getFeatureMapping().getSpecification();
		Alignment<Double> blank = new Alignment<>(specification);
		//
		//		for (List<Sequence> sequences : data) {
		//			alignments.add(algorithm.getAlignment(sequences));
		//		}

		List<Alignment<Double>> alignments = algorithm.align(data);

		double sum = 0.0;

		int size = alignments.size();
		for (int i = 0; i < size; i++) {
			Alignment<Double> a = alignments.get(i);
			Alignment<Double> s = standard.get(i);

			if (s.getNumberColumns() != 0) {
				boolean equals = a.equals(s) && !a.equals(blank);
				sum += equals ? 1 : 0;
			}
		}

		return sum;
	}
	//					lexiconLeft.add(left);
	//					lexiconRight.add(right);
	//					numberValid += 1.0;
	//				} else {
	//					lexiconLeft.add(sequence);
	//					lexiconRight.add(sequence);
	//				}
	//			} else {
	//				lexiconLeft.add(sequence);
	//				lexiconRight.add(sequence);
	//			}
	//		}
	//		return numberValid;
	//	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public double getStrength() {
		return strength;
	}

	@Override
	public String toString() {
		return "LexiconConstraint{" + "factory=" + factory + ", data=" + data +
				", standard=" + standard + ", name='" + name + '\'' +
				", strength=" + strength + '}';
	}

	private int computeStrength() {
		//noinspection NumericCastThatLosesPrecision
		return (int) standard.stream()
				.filter(lists -> lists.getNumberColumns() > 0)
				.count();
	}

	private Sequence<Double> normalizeSymbols(
			Iterable<Segment<Double>> sequence) {
		FeatureMapping<Double> featureMapping = factory.getFeatureMapping();
		FeatureModel<Double> model = featureMapping.getFeatureModel();

		Sequence<Double> normalized = factory.getNewSequence();
		for (Segment<Double> segment : sequence) {
			FeatureArray<Double> features = segment.getFeatures();
			String symbol = featureMapping.findBestSymbol(features);
			normalized.add(new StandardSegment<>(symbol, features, model));
		}
		return normalized;
	}
}
