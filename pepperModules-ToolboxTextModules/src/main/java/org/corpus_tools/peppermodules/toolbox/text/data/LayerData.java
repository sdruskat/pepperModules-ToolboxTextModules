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
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * TODO Description
 *
 * @author Stephan Druskat
 *
 */
public class LayerData {
	
	private static final Logger log = LoggerFactory.getLogger(LayerData.class);
	
	private ListMultimap<String, List<String>> data = ArrayListMultimap.create();
	private List<String> primaryData = new ArrayList<>();
	private ListMultimap<String, List<String>> annotations = ArrayListMultimap.create();
	private final ListMultimap<String, String> map;
	private final String originalPrimaryData;
	private final List<String> annotationMarkers;
	private final boolean segmented;
	private final String marker;
	private boolean isEmpty;
	private List<String> warnings = new ArrayList<>();
	
	/**
	 * @param markerContentMap
	 * @param marker
	 * @param annoMarkers
	 */
	public LayerData(ListMultimap<String, String> markerContentMap, String marker, String originalPrimaryData, List<String> annoMarkers, boolean segmented) {
		this.map = markerContentMap;
		this.originalPrimaryData = originalPrimaryData;
		this.annotationMarkers = annoMarkers;
		this.segmented = segmented;
		this.marker = marker;
	}
	
	public LayerData compile() {
		if (segmented) {
			primaryData.addAll(Arrays.asList(originalPrimaryData.split("\\s+")));
		}
		else {
			primaryData.add(originalPrimaryData.trim());
		}
		data.put(marker, primaryData);
		for (String annotationMarker : annotationMarkers) {
			for (String annotation : map.get(annotationMarker)) {
				if (segmented) {
					ArrayList<String> list = new ArrayList<>(Arrays.asList(annotation.split("\\s+")));
					annotations.put(annotationMarker, list);
					data.put(annotationMarker, list);
				}
				else {
					List<String> list = new ArrayList<>(Arrays.asList(annotation.trim()));
					annotations.put(annotationMarker, list);
					data.put(annotationMarker, list);
				}
			}
		}
		setEmpty(primaryData.isEmpty());
		if (!isEmpty && segmented) {
			runTests();
		}
		return this;
	}

	/**
	 * TODO: Description
	 *
	 */
	private void runTests() {
		for (Entry<String, List<String>> entry : annotations.entries()) {
			int annosN = entry.getValue().size(); 
			int primaryN = primaryData.size();
			if (annosN > primaryN) {
				warnings.add(": " + (annosN - primaryN) + " annotations too many on layer \"" + entry.getKey() + "\" (" + annosN + " annotations vs. " + primaryN + " " + marker + " tokens)!");
				// FIXME Do something about it: concatenate + errorcode + write errorcode (e.g. A+[entry.getKey] to new error line
			}
			else if (annosN < primaryN) {
				warnings.add(": " + (primaryN - annosN) + " annotations are missing on layer \"" + entry.getKey() + "\" (" + annosN + " annotations vs. " + primaryN + " " + marker + " tokens)!");
				// FIXME Do something about it: add + errorcode + write errorcode (e.g. A+[entry.getKey] to new error line
			}
		}
	}

	/**
	 * @return the data
	 */
	public final ListMultimap<String,List<String>> getData() {
		return data;
	}
	/**
	 * @param data the data to set
	 */
	public final void setData(ListMultimap<String, List<String>> data) {
		this.data = data;
	}
	/**
	 * @return the primaryData
	 */
	public final List<String> getPrimaryData() {
		return primaryData;
	}
	/**
	 * @param primaryData the primaryData to set
	 */
	public final void setPrimaryData(List<String> primaryData) {
		this.primaryData = primaryData;
	}
	/**
	 * @return the annotations
	 */
	public final ListMultimap<String,List<String>> getAnnotations() {
		return annotations;
	}
	/**
	 * @param annotations the annotations to set
	 */
	public final void setAnnotations(ListMultimap<String, List<String>> annotations) {
		this.annotations = annotations;
	}

	/**
	 * @return the isEmpty
	 */
	public final boolean isEmpty() {
		return isEmpty;
	}

	/**
	 * @param isEmpty the isEmpty to set
	 */
	public final void setEmpty(boolean isEmpty) {
		this.isEmpty = isEmpty;
	}

	/**
	 * TODO: Description
	 *
	 * @param docName
	 * @param ref
	 */
	public void warn(String docName, String ref) {
		if (!warnings.isEmpty()) {
			for (String warning: warnings) {
				log.warn("Document " + docName + ", reference " + ref + warning);
				System.err.println("Document " + docName + ", reference " + ref + warning);
			}
		}
		
	}

}
