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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * TODO Description
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 *
 */
public class LayerData {
	
	private List<String> primaryData = new ArrayList<>();
	private ListMultimap<String, List<String>> annotations = ArrayListMultimap.create();
	private final ListMultimap<String, String> map;
	private final String originalPrimaryData;
	private final List<String> annotationMarkers;
	private boolean segmented;
	private final String marker;
	protected String docName;
	protected String ref;
	
	/**
	 * @param markerContentMap
	 * @param marker
	 * @param annoMarkers
	 * @param ref2 
	 * @param string 
	 * @param fixErrors2 
	 * @param missingAnnoString2 
	 * @param b 
	 */
	public LayerData(ListMultimap<String, String> markerContentMap, String marker, String originalPrimaryData, List<String> annoMarkers, boolean segmented, String missingAnnoString, boolean fixErrors, String docName, String ref) {
		this.map = markerContentMap;
		this.marker = marker;
		this.originalPrimaryData = originalPrimaryData;
		this.annotationMarkers = annoMarkers;
		this.segmented = segmented;
		this.docName = docName;
		this.ref = ref;
	}
	
	public LayerData compile() {
		if (segmented) {
			/*
			 * "Tokenization", i.e., a simple split on whitespace(s).
			 * This is leaving the complexity of tokenization with
			 * Toolbox (users), which is permissible as it should be assumed
			 * that Toolbox provides correct data. Yea, right.
			 */
			primaryData.addAll(Arrays.asList(originalPrimaryData.split("\\s+")));
		}
		else {
			primaryData.add(originalPrimaryData.trim());
		}
		for (String annotationMarker : annotationMarkers) {
			for (String annotation : map.get(annotationMarker)) {
				if (segmented) {
					ArrayList<String> list = new ArrayList<>(Arrays.asList(annotation.split("\\s+")));
					annotations.put(annotationMarker, list);
				}
				else {
					List<String> list = new ArrayList<>(Arrays.asList(annotation.trim()));
					annotations.put(annotationMarker, list);
				}
			}
		}
		setEmpty(primaryData.isEmpty());
		return this;
	}

	/**
	 * TODO: Description
	 *
	 * @param key
	 * @param value
	 */
	public void addAnnotation(String key, List<String> value) {
		annotations.put(key, value);
		
	}

	/**
	 * TODO: Description
	 *
	 * @param key
	 * @param value
	 */
	public void addToAnnotation(String key, String value) {
		if (annotations.get(key).size() > 0) {
			List<String> oldList = new ArrayList<>(annotations.get(key).get(0));
			annotations.remove(key, oldList);
			oldList.add(value);
			annotations.put(key, new ArrayList<>(oldList));
		}
		else {
			annotations.put(key, Arrays.asList(new String[]{value}));
		}
	}

	/**
	 * @return the primaryData
	 */
	public List<String> getPrimaryData() {
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
	public ListMultimap<String,List<String>> getAnnotations() {
		return annotations;
	}
	/**
	 * @param annotations the annotations to set
	 */
	public final void setAnnotations(ListMultimap<String, List<String>> annotations) {
		this.annotations = annotations;
	}

	/**
	 * @param isEmpty the isEmpty to set
	 */
	public final void setEmpty(boolean isEmpty) {
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.docName + "::" + this.ref + ":" + this.marker + "\n");
		sb.append(this.getPrimaryData() + "\n");
		for (Entry<String, List<String>> anno : this.getAnnotations().entries()) {
			sb.append("    " + anno.getKey() + ":" + anno.getValue() + "\n");
		}
		return sb.toString().trim();
	}

	/**
	 * @return the marker
	 */
	public String getMarker() {
		return marker;
	}

	/**
	 * @return the docName
	 */
	public String getDocName() {
		return docName;
	}

	/**
	 * @return the ref
	 */
	public String getRef() {
		return ref;
	}

}
