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
package org.corpus_tools.peppermodules.toolbox.text.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Range;
import org.corpus_tools.peppermodules.toolbox.text.data.SubrefDefinition.SUBREF_TYPE;

/**
 * TODO Description
 *
 * @author Stephan Druskat
 *
 */
public class SubrefDefinition {

	/**
	 * TODO Description
	 *
	 * @author Stephan Druskat
	 *
	 */
	public enum SUBREF_TYPE {
		SIMPLE, SIMPLE_TARGETED, UNIDENTIFIED_GLOBAL, UNIDENTIFIED_GLOBAL_TARGETED, IDENTIFIED_GLOBAL, IDENTIFIED_GLOBAL_TARGETED, DISCONTINUOUS_TARGETED, FULL_REF_ANNOTATION
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
