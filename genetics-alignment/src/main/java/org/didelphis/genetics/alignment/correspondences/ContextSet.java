package org.didelphis.genetics.alignment.correspondences;

import org.didelphis.genetics.alignment.Alignment;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 6/9/2015
 */
public class ContextSet {

	private final Collection<AlignmentContext> environments;
	private int count;

	public ContextSet() {
		environments = new HashSet<>();
	}

	public void addContext(Alignment<Double> ante, Alignment<Double> post) {
		environments.add(new AlignmentContext(ante, post));
	}

	@Override
	public int hashCode() {
		return environments.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ContextSet)) {
			return false;
		}

		ContextSet contextSet = (ContextSet) o;
		return environments.equals(contextSet.environments);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		Iterator<AlignmentContext> li = environments.iterator();
		while (li.hasNext()) {
			AlignmentContext context = li.next();
			sb.append(context);
			if (li.hasNext()) {
				sb.append('\t');
			}
		}
		return sb.toString();
	}

	public void increment() {
		count++;
	}

	public void decrement() {
		count--;
	}

	public int getCount() {
		return count;
	}
}
