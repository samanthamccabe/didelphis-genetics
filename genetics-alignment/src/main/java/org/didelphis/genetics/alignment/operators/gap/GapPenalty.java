package org.didelphis.genetics.alignment.operators.gap;

import org.didelphis.language.phonetic.SpecificationBearer;
import org.didelphis.language.phonetic.segments.Segment;
import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.language.phonetic.sequences.Sequence;

/**
 * @author Samantha Fiona McCabe
 * Created: 6/3/2015
 */
public interface GapPenalty<N> extends SpecificationBearer {

	double evaluate(Alignment<N> alignment);

	Sequence<N> getGap();

}
