package org.didelphis.genetics.alignment.reconstruction;

import org.didelphis.common.language.phonetic.features.FeatureArray;
import org.didelphis.common.structures.tables.RectangularTable;
import org.didelphis.common.structures.tables.Table;
import org.didelphis.genetics.alignment.correspondences.ContextSet;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 4/9/2016
 */
public class SingleLayerReconstructor implements Reconstructor {

	private final int n; // number of features
	private final int m; // number of hidden nodes

	private final Table<Double> firstNetwork;
	private final Table<Double> secondNetwork;

	private final Collection<Double> thresholds; // for hidden-layer outputs

	public SingleLayerReconstructor(int n, int m) {
		this.n = n;
		this.m = m;

		firstNetwork = new RectangularTable<>(0.0, n, m);
		secondNetwork = new RectangularTable<>(0.0, m, n);

		thresholds = new ArrayList<>(m);
		for (int i = 0; i < m; i++) {
			thresholds.add(0.0);
		}
	}

	@Override
	public FeatureArray<Double> evaluate(FeatureArray<Double> left,
			FeatureArray<Double> right, ContextSet contextSet) {
		return null;
	}
}
