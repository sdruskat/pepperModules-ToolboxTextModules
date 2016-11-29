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
package org.corpus_tools.peppermodules.toolbox.text.utils;

/**
 * TODO Description
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 *
 */
public class MappingIndices {
	
	private int lexTimelineIndex = 0;
	private int morphTimelineIndex = 0;
	private int lexDSIndex = 0;
	private int morphDSIndex = 0;
	
	/**
	 * @return the lexTimelineIndex
	 */
	public final int getLexTimelineIndex() {
		return lexTimelineIndex;
	}
	/**
	 * @param lexTimelineIndex the lexTimelineIndex to set
	 */
	public final void setLexTimelineIndex(int lexTimelineIndex) {
		this.lexTimelineIndex = lexTimelineIndex;
	}
	/**
	 * @return the lexDSIndex
	 */
	public final int getLexDSIndex() {
		return lexDSIndex;
	}
	/**
	 * @param lexDSIndex the lexDSIndex to set
	 */
	public final void setLexDSIndex(int lexDSIndex) {
		this.lexDSIndex = lexDSIndex;
	}
	/**
	 * @return the morphDSIndex
	 */
	public final int getMorphDSIndex() {
		return morphDSIndex;
	}
	/**
	 * @param morphDSIndex the morphDSIndex to set
	 */
	public final void setMorphDSIndex(int morphDSIndex) {
		this.morphDSIndex = morphDSIndex;
	}
	/**
	 * @return the morphTimelineIndex
	 */
	public final int getMorphTimelineIndex() {
		return morphTimelineIndex;
	}
	/**
	 * @param morphTimelineIndex the morphTimelineIndex to set
	 */
	public final void setMorphTimelineIndex(int morphTimelineIndex) {
		this.morphTimelineIndex = morphTimelineIndex;
	}
	
	

}
