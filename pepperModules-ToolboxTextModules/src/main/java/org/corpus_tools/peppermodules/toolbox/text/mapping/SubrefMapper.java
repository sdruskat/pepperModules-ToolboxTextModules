/*******************************************************************************
 * Copyright (c) 2017 Stephan Druskat
 * Exploitation rights belong exclusively to Humboldt-Universität zu Berlin
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.Range;
import org.corpus_tools.peppermodules.toolbox.text.AbstractToolboxTextMapper;
import org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporterProperties;
import org.corpus_tools.peppermodules.toolbox.text.data.LayerData;
import org.corpus_tools.peppermodules.toolbox.text.data.SubrefDefinition;
import org.corpus_tools.peppermodules.toolbox.text.data.SubrefDefinition.SUBREF_TYPE;
import org.corpus_tools.peppermodules.toolbox.text.utils.ToolboxTextModulesUtils;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

/**
 * TODO Description
 * Subrefs are described here: [MelaTAMP wiki](https://wikis.hu-berlin.de/melatamp/Clause_segmentation_and_annotation).
 *
 * @author Stephan Druskat
 *
 */
public class SubrefMapper extends AbstractToolboxTextMapper {
	
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
	private final boolean refHasMorphology;

	private final Map<String, String> markerMap;
	
	
	
	/**
	 * @param markerMap 
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
	public SubrefMapper(Map<String, String> markerMap, ToolboxTextImporterProperties properties, SDocumentGraph graph, LayerData refData, List<SToken> lexTokens, List<SToken> morphTokens, ListMultimap<String,String> markerContentMap, boolean refHasMorphology) {
		this.graph = graph;
		this.refData = refData;
		this.lexTokens = lexTokens;
		this.morphTokens = morphTokens;
		this.markerContentMap = markerContentMap;
		this.lexMarker = properties.getLexMarker();
		this.morphMarker = properties.getMorphMarker();
		this.subRefDefinitionMarker = properties.getSubrefDefinitionMarker();
		this.subRefAnnotationMarkers = properties.getSubRefAnnotationMarkers();
		this.refHasMorphology = refHasMorphology;
		this.markerMap = markerMap;
	}

	/**
	 * TODO: Description
	 */
	public void map() {
		boolean mapGlobal = false;
		Map<String, SubrefDefinition> subrefMap = new HashMap<>();
		Multimap<SubrefDefinition, Entry<String, String>> subrefAnnoMap = ArrayListMultimap.create();
		Multimap<String, String> simpleSubrefMap = ArrayListMultimap.create();
		for (String subrefLine : markerContentMap.get(subRefDefinitionMarker)) {
			SubrefDefinition subref = createSubrefDefinitionFromSubrefLine(subrefLine);
			SubrefDefinition previous = subrefMap.put(subref.getIdentifier(), subref);
			if (subref.getType() == SUBREF_TYPE.UNIDENTIFIED_GLOBAL
					|| subref.getType() == SUBREF_TYPE.UNIDENTIFIED_GLOBAL_TARGETED) {
				mapGlobal = true;
				if (previous != null && (previous.getType() == SUBREF_TYPE.UNIDENTIFIED_GLOBAL
						|| previous.getType() == SUBREF_TYPE.UNIDENTIFIED_GLOBAL_TARGETED)) {
					/*
					 * Client has tried to map more than one subref of type
					 * UNIDENTIFIED_GLOBAL or UNIDENTIFIED_GLOBAL_TARGETED. This
					 * is implicitly illegal, as there can only be exactly one
					 * subref of either of these types in one ref.
					 */
					log.warn("Illegal subref definition in ref '{}', document '{}'!\nThere can only be exactly one unidentified global subref definition per ref! Cancelling definition overwrite.",
							refData.getRef(), refData.getDocName());
					subrefMap.put(previous.getIdentifier(), previous);
				}
			}
		}
		
		/* 
		 * Collect all lines that are candidates for subref annotation.
		 */
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
		
		if (subrefMap.size() > 0) {
			/*
			 * If there is a global subref definition, it overrides all other
			 * definitions, hence, only proceed with resolving the others if
			 * there is no global definition.
			 */
			if (!mapGlobal) {
				for (Entry<String, String> subrefAnnoLineEntry : subrefAnnoLines.entries()) {
					String subrefAnnoLine = subrefAnnoLineEntry.getValue();

					// Treat annotations targeting defined subrefs
					String[] definitionSplit = subrefAnnoLine.split("\\s+", 2);
					SubrefDefinition definition = subrefMap.get(definitionSplit[0]);
					if (definition != null) {
						subrefAnnoMap.put(definition, subrefAnnoLineEntry);
						continue;
					}
					else {
						simpleSubrefMap.put(subrefAnnoLineEntry.getKey(), subrefAnnoLineEntry.getValue());
						continue;
					}
				}
				if (simpleSubrefMap.size() > 0) {
					mapSimpleCandidates(simpleSubrefMap);
				}
				if (subrefAnnoMap.size() > 0) {
					mapDefined(subrefAnnoMap);
				}
			}
			else {
				// If there is a global subref definition, the subrefMap can
				// only have size 1.
				mapGlobal(subrefMap.values().iterator().next(), subrefAnnoLines);
			}
		}
		else {
			mapSimpleCandidates(subrefAnnoLines);
		}
	}

	/**
	 * TODO: Description
	 *
	 * @param subrefAnnoMap
	 */
	private void mapDefined(Multimap<SubrefDefinition, Entry<String, String>> subrefAnnoMap) {
		subrefannotationlines:
		for (Entry<SubrefDefinition, Entry<String, String>> entry : subrefAnnoMap.entries()) {
			SubrefDefinition definition = entry.getKey();
			String targetLayer = definition.getTargetLayer();
			String annoKey = entry.getValue().getKey();
			String annoLine = entry.getValue().getValue();
			SSpan subref = null;
			List<SToken> subrefTokens = new ArrayList<>();
			boolean mapToMorphTokens = false;
			if (targetLayer != null && targetLayer.equals(morphMarker)) {
				mapToMorphTokens = true;
			}
			else if (targetLayer == null && refHasMorphology) {
				mapToMorphTokens = true;
			}
			List<SToken> orderedTokens = graph.getSortedTokenByText(mapToMorphTokens ? morphTokens : lexTokens);
			String annoValue = null;
			if (annoLine.split("\\s+").length > 1) {
				annoValue = annoLine.split("\\s+", 2)[1].trim();
			}
			else {
				log.debug("There is no annotation value for subref '{}' with key '{}' in document '{}', ref '{}'. Ignoring this line.", definition.getIdentifier(), annoKey, refData.getDocName(), refData.getRef());
				continue subrefannotationlines;
			}
			ranges:
			for (Range<Integer> range : definition.getRanges()) {
				if (orderedTokens.size() < range.getMaximum() + 1) {
					log.warn("Subref {} in segment \'{}\' in document \"{}\" could not be resolved, as one or more subref token indices were outside of the range of token indices.\nNote that this may be due to earlier modification of the ref (excess tokens, etc.).\nTherefore please check previous warnings for this ref.\nIgnoring subref, please fix the source data.", range.getMinimum() + "-" + range.getMaximum(), refData.getRef(), refData.getDocName());
					continue ranges;
				}
				else {
					subrefTokens.addAll(orderedTokens.subList(range.getMinimum(), range.getMaximum() + 1));	
				}
			}
			if (subrefTokens.isEmpty()) {
				continue subrefannotationlines;
			}
			subref = getSubrefSpan(subrefTokens);
			if (subref.getAnnotation("toolbox::" + annoKey) != null) {
				log.warn("Duplicate annotation in '{}'-'{}'! There already exists an annotation with the key \"{}\". This might be an error in the source data. If it is not, please file a bug report.", refData.getDocName(), refData.getRef(), annoKey);
			}
			else {
				subref.createAnnotation("toolbox", annoKey, annoValue);
			}
			SLayer layer = graph.getLayerByName(mapToMorphTokens ? markerMap.get(morphMarker) : markerMap.get(lexMarker)).get(0);
			layer.addNode(subref);
		}
	}

	/**
	 * TODO: Description
	 *
	 * @param simpleSubrefMap
	 */
	private void mapSimpleCandidates(Multimap<String, String> simpleSubrefMap) {
		subrefannotationlines:
		for (Entry<String, String> anno : simpleSubrefMap.entries()) {
			SSpan subref = null;
			List<SToken> subrefTokens = new ArrayList<>();
			List<SToken> orderedTokens = null;
			SLayer layer = null;
			String[] typeSplit = anno.getValue().split("\\s+", 4);
			String annoValue = null;
			boolean mapToMorphTokens = false;
			boolean fullref = false;
			Range<Integer> range = null;
			String name = subRefDefinitionMarker;

			if ((typeSplit[0].equals(lexMarker) || typeSplit[0].equals(morphMarker)) && ToolboxTextModulesUtils.isInteger(typeSplit[1]) && ToolboxTextModulesUtils.isInteger(typeSplit[2])) {
				// SUBREF_TYPE.SIMPLE_TARGETED
				mapToMorphTokens = typeSplit[0].equals(morphMarker);
				range = Range.between(Integer.parseInt(typeSplit[1]), Integer.parseInt(typeSplit[2]) + 1);
				annoValue = typeSplit[3];
			}
			else {
				if (ToolboxTextModulesUtils.isInteger(typeSplit[0]) && ToolboxTextModulesUtils.isInteger(typeSplit[1])) {
					// SUBREF_TYPE.SIMPLE
					mapToMorphTokens = refHasMorphology;
					range = Range.between(Integer.parseInt(typeSplit[0]), Integer.parseInt(typeSplit[1]) + 1);
					String[] split = anno.getValue().split("\\s+", 3);
					if (split.length < 3) {
						log.debug("No value for annotation with key \"{}\" in document '{}', reference '{}'. Ignoring ...", anno.getKey(), refData.getDocName(), refData.getRef());
						continue subrefannotationlines;
					}
					else {
						annoValue = split[2];
					}
				}
				else {
					// SUBREF_TYPE.FULL_REF_ANNOTATION
					fullref = true;
					range = Range.between(0, lexTokens.size());
					annoValue = anno.getValue();
					name = "fullref";
				}
			}
			orderedTokens = graph.getSortedTokenByText(mapToMorphTokens ? morphTokens : lexTokens);
			if (orderedTokens.size() < range.getMaximum()) {
				log.warn("The maximum of subref range {}..{} in document '{}', reference '{}' is larger than the highest token index. Please fix source data! Ignoring this annotation ...", range.getMinimum(), range.getMaximum() - 1, refData.getDocName(), refData.getRef());
				continue subrefannotationlines;
			}
			else {
				subrefTokens.addAll(orderedTokens.subList(range.getMinimum(), range.getMaximum()));
			}
			if (subrefTokens.isEmpty()) {
				continue subrefannotationlines;
			}
			subref = getSubrefSpan(subrefTokens);
			subref.setName(name);
			/* 
			 * Full refs (i.e., annotations marked with a subref marker but without a subref definition
			 * or identifier contained in the annotation) can at this time be spread over multiple
			 * lines. This is correct because in this case, subref annotation lines starting with the
			 * same marker can potentially occur more than once in a ref, e.g., when they point to
			 * different subref definitions.
			 * 
			 * During mapping, however, the content of the second line would not be mapped, but instead
			 * a SaltInsertionException would be thrown because the combination of namespace and annotation
			 * key for the annotation already exists on the respective node. Hence, check if we're
			 * dealing with a fullref here, and append if the annotation already exists.
			 */
			String key = anno.getKey();
			if (key.equals(refData.getMarker()) || key.equals(subRefDefinitionMarker) || key.equals(lexMarker) || key.equals(morphMarker)) {
				key = markerMap.get(key);
			}
			if (fullref) {
				SAnnotation skeletonAnno = subref.getAnnotation(SALT_NAMESPACE_TOOLBOX + "::" + anno.getKey());
				if (skeletonAnno != null) {
					String oldValue = skeletonAnno.getValue_STEXT();
					String newValue = oldValue.concat(" " + annoValue).trim();
					skeletonAnno.setValue(newValue);
				}
				else {
					subref.createAnnotation("toolbox", key, annoValue);
				}
			}
			else {
				subref.createAnnotation("toolbox", key, annoValue);
			}
			layer = graph.getLayerByName(mapToMorphTokens ? markerMap.get(morphMarker) : markerMap.get(lexMarker)).get(0);
			layer.addNode(subref);
		}
	}

	/**
	 * TODO: Description
	 *
	 * @param definition
	 * @param subrefAnnoLines
	 */
	private void mapGlobal(SubrefDefinition definition, Multimap<String, String> subrefAnnoLines) {
		SSpan subref = null;
		List<SToken> subrefTokens = new ArrayList<>();
		List<SToken> orderedTokens = null;
		SLayer layer = null;
		// Global definitions only take a single range
		Range<Integer> range = definition.getRanges().get(0);
		boolean mapToMorphTokens = false;
		if (definition.getType() == SUBREF_TYPE.UNIDENTIFIED_GLOBAL) {
			if (refHasMorphology) {
				mapToMorphTokens = true;
			}
		}
		else {
			if (definition.getTargetLayer().equals(morphMarker)) {
				mapToMorphTokens = true;
			}
		}
		orderedTokens = mapToMorphTokens ? graph.getSortedTokenByText(morphTokens) : graph.getSortedTokenByText(lexTokens);
		if (orderedTokens.size() < range.getMaximum() + 1) {
			log.warn("Document '{}', reference '{}': The indices defined in the global subdef are outside of the index range of the target tokens. Please fix the source data! Ignoring this subref ...", refData.getDocName(), refData.getRef());
			return;
		}
		else {
			subrefTokens.addAll(orderedTokens.subList(range.getMinimum(), range.getMaximum() + 1));
		}
		subref = getSubrefSpan(subrefTokens);
		for (Entry<String, String> anno : subrefAnnoLines.entries()) {
			String key = anno.getKey();
			if (key.equals(refData.getMarker()) || key.equals(subRefDefinitionMarker) || key.equals(lexMarker) || key.equals(morphMarker)) {
				key = markerMap.get(key);
			}
			subref.createAnnotation("toolbox", key, anno.getValue());
		}
		layer = graph.getLayerByName(mapToMorphTokens ? markerMap.get(morphMarker) : markerMap.get(lexMarker)).get(0);
		layer.addNode(subref);
	}

	/**
	 * TODO: Description, take from below comment
	 *
	 * @return
	 */
	private SSpan getSubrefSpan(List<SToken> subrefTokens) {
		SSpan subref = null;
		/*
		 * Compare the list of subrefTokens with the list of sorted tokens
		 * for each span to find out if a span spanning the same tokens
		 * already exists in the document graph.
		 * 
		 */
		forspans: for (SSpan span : graph.getSpans()) {
			List<SToken> sortedSpanTokens = graph.getSortedTokenByText(graph.getOverlappedTokens(span));
			if (sortedSpanTokens.size() == subrefTokens.size()) {
				if (sortedSpanTokens.containsAll(subrefTokens)) {
					subref = span;
					String name = subref.getName();
					if (name.isEmpty()) {
						subref.setName(markerMap.get(subRefDefinitionMarker));
					}
					break forspans;
				}
			}
		}
		if (subref == null) {
			subref = graph.createSpan(subrefTokens);
			subref.setName(markerMap.get(subRefDefinitionMarker));
		}
		return subref;
	}

	/**
	 * TODO: Description
	 *
	 * @param subrefLine
	 * @return
	 */
	public SubrefDefinition createSubrefDefinitionFromSubrefLine(String subrefLine) {
		final String[] split = subrefLine.split("\\s+");
		SUBREF_TYPE type = determineSubrefType(split);
		List<Range<Integer>> ranges = new ArrayList<>();
		String identifier = null;
		String targetLayer = null;
		if (type != null) {
			switch (type) {
			case UNIDENTIFIED_GLOBAL:
				ranges.add(Range.between(Integer.parseInt(split[0]), Integer.parseInt(split[1])));
				break;

			case UNIDENTIFIED_GLOBAL_TARGETED:
				targetLayer = split[0];
				ranges.add(Range.between(Integer.parseInt(split[1]), Integer.parseInt(split[2])));
				break;

			case IDENTIFIED_GLOBAL:
				identifier = split[0];
				ranges.add(Range.between(Integer.parseInt(split[1]), Integer.parseInt(split[2])));
				break;

			case IDENTIFIED_GLOBAL_TARGETED:
				identifier = split[0];
				targetLayer = split[1];
				ranges.add(Range.between(Integer.parseInt(split[2]), Integer.parseInt(split[3])));
				break;

			case DISCONTINUOUS_TARGETED:
				identifier = split[0];
				targetLayer = split[1];
				for (int i = 2; i < split.length; i++) {
					ranges.add(Range.between(Integer.parseInt(split[i]), Integer.parseInt(split[++i])));
				}
				break;

			default:
				break;
			}
			return new SubrefDefinition(type, ranges, identifier, targetLayer);
		}
		return null;
	}

	/**
	 * TODO: Description
	 *
	 * @param split
	 * @return
	 */
	public SUBREF_TYPE determineSubrefType(String[] split) {
		switch (split.length) {
		case 2:
			if (testInts(split[0], split[1])) {
				return SUBREF_TYPE.UNIDENTIFIED_GLOBAL;
			}
			break;

		case 3:
			if (testInts(split[1], split[2])) {
				if (split[0].equals(lexMarker) || split[0].equals(morphMarker))
					return SUBREF_TYPE.UNIDENTIFIED_GLOBAL_TARGETED;
				else
					return SUBREF_TYPE.IDENTIFIED_GLOBAL;
			}
			break;
			
		case 4: 
			if (testInts(split[2], split[3])) {
				return SUBREF_TYPE.IDENTIFIED_GLOBAL_TARGETED;
			}
			break;
			
		default:
			if (split.length > 4 && Arrays.copyOfRange(split, 2, split.length).length % 2 == 0) {
				for (int i = 2; i < split.length; i++) {
					if (!testInts(split[i], split[++i])) {
						return null;
					}
				}
				return SUBREF_TYPE.DISCONTINUOUS_TARGETED;
			}
			break;
		}
		return null;
	}

	/**
	 * TODO: Description
	 *
	 * @param string
	 * @param string2
	 * @return
	 */
	private boolean testInts(String string, String string2) {
		return (ToolboxTextModulesUtils.isInteger(string) && ToolboxTextModulesUtils.isInteger(string2));
	}

}
