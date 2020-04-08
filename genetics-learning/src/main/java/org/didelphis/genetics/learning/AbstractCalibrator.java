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

package org.didelphis.genetics.learning;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.genetics.alignment.AlignmentResult;
import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.algorithm.AlignmentMode;
import org.didelphis.genetics.alignment.algorithm.NeedlemanWunschAlgorithm;
import org.didelphis.genetics.alignment.common.Utilities;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.comparators.ReinforcementComparator;
import org.didelphis.io.FileHandler;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.model.FeatureMapping;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.model.FeatureSpecification;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.PhoneticSequence;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.maps.GeneralTwoKeyMap;
import org.didelphis.structures.maps.interfaces.TwoKeyMap;
import org.didelphis.structures.tuples.Triple;
import org.didelphis.structures.tuples.Twin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode
@FieldDefaults (level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class AbstractCalibrator<T, P> {

	private static final Logger LOG = LogManager.getLogger(AbstractCalibrator.class);

	@Getter boolean useReinforcement;

	FileHandler handler;
	Sequence<T> gap;
	SequenceFactory<T> factory;
	FeatureModel<T> featureModel;

	List<Twin<Integer>> correlatedFeatures;
	Map<String, List<List<Alignment<T>>>> trainingData;

	Segment<T> lMerge;
	Segment<T> rMerge;

	protected AbstractCalibrator(
			FileHandler handler,
			Sequence<T> gap,
			SequenceFactory<T> factory,
			boolean useReinforcement
	) {
		this.handler = handler;
		this.gap = gap;
		this.factory = factory;
		this.useReinforcement = useReinforcement;

		featureModel = factory.getFeatureMapping().getFeatureModel();

		correlatedFeatures = new ArrayList<>();
		trainingData = new HashMap<>();

		lMerge = factory.toSegment("<");
		rMerge = factory.toSegment(">");
	}

	@NonNull
	abstract P optimize();

	@NonNull
	abstract AlignmentAlgorithm<T> toAlgorithm(@NonNull P parameters);

	protected abstract double getReinforcementWeight(@NonNull P parameter);

	@NonNull
	public Sequence<T> getGap() {
		return gap;
	}

	@NonNull
	public SequenceFactory<T> getFactory() {
		return factory;
	}

	@NonNull
	public List<Twin<Integer>> getCorrelatedFeatures() {
		return correlatedFeatures;
	}

	public void addCorrelation(
			@NonNull String feature1, @NonNull String feature2
	) {
		FeatureMapping<T> featureMapping = factory.getFeatureMapping();
		FeatureSpecification specification = featureMapping.getSpecification();
		Map<String, Integer> featureIndices = specification.getFeatureIndices();
		int indexLeft = featureIndices.getOrDefault(feature1, -1);
		int indexRight = featureIndices.getOrDefault(feature2, -1);
		correlatedFeatures.add(new Twin<>(indexLeft, indexRight));
	}

	public void addSDM(@NonNull String filePath) {
		String fileData;
		try {
			fileData = handler.read(filePath);
		} catch (IOException e) {
			return;
		}
		trainingData.put(filePath, Utilities.loadSDM(fileData, factory));
	}

	public double fitness(@NonNull P parameters, double sampleRate) {
		Set<String> filePaths = trainingData.keySet();
		AlignmentAlgorithm<T> algorithm = toAlgorithm(parameters);

		double reinforcementWeight = getReinforcementWeight(parameters);

		return evaluate(filePaths, algorithm, sampleRate, reinforcementWeight);
	}

	public void writeBestAlignments(
			@NonNull String path,
			@NonNull String fileName,
			@NonNull AlignmentAlgorithm<T> algorithm
	) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
			writer.write("Correct,Left,Right,Output Left, Output Right\n");
			List<List<Alignment<T>>> alignmentGroup = trainingData.get(path);
			for (List<Alignment<T>> alignments : alignmentGroup) {
				// Only retrieve the first alignment to create the sequences;
				// Any second entry that exists should create the same sequence
				Alignment<T> baseAlignment = alignments.get(0);

				List<String> charSequences = Alignment.buildPrettyAlignments(baseAlignment);
				if (charSequences.size() != 2) {
					continue;
				}

				List<Sequence<T>> sequences = getSequences(baseAlignment);
				AlignmentResult<T> result = algorithm.apply(
						sequences.get(0),
						sequences.get(1)
				);
				writer.write(matches(alignments, result) ? "1," : "0,");
				writer.write(charSequences.get(0) + "," + charSequences.get(1) + ",");
				List<String> list = Alignment.buildPrettyAlignments(result.getAlignments().get(0));
				for (CharSequence sequence : list) {
					writer.write(sequence + ",");
				}
				writer.write("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@NonNull
	private TwoKeyMap<Segment<T>, Segment<T>, Double> initMap() {
		return new GeneralTwoKeyMap<>(HashMap.class);
	}

	private void populateCorrespondences(
			TwoKeyMap<? super Segment<T>, ? super Segment<T>, Double> corrMap,
			AlignmentResult<T> result
	) {
		List<Alignment<T>> resultAlignments = result.getAlignments();
		int size = resultAlignments.size();
		if (size <= 0) {
			return;
		}
		Alignment<T> alignment = resultAlignments.get(0);
		for (int i = 0; i < alignment.columns(); i++) {
			List<Segment<T>> column = alignment.getColumn(i);
			Segment<T> s1 = column.get(0);
			Segment<T> s2 = column.get(1);
			if (corrMap.contains(s1, s2)) {
				Double value = corrMap.get(s1, s2);
				if (value == null) {
					corrMap.put(s1, s1, 1.0);
				} else {
					corrMap.put(s1, s1, value + 1);
				}
			} else {
				corrMap.put(s1, s2, 1.0);
			}
		}
	}

	private double evaluate(
			@NonNull Iterable<String> paths,
			@NonNull AlignmentAlgorithm<T> algorithm,
			double fraction,
			double reinforcementWeight
	) {

		Map<String, List<List<Alignment<T>>>> data = new HashMap<>();

		for (String path : paths) {
			List<List<Alignment<T>>> list = trainingData.get(path)
					.stream()
					.filter((alignments) -> Math.random() < fraction)
					.collect(Collectors.toList());
			data.put(path, list);
		}

		double correct = 0.0;
		double total = data.values()
				.stream()
				.mapToDouble(List::size)
				.sum();

		for (String path : paths) {
			TwoKeyMap<Segment<T>, Segment<T>, Double> corrMap = initMap();
			List<List<Alignment<T>>> get = data.get(path);
			int i = 0;
			for (List<Alignment<T>> alignments : get) {
				i++;
				// Only retrieve the first alignment to create the sequences;
				// Any second entry that exists should create the same sequence
				// TODO: Though it might be worth checking it for integrity
				Alignment<T> alignment = alignments.get(0);
				List<Sequence<T>> sequences = getSequences(alignment);

				if (sequences.size() < 2) {
					List<String> list = Alignment.buildPrettyAlignments(alignment);
					String join = String.join("\n\t\t", list);
					LOG.error("Data was found with incorrect format:\n\tPath: {}\n\tEntry: {}\n\tAlignment:{}", path, i, join);
					continue;
				}

				AlignmentResult<T> result = algorithm.apply(
						sequences.get(0),
						sequences.get(1)
				);

				if (useReinforcement) {
					populateCorrespondences(corrMap, result);
				} else if (matches(alignments, result)) {
					correct++;
				}
			}

			if (!useReinforcement) continue;

			AlignmentAlgorithm<T> rlAlgorithm = getReinforcementAlgorithm(
					algorithm,
					corrMap,
					reinforcementWeight
			);

			for (List<Alignment<T>> alignments : trainingData.get(path)) {
				List<Sequence<T>> sequences = getSequences(alignments.get(0));
				AlignmentResult<T> result2 = rlAlgorithm.apply(
						sequences.get(0),
						sequences.get(1)
				);
				if (matches(alignments, result2)) {
					correct++;
				}
			}
		}

		return total == 0.0 ? 0.0 : correct / total;
	}

	@NonNull
	private AlignmentAlgorithm<T> getReinforcementAlgorithm(
			@NonNull AlignmentAlgorithm<T> algorithm,
			@NonNull TwoKeyMap<Segment<T>, Segment<T>, Double> map,
			double reinforcementWeight
	) {
		double sum = map.stream().mapToDouble(Triple::third).sum();

		// Probability map
		TwoKeyMap<Segment<T>, Segment<T>, Double> pMap = getTriples(map, sum);

		SequenceComparator<T> comparator = algorithm.getComparator();
		SequenceComparator<T> rlComparator = new ReinforcementComparator<>(
				comparator,
				pMap,
				reinforcementWeight
		);

		return new NeedlemanWunschAlgorithm<>(
				algorithm.getOptimization(),
				AlignmentMode.GLOBAL,
				rlComparator,
				algorithm.getGapPenalty(),
				algorithm.getFactory()
		);
	}

	@NonNull
	private TwoKeyMap<Segment<T>, Segment<T>, Double> getTriples(
			@NonNull TwoKeyMap<Segment<T>, Segment<T>, Double> map, double sum
	) {

		List<Double> values = map.stream()
				.map(Triple::third)
				.collect(Collectors.toList());

		double max = values.stream().mapToDouble(d -> d).max().orElse(0);
		double min = values.stream().mapToDouble(d -> d).min().orElse(0);

		double mid = (max - min) / 2 + min;

		TwoKeyMap<Segment<T>, Segment<T>, Double> pMap = initMap();
		map.stream()
				.filter(t -> t.third() >= mid)
				.forEach(t -> pMap.put(t.first(), t.second(), t.third() / sum));
		return pMap;
	}

	@NonNull
	private List<Sequence<T>> getSequences(@NonNull Alignment<T> alignment) {
		List<Sequence<T>> sequences = new ArrayList<>();
		for (int i = 0; i < alignment.rows(); i++) {
			List<Segment<T>> list = alignment.getRow(i)
					.stream()
					.filter(segment -> !segment.equals(lMerge))
					.filter(segment -> !segment.equals(rMerge))
					.filter(segment -> !segment.equals(gap.get(0)))
					.collect(Collectors.toList());
			sequences.add(new PhoneticSequence<>(list, featureModel));
		}
		return sequences;
	}

	private static <T> boolean matches(
			Collection<Alignment<T>> alignments, AlignmentResult<T> result
	) {
		List<Alignment<T>> list = result.getAlignments();
		return alignments.stream().anyMatch(list::contains);
	}
}
