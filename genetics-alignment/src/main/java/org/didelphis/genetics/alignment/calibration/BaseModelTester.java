package org.didelphis.genetics.alignment.calibration;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.didelphis.genetics.alignment.AlignmentSet;
import org.didelphis.genetics.alignment.constraints.Constraint;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.sequences.Sequence;
import org.didelphis.structures.tables.ColumnTable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Samantha Fiona McCabe
 * Created: 11/14/2015
 */
@ToString
@EqualsAndHashCode
public class BaseModelTester<T> {

	private static final Pattern EXTENSION = Pattern.compile("\\.[^.]*");

	protected final Collection<Constraint<T>> constraints;
	protected final SequenceFactory<T> sequenceFactory;

	protected BaseModelTester(SequenceFactory<T> factoryParam) {
		sequenceFactory = factoryParam;
		constraints = new HashSet<>();
	}

	protected void loadLexicon(File file, ColumnTable<Sequence<T>> data) {
		AlignmentSet<T> alignments = null;
		try {
			alignments = AlignmentSet.loadFromFile(
					file.getAbsolutePath(),
					sequenceFactory
			);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Map<String, List<Sequence<T>>> subMap = new LinkedHashMap<>();

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
