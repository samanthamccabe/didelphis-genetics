package org.didelphis.genetics.alignment.algorithm;

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
@FunctionalInterface
public interface AlignmentAlgorithm<N> {

	@NotNull
	@Contract("null -> fail")
	Alignment<N> getAlignment(@NotNull List<Sequence<N>> sequences);

}
