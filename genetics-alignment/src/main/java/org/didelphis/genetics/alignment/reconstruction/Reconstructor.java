package org.didelphis.genetics.alignment.reconstruction;

import org.didelphis.language.phonetic.features.FeatureArray;
import org.didelphis.genetics.alignment.correspondences.ContextSet;

/**
 * @author Samantha Fiona McCabe
 * Created: 4/8/2016
 */
@FunctionalInterface
public interface Reconstructor {

	FeatureArray<Integer> evaluate(FeatureArray<Integer> left,
			FeatureArray<Integer> right, ContextSet contextSet);
}
