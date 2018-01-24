/**
 * Copyright 2016ff. Humboldt-Universität zu Berlin.
 * Exploitation rights belong exclusively to Humboldt-Universität zu Berlin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Stephan Druskat (toolboxtextmodules@sdruskat.net) - initial API and implementation
 */
package org.corpus_tools.peppermodules.toolbox.text.data;

import java.util.List;

import org.apache.commons.lang3.Range;

/**
 * The definition of a subref. In brief, a subref is a unit within a
 * Toolbox reference block (usually a sentence), that can be
 * annotated independently from both tokens and the complete
 * ref. It is defined via a span of token indices.
 * 
 * For comprehensive documentation on subrefs, 
 * see <https://wikis.hu-berlin.de/melatamp/Clause_segmentation_and_annotation>.
 * 
 * A subref has a type and one or more ranges of token indices which it
 * spans. It can have a specified target layer (the lexical or morphological
 * token layer) and an identifier.
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 */
public class SubrefDefinition {

	/**
	 * Subref *type* enumerations.
	 *
	 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
	 */
	public enum SUBREF_TYPE {
		/**
		 * @see <a href="https://wikis.hu-berlin.de/melatamp/Clause_segmentation_and_annotation#SIMPLE">wiki</a>
		 */
		SIMPLE, 
		/**
		 * @see <a href="https://wikis.hu-berlin.de/melatamp/Clause_segmentation_and_annotation#SIMPLE_TARGETED">wiki</a>
		 */
		SIMPLE_TARGETED, 
		/**
		 * @see <a href="https://wikis.hu-berlin.de/melatamp/Clause_segmentation_and_annotation#UNIDENTIFIED_GLOBAL">wiki</a>		 
		 */
		UNIDENTIFIED_GLOBAL, 
		/**
		 * @see <a href="https://wikis.hu-berlin.de/melatamp/Clause_segmentation_and_annotation#UNIDENTIFIED_GLOBAL_TARGETED">wiki</a>		 
		 */
		UNIDENTIFIED_GLOBAL_TARGETED, 
		/**
		 * @see <a href="https://wikis.hu-berlin.de/melatamp/Clause_segmentation_and_annotation#IDENTIFIED_GLOBAL">wiki</a>		 
		 */
		IDENTIFIED_GLOBAL, 
		/**
		 * @see <a href="https://wikis.hu-berlin.de/melatamp/Clause_segmentation_and_annotation#IDENTIFIED_GLOBAL_TARGETED">wiki</a>		 
		 */
		IDENTIFIED_GLOBAL_TARGETED, 
		/**
		 * @see <a href="https://wikis.hu-berlin.de/melatamp/Clause_segmentation_and_annotation#DISCONTINUOUS_TARGETED">wiki</a>		 
		 */
		DISCONTINUOUS_TARGETED, 
		/**
		 * @see <a href="https://wikis.hu-berlin.de/melatamp/Clause_segmentation_and_annotation#FULL_REF_ANNOTATION">wiki</a>		 
		 */
		FULL_REF_ANNOTATION
	};

	private SUBREF_TYPE type;
	private String identifier;
	private List<Range<Integer>> ranges;
	private String targetLayer;

	/**
	 * @param type
	 * @param ranges
	 * @param identifier
	 * @param targetLayer
	 */
	public SubrefDefinition(SUBREF_TYPE type, List<Range<Integer>> ranges, String identifier, String targetLayer) {
		this.type = type;
		this.identifier = identifier;
		this.ranges = ranges;
		this.targetLayer = targetLayer;
	}

	/**
	 * @return the type
	 */
	public SUBREF_TYPE getType() {
		return type;
	}

	/**
	 * @return the identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * @return the ranges
	 */
	public List<Range<Integer>> getRanges() {
		return ranges;
	}

	/**
	 * @return the targetLayer
	 */
	public String getTargetLayer() {
		return targetLayer;
	}

}
