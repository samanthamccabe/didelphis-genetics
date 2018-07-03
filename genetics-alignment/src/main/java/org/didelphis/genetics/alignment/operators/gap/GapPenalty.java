package org.didelphis.genetics.alignment.operators.gap;

import org.didelphis.language.phonetic.ModelBearer;
import org.didelphis.language.phonetic.sequences.Sequence;

import java.util.function.IntToDoubleFunction;

/**
 * @author Samantha Fiona McCabe
 * Created: 6/3/2015
 */
public interface GapPenalty<T> extends ModelBearer<T>, IntToDoubleFunction {

	Sequence<T> getGap();

}
