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

import java.util.List;
import java.util.Set;

import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Description
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 *
 */
public class MarkerContentMapConsistencyChecker {
	
	private static final Logger log = LoggerFactory.getLogger(MarkerContentMapConsistencyChecker.class);
	
	Set<String> set;
	String refMarker;
	String lexMarker;
	String morphMarker;
	String unitrefMarker;
	List<String> lexAnnoMarkers;
	List<String> morphAnnoMarkers;
	List<String> unitrefAnnoMarkers;
	List<String> refAnnoMarkers;
	private String ref;
	
	/**
	 * @param keySet
	 * @param refMarker
	 * @param lexMarker
	 * @param morphMarker
	 * @param unitrefMarker
	 * @param lexAnnoMarkers
	 * @param morphAnnoMarkers
	 * @param unitrefAnnoMarkers
	 * @param refAnnoMarkers
	 */
	public MarkerContentMapConsistencyChecker(Set<String> keySet, String refMarker, String lexMarker, String morphMarker, String unitrefMarker, List<String> lexAnnoMarkers, List<String> morphAnnoMarkers, List<String> unitrefAnnoMarkers, List<String> refAnnoMarkers, List<String> refList) {
		this.set = keySet;
		this.refMarker = refMarker;
		this.lexMarker = lexMarker;
		this.morphMarker = morphMarker;
		this.unitrefMarker = unitrefMarker;
		this.lexAnnoMarkers = lexAnnoMarkers;
		this.morphAnnoMarkers = morphAnnoMarkers;
		this.unitrefAnnoMarkers = unitrefAnnoMarkers;
		this.refAnnoMarkers = refAnnoMarkers;
		this.ref = refList.get(0);
	}

	public void run() {
		set.remove(refMarker);
		set.remove(lexMarker);
		set.remove(morphMarker);
		set.remove(unitrefMarker);
		for (String marker : lexAnnoMarkers) {
			set.remove(marker);
		}
		for (String marker : morphAnnoMarkers) {
			set.remove(marker);
		}
		for (String marker : unitrefAnnoMarkers) {
			set.remove(marker);
		}
		for (String marker : refAnnoMarkers) {
			set.remove(marker);
		}
		if (!set.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (String entry : set) {
				sb.append(entry + ", ");
			}
			throw new PepperModuleException("Failed to catch all markers used in reference \"" + ref + "\": \'" + sb.toString().trim() + "\'! Please report this as a bug, attaching this message and the respective file!");
		}
		else {
			log.debug("All markers have been caught.");
		}
	}

}
