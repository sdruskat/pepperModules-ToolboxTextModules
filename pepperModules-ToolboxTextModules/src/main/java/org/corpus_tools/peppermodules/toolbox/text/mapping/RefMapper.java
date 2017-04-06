/*******************************************************************************
 * Copyright 2016 Humboldt-Universität zu Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 * Stephan Druskat - initial API and implementation
 *******************************************************************************/
package org.corpus_tools.peppermodules.toolbox.text.mapping;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter;
import org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporterProperties;
import org.corpus_tools.peppermodules.toolbox.text.data.LayerData;
import org.corpus_tools.peppermodules.toolbox.text.data.MorphLayerData;
import org.corpus_tools.peppermodules.toolbox.text.utils.MappingIndices;
import org.corpus_tools.peppermodules.toolbox.text.utils.MarkerContentMapConsistencyChecker;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * This class provides mapping functionality to map lines
 * from a Toolbox file that are the \ref line or a following line
 * before the next \ref line, to the respective elements, e.g.
 * {@link SToken}, {@link SSpan}, {@link SAnnotation} etc.,
 * in the target {@link SDocument}'s {@link SDocumentGraph}.
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 *
 */
public class RefMapper extends AbstractBlockMapper {

	private static final Logger log = LoggerFactory.getLogger(RefMapper.class);
	private static final String ERROR_LAYER_NAME = "err";
	private static final String ERROR_TOO_MANY = "-p";
	private static final String ERROR_TOO_FEW = "-m";
	private final boolean docHasMorphology;
	private MappingIndices indices;
	private final STextualDS lexDS;
	private final STextualDS morphDS;
	private final Map<String, SLayer> layers;
	private LayerData refData;
	private List<SToken> lexTokens;
	private List<SToken> morphTokens;
	private boolean refHasMorphology = false;

	/**
	 * @param properties
	 * @param graph
	 * @param trimmedInputString
	 * @param hasMorphology
	 * @param indices
	 * @param morphDS 
	 * @param lexDS 
	 * @param layers 
	 */
	public RefMapper(PepperModuleProperties properties, SDocumentGraph graph, String trimmedInputString, boolean hasMorphology, MappingIndices indices, STextualDS lexDS, STextualDS morphDS, Map<String, SLayer> layers) {
		super(properties, graph, trimmedInputString);
		this.docHasMorphology = hasMorphology;
		this.indices = indices;
		this.lexDS = lexDS;
		this.morphDS = morphDS;
		this.layers = layers;
	}

	/**
	 * TODO Complete this with a general decription of the method.
	 * 
	 * ## Fixing interlinearization (interl11n) problems
	 * 
	 * Interlinearization problems are fixed depending on the value of
	 * {@link ToolboxTextImporterProperties#PROP_FIX_INTERL11N}.
	 * 
	 * @see ToolboxTextImporterProperties#PROP_FIX_INTERL11N
	 */
	/* (non-Javadoc)
	 * @see org.corpus_tools.peppermodules.toolbox.text.mapping.AbstractBlockMapper#map()
	 */
	@Override
	public boolean map() {
		
		// Single Markers
		String refMarker = properties.getRefMarker();
		String lexMarker = properties.getLexMarker();
		String morphMarker = properties.getMorphMarker();
		String subrefMarker = properties.getSubRefDefinitionMarker();

		// Marker groups
		String mks;
		List<String> lexAnnoMarkers = (mks = properties.getLexAnnotationMarkers()) != null ? Arrays.asList(mks.split(ToolboxTextImporter.COMMA_DELIM_SPLIT_REGEX)) : new ArrayList<String>();
		List<String> morphAnnoMarkers = (mks = properties.getMorphAnnotationMarkers()) != null ? Arrays.asList(mks.split(ToolboxTextImporter.COMMA_DELIM_SPLIT_REGEX)) : new ArrayList<String>();
		List<String> subrefAnnoMarkers = properties.getSubRefAnnotationMarkers() != null ? properties.getSubRefAnnotationMarkers() : new ArrayList<String>();
		List<String> refAnnoMarkers = new ArrayList<>();
		for (String key : markerContentMap.keySet()) {
			if (!key.equals(refMarker) && !key.equals(lexMarker) && !key.equals(morphMarker) && !key.equals(subrefMarker) && !lexAnnoMarkers.contains(key) && !morphAnnoMarkers.contains(key) && !subrefAnnoMarkers.contains(key)) {
				refAnnoMarkers.add(key);
			}
		}
		
		// Test if \ref is named
		if (getSingleLine(refMarker) == null) {
			markerContentMap.removeAll(refMarker);
			markerContentMap.put(refMarker, "Unnamed \\ref (" + new BigInteger(130, new SecureRandom()).toString(32) + ")");
		}
		
		// Test if all markers have been caught in a group or as a single marker
		new MarkerContentMapConsistencyChecker(new HashSet<>(markerContentMap.keySet()), refMarker, lexMarker, morphMarker, subrefMarker, lexAnnoMarkers, morphAnnoMarkers, subrefAnnoMarkers, refAnnoMarkers, markerContentMap.get(refMarker)).run();

		// Single lines
		String ref = getSingleLine(refMarker);
		String lex = getSingleLine(lexMarker);
		String morph = docHasMorphology ? getSingleLine(morphMarker) : null;
		refHasMorphology = morph != null;

		// Stop here if lex is empty or null
		if (lex == null || lex.isEmpty()) {
			log.warn("The reference \"" + ref + "\" in identifier \'" + getDocName() + "\' does not contain any primary data source (\\" + lexMarker + ") and will be ignored.");
			return false;
		}

		/*
		 * Prepare lexical and morphological layer lines and their annotation
		 * lines by fixing interl11n where needed.
		 */
		String missingAnnoString = properties.getMissingAnnoString();
		boolean fixErrors = properties.fixInterl11n();
		LayerData lexData = new LayerData(markerContentMap, lexMarker, lex, lexAnnoMarkers, true, missingAnnoString, fixErrors, getDocName(), ref).compile();
		LayerData refData = new LayerData(markerContentMap, refMarker, ref, refAnnoMarkers, false, missingAnnoString, fixErrors, getDocName(), ref).compile();
		MorphLayerData morphData = null;
		if (docHasMorphology && refHasMorphology) {
			morphData = new MorphLayerData(markerContentMap, morphMarker, morph, morphAnnoMarkers, true, missingAnnoString, fixErrors, getDocName(), ref).compile();
			morphData.compileMorphWords(properties.getAffixDelim(), properties.getCliticDelim(), properties.getLiaisonDelim(), properties.attachDelimiter(), properties.attachDelimiterToNext());
			morphData = checkLexMorphInterl11n(lexData, morphData, refData);
		}
		else {
			log.warn("The reference \"" + ref + "\" in identifier \'" + getDocName() + "\' does not contain a line with morphological items.");
		}
		
		// Now that we can have consistent token lines, check the
		// token-annotation interlinearization
		checkTokenAnnotationInterl11n(lexData, refData);
		if (refHasMorphology) {
			checkTokenAnnotationInterl11n(morphData, refData);
		}

		/*
		 * At this point, we should have consistent token and annotations lines,
		 * so let the mapping commence!
		 */
		Pair<List<SToken>, List<SToken>> tokens = mapTokens(refHasMorphology, lexData, morphData, refData);
		List<SToken> lexTokens = tokens.getLeft();
		List<SToken> morphTokens = tokens.getRight();
		mapRef(refData, lexTokens);
		
		this.refData = refData;
		this.lexTokens = lexTokens;
		this.morphTokens = morphTokens;
		return true;
	}

	/**
	 * TODO: Description
	 * FIXME: Declare why spans are always over lex tokens!
	 *
	 * @param refData
	 * @param lexTokens 
	 * @return 
	 */
	private SSpan mapRef(LayerData refData, List<SToken> lexTokens) {
		SSpan span = graph.createSpan(lexTokens);
		layers.get(refData.getMarker()).addNode(span);
		/*
		 *  Add the actual primary data as annotation.
		 *  As we deal with refData, for which #segmented == false,
		 *  we can safely assume that the list of primary data has
		 *  size 1 and contains only the complete ref name String.
		 */
		span.createAnnotation(SALT_NAMESPACE_TOOLBOX, refData.getMarker(), refData.getPrimaryData().get(0).trim());
		span.setName(refData.getPrimaryData().get(0).trim());
		addAnnotations(refData, Arrays.asList(new SNode[]{span}), false);
		return span;
	}

	/**
	 * TODO: Description
	 *
	 * @param hasMorphology
	 * @param lexData
	 * @param morphData
	 * @param refData 
	 * @return 
	 */
	private Pair<List<SToken>,List<SToken>> mapTokens(boolean hasMorphology, LayerData lexData, MorphLayerData morphData, LayerData refData) {
		List<SToken> lexTokens = new ArrayList<>();
		List<SToken> morphTokens = new ArrayList<>();
		// Build tokens, text and timeline
		if (docHasMorphology) {
			boolean hasLiaisonDelimiter = false;
			if (hasMorphology) {
//				boolean isLexInfoMappable = true;
				STimeline timeline = graph.getTimeline();
				int morphTimelineEnd = timeline.getEnd() == null ? 0 : timeline.getEnd();
				int lexTimelineEnd = morphTimelineEnd;
				for (String morpheme : morphData.getPrimaryData()) {
					// Drop liaison delimiter if necessary
					boolean dropLiaisonDelim = false;
					if (morpheme.startsWith(properties.getLiaisonDelim())) {
						hasLiaisonDelimiter = true;
						dropLiaisonDelim = true;
					}
					morpheme = dropLiaisonDelim ? morpheme.substring(1) : morpheme;
					// Create morphological token;
					morphDS.setText(morphDS.getText() + morpheme);
					SToken token = graph.createToken(morphDS, morphDS.getEnd() - morpheme.length(), morphDS.getEnd());
					morphTokens.add(token);
					STimelineRelation timeLineRel = SaltFactory.createSTimelineRelation();
					timeLineRel.setSource(token);
					timeLineRel.setTarget(timeline);
					timeLineRel.setStart(morphTimelineEnd);
					timeLineRel.setEnd(morphTimelineEnd += 1);
					graph.addRelation(timeLineRel);
					layers.get(morphData.getMarker()).addNode(token);
				}
				/* 
				 * Use int-based iteration because this helps interacting
				 * with the list of morph words.
				 */
				// First, re-build morph words, because the original ones might have changed in the meantimme
				for (int i = 0; i < lexData.getPrimaryData().size(); i++) {
					// Create lexical token
					String lexUnit = lexData.getPrimaryData().get(i);
					lexDS.setText(lexDS.getText().isEmpty() ? lexUnit : lexDS.getText() + " " + lexUnit);
					SToken token = graph.createToken(lexDS, lexDS.getEnd() - lexUnit.length(), lexDS.getEnd());
					lexTokens.add(token);
					/*
					 * timeSteps are calculated using the size of the list
					 * of morphemes for the morph word at the same index as
					 * the current lexUnit.
					 */
					int timeSteps = morphData.getMorphemesInMorphWordList().get(i).length;
					STimelineRelation timeLineRel = SaltFactory.createSTimelineRelation();
					timeLineRel.setSource(token);
					timeLineRel.setTarget(timeline);
					timeLineRel.setStart(lexTimelineEnd);
					timeLineRel.setEnd(lexTimelineEnd += timeSteps);
					timeline.increasePointOfTime(timeSteps);
					graph.addRelation(timeLineRel);
					layers.get(lexData.getMarker()).addNode(token);
				}
				addAnnotations(lexData, lexTokens, false);
				addAnnotations(morphData, morphTokens, hasLiaisonDelimiter);

			}
			else {
				// Document has morphology, but no morphology line in this ref
				STimeline timeline = graph.getTimeline();
				int timelineEnd = timeline.getEnd() == null ? 0 : timeline.getEnd();
				for (int i = 0; i < lexData.getPrimaryData().size(); i++) {
					// Create lexical token
					String lexUnit = lexData.getPrimaryData().get(i);
					lexDS.setText(lexDS.getText().isEmpty() ? lexUnit : lexDS.getText() + " " + lexUnit);
					SToken token = graph.createToken(lexDS, lexDS.getEnd() - lexUnit.length(), lexDS.getEnd());
					lexTokens.add(token);
					STimelineRelation timeLineRel = SaltFactory.createSTimelineRelation();
					timeLineRel.setSource(token);
					timeLineRel.setTarget(timeline);
					timeLineRel.setStart(timelineEnd);
					timeLineRel.setEnd(timelineEnd += 1);
					timeline.increasePointOfTime(1);
					graph.addRelation(timeLineRel);
					layers.get(lexData.getMarker()).addNode(token);
				}
				addAnnotations(lexData, lexTokens, false);
			}
		}
		else {				
			STimeline timeline = graph.getTimeline();
			int timelineEnd = timeline.getEnd() == null ? 0 : timeline.getEnd();
			for (String lexUnit : lexData.getPrimaryData()) {
				// Create lexical token
				lexDS.setText(lexDS.getText().isEmpty() ? lexUnit : lexDS.getText() + " " + lexUnit);
				SToken token = graph.createToken(lexDS, lexDS.getEnd() - lexUnit.length(), lexDS.getEnd());
				lexTokens.add(token);
				layers.get(lexData.getMarker()).addNode(token);
				STimelineRelation timeLineRel = SaltFactory.createSTimelineRelation();
				timeLineRel.setSource(token);
				timeLineRel.setTarget(timeline);
				timeLineRel.setStart(timelineEnd);
				timeLineRel.setEnd(timelineEnd += 1);
				timeline.increasePointOfTime(1);
				graph.addRelation(timeLineRel);
			}
			addAnnotations(lexData, lexTokens, false);
		}
		return Pair.of(lexTokens, morphTokens);
	}

	/**
	 * TODO: Description
	 *
	 * @param data
	 * @param nodes
	 * @param hasLiaisonDelimiter 
	 */
	private void addAnnotations(LayerData data, List<?> nodes, boolean hasLiaisonDelimiter) {
		for (Entry<String, List<String>> annotation : data.getAnnotations().entries()) {
			for (int i = 0; i < nodes.size(); i++) {
				Object node = nodes.get(i);
				if (!(node instanceof SNode)) {
					throw new PepperModuleException("Cannot add an annotation to an object that is not of type " + SNode.class.getSimpleName() + " (here: " + node.getClass().getName() + ")!");
				}
				else {
					String annotationValue = null;
					if (annotation.getKey().equals(ERROR_LAYER_NAME) || annotation.getKey().endsWith(ERROR_TOO_FEW) || annotation.getKey().endsWith(ERROR_TOO_MANY)) {
						StringBuilder sb = new StringBuilder();
						for (int j = 0; j < annotation.getValue().size(); j++) {
							if (hasLiaisonDelimiter) {
								if ((annotationValue = annotation.getValue().get(j)).startsWith(properties.getLiaisonDelim())) {
									annotationValue = annotationValue.substring(1);
								}
							}
							else {
								annotationValue = annotation.getValue().get(j);
							}
							if (j != annotation.getValue().size() - 1) {
								sb.append(annotationValue + ", ");
							}
							else {
								sb.append(annotationValue);
							}
						}
						((SNode) node).createAnnotation(SALT_NAMESPACE_TOOLBOX, annotation.getKey(), sb.toString().trim());
					}
					else {
						if (hasLiaisonDelimiter && annotation.getValue().get(i).startsWith(properties.getLiaisonDelim())) {
								annotationValue = annotation.getValue().get(i).substring(1);
						}
						else {
							annotationValue = annotation.getValue().get(i);
						}
						((SNode) node).createAnnotation(SALT_NAMESPACE_TOOLBOX, annotation.getKey(), annotationValue);
					}
				}
			}
		}
	}

	/**
	 * TODO: Description
	 *
	 * @param data
	 * @param refData
	 */
	private void checkTokenAnnotationInterl11n(LayerData data, LayerData refData) {
		Map<String, List<String>> errors = new HashMap<>();
		List<String> primaryData = data.getPrimaryData();
		ListMultimap<String, List<String>> annotations = data.getAnnotations();
		Collection<Entry<String, List<String>>> entriesCopy = new ArrayList<>(annotations.entries());
		for (Entry<String, List<String>> entry : entriesCopy) {
			String key = entry.getKey();
			List<List<String>> valuesCopy = new ArrayList<>(annotations.get(key));
			for (List<String> anno : valuesCopy) {
				List<String> shallowAnnoCopy = new ArrayList<>(anno);
				int annosN = anno.size();
				int primaryN = primaryData.size();
				// If there are more annotations than tokens
				if (annosN > primaryN) {
					String logMessage = "Document \"" + getDocName() + "\", reference " + refData.getPrimaryData() + ": The number of \'" + key + "\' annotations is larger than the number of \'" + data.getMarker() + "\' tokens (" + annosN + " annotations vs. " + primaryN + " tokens)!";
					errors.put(key.concat(ERROR_TOO_MANY), shallowAnnoCopy);
					if (properties.fixInterl11n()) {
						// Remove excess annotations
						logMessage += "\nRemoving excess morphological annotations from layer \'" + key + "\'!";
						annotations.remove(key, anno);
						annotations.put(key, anno.subList(0, primaryN));
					}
					else {
						// Concatenate excess annotations to last non-excess
						// annotation
						ListIterator<String> itr = anno.listIterator(primaryN);
						String concatResult = anno.get(primaryN - 1);
						while (itr.hasNext()) {
							concatResult += " " + itr.next();
						}
						List<String> annoCopy = anno.subList(0, primaryN);
						annoCopy.set(primaryN - 1, concatResult);
						annotations.remove(key, anno);
						annotations.put(key, annoCopy);
					}
					// else do nothing
					log.warn(logMessage);
				}
				// If there are less annotations than tokens
				else if (annosN < primaryN) {
					String logMessage = "Document \"" + getDocName() + "\", reference " + refData.getPrimaryData() + ": The number of \'" + key + "\' annotations is lower than the number of \'" + data.getMarker() + "\' tokens (" + annosN + " annotations vs. " + primaryN + " tokens)!";
					errors.put(key.concat(ERROR_TOO_FEW), anno);
					if (properties.fixInterl11n()) {
						String missingString = properties.getMissingAnnoString();
						// Replace missing annotations
						logMessage += "\nReplacing missing annotation on layer \'" + key + "\' with string \"" + missingString + "\"!";
						annotations.remove(key, anno);
						List<String> annoCopy = new ArrayList<>(anno);
						for (int i = 0; i < (primaryN - annosN); i++) {
							annoCopy.add(missingString);
						}
						annotations.put(key, annoCopy);
					}
					// else do nothing
					log.warn(logMessage);
				}
			}
		}
		if (properties.recordErrors()) {
			for (Entry<String, List<String>> error : errors.entrySet()) {
				refData.addAnnotation(error.getKey(), error.getValue());
				refData.addToAnnotation(ERROR_LAYER_NAME, error.getKey());
			}
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
		boolean isInterl11nFaulty = false;
		Map<String, List<String>> errors = new HashMap<>();
		int sumLex = lexData.getPrimaryData().size();
		List<String> morphWords = morphData.getMorphWords();
		List<String> morphs = morphData.getPrimaryData();
		// Copy for recording errors
		final List<String> shallowMorphsCopy = new ArrayList<>(morphs);
		int sumMorphWords = morphWords.size();
		// If there are more "morph words" than lexical items
		if (sumMorphWords > sumLex) {
			isInterl11nFaulty = true;
			String logMessage = "Document \"" + getDocName() + "\", reference " + refData.getPrimaryData() + ": The number of morphological units is larger than the number of lexical tokens (" + sumMorphWords + " morphological units vs. " + sumLex + " lexical tokens)!";
			logMessage += "\nThe number of annotations on these units may be too high as well!";
			errors.put(properties.getMorphMarker().concat(ERROR_TOO_MANY), shallowMorphsCopy);
			int excessMorphWordsSum = sumMorphWords - sumLex;
			int excessMorphemesSum = 0;
			ArrayList<String[]> morphWordsList = morphData.getMorphemesInMorphWordList();
			for (int i = sumMorphWords - excessMorphWordsSum; i < sumMorphWords; i++) {
				excessMorphemesSum += morphWordsList.get(i).length;
			}
			if (properties.fixInterl11n()) {
				// Remove excess data
				logMessage += "\nRemoving excess morphological units: " + morphs.subList(morphs.size() - excessMorphemesSum, morphs.size()) + "!";
				morphData.setPrimaryData(morphs.subList(0, morphs.size() - excessMorphemesSum));
				// Remove excess data in annotations if possible
				ArrayListMultimap<String, List<String>> morphAnnosCopy = ArrayListMultimap.create(morphData.getAnnotations());
				for (Iterator<Entry<String, List<String>>> iterator = morphAnnosCopy.entries().iterator(); iterator.hasNext();) {
					Entry<String, List<String>> anno = iterator.next();
					ArrayList<String> valueCopy = new ArrayList<>(anno.getValue());
					String key = anno.getKey();
					int sumAnno = anno.getValue().size();
					if (sumAnno == shallowMorphsCopy.size()) {
						errors.put(key.concat(ERROR_TOO_MANY), valueCopy);
						logMessage += "\nRemoving excess morphological annotations from layer \'" + key + "\'!";
						morphData.getAnnotations().remove(key, anno.getValue());
						morphData.getAnnotations().put(key, anno.getValue().subList(0, anno.getValue().size() - excessMorphemesSum));

					}
					// else leave checking / fixing for later stage
				}
			}
			else {
				// Concatenate excess data
				int firstExcessMorphIndex = morphs.size() - excessMorphemesSum;
				ListIterator<String> itr = morphs.listIterator(firstExcessMorphIndex);
				String concatResult = morphs.get(firstExcessMorphIndex - 1);
				while (itr.hasNext()) {
					concatResult += " " + itr.next();
				}
				List<String> morphsCopy = morphs.subList(0, firstExcessMorphIndex);
				morphsCopy.set(firstExcessMorphIndex - 1, concatResult);
				morphData.setPrimaryData(morphsCopy);
			}
			log.warn(logMessage);
		}
		// If there are fewer morphological units than lexical units
		else if (sumMorphWords < sumLex) {
			isInterl11nFaulty = true;
			String logMessage = "Document \"" + getDocName() + "\", reference \'" + refData.getPrimaryData() + "\': The number of morphological units is lower than the number of lexical tokens (" + sumMorphWords + " morphological units vs. " + sumLex + " lexical tokens)!";
			logMessage += "\nThe number of annotations on these units may be too low as well!";
			errors.put(properties.getMorphMarker().concat(ERROR_TOO_FEW), shallowMorphsCopy);
			int diffMorphWordsSum = sumLex - sumMorphWords;
			String missingString = properties.getMissingAnnoString();
			if (properties.fixInterl11n()) {
				logMessage += "\nReplacing missing morphological units with string \"" + missingString + "\"!";
				for (int i = 0; i < diffMorphWordsSum; i++) {
					// Fill data
					morphData.getPrimaryData().add(missingString);
				}
				// Fill data in annotations if possible
				for (Iterator<Entry<String, List<String>>> iterator = morphData.getAnnotations().entries().iterator(); iterator.hasNext();) {
					Entry<String, List<String>> anno = iterator.next();
					String key = anno.getKey();
					ArrayList<String> valueCopy = new ArrayList<>(anno.getValue());
					int sumAnno = anno.getValue().size();
					if (sumAnno == shallowMorphsCopy.size()) {
						logMessage += "\nReplacing missing annotation on layer \'" + key + "\' with string \"" + missingString + "\"!";
						for (int i = 0; i < diffMorphWordsSum; i++) {
							errors.put(key.concat(ERROR_TOO_FEW), valueCopy);
							anno.getValue().add(missingString);
						}
					}
					// else leave checking / fixing for later stage
				}
			}
			// else do nothing
			log.warn(logMessage);
		}
		if (properties.recordErrors()) {
			for (Entry<String, List<String>> error : errors.entrySet()) {
				refData.addAnnotation(error.getKey(), error.getValue());
				refData.addToAnnotation(ERROR_LAYER_NAME, error.getKey());
			}
		}
		/*
		 *  If the interl11n has proven faulty, the morph words
		 *  need to be re-compiled as they are used later to
		 *  calculate time steps! 
		 */
		if (isInterl11nFaulty) {
			morphData.compileMorphWords(properties.getAffixDelim(), properties.getCliticDelim(), properties.getLiaisonDelim(), properties.attachDelimiter(), properties.attachDelimiterToNext());
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
			throw new PepperModuleException("Reference block contains two \\" + marker + " lines, which at this point shouldn't be the case. Please report this as a bug!"); 
			// FIXME: Provide link to final issue site
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
	 * @param indices
	 *            the indices to set
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

	/**
	 * @return the refData
	 */
	public final LayerData getRefData() {
		return refData;
	}

	/**
	 * @return the lexTokens
	 */
	public final List<SToken> getLexTokens() {
		return lexTokens;
	}

	/**
	 * @return the morphTokens
	 */
	public final List<SToken> getMorphTokens() {
		return morphTokens;
	}

	/**
	 * TODO: Description
	 *
	 * @return
	 */
	public boolean refHasMorphology() {
		return refHasMorphology;
	}

}
