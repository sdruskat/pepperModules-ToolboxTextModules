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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporterProperties;
import org.corpus_tools.peppermodules.toolbox.text.data.LayerData;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ListMultimap;

/**
 * TODO Description
 * Subrefs are described here: [MelaTAMP wiki, tagset page](https://wikis.hu-berlin.de/melatamp/Tagset).
 *
 * @author Stephan Druskat
 *
 */
public class SubrefMapper /*extends AbstractBlockMapper*/ {
	
	private static final Logger log = LoggerFactory.getLogger(SubrefMapper.class);

	private final LayerData refData;
	private final List<SToken> lexTokens;
	private final List<SToken> morphTokens;
	private final SDocumentGraph graph;
	private final ListMultimap<String, String> markerContentMap;
	private final String lexMarker;
	private final String morphMarker;
	private final String subRefDefinitionMarker;
	private final List<String> subRefAnnotationMarkers;
	private final String refMarker;
	private final boolean refHasMorphology;
	
	/**
	 * TODO Description
	 *
	 * @author Stephan Druskat
	 *
	 */
	private enum SUBREF_TYPE {
		SIMPLE,
		SIMPLE_TARGETED,
		UNDEFINED_GLOBAL,
		UNDEFINED_GLOBAL_TARGETED,
		DEFINED_GLOBAL,
		DEFINED_GLOBAL_TARGETED,
		LINKED_TARGETED
	};
	
	/**
	 * @param properties
	 * @param graph
	 * @param morphTokens 
	 * @param lexTokens 
	 * @param refData 
	 * @param markerContentMap 
	 * @param trimmedInputString
	 * @param hasMorphology
	 * @param indices
	 * @param lexDS
	 * @param morphDS
	 * @param layers
	 */
	public SubrefMapper(ToolboxTextImporterProperties properties, SDocumentGraph graph, LayerData refData, List<SToken> lexTokens, List<SToken> morphTokens, ListMultimap<String,String> markerContentMap, boolean refHasMorphology) {
		this.graph = graph;
		this.refData = refData;
		this.lexTokens = lexTokens;
		this.morphTokens = morphTokens;
		this.markerContentMap = markerContentMap;
		this.lexMarker = properties.getLexMarker();
		this.morphMarker = properties.getMorphMarker();
		this.subRefDefinitionMarker = properties.getSubRefDefinitionMarker();
		this.subRefAnnotationMarkers = properties.getSubRefAnnotationMarkers();
		this.refMarker = properties.getRefMarker();
		this.refHasMorphology = refHasMorphology;
	}

	/**
	 * TODO: Description
	 */
	public void map() {
		Set<String[]> definedSubrefs = new HashSet<>();
		for (String subrefLine : markerContentMap.get(subRefDefinitionMarker)) {
			definedSubrefs.add(subrefLine.split("\\s+"));
		}
		Map<String, String> subrefAnnoLines = new HashMap<>();
		for (String subrefAnnoMarker : subRefAnnotationMarkers) {
			if (markerContentMap.get(subrefAnnoMarker) != null) {
				if (!markerContentMap.get(subrefAnnoMarker).isEmpty()) {
					subrefAnnoLines.put(subrefAnnoMarker, markerContentMap.get(subrefAnnoMarker).get(0));
				}
			
			}
		}
		for (Entry<String, String> subrefAnnoLineEntry : subrefAnnoLines.entrySet()) {
			String marker = subrefAnnoLineEntry.getKey();
			String subrefAnnoLine = subrefAnnoLineEntry.getValue();
			String[] split = subrefAnnoLine.split("\\s+");
			SUBREF_TYPE type = null;
			type = determineType(definedSubrefs, split);
			if (type != null) {
				switch (type) {
				case SIMPLE_TARGETED:
					mapSimpleTargeted(subrefAnnoLine, marker);
					break;

				case SIMPLE:
					mapSimple(subrefAnnoLine, marker);
					break;

				case UNDEFINED_GLOBAL:
					mapUndefinedGlobal(subrefAnnoLine, marker);
					break;

				case UNDEFINED_GLOBAL_TARGETED:
					mapUndefinedGlobalTargeted(subrefAnnoLine, marker);
					break;

				case DEFINED_GLOBAL:
					mapDefinedGlobal(subrefAnnoLine, marker);
					break;

				case DEFINED_GLOBAL_TARGETED:
					mapDefinedGlobalTargeted(subrefAnnoLine, marker);
					break;

				case LINKED_TARGETED:
					mapLinkedTargeted(subrefAnnoLine, marker);
					break;

				default:
					log.warn("ERROR!");
					break;
				}
			}
			else {
				log.warn("Subref type in \"\\" + refData.getMarker() + "\" block could not be determined! Ignoring subrefs in block \"" + refData.getPrimaryData() + "\"!");
			}
		}
	}

	/**
	 * TODO: Description
	 *
	 * @param subrefAnnoLine
	 * @param marker
	 */
	private void mapSimple(String subrefAnnoLine, String marker) {
		String[] split = subrefAnnoLine.split("\\s+", 3);
		int from = -1, to = -1;
		try {
			from = Integer.parseInt(split[0]);
			/*
			 * 1 must be added to *to* because List#subList(from,to)
			 * works with **excluive** *to*!
			 */
			to = Integer.parseInt(split[1]) + 1;
		}
		catch (NumberFormatException e) {
			log.warn("The subref annotation \"" + subrefAnnoLine + "\" is not in the correct format ({int} {int} {annotation})! Ignoring it.");
		}
		SSpan subref = null;
		if (refHasMorphology) {
			// Map to mb
			List<SToken> orderedMorphTokens = graph.getSortedTokenByText(morphTokens);
			subref = graph.createSpan(orderedMorphTokens.subList(from, to));
			graph.getLayerByName(morphMarker).get(0).addNode(subref);
		}
		else {
			// map to tx
			List<SToken> orderedLexTokens = graph.getSortedTokenByText(lexTokens);
			subref = graph.createSpan(orderedLexTokens.subList(from, to));
			graph.getLayerByName(refMarker).get(0).addNode(subref);
		}
		if (subref != null) {
			subref.createAnnotation("toolbox", marker, split[2]); // FIXME!
		}
		System.out.println(refData.toString());
		for (SAnnotation anno : subref.getAnnotations()) {
			System.out.println("ANNO: " + anno.getNamespace() + " :: " + anno.getName() + " : " + anno.getValue_STEXT());
		}
	}

	/**
	 * TODO: Description
	 * @param marker 
	 * @param subrefAnnoLine 
	 *
	 */
	private void mapSimpleTargeted(String subrefAnnoLine, String marker) {;
		String[] split = subrefAnnoLine.split("\\s+", 4);
		int from = -1, to = -1;
		try {
			from = Integer.parseInt(split[1]);
			/*
			 * 1 must be added to *to* because List#subList(from,to)
			 * works with **excluive** *to*!
			 */
			to = Integer.parseInt(split[2]) + 1;
		}
		catch (NumberFormatException e) {
			log.warn("The subref annotation \"" + subrefAnnoLine + "\" is not in the correct format ({marker} {int} {int} {annotation})! Ignoring it.");
		}
		SSpan subref = null;
		if (split[0].equals(lexMarker)) {
			List<SToken> orderedLexTokens = graph.getSortedTokenByText(lexTokens);
			subref = graph.createSpan(orderedLexTokens.subList(from, to));
			graph.getLayerByName(refMarker).get(0).addNode(subref);
		}
		else if (split[0].equals(morphMarker)) {
			List<SToken> orderedMorphTokens = graph.getSortedTokenByText(morphTokens);
			subref = graph.createSpan(orderedMorphTokens.subList(from, to));
			graph.getLayerByName(morphMarker).get(0).addNode(subref);
		}
		else {
			log.warn("The targeted line must be marked with either the lexical marker \"" + lexMarker + "\" or the morphological marker \"" + morphMarker + "\"!\nInstead, the target marker is given as \"" + split[0] + "\"!");
			System.err.println("\nERROR!\nThe targeted line must be marked with either the lexical marker \"" + lexMarker + "\" or the morphological marker \"" + morphMarker + "\"!\nInstead, the target marker is given as \"" + split[0] + "\"!\n");
		}
		if (subref != null) {
			subref.createAnnotation("toolbox", marker, split[3]);
		}
		System.out.println(refData.toString());
		for (SAnnotation anno : subref.getAnnotations()) {
			System.out.println("ANNO: " + anno.getNamespace() + " :: " + anno.getName() + " : " + anno.getValue_STEXT());
		}
	}

	/**
	 * TODO: Description
	 *
	 * @param subrefAnnoLine
	 * @param marker
	 */
	private void mapUndefinedGlobal(String subrefAnnoLine, String marker) {
		System.out.println("UNDEFINED_GLOBAL SUBREF: " + marker + " : " + subrefAnnoLine + "\n" + refData.toString() + "\n\n");
	}

	/**
	 * TODO: Description
	 *
	 * @param subrefAnnoLine
	 * @param marker
	 */
	private void mapUndefinedGlobalTargeted(String subrefAnnoLine, String marker) {
		System.out.println("UNDEFINED_GLOBAL_TARGETED SUBREF: " + marker + " : " + subrefAnnoLine + "\n" + refData.toString() + "\n\n");
	}

	/**
	 * TODO: Description
	 *
	 * @param subrefAnnoLine
	 * @param marker
	 */
	private void mapDefinedGlobal(String subrefAnnoLine, String marker) {
		System.out.println("DEFINED_GLOBAL SUBREF: " + marker + " : " + subrefAnnoLine + "\n" + refData.toString() + "\n\n");
	}

	/**
	 * TODO: Description
	 *
	 * @param subrefAnnoLine
	 * @param marker
	 */
	private void mapDefinedGlobalTargeted(String subrefAnnoLine, String marker) {
		System.out.println("DEFINED_GLOBAL_TARGETED SUBREF: " + marker + " : " + subrefAnnoLine + "\n" + refData.toString() + "\n\n");
	}

	/**
	 * TODO: Description
	 *
	 * @param subrefAnnoLine
	 * @param marker
	 */
	private void mapLinkedTargeted(String subrefAnnoLine, String marker) {
		System.out.println("LINKED SUBREF: " + marker + " : " + subrefAnnoLine + "\n" + refData.toString() + "\n\n");
	}

	/**
	 * TODO: Description
	 *
	 * @param definedSubrefs
	 * @param split
	 * @return
	 */
	private SUBREF_TYPE determineType(Set<String[]> definedSubrefs, String[] split) {
		if (definedSubrefs.isEmpty()) {
			// Can be SIMPLE, SIMPLE_TARGETED
			if (split[0].equals(lexMarker) || split[0].equals(morphMarker)) {
				return SUBREF_TYPE.SIMPLE_TARGETED;
			}
			else {
				return SUBREF_TYPE.SIMPLE;
			}
		}
		else {
			/*
			 * Can be UNDEFINED_GLOBAL, UNDEFINED_GLOBAL_TARGETED, 
			 * DEFINED_GLOBAL, DEFINED_GLOBAL_TARGETED, LINKED_TARGETED,
			 * with, 2, 3, 4, or more arguments
			 */
			int numberOfArguments = -1;
			for (String[] definedSubRef : definedSubrefs) {
				if (numberOfArguments == -1) {
					numberOfArguments = definedSubRef.length;
				}
				else {
					if (numberOfArguments != definedSubRef.length) {
						log.warn("Subref types cannot be mixed within one \"\\" + refData.getMarker() + "\" block! Ignoring subrefs in block \"" + refData.getPrimaryData() + "\"!");
						return null;
					}
				}
			}
			if (numberOfArguments < 5) {
				switch (numberOfArguments) {
				case 2:
					return SUBREF_TYPE.UNDEFINED_GLOBAL;

				case 3:
					String firstDefinedSubref = definedSubrefs.iterator().next()[0];
					if (firstDefinedSubref.equals(lexMarker) || firstDefinedSubref.equals(morphMarker)) {
						return SUBREF_TYPE.UNDEFINED_GLOBAL_TARGETED;
					}
					else {
						return SUBREF_TYPE.DEFINED_GLOBAL;
					}

				case 4:
					return SUBREF_TYPE.DEFINED_GLOBAL_TARGETED;

				default:
					return null;
				}
			}
			else {
				// Number of arguments > 4
				return SUBREF_TYPE.LINKED_TARGETED;
			}
		}
	}

}
