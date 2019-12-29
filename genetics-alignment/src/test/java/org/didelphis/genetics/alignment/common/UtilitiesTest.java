/******************************************************************************
 * General components for language modeling and analysis                      *
 *                                                                            *
 * Copyright (C) 2014-2019 Samantha F McCabe                                  *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.     *
 *                                                                            *
 ******************************************************************************/

package org.didelphis.genetics.alignment.common;

import lombok.NonNull;

import org.didelphis.genetics.alignment.Alignment;
import org.didelphis.io.ClassPathFileHandler;
import org.didelphis.language.parsing.FormatterMode;
import org.didelphis.language.phonetic.SequenceFactory;
import org.didelphis.language.phonetic.features.IntegerFeature;
import org.didelphis.language.phonetic.model.FeatureMapping;
import org.didelphis.language.phonetic.model.FeatureModelLoader;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UtilitiesTest {

	private static final ClassPathFileHandler handler = ClassPathFileHandler.INSTANCE;

	@Test
	void dsvToTable() {
	}

	@Test
	void csvToTable() {
	}

	@Test
	void tsvToTable() {
	}

	@Test
	void testDsvToTable() {
	}

	@Test
	void testCsvToTable() {
	}

	@Test
	void testTsvToTable() {
	}

	@Test
	void toPhoneticTable() {
	}

	@Test
	void testToPhoneticTable() {
	}

	@Test
	void loadMatrixComparator() {
	}

	@Test
	void toAlignments() {
	}

	@Test
	void formatStrings() {
	}

	@Test
	void format() {
	}

	@Test
	void computeEnvironments() {
	}

	@Test
	void getTupleDistances() {
	}

	@Test
	void computeSegmentCounts() {
	}

	@Test
	void loadSDM() throws IOException {
		String read = handler.read("test.sdm");

		FeatureModelLoader<Integer> loader
				= new FeatureModelLoader<>(
				IntegerFeature.INSTANCE,
				handler,
				"AT_extended_x.model"
		);

		FeatureMapping<Integer> featureMapping = loader.getFeatureMapping();
		SequenceFactory<Integer> factory = new SequenceFactory<>(
				featureMapping,
				FormatterMode.INTELLIGENT
		);
		List<List<Alignment<Integer>>> lists = Utilities.loadSDM(read, factory);
		for (int i = 0; i < lists.size(); i++) {
			List<Alignment<Integer>> list = lists.get(i);
			assertFalse(list.isEmpty(), "Block " + i + " was empty");
			for (int j = 0; j < list.size(); j++) {
				Alignment<Integer> alignment = list.get(j);
				assertTrue(alignment.columns() > 0, getMessage(i, j));
				assertTrue(alignment.rows() > 0, getMessage(i, j));
			}
		}
	}

	@NonNull
	private String getMessage(int i, int j) {
		return "Alignment " + j + " in block " + i + " was empty";
	}

	@Test
	void toAlignment() {
	}

	@Test
	void toSequences() {
	}
}
