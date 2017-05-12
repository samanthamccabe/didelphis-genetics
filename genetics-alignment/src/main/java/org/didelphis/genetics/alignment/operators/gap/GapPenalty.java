package org.didelphis.genetics.alignment.operators.gap;

import org.didelphis.common.language.phonetic.SpecificationBearer;
import org.didelphis.common.language.phonetic.segments.Segment;
import org.didelphis.genetics.alignment.Alignment;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 6/3/2015
 */
public interface GapPenalty extends SpecificationBearer {

	double evaluate(Alignment<Double> alignment);

	Segment<Double> getGapSegment();

}
