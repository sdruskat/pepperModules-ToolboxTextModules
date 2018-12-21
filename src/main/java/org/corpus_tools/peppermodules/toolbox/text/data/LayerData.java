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
package org.corpus_tools.peppermodules.toolbox.text.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * A creator type for specific annotation layer data.
 * 
 * The constructor is passed raw data, which by {@link #compile()}
 * is subsequently compiled to data processable downstream, i.e.,
 * 
 * - Primary data
 * - Annotations on the primary data
 * - Reference data for the reference (Toolbox' \ref) governing the data
 * - The respective annotation layer marker
 * - The name of the document containing the data
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
	 * @param originalPrimaryData 
	 * @param annoMarkers
	 * @param segmented 
	 * @param missingAnnoString 
	 * @param fixErrors 
	 * @param docName 
	 * @param ref 
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
	
	/**
	 * Compiles the primary data and annotations for
	 * the annotation layer.
	 * 
	 * @return The compiled {@link LayerData} object containing all relevant data.
	 */
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
		return this;
	}

	/**
	 * Adds an annotation key and its value to the map
	 * of annotations for this layer.
	 *
	 * @param key The annotation key
	 * @param value The value for this annotation
	 */
	public void addAnnotation(String key, List<String> value) {
		annotations.put(key, value);
		
	}

	/**
	 * Adds an annotation to a layer's annotations.
	 * 
	 * This method is used for ad hoc additions to the
	 * annotations, e.g., for adding a layer recording errors.
	 *
	 * @param key The annotation key
	 * @param value The value for this annotation
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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
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
