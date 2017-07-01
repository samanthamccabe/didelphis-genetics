package org.didelphis.genetics.alignment.algorithm;

import org.didelphis.genetics.alignment.operators.Comparator;
import org.didelphis.genetics.alignment.operators.gap.GapPenalty;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.ColumnTable;
import org.didelphis.genetics.alignment.Alignment;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Samantha Fiona McCabe
 * Created: 5/31/2015
 */

public interface AlignmentAlgorithm<N> {

	@NotNull
	@Contract("null -> fail")
	Alignment<N> getAlignment(@NotNull List<Sequence<N>> sequences);

	@NotNull
	GapPenalty<N> getGapPenalty();
	@NotNull
	SequenceFactory<N> getFactory();
	@NotNull
	Comparator<N, Double> getComparator();
}
