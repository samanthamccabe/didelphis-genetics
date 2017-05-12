package org.didelphis.genetics.alignment.correspondences;

import org.didelphis.common.language.enums.FormatterMode;
import org.didelphis.common.language.phonetic.SequenceFactory;
import org.didelphis.common.language.phonetic.model.empty.EmptyFeatureMapping;
import org.didelphis.common.language.phonetic.model.interfaces.FeatureModel;
import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.common.language.phonetic.sequences.BasicSequence;
import org.didelphis.common.language.phonetic.sequences.Sequence;
import org.didelphis.common.structures.tuples.Tuple;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 8/31/2015
 */
public class EnvironmentMap {
	private final Map<Segment<Double>, Environment> environments;
	private final SequenceFactory<Double> factory;

	public EnvironmentMap() {
		environments = new HashMap<>();
		factory = new SequenceFactory<>(EmptyFeatureMapping.DOUBLE,
				FormatterMode.NONE);
	}

	public EnvironmentMap(Iterable<Sequence<Double>> column,
			SequenceFactory<Double> factoryParam) {
		environments = new TreeMap<>();
		factory = factoryParam;

		// Populate data structure
		for (Sequence<Double> sequence : column) {
			Sequence<Double> head = factory.getNewSequence();
			Sequence<Double> tail = new BasicSequence<>(sequence);
			while (!tail.isEmpty()) {
				Segment<Double> segment = tail.remove(0);
				// segment is defensively copied inside
				add(segment, head, tail);
				head.add(segment);
			}
		}
	}

	public Environment get(Segment<? extends Number> s) {
		return environments.get(s);
	}

	public final void add(Segment<Double> segment, Sequence<Double> head,
			Sequence<Double> tail) {
		Tuple<Sequence<Double>, Sequence<Double>> tuple =
				new Tuple<>(new BasicSequence<>(head),
						new BasicSequence<>(tail));
		if (environments.containsKey(segment)) {
			Environment tuples = environments.get(segment);
			tuples.add(tuple);
		} else {
			Environment tuples = new Environment(factory);
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
	public EnvironmentMap buildAbstraction() {
		EnvironmentMap abstraction = new EnvironmentMap();

		FeatureModel<? extends Number> featureModel =
				factory.getFeatureMapping().getFeatureModel();

		//		int n = featureModel.getNumberOfFeatures();

		for (Map.Entry<Segment<Double>, Environment> entry : environments.entrySet()) {
			entry.getKey();
			for (Tuple<Sequence<Double>, Sequence<Double>> tuple : entry.getValue()) {
			}
		}


		return abstraction;
	}

	@Override
	public String toString() {
		return "EnvironmentMap{" + "environments=" + environments +
				", factory=" + factory + '}';
	}
}
