package org.didelphis.genetics.alignment.operators.gap;

import org.didelphis.language.phonetic.ModelBearer;
import org.didelphis.language.phonetic.sequences.Sequence;

/**
 * @author Samantha Fiona McCabe
 * Created: 6/3/2015
 */
public interface GapPenalty<T> extends ModelBearer<T> {

	double evaluate(int currentGapLength);

	Sequence<T> getGap();

}
