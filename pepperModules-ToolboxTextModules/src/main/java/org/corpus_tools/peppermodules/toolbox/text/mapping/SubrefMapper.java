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
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
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
		UNIDENTIFIED_GLOBAL,
		UNIDENTIFIED_GLOBAL_TARGETED,
		IDENTIFIED_GLOBAL,
		IDENTIFIED_GLOBAL_TARGETED,
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
			String[] typeSplit = subrefAnnoLine.split("\\s+");
			SUBREF_TYPE type = null;
			type = determineType(definedSubrefs, typeSplit);
			if (type != null) {
				switch (type) {
				
				case FULL_REF_ANNOTATION:
					mapFullRef(subrefAnnoLine, marker);
					break;

				default:
					SplitResult split = null;
					switch (type) {

					case SIMPLE:
						split = new SplitResult(3, subrefAnnoLine, null, false, false, false, "{int} {int} {annotation}").split();
						break;

					case SIMPLE_TARGETED:
						split = new SplitResult(4, subrefAnnoLine, null, true, false, false, "{target marker} {int} {int} {annotation}").split();
						break;

					case UNIDENTIFIED_GLOBAL:
						if (definedSubrefs.size() > 1) {
							log.warn("Subref type \"" + SUBREF_TYPE.UNIDENTIFIED_GLOBAL + "\" only allows for exactly one subref definition with marker \"" + subRefDefinitionMarker + "\"!\nHowever, " + definedSubrefs.size() + " were found. Ignoring subrefs!");
						}
						split = new SplitResult(3, subrefAnnoLine, definedSubrefs, false, false, false, "{int} {int}").split();
						break;

					case UNIDENTIFIED_GLOBAL_TARGETED:
						if (definedSubrefs.size() > 1) {
							log.warn("Subref type \"" + SUBREF_TYPE.UNIDENTIFIED_GLOBAL_TARGETED + "\" only allows for exactly one subref definition with marker \"" + subRefDefinitionMarker + "\"!\nHowever, " + definedSubrefs.size() + " were found. Ignoring subrefs!");
						}
						split = new SplitResult(4, subrefAnnoLine, definedSubrefs, true, false, false, "{definition} {target marker} {int} {int}").split();
						break;

					case IDENTIFIED_GLOBAL:
						split = new SplitResult(4, subrefAnnoLine, definedSubrefs, false, true, false, "{definition} {int} {int}").split();
						break;

					case IDENTIFIED_GLOBAL_TARGETED:
						split = new SplitResult(5, subrefAnnoLine, definedSubrefs, true, true, false, "{definition} {target marker} {int} {int}").split();
						break;

					case LINKED_TARGETED:
						split = new SplitResult(-1, subrefAnnoLine, definedSubrefs, true, true, true, "{identification} {target marker} {{int} {int}}*").split();
						break;

					default:
						log.warn("Something happened that shouldn't happen! Please report this as a bug!"); // FIXME
																											// Insert
																											// bug
																											// link!
						break;
					}
					if (split != null) {
						mapSubref(split, split.getTargetMarker() != null, marker);
					}
					else {
						log.warn("At this point, the " + SplitResult.class.getName() + " should not be null. Please report this as a bug!"); // FIXME
					}
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
		span.setName(fullRefAnnoLine);
		// Span *can* be null here for refs that don't have lexical tokens.
		if (span != null) {
			graph.getLayerByName(refData.getMarker()).get(0).addNode(span);
			span.createAnnotation("toolbox", marker, fullRefAnnoLine);
		}
	}

	/**
	 * TODO: Description
	 *
	 * @param split
	 * @param checkAgainstMarker
	 * @param marker
	 */
	private void mapSubref(SplitResult split, boolean checkAgainstMarker, String marker) {
		if (split.getRanges() == null) {
			Integer from = split.getSingleRange().getLeft();
			Integer to = split.getSingleRange().getRight();
			if (checkAgainstMarker) {
				if (split.getTargetMarker().equals(morphMarker)) {
					if (refHasMorphology) {
						mapData(split, from, to, marker, true);
					}
					else {
						log.warn("Found a subref annotation line (" + marker + " " + split.getAnnotation() + ") targeted at the morphology line. However, the ref \"" + refData.getPrimaryData() + "\" does not contain morphological information. Aborting mapping of subref.");
						return;
					}
				}
				else {
					mapData(split, from, to, marker, false);
				}
			}
			else {
				if (refHasMorphology) {
					mapData(split, from, to, marker, true);
				}
				else {
					mapData(split, from, to, marker, false);
				}
			}
		}
		else {
			mapLinkedSubref(split, marker);
		}
	}

	/**
	 * TODO: Description
	 *
	 * @param split
	 * @param marker
	 * @param mapToMorphology 
	 * @return
	 */
	private SSpan mapData(SplitResult split, int from, int to, String marker, boolean mapToMorphology) {
		SSpan subref = null;
		List<SSpan> spanList = graph.getSpansBySequence(new DataSourceSequence<Number>(mapToMorphology ? morphDS : lexDS, from, to));
		if (spanList != null && spanList.size() > 0) {
			subref = spanList.get(0);
		}
		else {
			List<SToken> orderedTokens = graph.getSortedTokenByText(mapToMorphology ? morphTokens : lexTokens);
			if (orderedTokens.size() < to) {
				log.warn("Cannot create subref as end offset is > index of tokens in " + refData.getPrimaryData() + "!");
				return null;
			}
			subref = graph.createSpan(orderedTokens.subList(from, to));
			subref.setName(marker);
		}
		// FIXME Bug: If annotation id already exists, fails here (cf. daakaka conversion)
		graph.getLayerByName(mapToMorphology ? morphMarker : lexMarker).get(0).addNode(subref);
		SAnnotation anno;
		if ((anno = subref.getAnnotation("toolbox::" + marker)) != null) {
			log.warn("Annotation {} already exists in reference {} in document {}!\nWill NOT change annotation value from {} to {}!", marker, refData.getRef(), refData.getDocName(), anno.getValue_STEXT(), split.getAnnotation());
		}
		else {
			subref.createAnnotation("toolbox", marker, split.getAnnotation());
		}
		return subref;
	}

	/**
	 * TODO: Description
	 *
	 * @param split
	 * @param marker 
	 */
	private void mapLinkedSubref(SplitResult split, String marker) {
		if (split.getTargetMarker().equals(morphMarker)) {
			if (refHasMorphology) {
				SSpan lastSpan = null;
				SSpan subref = null;
				for (Pair<Integer, Integer> range : split.getRanges()) {
					subref = mapData(split, range.getLeft(), range.getRight(), marker, true);
					if (subref == null) {
						return;
					}
					if (lastSpan != null) {
						SPointingRelation rel = (SPointingRelation) graph.createRelation(lastSpan, subref, SALT_TYPE.SPOINTING_RELATION, null);
						rel.setType("l");
						graph.getLayerByName(morphMarker).get(0).addRelation(rel);
					}
					lastSpan = subref;
				}
			}
			else {
				log.warn("Found a subref annotation line (" + marker + " " + split.getAnnotation() + ") targeted at the morphology line. However, the ref \"" + refData.getPrimaryData() + "\" does not contain morphological information. Aborting mapping of subref.");
			}
		}
		else {
			// map to tx
			SSpan lastSpan = null;
			SSpan subref = null;
			for (Pair<Integer, Integer> range : split.getRanges()) {
				subref = mapData(split, range.getLeft(), range.getRight(), marker, false);
				if (subref == null) {
					return;
				}
				if (lastSpan != null) {
					SPointingRelation rel = (SPointingRelation) graph.createRelation(lastSpan, subref, SALT_TYPE.SPOINTING_RELATION, null);
					rel.setType("l");
					graph.getLayerByName(morphMarker).get(0).addRelation(rel);
				}
				lastSpan = subref;
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
					return SUBREF_TYPE.UNIDENTIFIED_GLOBAL;

				case 3:
					String firstDefinedSubref = definedSubrefs.iterator().next().split("\\s+", 2)[0];
					if (firstDefinedSubref.equals(lexMarker) || firstDefinedSubref.equals(morphMarker)) {
						return SUBREF_TYPE.UNIDENTIFIED_GLOBAL_TARGETED;
					}
					else {
						return SUBREF_TYPE.IDENTIFIED_GLOBAL;
					}

				case 4:
					return SUBREF_TYPE.IDENTIFIED_GLOBAL_TARGETED;

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
	
	private class SplitResult {
		
		private String targetMarker = null;
		private String annotation = null;
		private Pair<Integer, Integer> singleRange = null;
		private List<Pair<Integer, Integer>> ranges = null;
		private final int numberOfSplits;
		private final String subrefAnnoLine;
		private final Set<String> definedSubrefs;
		private final boolean defined;
		private final boolean targeted;
		private final boolean identified;
		private final boolean linked;
		private final String format;

		/**
		 * @param numberOfSplits
		 * @param subrefAnnoLine
		 * @param definedSubrefs
		 * @param targeted
		 * @param identified
		 * @param linked
		 * @param format
		 */
		private SplitResult(int numberOfSplits, String subrefAnnoLine, Set<String> definedSubrefs, boolean targeted, boolean identified, boolean linked, String format) {
			this.numberOfSplits = numberOfSplits;
			this.subrefAnnoLine = subrefAnnoLine;
			this.definedSubrefs = definedSubrefs;
			this.defined = definedSubrefs != null;
			this.targeted = targeted;
			this.identified = identified;
			this.linked = linked;
			this.format = format;
		}
		
		protected SplitResult split() {
			if (!defined) {
				String[] split = subrefAnnoLine.split("\\s+", numberOfSplits);
				/*
				 * Not necessary to check for identified, as undefined
				 * subrefs cannot be identified.
				 */
				if (!targeted) {
					// SUBREF_TYPE.SIMPLE
					if (split.length < 3) {
						log.warn("Cannot map subref \"" + subrefAnnoLine + "\" in ref " + refData.getPrimaryData() + "! Line is too short.");
						return null;
					}
					/*
					 * 1 must be added to *to* because List#subList(from,to)
					 * works with **exclusive** *to*!
					 */
					singleRange = pairify(split[0], split[1]);
					annotation = split[2].trim();
				}
				else {
					// SUBREF_TYPE.SIMPLE_TARGETED
					if (split.length < 4) {
						log.warn("Cannot map subref \"" + subrefAnnoLine + "\" in ref " + refData.getPrimaryData() + "! Line is too short.");
						return null;
					}
					targetMarker = split[0];
					singleRange = pairify(split[1], split[2]);
					annotation = split[3];
				}
				if (singleRange == null) {
					return null;
				}
			}
			else {
				if (definedSubrefs == null || definedSubrefs.isEmpty()) {
					log.warn("Found an annotation on a defined subref in ref " + refData.getPrimaryData() + ", but could not find any definitions!");
					return null;
				}
				if (!identified) {
					String[] split = definedSubrefs.iterator().next().split("\\s+", numberOfSplits);
					if (!targeted) {
						// SUBREF_TYPE.UNIDENTIFIED_GLOBAL
						if (split.length < 2) {
							log.warn("Subref definition line is too short: \"" + Arrays.toString(split) + "\" in ref " + refData.getPrimaryData() + "!");
							return null;
						}
						singleRange = pairify(split[0], split[1]);
						annotation = subrefAnnoLine.trim();
					}
					else {
						// SUBREF_TYPE.UNIDENTIFIED_GLOBAL_TARGETED
						if (split.length < 3) {
							log.warn("Subref definition line is too short: \"" + Arrays.toString(split) + "\" in ref " + refData.getPrimaryData() + "!");
							return null;
						}
						targetMarker = split[0];
						singleRange = pairify(split[1], split[2]);
						annotation = subrefAnnoLine.trim();
					}
					if (singleRange == null) {
						return null;
					}
				}
				else {
					String[] annoSplit = subrefAnnoLine.split("\\s+", 2);
					String definitionLine = null;
					for (String definedSubref : definedSubrefs) {
						if (definedSubref.split("\\s+", 2)[0].equals(annoSplit[0])) {
							definitionLine = definedSubref;
						}
					}
					if (definitionLine == null) {
						log.warn("There is no subref definition \"" + annoSplit[0] + "\" in " + refData.getPrimaryData() + " for annotation line \"\\" + subrefAnnoLine + "\"! Ignoring subref!\nIf you think this is a bug, please report it!"); // FIXME Bug link!
						return null;
					}
					String[] split = definitionLine.split("\\s+", numberOfSplits);
					if (!linked) {
						if (!targeted) {
							// SUBREF_TYPE.IDENTIFIED_GLOBAL
							if (split.length < 3 || annoSplit.length < 2) {
								log.warn("Cannot map subref \"" + subrefAnnoLine + "\" in ref " + refData.getPrimaryData() + "! Line is too short.\n(definition: " + definitionLine + ", annotation: " + Arrays.toString(annoSplit) + ").");
								return null;
							}
							singleRange = pairify(split[1], split[2]);
							annotation = annoSplit[1].trim();
						}
						else {
							// SUBREF_TYPE.IDENTIFIED_GLOBAL_TARGETED
							if (split.length < 4 || annoSplit.length < 2) {
								log.warn("Cannot map subref \"" + subrefAnnoLine + "\" in ref " + refData.getPrimaryData() + "! Line is too short.\n(definition: " + definitionLine + ", annotation: " + Arrays.toString(annoSplit) + ").");
								return null;
							}
							targetMarker = split[1];
							singleRange = pairify(split[2], split[3]);
							annotation = annoSplit[1].trim();
						}
						if (singleRange == null) {
							return null;
						}
					}
					else {
						// SUBREF_TYPE.LINKED_TARGETED;
						if (annoSplit.length < 2) {
							log.warn("Cannot map subref \"" + subrefAnnoLine + "\" in ref " + refData.getPrimaryData() + "! Line is too short.\n(definition: " + definitionLine + ", annotation: " + Arrays.toString(annoSplit) + ").");
							return null;
						}
						split = definitionLine.split("\\s+");
						targetMarker = split[1];
						ranges = new ArrayList<>();
						for (int firstProbInt = 2; firstProbInt < split.length; firstProbInt += 2) {
							Pair<Integer, Integer> range = pairify(split[firstProbInt], split[firstProbInt + 1]);
							if (range == null) {
								return null;
							}
							ranges.add(range);
						}
						annotation = annoSplit[1].trim();
					}
				}
			}
			return this;
		}

		/**
		 * TODO: Description
		 *
		 * @param string
		 * @param string2
		 * @return
		 */
		private Pair<Integer, Integer> pairify(String from, String to) {
			Pair<Integer, Integer> pair = null;
			try {
				int fromInt = Integer.parseInt(from);
				int toInt = Integer.parseInt(to);
				pair = Pair.of(fromInt, toInt + 1);
			}
			catch (NumberFormatException e) {
				log.warn("The subref \"" + (defined ? definedSubrefs : subrefAnnoLine) + "\" is not in the correct format (" + format + ")! Ignoring it.\nIf you think this is a bug, please report it!"); // FIXME INsert bug link!
				return null;
			}
			return pair;
		}

		/**
		 * @return the targetMarker
		 */
		public final String getTargetMarker() {
			return targetMarker;
		}

		/**
		 * @return the singleRange
		 */
		public final Pair<Integer, Integer> getSingleRange() {
			return singleRange;
		}

		/**
		 * @return the ranges
		 */
		public final List<Pair<Integer, Integer>> getRanges() {
			return ranges;
		}

		/**
		 * @return the annotation
		 */
		public final String getAnnotation() {
			return annotation;
		}
		
	}

}
