package org.didelphis.genetics.alignment.correspondences;

import org.didelphis.common.language.phonetic.SequenceFactory;
import org.didelphis.common.language.phonetic.model.interfaces.FeatureModel;
import org.didelphis.common.language.phonetic.sequences.Sequence;
import org.didelphis.common.structures.tuples.Tuple;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Created by samantha on 9/20/15.
 */
public class Environment
		implements Iterable<Tuple<Sequence<Double>, Sequence<Double>>> {

	private final Collection<Tuple<Sequence<Double>, Sequence<Double>>>
			environment;
	private final SequenceFactory<Double> factory;

	public Environment(SequenceFactory<Double> factoryParam) {
		environment = new TreeSet<>();
		factory = factoryParam;
	}

	@Override
	public Iterator<Tuple<Sequence<Double>, Sequence<Double>>> iterator() {
		return environment.iterator();
	}

	public void add(Tuple<Sequence<Double>, Sequence<Double>> tuple) {
		environment.add(tuple);
	}

	public Environment buildAbstraction() {
		Environment absraction = new Environment(factory);

		FeatureModel<Double> model =
				factory.getFeatureMapping().getFeatureModel();
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
