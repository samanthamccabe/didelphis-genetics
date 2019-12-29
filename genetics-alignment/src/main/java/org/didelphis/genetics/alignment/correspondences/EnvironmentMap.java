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

package org.didelphis.genetics.alignment.correspondences;

import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.PhoneticSequence;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tuples.Tuple;
import org.didelphis.structures.tuples.Twin;

import java.util.Map;
import java.util.TreeMap;

public class EnvironmentMap<T> {
	private final Map<Segment<T>, Environment<T>> environments;
	private final SequenceFactory<T> factory;

	public EnvironmentMap(Iterable<Sequence<T>> column,
			SequenceFactory<T> factoryParam) {
		environments = new TreeMap<>();
		factory = factoryParam;

		// Populate data structure
		for (Sequence<T> sequence : column) {
			Sequence<T> head = factory.toSequence("");
			Sequence<T> tail = new PhoneticSequence<>(sequence);
			while (!tail.isEmpty()) {
				Segment<T> segment = tail.remove(0);
				// segment is defensively copied inside
				add(segment, head, tail);
				head.add(segment);
			}
		}
	}

	public Environment<T> get(Segment<T> s) {
		return environments.get(s);
	}

	public final void add(Segment<T> segment, Sequence<T> head, Sequence<T> tail) {
		PhoneticSequence<T> nHead = new PhoneticSequence<>(head);
		PhoneticSequence<T> nTail = new PhoneticSequence<>(tail);
		Twin<Sequence<T>> tuple = new Twin<>(nHead, nTail);
		if (environments.containsKey(segment)) {
			Environment<T> tuples = environments.get(segment);
			tuples.add(tuple);
		} else {
			Environment<T> tuples = new Environment<>(factory);
			tuples.add(tuple);
			environments.put(segment, tuples);
		}
	}

	/**
	 * Finds the smallest set of environments using the most specific feature
	 * arrays possible
	 *
	 * @return
	 */
	public EnvironmentMap<T> buildAbstraction() {
//		EnvironmentMap<T> abstraction = new EnvironmentMap<>();

		FeatureModel<T> featureModel =
				factory.getFeatureMapping().getFeatureModel();

		//		int n = featureModel.getNumberOfFeatures();

		for (Map.Entry<Segment<T>, Environment<T>> entry : environments.entrySet()) {
			entry.getKey();
			for (Tuple<Sequence<T>, Sequence<T>> tuple : entry.getValue()) {
			}
		}

//		return abstraction;
		return null;
	}

	@Override
	public String toString() {
		return "EnvironmentMap{" + "environments=" + environments +
				", factory=" + factory + '}';
	}
}
