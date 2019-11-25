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

package org.didelphis.genetics.alignment.configuration;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import org.didelphis.genetics.alignment.common.StringTransformer;
import org.didelphis.language.parsing.FormatterMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@FieldDefaults (level = AccessLevel.PRIVATE)
public class DataFile {
	String path;
	String type;
	String groupName;
	Map<String, String> displayNames;
	Map<String, StringTransformer> transformations;
	List<List<String>> keys;

	public void setTransformations(Map<String, List<List<String>>> map) {
		transformations = new HashMap<>();
		for (Map.Entry<String, List<List<String>>> entry : map.entrySet()) {
			String key = entry.getKey();
			List<List<String>> value = entry.getValue();
			FormatterMode mode = FormatterMode.INTELLIGENT;
			StringTransformer transformer = new StringTransformer(value, mode);
			transformations.put(key, transformer);
		}
	}
}
