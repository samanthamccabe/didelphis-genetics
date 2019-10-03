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

import lombok.NonNull;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tuples.Twin;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

public class Environment<T> implements Iterable<Twin<Sequence<T>>> {

	private final Collection<Twin<Sequence<T>>> environment;
	private final SequenceFactory<T> factory;

	public Environment(SequenceFactory<T> factoryParam) {
		environment = new TreeSet<>();
		factory = factoryParam;
	}

	@NonNull
	@Override
	public Iterator<Twin<Sequence<T>>> iterator() {
		return environment.iterator();
	}

	public void add(Twin<Sequence<T>> tuple) {
		environment.add(tuple);
	}

	public Environment<T> buildAbstraction() {
		Environment<T> absraction = new Environment<>(factory);

		FeatureModel<T> model = factory.getFeatureMapping().getFeatureModel();
		//		int n = model.getNumberOfFeatures();

		return absraction;
	}

	public int size() {
		return environment.size();
	}

	@Override
	public String toString() {
		return "Environment{" + "environment=" + environment + ", factory=" +
				factory + '}';
	}
}
