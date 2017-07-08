package org.didelphis.genetics.alignment.algorithm;

import org.didelphis.genetics.alignment.AlignmentResult;
import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Samantha Fiona McCabe
 * Created: 5/31/2015
 */

public interface AlignmentAlgorithm<T> {

	@NotNull
	@Contract("null -> fail")
	AlignmentResult<T> getAlignment(@NotNull List<Sequence<T>> sequences);

	@NotNull
	GapPenalty<T> getGapPenalty();

	@NotNull
	SequenceFactory<T> getFactory();

	@NotNull
	Comparator<T> getComparator();

	Optimization getOptimization();
}
