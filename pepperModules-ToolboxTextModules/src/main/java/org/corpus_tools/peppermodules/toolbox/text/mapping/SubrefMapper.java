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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporterProperties;
import org.corpus_tools.peppermodules.toolbox.text.data.LayerData;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SPointingRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.util.DataSourceSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

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

	private final STextualDS morphDS;

	private final STextualDS lexDS;
	
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
		LINKED_TARGETED, 
		FULL_REF_ANNOTATION
	};
	
	/**
	 * @param properties
	 * @param graph
	 * @param morphTokens 
	 * @param lexTokens 
	 * @param refData 
	 * @param markerContentMap 
	 * @param morphDS 
	 * @param lexDS 
	 * @param trimmedInputString
	 * @param hasMorphology
	 * @param indices
	 * @param lexDS
	 * @param morphDS
	 * @param layers
	 */
	public SubrefMapper(ToolboxTextImporterProperties properties, SDocumentGraph graph, LayerData refData, List<SToken> lexTokens, List<SToken> morphTokens, ListMultimap<String,String> markerContentMap, boolean refHasMorphology, STextualDS lexDS, STextualDS morphDS) {
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
		this.morphDS = morphDS;
		this.lexDS = lexDS;
	}

	/**
	 * TODO: Description
	 */
	public void map() {
		Set<String> definedSubrefs = new HashSet<>();
		for (String subrefLine : markerContentMap.get(subRefDefinitionMarker)) {
			definedSubrefs.add(subrefLine);
		}
		// There can be more than one subref annotation line, hence no simmple HashMap
		Multimap<String, String> subrefAnnoLines = ArrayListMultimap.create();
		for (String subrefAnnoMarker : subRefAnnotationMarkers) {
			if (markerContentMap.get(subrefAnnoMarker) != null) {
				if (!markerContentMap.get(subrefAnnoMarker).isEmpty()) {
					for (String subrefLine : markerContentMap.get(subrefAnnoMarker)) {
						subrefAnnoLines.put(subrefAnnoMarker, subrefLine);
					}
				}
			
			}
		}
		for (Entry<String, String> subrefAnnoLineEntry : subrefAnnoLines.entries()) {
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
					mapUndefinedGlobal(definedSubrefs, subrefAnnoLine, marker);
					break;

				case UNDEFINED_GLOBAL_TARGETED:
					mapUndefinedGlobalTargeted(definedSubrefs, subrefAnnoLine, marker);
					break;

				case DEFINED_GLOBAL:
					mapDefinedGlobal(definedSubrefs, subrefAnnoLine, marker);
					break;

				case DEFINED_GLOBAL_TARGETED:
					mapDefinedGlobalTargeted(definedSubrefs, subrefAnnoLine, marker);
					break;

				case LINKED_TARGETED:
					mapLinkedTargeted(definedSubrefs, subrefAnnoLine, marker);
					break;
				
				case FULL_REF_ANNOTATION:
					mapFullRef(subrefAnnoLine, marker);
					break;

				default:
					log.warn("Something happened that shouldn't happen! Please report this as a bug!"); // FIXME Insert bug link!
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
	private void mapFullRef(String fullRefAnnoLine, String marker) {
		SSpan span = graph.createSpan(lexTokens);
		graph.getLayerByName(refData.getMarker()).get(0).addNode(span);
		span.createAnnotation("toolbox", marker, fullRefAnnoLine);
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
			log.warn("The subref annotation \"" + subrefAnnoLine + "\" is not in the correct format ({int} {int} {annotation})! Ignoring it.\nIf you think this is a bug, please report it!"); // FIXME INsert bug link!
			return;
		}
		SSpan subref = null; // FIXME: DON'T CREATE A NEW SPAN FOR EVEERY LINE!!!
		if (refHasMorphology) {
			// Map to mb
			List<SSpan> spanList = graph.getSpansBySequence(new DataSourceSequence<Number>(morphDS, from, to));
			if (spanList != null && spanList.size() > 0) {
				subref = spanList.get(0);
			}
			else {
				List<SToken> orderedMorphTokens = graph.getSortedTokenByText(morphTokens);
				subref = graph.createSpan(orderedMorphTokens.subList(from, to));
			}
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
		System.err.println("\n\n\n" + refData.toString());
		for (SAnnotation anno : subref.getAnnotations()) {
			System.err.println("ANNO: " + anno.getNamespace() + " :: " + anno.getName() + " : " + anno.getValue_STEXT());
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
			log.warn("The subref annotation \"" + subrefAnnoLine + "\" is not in the correct format ({marker} {int} {int} {annotation})! Ignoring it.\nIf you think this is a bug, please report it!"); // FIXME INsert bug link!
			return;
		}
		SSpan subref = null;
		if (split[0].equals(lexMarker)) {
			// map to tx
			List<SSpan> spanList = graph.getSpansBySequence(new DataSourceSequence<Number>(lexDS, from, to));
			if (spanList != null && spanList.size() > 0) {
				subref = spanList.get(0);
			}
			else {
				List<SToken> orderedLexTokens = graph.getSortedTokenByText(lexTokens);
				subref = graph.createSpan(orderedLexTokens.subList(from, to));
			}
			graph.getLayerByName(refMarker).get(0).addNode(subref);
		}
		else if (split[0].equals(morphMarker)) {
			List<SSpan> spanList = graph.getSpansBySequence(new DataSourceSequence<Number>(morphDS, from, to));
			if (spanList != null && spanList.size() > 0) {
				subref = spanList.get(0);
			}
			else {
				List<SToken> orderedMorphTokens = graph.getSortedTokenByText(morphTokens);
				subref = graph.createSpan(orderedMorphTokens.subList(from, to));
			}
			graph.getLayerByName(morphMarker).get(0).addNode(subref);
		}
		else {
			log.warn("The targeted line must be marked with either the lexical marker \"" + lexMarker + "\" or the morphological marker \"" + morphMarker + "\"!\nInstead, the target marker is given as \"" + split[0] + "\"!");
			return;
		}
		if (subref != null) {
			subref.createAnnotation("toolbox", marker, split[3]);
		}
		System.err.println("\n\n\n" + refData.toString());
		for (SAnnotation anno : subref.getAnnotations()) {
			System.err.println("ANNO: " + anno.getNamespace() + " :: " + anno.getName() + " : " + anno.getValue_STEXT());
		}
	}

	/**
	 * TODO: Description
	 * @param definedSubrefs 
	 *
	 * @param subrefAnnoLine
	 * @param marker
	 */
	private void mapUndefinedGlobal(Set<String> definedSubrefs, String subrefAnnoLine, String marker) {
		if (definedSubrefs.size() > 1) {
			log.warn("Subref type \"" + SUBREF_TYPE.UNDEFINED_GLOBAL + "\" only allows for exactly one subref definition with marker \"" + subRefDefinitionMarker + "\"!\nHowever, " + definedSubrefs.size() + " were found. Ignoring subrefs!");
		}
		String definition = definedSubrefs.iterator().next();
		String[] split = definition.split("\\s+", 3);
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
			log.warn("The subref definition \"\\" + subRefDefinitionMarker + " "+ definition + "\" is not in the correct format ({marker} {int} {int})! Ignoring it.\nIf you think this is a bug, please report it!"); // FIXME INsert bug link!
		}
		SSpan subref = null;
		if (refHasMorphology) {
			// Map to mb
			List<SSpan> spanList = graph.getSpansBySequence(new DataSourceSequence<Number>(morphDS, from, to));
			if (spanList != null && spanList.size() > 0) {
				subref = spanList.get(0);
			}
			else {
				List<SToken> orderedMorphTokens = graph.getSortedTokenByText(morphTokens);
				subref = graph.createSpan(orderedMorphTokens.subList(from, to));
			}
			graph.getLayerByName(morphMarker).get(0).addNode(subref);
		}
		else {
			// map to tx
			List<SSpan> spanList = graph.getSpansBySequence(new DataSourceSequence<Number>(lexDS, from, to));
			if (spanList != null && spanList.size() > 0) {
				subref = spanList.get(0);
			}
			else {
				List<SToken> orderedLexTokens = graph.getSortedTokenByText(lexTokens);
				subref = graph.createSpan(orderedLexTokens.subList(from, to));
			}
			graph.getLayerByName(refMarker).get(0).addNode(subref);
		}
		if (subref != null) {
			subref.createAnnotation("toolbox", marker, subrefAnnoLine.trim());
		}
		System.err.println("\n\n\n" + refData.toString());
		for (SAnnotation anno : subref.getAnnotations()) {
			System.err.println("ANNO: " + anno.getNamespace() + " :: " + anno.getName() + " : " + anno.getValue_STEXT());
		}
	}

	/**
	 * TODO: Description
	 *
	 * @param subrefAnnoLine
	 * @param marker
	 */
	private void mapUndefinedGlobalTargeted(Set<String> definedSubrefs, String subrefAnnoLine, String marker) {
		if (definedSubrefs.size() > 1) {
			log.warn("Subref type \"" + SUBREF_TYPE.UNDEFINED_GLOBAL_TARGETED + "\" only allows for exactly one subref definition with marker \"" + subRefDefinitionMarker + "\"!\nHowever, " + definedSubrefs.size() + " were found. Ignoring subrefs!");
		}
		String definition = definedSubrefs.iterator().next();
		String[] split = definition.split("\\s+", 4);
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
			log.warn("The subref definition \"\\" + subRefDefinitionMarker + " "+ definition + "\" is not in the correct format ({marker} {int} {int})! Ignoring it.\nIf you think this is a bug, please report it!"); // FIXME INsert bug link!
		}
		SSpan subref = null;
		if (split[0].equals(lexMarker)) {
			// map to tx
			List<SSpan> spanList = graph.getSpansBySequence(new DataSourceSequence<Number>(lexDS, from, to));
			if (spanList != null && spanList.size() > 0) {
				subref = spanList.get(0);
			}
			else {
				List<SToken> orderedLexTokens = graph.getSortedTokenByText(lexTokens);
				subref = graph.createSpan(orderedLexTokens.subList(from, to));
			}
			graph.getLayerByName(refMarker).get(0).addNode(subref);
		}
		else if (split[0].equals(morphMarker)) {
			List<SSpan> spanList = graph.getSpansBySequence(new DataSourceSequence<Number>(morphDS, from, to));
			if (spanList != null && spanList.size() > 0) {
				subref = spanList.get(0);
			}
			else {
				List<SToken> orderedMorphTokens = graph.getSortedTokenByText(morphTokens);
				subref = graph.createSpan(orderedMorphTokens.subList(from, to));
			}
			graph.getLayerByName(morphMarker).get(0).addNode(subref);
		}
		else {
			log.warn("The targeted line must be marked with either the lexical marker \"" + lexMarker + "\" or the morphological marker \"" + morphMarker + "\"!\nInstead, the target marker is given as \"" + split[0] + "\"!");
			return;
		}
		if (subref != null) {
			subref.createAnnotation("toolbox", marker, subrefAnnoLine.trim());
		}
		System.err.println("\n\n\n" + refData.toString());
		for (SAnnotation anno : subref.getAnnotations()) {
			System.err.println("ANNO: " + anno.getNamespace() + " :: " + anno.getName() + " : " + anno.getValue_STEXT());
		}
	}

	/**
	 * TODO: Description
	 * @param definedSubrefs 
	 *
	 * @param subrefAnnoLine
	 * @param marker
	 */
	private void mapDefinedGlobal(Set<String> definedSubrefs, String subrefAnnoLine, String marker) {
		Map<String, int[]> definitionMap = null;
		String[] annoSplit = subrefAnnoLine.split("\\s+", 2);
		for (String definition : definedSubrefs) {
			String[] split = definition.split("\\s+", 4);
			if (split[0].equals(annoSplit[0])) {
				try {
					definitionMap = Collections.singletonMap(split[0], new int[] { Integer.parseInt(split[1]), Integer.parseInt(split[2]) + 1 });
				}
				catch (NumberFormatException e) {
					log.warn("The subref definition \"\\" + subRefDefinitionMarker + " " + definition + "\" is not in the correct format ({definition} {int} {int})! Ignoring it.\nIf you think this is a bug, please report it!"); // FIXME INsert bug link!
				}
			}
		}
		if (definitionMap == null) {
			log.warn("There is no subref definition \"" + annoSplit[0] + "\" in " + refData.getPrimaryData() + " for annotation line \"\\" + marker + " " + subrefAnnoLine + "\"!");
			return;
		}
		SSpan subref = null;
		int[] intArr = definitionMap.values().iterator().next();
		if (refHasMorphology) {
			// Map to mb
			List<SSpan> spanList = graph.getSpansBySequence(new DataSourceSequence<Number>(morphDS, intArr[0], intArr[1]));
			if (spanList != null && spanList.size() > 0) {
				subref = spanList.get(0);
			}
			else {
				List<SToken> orderedMorphTokens = graph.getSortedTokenByText(morphTokens);
				subref = graph.createSpan(orderedMorphTokens.subList(intArr[0], intArr[1]));
			}
			graph.getLayerByName(morphMarker).get(0).addNode(subref);
		}
		else {
			// map to tx
			List<SSpan> spanList = graph.getSpansBySequence(new DataSourceSequence<Number>(lexDS, intArr[0], intArr[1]));
			if (spanList != null && spanList.size() > 0) {
				subref = spanList.get(0);
			}
			else {
				List<SToken> orderedLexTokens = graph.getSortedTokenByText(lexTokens);
				subref = graph.createSpan(orderedLexTokens.subList(intArr[0], intArr[1]));
			}
			graph.getLayerByName(refMarker).get(0).addNode(subref);
		}
		if (subref != null) {
				subref.createAnnotation("toolbox", marker, annoSplit[1].trim()); // FIXME: Change "toolbox" to constant! 
		}
		System.err.println("\n\n\n" + refData.toString());
		for (SAnnotation anno : subref.getAnnotations()) {
			System.err.println("ANNO: " + anno.getNamespace() + " :: " + anno.getName() + " : " + anno.getValue_STEXT());
		}
	}

	/**
	 * TODO: Description
	 * @param definedSubrefs 
	 *
	 * @param subrefAnnoLine
	 * @param marker
	 */
	private void mapDefinedGlobalTargeted(Set<String> definedSubrefs, String subrefAnnoLine, String marker) {
		Map<String[], int[]> definitionMap = null;
		String[] annoSplit = subrefAnnoLine.split("\\s+", 2);
		for (String definition : definedSubrefs) {
			String[] split = definition.split("\\s+", 5);
			if (split[0].equals(annoSplit[0])) {
				try {
					definitionMap = Collections.singletonMap(new String[]{split[0], split[1]}, new int[] { Integer.parseInt(split[2]), Integer.parseInt(split[3]) + 1 });
				}
				catch (NumberFormatException e) {
					log.warn("The subref definition \"\\" + subRefDefinitionMarker + " " + definition + "\" is not in the correct format ({definition} {target marker} {int} {int})! Ignoring it.\nIf you think this is a bug, please report it!"); // FIXME INsert bug link!
				}
			}
		}
		if (definitionMap == null) {
			log.warn("There is no subref definition \"" + annoSplit[0] + "\" in " + refData.getPrimaryData() + " for annotation line \"\\" + marker + " " + subrefAnnoLine + "\"!");
			return;
		}
		SSpan subref = null;
		int[] intArr = definitionMap.values().iterator().next();
		if (definitionMap.keySet().iterator().next()[1].equals(morphMarker)) {
			// Map to mb
			List<SSpan> spanList = graph.getSpansBySequence(new DataSourceSequence<Number>(morphDS, intArr[0], intArr[1]));
			if (spanList != null && spanList.size() > 0) {
				subref = spanList.get(0);
			}
			else {
				List<SToken> orderedMorphTokens = graph.getSortedTokenByText(morphTokens);
				subref = graph.createSpan(orderedMorphTokens.subList(intArr[0], intArr[1]));
			}
			graph.getLayerByName(morphMarker).get(0).addNode(subref);
		}
		else {
			// map to tx
			List<SSpan> spanList = graph.getSpansBySequence(new DataSourceSequence<Number>(lexDS, intArr[0], intArr[1]));
			if (spanList != null && spanList.size() > 0) {
				subref = spanList.get(0);
			}
			else {
				List<SToken> orderedLexTokens = graph.getSortedTokenByText(lexTokens);
				subref = graph.createSpan(orderedLexTokens.subList(intArr[0], intArr[1]));
			}
			graph.getLayerByName(refMarker).get(0).addNode(subref);
		}
		if (subref != null) {
				subref.createAnnotation("toolbox", marker, annoSplit[1].trim()); // FIXME: Change "toolbox" to constant! 
		}
		System.err.println("\n\n\n" + refData.toString());
		for (SAnnotation anno : subref.getAnnotations()) {
			System.err.println("ANNO: " + anno.getNamespace() + " :: " + anno.getName() + " : " + anno.getValue_STEXT());
		}
	}

	/**
	 * TODO: Description
	 * @param definedSubrefs 	
	 *
	 * @param subrefAnnoLine
	 * @param marker
	 */
	private void mapLinkedTargeted(Set<String> definedSubrefs, String subrefAnnoLine, String marker) {
		String[] definitionAndTarget = null;
		String[] annoSplit = subrefAnnoLine.split("\\s+", 2);
		List<int[]> offsetList = new ArrayList<>();
		for (String definition : definedSubrefs) {
			String[] split = definition.split("\\s+");
			if (split[0].equals(annoSplit[0])) {
				definitionAndTarget = new String[] { split[0], split[1] };
				for (int firstProbInt = 2; firstProbInt < split.length; firstProbInt += 2) {
					try {
						offsetList.add(new int[] {Integer.parseInt(split[firstProbInt]), Integer.parseInt(split[firstProbInt + 1]) + 1});
					}
					catch (NumberFormatException e) {
						log.warn("The subref definition \"\\" + subRefDefinitionMarker + " " + definition + "\" is not in the correct format ({definition} {target marker} {{int} {int}}*)! Ignoring it.\nIf you think this is a bug, please report it!"); // FIXME INsert bug link!
					}
				}
			}
		}
		if (definitionAndTarget == null) {
			log.warn("There is no subref definition \"" + annoSplit[0] + "\" in " + refData.getPrimaryData() + " for annotation line \"\\" + marker + " " + subrefAnnoLine + "\"!");
			return;
		}
		List<SSpan> subrefs = new ArrayList<>();
		if (definitionAndTarget[1].equals(morphMarker)) {
			// FIXME! TODO! If no \mb exists but linked subref is still set to \mb, this throws an error!
			// So instead of just going "is mb marker", check in if above (&& refHasMorphology) 
			// And introduce an "else" with a warning, probably in all mappgins!
			SSpan lastSpan = null;
			SSpan subref = null;
			// Map to mb
			for (int[] offset : offsetList) {
				List<SSpan> spanList = graph.getSpansBySequence(new DataSourceSequence<Number>(morphDS, offset[0], offset[1]));
				if (spanList != null && spanList.size() > 0) {
					subref = spanList.get(0);
				}
				else {
					List<SToken> orderedMorphTokens = graph.getSortedTokenByText(morphTokens);
					subref = graph.createSpan(orderedMorphTokens.subList(offset[0], offset[1]));
				}
				graph.getLayerByName(morphMarker).get(0).addNode(subref);
				subrefs.add(subref);
				if (lastSpan != null) {
					SPointingRelation rel = (SPointingRelation) graph.createRelation(lastSpan, subref, SALT_TYPE.SPOINTING_RELATION, null);
					rel.setType("l");
					graph.getLayerByName(morphMarker).get(0).addRelation(rel);
				}
				lastSpan = subref;
			}
		}
		else {
			// map to tx
			SSpan lastSpan = null;
			SSpan subref = null;
			for (int[] offset : offsetList) {
				List<SSpan> spanList = graph.getSpansBySequence(new DataSourceSequence<Number>(lexDS, offset[0], offset[1]));
				if (spanList != null && spanList.size() > 0) {
					subref = spanList.get(0);
				}
				else {
					List<SToken> orderedLexTokens = graph.getSortedTokenByText(lexTokens);
					subref = graph.createSpan(orderedLexTokens.subList(offset[0], offset[1]));
				}
				graph.getLayerByName(lexMarker).get(0).addNode(subref);
				subrefs.add(subref);
				if (lastSpan != null) {
					SPointingRelation rel = (SPointingRelation) graph.createRelation(lastSpan, subref, SALT_TYPE.SPOINTING_RELATION, null);
					rel.setType("l");
					graph.getLayerByName(lexMarker).get(0).addRelation(rel);
				}
				lastSpan = subref;
			}
		}
		if (!subrefs.isEmpty()) {
			for (SSpan subref : subrefs) {
				subref.createAnnotation("toolbox", marker, annoSplit[1].trim()); // FIXME: Change "toolbox" to constant!
			}
		}
		System.err.println("\n\n\n" + refData.toString());
		for (SSpan subref : subrefs) {
			for (SAnnotation anno : subref.getAnnotations()) {
				System.err.println("ANNO: " + anno.getNamespace() + " :: " + anno.getName() + " : " + anno.getValue_STEXT());
			}
		}
	}

	/**
	 * TODO: Description
	 *
	 * @param definedSubrefs
	 * @param split
	 * @return
	 */
	private SUBREF_TYPE determineType(Set<String> definedSubrefs, String[] split) {
		if (definedSubrefs.isEmpty()) {
			// Can be SIMPLE, SIMPLE_TARGETED
			if (split[0].equals(lexMarker) || split[0].equals(morphMarker)) {
				return SUBREF_TYPE.SIMPLE_TARGETED;
			}
			else {
				/*
				 *  Catch cases, where the marker is defined as a subref annotation
				 *  marker, but the content is not actually a subref, but should
				 *  annotate the full ref. 
				 */
				try {
					Integer.parseInt(split[0]);
					Integer.parseInt(split[1]);
				}
				catch (NumberFormatException e) {
					// This is not a subref!
					return SUBREF_TYPE.FULL_REF_ANNOTATION;
				}
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
			for (String definedSubRefString : definedSubrefs) {
				String[] definedSubRef = definedSubRefString.split("\\s+");
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
					String firstDefinedSubref = definedSubrefs.iterator().next().split("\\s+", 2)[0];
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
