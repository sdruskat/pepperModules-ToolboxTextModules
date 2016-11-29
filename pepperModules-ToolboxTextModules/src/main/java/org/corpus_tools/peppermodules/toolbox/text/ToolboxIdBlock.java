/*******************************************************************************
 * Copyright 2016 Stephan Druskat
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
package org.corpus_tools.peppermodules.toolbox.text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.corpus_tools.salt.common.SSpan;

/**
 * Model object representing an \id block in a toolbox file.
 * \id blocks can contain \id-level annotations and 1* ref
 * blocks, i.e., spans.
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 *
 */
public class ToolboxIdBlock {
	
	private Map<String, String> idAnnotations = new HashMap<>();
	private Set<SSpan> refs = new HashSet<>();
	private String id = null;
	private boolean isHeaderMapped = false;
	
	public boolean reset() {
		getIdAnnotations().clear();
		getRefs().clear();
		id = null;
		setHeaderMapped(false);
		return getIdAnnotations().isEmpty() && getRefs().isEmpty();
	}

	/**
	 * @return the idAnnotations
	 */
	public Map<String, String> getIdAnnotations() {
		return idAnnotations;
	}

	/**
	 * @param idAnnotations the idAnnotations to set
	 */
	public void setIdAnnotations(Map<String, String> idAnnotations) {
		this.idAnnotations = idAnnotations;
	}

	/**
	 * @return the refs
	 */
	public Set<SSpan> getRefs() {
		return refs;
	}

	/**
	 * @param refs the refs to set
	 */
	public void setRefs(Set<SSpan> refs) {
		this.refs = refs;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String marker, String rawId) {
		String id = rawId.split("\\s*\\\\" + marker + "\\s*")[1].trim();
		this.id = id;
	}

	/**
	 * @return the isHeaderMapped
	 */
	public boolean isHeaderMapped() {
		return isHeaderMapped;
	}

	/**
	 * @param isHeaderMapped the isHeaderMapped to set
	 */
	public void setHeaderMapped(boolean isHeaderMapped) {
		this.isHeaderMapped = isHeaderMapped;
	}

}
