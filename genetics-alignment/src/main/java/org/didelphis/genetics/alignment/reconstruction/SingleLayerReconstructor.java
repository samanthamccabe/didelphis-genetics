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

package org.didelphis.genetics.alignment.reconstruction;

import org.didelphis.language.phonetic.features.FeatureArray;
import org.didelphis.structures.tables.RectangularTable;
import org.didelphis.structures.tables.Table;
import org.didelphis.genetics.alignment.correspondences.ContextSet;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Samantha Fiona McCabe
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
	public FeatureArray evaluate(FeatureArray left, FeatureArray right,
			ContextSet contextSet
	) {
		return null;
	}
}
