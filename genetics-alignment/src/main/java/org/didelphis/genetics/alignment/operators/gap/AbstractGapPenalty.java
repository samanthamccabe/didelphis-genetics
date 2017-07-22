package org.didelphis.genetics.alignment.operators.gap;

import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.model.FeatureSpecification;
import org.didelphis.language.phonetic.sequences.Sequence;

/**
 * @author Samantha Fiona McCabe
 * Created: 6/3/2015
 */
public abstract class AbstractGapPenalty<N>
		implements GapPenalty<N> {

	private final Sequence<N> gap;

	protected AbstractGapPenalty(Sequence<N> gap) {
		this.gap = gap;
	}

	@Override
	public FeatureSpecification getSpecification() {
		return gap.getSpecification();
	}

	@Override
	public FeatureModel<N> getFeatureModel() {
		return gap.getFeatureModel();
	}

	@Override
	public Sequence<N> getGap() {
		return gap;
	}
}
