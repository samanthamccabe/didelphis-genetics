package org.didelphis.genetics.alignment.correspondences;

import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.language.phonetic.sequences.BasicSequence;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tuples.Tuple;
import org.didelphis.structures.tuples.Twin;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Samantha Fiona McCabe
 * Created: 8/31/2015
 */
public class EnvironmentMap<T> {
	private final Map<Segment<T>, Environment<T>> environments;
	private final SequenceFactory<T> factory;

	public EnvironmentMap(Iterable<Sequence<T>> column,
			SequenceFactory<T> factoryParam) {
		environments = new TreeMap<>();
		factory = factoryParam;

		// Populate data structure
		for (Sequence<T> sequence : column) {
			Sequence<T> head = factory.getSequence("");
			Sequence<T> tail = new BasicSequence<>(sequence);
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
		BasicSequence<T> nHead = new BasicSequence<>(head);
		BasicSequence<T> nTail = new BasicSequence<>(tail);
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
