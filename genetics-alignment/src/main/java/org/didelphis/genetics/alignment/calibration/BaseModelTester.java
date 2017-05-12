package org.didelphis.genetics.alignment.calibration;

import org.didelphis.common.language.phonetic.SequenceFactory;
import org.didelphis.common.language.phonetic.sequences.Sequence;
import org.didelphis.common.structures.tables.ColumnTable;
import org.didelphis.common.structures.tables.DataTable;
import org.didelphis.genetics.alignment.algorithm.AlignmentSet;
import org.didelphis.genetics.alignment.constraints.Constraint;
import org.didelphis.genetics.alignment.constraints.LexiconConstraint;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Samantha Fiona Morrigan McCabe
 * Created: 11/14/2015
 */
public abstract class BaseModelTester {

	private static final Pattern EXTENSION = Pattern.compile("\\.[^.]*");

	protected final Collection<Constraint> constraints;
	protected final SequenceFactory<Double> sequenceFactory;

	protected BaseModelTester(SequenceFactory<Double> factoryParam) {
		sequenceFactory = factoryParam;
		constraints = new HashSet<>();
	}

	protected void loadLexicon(File file, ColumnTable<Sequence<Double>> data)
			throws IOException {
		AlignmentSet alignments =
				AlignmentSet.loadFromFile(file.getAbsolutePath(),
						sequenceFactory);

		Map<String, List<Sequence<Double>>> subMap = new LinkedHashMap<>();

		for (String key : alignments.getKeys()) {
			subMap.put(key, data.getColumn(key));
		}

		ColumnTable<Sequence<Double>> dataSubset = new DataTable<>(subMap);
		String name = EXTENSION.matcher(file.getName()).replaceAll("");
		Constraint constraint =
				new LexiconConstraint(name, sequenceFactory, dataSubset,
						alignments);
		constraints.add(constraint);
	}
}
