package org.didelphis.genetics.alignment.operators.comparators;

import org.didelphis.common.language.phonetic.features.FeatureArray;
import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.genetics.alignment.operators.Comparator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by samantha on 8/4/16.
 */
public final class NdArrayComparator implements Comparator<Segment<Double>> {

	private static final transient Logger LOGGER =
			getLogger(NdArrayComparator.class);

	private final INDArray weights;

	public NdArrayComparator(double[] array) {
		weights = Nd4j.create(array);
	}

	@Override
	public Double apply(Segment<Double> left, Segment<Double> right) {
		INDArray l = getNdFeatureArray(left);
		INDArray r = getNdFeatureArray(right);

		INDArray dif = l.sub(r);
		// in-place element-wise multiplication
		dif.muli(weights);
		return dif.sumNumber().doubleValue();
	}

	private INDArray getNdFeatureArray(Segment left) {
		FeatureArray<Double> featureArray = left.getFeatures();
		INDArray ndArray;
		//		if (featureArray instanceof NdFeatureArray) {
		//			ndArray = ((NdFeatureArray) featureArray).getNdArray();
		//		} else {
		//			ndArray = new NdFeatureArray(featureArray).getNdArray();
		//		}
		//		return ndArray;
		return null; // TODO:
	}
}
