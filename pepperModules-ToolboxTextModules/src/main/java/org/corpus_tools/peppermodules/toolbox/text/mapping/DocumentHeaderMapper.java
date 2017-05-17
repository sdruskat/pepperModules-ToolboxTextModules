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

import java.util.regex.Pattern;

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.core.SMetaAnnotation;

/**
 * This class provides mapping functionality to map lines
 * from a Toolbox file that are the \id line or a following line
 * before the first \ref line to the name of the {@link SDocument}
 * being mapped onto, and {@link SMetaAnnotation}s on it
 * respectively.
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 *
 */
public class DocumentHeaderMapper extends AbstractBlockMapper {

	/**
	 * @param properties 
	 * @param graph
	 * @param trimmedInputString
	 */
	public DocumentHeaderMapper(PepperModuleProperties properties, SDocumentGraph graph, String trimmedInputString) {
		super(properties, graph, trimmedInputString);
	}
	
	/**
	 * For the prepared list of lines from the \id (= document)
	 * header in the Toolbox file to be mapped, if the line
	 * starts with the ID marker (default: \id), set its
	 * following contents (after, e.g., "\id ") to the name
	 * field of the {@link SDocument} being mapped to, and
	 * for all other lines, create an {@link SMetaAnnotation}
	 * on the document, using the result of a {@link String#split(String, int)}
	 * operation (with the parameter 2, so that the splitting
	 * occurs a maximal times of parameter - 1 times = once) by
	 * setting the meta annotation's name to the first, and its
	 * value to the second index in the split's resulting {@link String}
	 * array.
	 */
	@Override
	public boolean map() {
		for (String line : lines) {
			if (line.startsWith("\\" + properties.getIdMarker() + " ")) {
				String name = line.split("\\s+", 2)[1];
				if (properties.normalizeDocNames()) {
					String d1 = name.replaceAll(" ", "-");
					String d2 = d1.replaceAll("\\.", "_");
					String d3 = d2.replaceAll("\\n", "_");
					String d4 = d3.replaceAll(":", "_");
					String d5 = d4.replaceAll(",", "_");
					String d6 = d5.replaceAll("-", "_");
					String d7 = d6.replaceAll(Pattern.quote("("), "");
					String d8 = d7.replaceAll(Pattern.quote(")"), "");
					name = d8;
				}
				graph.getDocument().setName(name);
			}
			else {
				String[] markerContent = line.split("\\s+", 2);
				if (graph.getDocument().getMetaAnnotation(super.SALT_NAMESPACE_TOOLBOX + "::" + markerContent[0].substring(1)) != null) {
					String oldVal = graph.getDocument().getMetaAnnotation(super.SALT_NAMESPACE_TOOLBOX + "::" + markerContent[0].substring(1)).getValue_STEXT();
					String newVal = oldVal + " " + markerContent[1];
					graph.getDocument().getMetaAnnotation(super.SALT_NAMESPACE_TOOLBOX + "::" + markerContent[0].substring(1)).setValue(newVal);
				}
				else {
					graph.getDocument().createMetaAnnotation(super.SALT_NAMESPACE_TOOLBOX, markerContent[0].substring(1), markerContent[1]);
				}
			}
		}
		return true;
	}

}
