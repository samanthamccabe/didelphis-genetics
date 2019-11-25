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

package org.didelphis.genetics.learning;

import lombok.ToString;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.didelphis.utilities.Templates;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Class {@code AlignmentSupplier}
 *
 */
@ToString
public class FileAlignmentSupplier implements Supplier<List<String>> {

	private static final Logger LOG = LogManager.getLogger(FileAlignmentSupplier.class);

	private final BufferedReader reader;

	public FileAlignmentSupplier(String filePath) {
		BufferedReader reader = getReader(filePath);
		if (reader == null) {
			String message = Templates.create()
					.add("Fatal error: unable to load alignments from {}")
					.with(filePath)
					.build();
			throw new IllegalStateException(message);
		}
		this.reader = reader;
	}

	@Override
	public @Nullable List<String> get() {
		try {
			String line = reader.readLine();
			return Arrays.asList(line.split("\t"));
		} catch (IOException e) {
			LOG.error("Unexpected failure encountered: {}", e);
		}
		return null;
	}

	private static BufferedReader getReader(String path) {
		try {
			File file = new File(path);
			return new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			LOG.error("Failed to read from file: {}", path, e);
		}
		return null;
	}
}
