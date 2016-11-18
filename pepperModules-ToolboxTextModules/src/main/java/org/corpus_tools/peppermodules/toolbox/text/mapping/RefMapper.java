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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter;
import org.corpus_tools.peppermodules.toolbox.text.data.LayerData;
import org.corpus_tools.peppermodules.toolbox.text.data.MorphLayerData;
import org.corpus_tools.peppermodules.toolbox.text.utils.MappingIndices;
import org.corpus_tools.peppermodules.toolbox.text.utils.MarkerContentMapConsistencyChecker;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides mapping functionality to map lines
 * from a Toolbox file that are the \ref line or a following line
 * before the next \ref line, to the respective elements, e.g. 
 * {@link SToken}, {@link SSpan}, {@link SAnnotation} etc.,
 * in the target {@link SDocument}'s {@link SDocumentGraph}.
 *
 * @author Stephan Druskat
 *
 */
public class RefMapper extends AbstractBlockMapper {
	
	private static final Logger log = LoggerFactory.getLogger(RefMapper.class);
	private static final String ERROR_LAYER_NAME = "err";
	private static final String ERROR_TOO_MANY = "+";
	private static final String ERROR_TOO_FEW = "-";
	private final boolean docHasMorphology;
	private MappingIndices indices;

	/**
	 * @param properties
	 * @param graph
	 * @param trimmedInputString
	 * @param hasMorphology 
	 * @param indices 
	 */
	public RefMapper(PepperModuleProperties properties, SDocumentGraph graph, String trimmedInputString, boolean hasMorphology, MappingIndices indices) {
		super(properties, graph, trimmedInputString);
		this.docHasMorphology = hasMorphology;
		this.indices = indices;
	}

	/**
	 * 
	 */
	@Override
	public void map() {
		
		/* \ref Unitref sentence schema 3 (defined global) to mb
		*	\met Sentence with two global unitrefs (morph-level) m26-m28 and m30-m31 on \\ur
		*	\\unitref 1 1 3
		*	\\unitref 2 5 6
		*	\tx Unitref with some random text just like that
		*	\ta t29 t30 t31 t32 t33 t34 t35 t36
		*	\mb m28 m29 m30 m31 m32 m33 m34 m35
		*	\ge M28 M29 M30 M31 M32 M33 M34 M35
		*	\\ur 1 Unitref m30-m32
		*	\\ur 2 Unitref m33-m34
		*	\ll uref three-mb
			*/
		
		// Single Markers
		String refMarker = properties.getRefMarker();
		String lexMarker = properties.getLexMarker();
		String morphMarker = properties.getMorphMarker();
		String unitrefMarker = properties.getUnitrefDefinitionMarker();
		
		// Marker groups
		String mks;
		List<String> lexAnnoMarkers = (mks = properties.getLexAnnotationMarkers()) != null ? Arrays.asList(mks.split(ToolboxTextImporter.COMMA_DELIM_SPLIT_REGEX)) : new ArrayList<String>();
		List<String> morphAnnoMarkers = (mks = properties.getMorphAnnotationMarkers()) != null ? Arrays.asList(mks.split(ToolboxTextImporter.COMMA_DELIM_SPLIT_REGEX)) : new ArrayList<String>();
		List<String> unitrefAnnoMarkers = (mks = properties.getUnitrefAnnotationMarkers()) != null ? Arrays.asList(mks.split(ToolboxTextImporter.COMMA_DELIM_SPLIT_REGEX)) : new ArrayList<String>();
		List<String> refAnnoMarkers = new ArrayList<>();
		for (String key : markerContentMap.keySet()) {
			if (!key.equals(refMarker) &&
					!key.equals(lexMarker) &&
					!key.equals(morphMarker) &&
					!key.equals(unitrefMarker) &&
					!lexAnnoMarkers.contains(key) && 
					!morphAnnoMarkers.contains(key) &&
					!unitrefAnnoMarkers.contains(key)) {
				refAnnoMarkers.add(key);
			}
		}
		
		// Test if all markers have been caught in a group or as a single marker 
		new MarkerContentMapConsistencyChecker(new HashSet<>(markerContentMap.keySet()),
				refMarker, lexMarker, morphMarker, unitrefMarker,
				lexAnnoMarkers, morphAnnoMarkers, unitrefAnnoMarkers, refAnnoMarkers,
				markerContentMap.get(refMarker)).run();
		
		// Single lines
		String ref = getSingleLine(refMarker);
		String lex = getSingleLine(lexMarker);
		String morph = docHasMorphology ? getSingleLine(morphMarker) : null;
		
		// Stop here if lex is empty or null
		if (lex == null || lex.isEmpty()) {
			log.warn("The reference \"" + ref + "\" in identifier \'" + getDocName() + "\' does not contain any primary data source (\\" + lexMarker + ") and will be ignored.");
			return;
		}
		
		// Prepare lexical and morphological layer lines and their annotation lines
		String missingAnnoString = properties.getMissingAnnoString();
		boolean fixErrors = properties.fixErrors();
		LayerData lexData = new LayerData(markerContentMap, lexMarker, lex, lexAnnoMarkers, true, missingAnnoString, fixErrors, getDocName(), ref).compile();
		LayerData refData = new LayerData(markerContentMap, refMarker, ref, refAnnoMarkers, false, missingAnnoString, fixErrors, getDocName(), ref).compile();
		MorphLayerData morphData = null;
		if (morph != null) {
			morphData = new MorphLayerData(markerContentMap, morphMarker, morph, morphAnnoMarkers, true, missingAnnoString, fixErrors, getDocName(), ref).compile();
			morphData.compileMorphWords(properties.getAffixDelim(), properties.getCliticDelim(), properties.attachDelimiter(), properties.attachDelimiterToNext());
			morphData = checkLexMorphInterl11n(lexData, morphData, refData);
		}
		else {
			log.warn("The reference \"" + ref + "\" in identifier \'" + getDocName() + "\' does not contain a line with morphological items.");
		}
		
	}

	/**
	 * TODO: Description
	 *
	 * @param lexData
	 * @param morphData
	 * @param ref
	 * @return 
	 */
	private MorphLayerData checkLexMorphInterl11n(LayerData lexData, MorphLayerData morphData, LayerData refData) {
		Map<String, List<String>> errors = new HashMap<>();
		int sumLex = lexData.getPrimaryData().size();
		List<String> morphWords = morphData.getMorphWords();
		List<String> morphs = morphData.getPrimaryData();
		// Copy for recording errors
		ArrayList<String> origMorphs = new ArrayList<>(morphs);
		int sumMorphWords = morphWords.size();
		if (sumMorphWords > sumLex) {
			log.warn("Document \"" + getDocName() + "\", reference " + refData.getPrimaryData() + ": The number of morphological units is larger than the number of lexical tokens (" + sumMorphWords + " morphological units vs. " + sumLex + " lexical tokens)!");
			System.err.println("Document \"" + getDocName() + "\", reference " + refData.getPrimaryData() + ": The number of morphological units is larger than the number of lexical tokens (" + sumMorphWords + " morphological units vs. " + sumLex + " lexical tokens)!");
			errors.put(properties.getMorphMarker().concat(ERROR_TOO_MANY), origMorphs);
			int excessMorphWordsSum = sumMorphWords - sumLex;
			List<String> excessMorphWords = morphWords.subList(morphWords.size() - excessMorphWordsSum, morphWords.size());
			int excessMorphemesSum = 0;
			for (String morphWord : excessMorphWords) {
				excessMorphemesSum += morphData.getMorphWordMorphemesMap().get(morphWord).size();
			}
			if (properties.fixErrors()) {
				// Concatenate excess data to last token
				List<String> coreMorphemes = morphs.subList(0, morphs.size() - excessMorphemesSum);
				List<String> excessMorphemes = morphs.subList(morphs.size() - excessMorphemesSum, morphs.size());
				for (Iterator<String> iterator = excessMorphemes.iterator(); iterator.hasNext();) {
					String em = iterator.next();
					int index = coreMorphemes.size() - 1;
					String lastMorpheme = coreMorphemes.get(index);
					lastMorpheme += " " + em;
					coreMorphemes.set(index, lastMorpheme);
				}
				morphData.setPrimaryData(coreMorphemes);
			}
			else {
				// Remove excess data
				morphData.setPrimaryData(morphs.subList(0, morphs.size() - excessMorphemesSum));
			}
		}
		else if (sumMorphWords < sumLex) {
			log.warn("Document \"" + getDocName() + "\", reference \'" + refData.getPrimaryData() + "\': The number of morphological units is lower than the number of lexical tokens (" + sumMorphWords + " morphological units vs. " + sumLex + " lexical tokens)!");
			System.out.println("Document \"" + getDocName() + "\", reference \'" + refData.getPrimaryData() + "\': The number of morphological units is lower than the number of lexical tokens (" + sumMorphWords + " morphological units vs. " + sumLex + " lexical tokens)!");
			errors.put(properties.getMorphMarker().concat(ERROR_TOO_FEW), origMorphs);
			int diffMorphWordsSum = sumLex - sumMorphWords;
			if (properties.fixErrors()) {
				for (int i = 0; i < diffMorphWordsSum; i++) {
					morphData.getPrimaryData().add(properties.getMissingAnnoString());
				}
			}
			else {
				// Ignore missing items
			}
		}
		if (properties.recordErrors()) {
			for (Entry<String, List<String>> error : errors.entrySet()) {
				refData.addAnnotation(error.getKey(), error.getValue());
				refData.addToAnnotation(ERROR_LAYER_NAME, error.getKey());
			}
		}
		return morphData;
	}

	/**
	 * Returns the single line for the marker, or throws
	 * an exception if there is more than one line marked
	 * with the marker, which at this point shouldn't be 
	 * the case for ref and morph markers!
	 *
	 * @param marker
	 * @return the contents of the single line marked by the marker.
	 */
	private String getSingleLine(String marker) {
		List<String> list = markerContentMap.get(marker);
		if (list.size() > 1) {
			throw new PepperModuleException("Reference block contains two \\" + marker + " lines, which at this point shouldn't be the case. Please report this as a bug!"); // FIXME: Provide link to final issues site.
		}
		else if (list.size() == 1) {
			return list.get(0);
		}
		else {
			return null;
		}
	}

	/**
	 * @return the indices
	 */
	public final MappingIndices getIndices() {
		return indices;
	}

	/**
	 * @param indices the indices to set
	 */
	public final void setIndices(MappingIndices indices) {
		this.indices = indices;
	}

	/**
	 * TODO: Description
	 *
	 * @return
	 */
	private String getDocName() {
		return graph.getDocument().getName();
	}

}
