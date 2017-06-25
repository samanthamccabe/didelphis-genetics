package org.didelphis.genetics.alignment.calibration;

import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.ColumnTable;
import org.didelphis.genetics.alignment.AlignmentSet;
import org.didelphis.genetics.alignment.constraints.Constraint;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Samantha Fiona McCabe
 * Created: 11/14/2015
 */
public abstract class BaseModelTester {

	private static final Pattern EXTENSION = Pattern.compile("\\.[^.]*");

	protected final Collection<Constraint> constraints;
	protected final SequenceFactory<Integer> sequenceFactory;

	protected BaseModelTester(SequenceFactory<Integer> factoryParam) {
		sequenceFactory = factoryParam;
		constraints = new HashSet<>();
	}

	protected void loadLexicon(File file, ColumnTable<Sequence<Integer>> data)
			throws IOException {
		AlignmentSet<Integer> alignments =
				AlignmentSet.loadFromFile(file.getAbsolutePath(),
						sequenceFactory);

		Map<String, List<Sequence<Integer>>> subMap = new LinkedHashMap<>();

		for (String key : alignments.getKeys()) {
			subMap.put(key, data.getColumn(key));
		}
// TODO:
//		ColumnTable<Sequence<Integer>> dataSubset = new DataTable<>(subMap);
//		String name = EXTENSION.matcher(file.getName()).replaceAll("");
//		Constraint constraint =
//				new LexiconConstraint(name, sequenceFactory, dataSubset,
//						alignments);
//		constraints.add(constraint);
	}
}
