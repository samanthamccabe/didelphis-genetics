package org.didelphis.genetics.alignment.reconstruction;

import org.didelphis.common.language.phonetic.features.FeatureArray;
import org.didelphis.genetics.alignment.correspondences.ContextSet;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 4/8/2016
 */
@FunctionalInterface
public interface Reconstructor {

	FeatureArray<Double> evaluate(FeatureArray<Double> left,
			FeatureArray<Double> right, ContextSet contextSet);
}
