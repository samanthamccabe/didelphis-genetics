package org.didelphis.genetics.alignment.algorithm;

import org.didelphis.genetics.alignment.AlignmentResult;
import org.didelphis.genetics.alignment.algorithm.optimization.Optimization;
import org.didelphis.genetics.alignment.operators.SequenceComparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

/**
 * @author Samantha Fiona McCabe
 * Created: 5/31/2015
 */
public interface AlignmentAlgorithm<T>
		extends Function<List<? extends Sequence<T>>, AlignmentResult<T>> {

	@NotNull
	GapPenalty<T> getGapPenalty();

	@NotNull
	SequenceFactory<T> getFactory();

	@NotNull SequenceComparator<T> getComparator();

	@NotNull Optimization getOptimization();
}
