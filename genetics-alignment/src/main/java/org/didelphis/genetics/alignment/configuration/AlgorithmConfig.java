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

package org.didelphis.genetics.alignment.configuration;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import org.didelphis.genetics.alignment.algorithm.AlignmentAlgorithm;
import org.didelphis.genetics.alignment.algorithm.AlignmentMode;
import org.didelphis.genetics.alignment.algorithm.NeedlemanWunschAlgorithm;
import org.didelphis.genetics.alignment.algorithm.optimization.BaseOptimization;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.comparators.LinearWeightComparator;
import org.didelphis.genetics.alignment.operators.comparators.SparseMatrixComparator;
import org.didelphis.genetics.alignment.operators.gap.ConvexGapPenalty;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.language.phonetic.model.FeatureMapping;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.model.FeatureSpecification;
import org.didelphis.structures.maps.GeneralTwoKeyMap;
import org.didelphis.structures.maps.interfaces.TwoKeyMap;

import java.util.List;
import java.util.Map;

@Data
@FieldDefaults (level = AccessLevel.PRIVATE)
public class AlgorithmConfig {

	String gapSymbol;
	String modelPath;
	BaseOptimization optimization;
	AlignmentMode alignmentMode;
	ComponentConfig comparatorConfig;
	ComponentConfig penaltyConfig;

	public void setOptimization(String name) {
		optimization = BaseOptimization.byName(name);
	}

	public void setAlignmentMode(String name) {
		alignmentMode = AlignmentMode.valueOf(name);
	}

	public <T> AlignmentAlgorithm<T> buildAlgorithm(SequenceFactory<T> factory) {
		SequenceComparator<T> comparator = buildComparator(factory);
		GapPenalty<T> gapPenalty = buildPenalty(factory, gapSymbol);
		return new NeedlemanWunschAlgorithm<>(
				optimization,
				alignmentMode,
				comparator,
				gapPenalty,
				factory
		);
	}

	public <T> GapPenalty<T> buildPenalty(SequenceFactory<T> factory, String gap) {
		String type = penaltyConfig.getType();
		if (type.equals("convex")) {
			List<?> parameters = penaltyConfig.getParameters();
			return new ConvexGapPenalty<>(
					factory.toSequence(gap),
					(Double) parameters.get(0),
					(Double) parameters.get(1)
			);
		}
		throw new IllegalArgumentException("Unrecognized penalty type "+ type);
	}

	public <T> SequenceComparator<T> buildComparator(SequenceFactory<T> factory) {

		FeatureMapping<T> mapping     = factory.getFeatureMapping();
		FeatureModel<T>   model       = mapping.getFeatureModel();
		FeatureType<T>    featureType = model.getFeatureType();
		List<?>           parameters  = comparatorConfig.getParameters();

		FeatureSpecification spec    = model.getSpecification();
		Map<String, Integer> indices = spec.getFeatureIndices();

		String type = comparatorConfig.getType();
		if (type.equals("sparse")) {

			@SuppressWarnings ("unchecked")
			List<Double> list = (List<Double>) parameters.get(0);
			@SuppressWarnings ("unchecked") Map<String, Double> map = (Map<String, Double>) parameters.get(1);

			TwoKeyMap<Integer, Integer, Double> sparseWeights = new GeneralTwoKeyMap<>();
			for (Map.Entry<String, Double> entry : map.entrySet()) {
				String[] keys = entry.getKey().split("-");
				Integer k1 = indices.get(keys[0]);
				Integer k2 = indices.get(keys[1]);
				sparseWeights.put(k1, k2, entry.getValue());
			}

			return new SparseMatrixComparator<>(
					featureType,
					new LinearWeightComparator<>(featureType, list),
					sparseWeights
			);
		} else if (type.equals("linear")) {
			@SuppressWarnings ("unchecked")
			List<Double> list = (List<Double>) parameters.get(0);
			return new LinearWeightComparator<>(featureType, list);
		}
		throw new IllegalArgumentException("Unrecognized penalty type "+ type);
	}
}
