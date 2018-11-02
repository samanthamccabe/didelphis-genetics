package org.didelphis.genetics.learning;

import lombok.ToString;
import org.didelphis.utilities.Logger;
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
 * 
 * 
 * @author Samantha Fiona McCabe
 * @date 7/16/2018
 */
@ToString
public class FileAlignmentSupplier implements Supplier<List<String>> {
	
	private static final Logger LOG = Logger.create(FileAlignmentSupplier.class);
	
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
