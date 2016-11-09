/*******************************************************************************
 * Copyright 2016 Humboldt-Universität zu Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Stephan Druskat - initial API and implementation
 *******************************************************************************/
package org.corpus_tools.peppermodules.toolbox.text.mapping;

import java.util.Map.Entry;

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;

/**
 * This class provides mapping functionality to map lines
 * from a Toolbox file that are the \ref line or a following line
 * before the next \ref line the respective elements, e.g. 
 * {@link SToken}, {@link SSpan}, {@link SAnnotation}m etc.,
 * onto the target {@link SDocument}'s {@link SDocumentGraph}.
 *
 * @author Stephan Druskat
 *
 */
public class RefMapper extends AbstractBlockMapper {

	/**
	 * @param properties
	 * @param graph
	 * @param trimmedInputString
	 */
	public RefMapper(PepperModuleProperties properties, SDocumentGraph graph, String trimmedInputString) {
		super(properties, graph, trimmedInputString);
	}

	/**
	 * 
	 */
	@Override
	public void map() {
		System.err.println("\n\n--------------------------\n");
		for (Entry<String, String> entry : markerContentMap.entries()) {
			System.err.println(entry.getKey() + " : " + entry.getValue());
		}
		System.err.println("\n\n\n\n");
	}

}
