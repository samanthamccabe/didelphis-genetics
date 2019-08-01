package org.didelphis.genetics.alignment;

import org.didelphis.language.phonetic.sequences.Sequence;

import java.util.List;

public class AlignmentSlice<T> {

	private Alignment<T> alignment;
	private int index;

	public AlignmentSlice(Alignment<T> alignment, int index) {
		this.alignment = alignment;
		this.index = index;
	}

	@Override
	public String toString() {
		return "("+index+") " + alignment;
	}
}
