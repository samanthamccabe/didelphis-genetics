package org.didelphis.genetics.alignment.correspondences;

import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tuples.Tuple;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Created by samantha on 9/20/15.
 */
public class Environment<T> implements Iterable<Tuple<Sequence<T>, Sequence<T>>> {

	private final Collection<Tuple<Sequence<T>, Sequence<T>>> environment;
	private final SequenceFactory<T> factory;

	public Environment(SequenceFactory<T> factoryParam) {
		environment = new TreeSet<>();
		factory = factoryParam;
	}

	@NotNull
	@Override
	public Iterator<Tuple<Sequence<T>, Sequence<T>>> iterator() {
		return environment.iterator();
	}

	public void add(Tuple<Sequence<T>, Sequence<T>> tuple) {
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
