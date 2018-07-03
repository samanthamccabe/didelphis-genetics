package org.didelphis.genetics.alignment.reconstruction;

import org.didelphis.language.phonetic.features.FeatureArray;
import org.didelphis.genetics.alignment.correspondences.ContextSet;

/**
 * @author Samantha Fiona McCabe
 * Created: 4/8/2016
 */
@FunctionalInterface
public interface Reconstructor<T> {

	FeatureArray<T> evaluate(FeatureArray<T> left,
			FeatureArray<T> right, ContextSet contextSet);
}
