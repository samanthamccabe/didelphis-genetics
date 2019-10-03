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

package org.didelphis.genetics.alignment.operators.gap;

import org.didelphis.language.phonetic.model.FeatureModel;
import org.didelphis.language.phonetic.model.FeatureSpecification;
import org.didelphis.language.phonetic.sequences.Sequence;

public abstract class AbstractGapPenalty<T>
		implements GapPenalty<T> {

	private final Sequence<T> gap;

	protected AbstractGapPenalty(Sequence<T> gap) {
		this.gap = gap;
	}

	@Override
	public FeatureSpecification getSpecification() {
		return gap.getSpecification();
	}

	@Override
	public FeatureModel<T> getFeatureModel() {
		return gap.getFeatureModel();
	}

	@Override
	public Sequence<T> getGap() {
		return gap;
	}
}
