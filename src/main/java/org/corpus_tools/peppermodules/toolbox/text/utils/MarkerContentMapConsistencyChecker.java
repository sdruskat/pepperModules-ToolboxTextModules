/*******************************************************************************
 * Copyright (c) 2016, 2018ff. Stephan Druskat
 * Exploitation rights for this version belong exclusively to Humboldt-Universität zu Berlin
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
 *     Stephan Druskat (mail@sdruskat.net) - initial API and implementation
 *******************************************************************************/
package org.corpus_tools.peppermodules.toolbox.text.utils;

import java.util.List;
import java.util.Set;

import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class checking whether the converted data
 * seems consistent.
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 *
 */
public class MarkerContentMapConsistencyChecker {
	
	private static final Logger LOG = LoggerFactory.getLogger(MarkerContentMapConsistencyChecker.class);
	
	private Set<String> set;
	private String refMarker;
	private String lexMarker;
	private String morphMarker;
	private String subrefMarker;
	private List<String> lexAnnoMarkers;
	private List<String> morphAnnoMarkers;
	private List<String> subrefAnnoMarkers;
	private List<String> refAnnoMarkers;
	private String ref;
	
	/**
	 * @param keySet
	 * @param refMarker
	 * @param lexMarker
	 * @param morphMarker
	 * @param subrefMarker
	 * @param lexAnnoMarkers
	 * @param morphAnnoMarkers
	 * @param subrefAnnoMarkers
	 * @param refAnnoMarkers
	 * @param refList 
	 */
	public MarkerContentMapConsistencyChecker(Set<String> keySet, String refMarker, String lexMarker, String morphMarker, String subrefMarker, List<String> lexAnnoMarkers, List<String> morphAnnoMarkers, List<String> subrefAnnoMarkers, List<String> refAnnoMarkers, List<String> refList) {
		this.set = keySet;
		this.refMarker = refMarker;
		this.lexMarker = lexMarker;
		this.morphMarker = morphMarker;
		this.subrefMarker = subrefMarker;
		this.lexAnnoMarkers = lexAnnoMarkers;
		this.morphAnnoMarkers = morphAnnoMarkers;
		this.subrefAnnoMarkers = subrefAnnoMarkers;
		this.refAnnoMarkers = refAnnoMarkers;
		this.ref = refList.get(0);
	}

	/**
	 * Runs the consistency check.
	 * 
	 * The consistency check itself removes all lines from
	 * the map mapping Toolbox markers to content one by one
	 * based on the supplied markers to be used and checks
	 * whether the resulting set is empty.
	 * 
	 * If there are data that haven't been taken into account,
	 * a {@link PepperModuleException} is thrown.
	 */
	public void run() {
		set.remove(refMarker);
		set.remove(lexMarker);
		set.remove(morphMarker);
		set.remove(subrefMarker);
		for (String marker : lexAnnoMarkers) {
			set.remove(marker);
		}
		for (String marker : morphAnnoMarkers) {
			set.remove(marker);
		}
		for (String marker : subrefAnnoMarkers) {
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
			LOG.debug("All markers have been caught.");
		}
	}

}
