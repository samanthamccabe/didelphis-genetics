package org.didelphis.genetics.alignment.algorithm;


import org.didelphis.common.language.phonetic.sequences.Sequence;
import org.didelphis.common.structures.tables.ColumnTable;
import org.didelphis.genetics.alignment.Alignment;

import java.util.List;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 5/31/2015
 */
public interface AlignmentAlgorithm {

	Alignment<Double> getAlignment(List<Sequence<Double>> sequences);

	List<Alignment<Double>> align(ColumnTable<Sequence<Double>> data);
}
