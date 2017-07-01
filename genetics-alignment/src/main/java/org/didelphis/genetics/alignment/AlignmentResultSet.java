package org.didelphis.genetics.alignment;

import org.didelphis.genetics.alignment.AlignmentResultSet.Result;
import org.didelphis.language.phonetic.features.FeatureType;
import org.didelphis.structures.tables.Table;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code AlignmentResult}
 *
 * Represents the complete output of an alignment process, intended mainly for
 * diagnostic or demonstration purposes. In addition to all alignments, it also
 * includes the edit distance matrix used in computing the alignment.
 *
 * {@code AlignmentResult} also functions as a POJO and is serializable to JSON
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0
 * Date: 2017-06-30
 */
public class AlignmentResultSet implements Iterable<Result>{

	private List<Result> results;

	public AlignmentResultSet() {
		results = new ArrayList<>();
	}

	public void add(Alignment<?> alignment, Table<Double> table) {
		results.add(new Result(alignment, table));
	}

	@NotNull
	@Override
	public Iterator<Result> iterator() {
		return results.iterator();
	}

	public List<Result> getResults() {
		return results;
	}

	public static final class Result {
		private Alignment<?> alignment;
		private Table<Double> table;

		private Result(Alignment<?> alignment, Table<Double> table) {
			this.alignment = alignment;
			this.table = table;
		}

		private Table<Double> getTable() {
			return table;
		}

		private Alignment<?> getAlignment() {
			return alignment;
		}
	}
}
