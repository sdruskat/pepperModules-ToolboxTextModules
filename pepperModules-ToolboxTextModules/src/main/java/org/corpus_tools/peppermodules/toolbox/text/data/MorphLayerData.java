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
package org.corpus_tools.peppermodules.toolbox.text.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

import org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporterProperties;
import org.corpus_tools.peppermodules.toolbox.text.mapping.RefMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ListMultimap;

/**
 * An extension of {@link LayerData}, specifically targeted at
 * compiling morphological data, which needs extra functionality
 * such as the linking of groups of morphemes to lexical units,
 * delimiter handling, etc.
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 *
 */
public class MorphLayerData extends LayerData {
	
	private static final Logger log = LoggerFactory.getLogger(MorphLayerData.class);
	
	private List<String> morphWords = new ArrayList<>();

	private ArrayList<String[]> morphemesInMorphWordList;

	/**
	 * @param markerContentMap
	 * @param marker
	 * @param originalPrimaryData
	 * @param annoMarkers
	 * @param segmented 
	 * @param ref 
	 * @param fixErrors 
	 * @param missingAnnoString 
	 * @param docName 
	 */
	public MorphLayerData(ListMultimap<String, String> markerContentMap, String marker, String originalPrimaryData, List<String> annoMarkers, boolean segmented, String missingAnnoString, boolean fixErrors, String docName, String ref) {
		super(markerContentMap, marker, originalPrimaryData, annoMarkers, segmented, missingAnnoString, fixErrors, docName, ref);
	}
	
	@Override
	public MorphLayerData compile() {
		return (MorphLayerData) super.compile();
	}

	/**
	 * Compiles "morphological words", i.e., groups of morphemes
	 * belonging to a lexical unit ("word").
	 * 
	 * First, any free affix delimiters are concatenated to the respective morpheme
	 * so that affixes can be used to group morphemes into "morphological words"
	 * downstream. Also, "liaison delimiters" are detected and memorized for later 
	 * deletion (during token mapping).
	 * 
	 * Then, the "morphological words are inferred and mapped to
	 * their lexical counterparts. 
	 * 
	 * @param affix The affix delimiter string
	 * @param clitic The clitic delimiter string
	 * @param liaison The liaison delimiter string
	 * @param attach Whether to attach delimiters
	 * @param attachToNext Whether to attach delimiters to the next morpheme per default
	 * @return The {@link MorphLayerData} object containing the compiled morphological layer data.
	 * 
	 * @see RefMapper#map()
	 * @see ToolboxTextImporterProperties#attachDelimiter()
	 * @see ToolboxTextImporterProperties#attachDelimiterToNext()
	 */
	public MorphLayerData compileMorphWords(String affix, String clitic, String liaison, boolean attach, boolean attachToNext) {
		List<String> morphs = getPrimaryData();
		attachDelimiters(affix, clitic, liaison, attach, attachToNext, morphs);
		/*
		 *  Count "words" and map words back to morphemes contained.
		 *  Concatenate prefixes first.
		 */
		morphemesInMorphWordList = new ArrayList<>();
		List<String> tmpList = new ArrayList<>();
		List<String> prefixWords = new ArrayList<>();
		List<Integer> liaisonIndices = new ArrayList<>();
		for (int i = 0; i < morphs.size(); i++) {
			String morph = morphs.get(i);
			/* 
			 * While we have an iterating index,
			 * check whether the morpheme starts
			 * with the liaison delimiter, and if
			 * so, remember the index. Based on these
			 * indices, the liaison delimiter will be
			 * dropped from the primary data later on.
			 */
			if (morph.startsWith(liaison)) {
				liaisonIndices.add(i);
			}
			if (morph.endsWith(affix) || morph.endsWith(clitic)) {
				tmpList.add(morph);
			}
			else {
				tmpList.add(morph);
				String prefixWord = "";
				for (String tmpMorph : tmpList) {
					prefixWord += tmpMorph;
				}
				prefixWords.add(prefixWord);
				morphemesInMorphWordList.add(tmpList.toArray(new String[tmpList.size()]));
				tmpList.clear();
			}
		}
		// Now concatenate suffixes
		List<String> morphWords = new ArrayList<>();
		ListIterator<String[]> iterator = morphemesInMorphWordList.listIterator();
		while (iterator.hasNext()) {
			String[] prefixWord = iterator.next();
			if (!prefixWord[0].startsWith(affix) && !prefixWord[0].startsWith(clitic) && !prefixWord[0].startsWith(liaison)) {
				String suffixWord = "";
				for (int j = 0; j < prefixWord.length; j++) {
					suffixWord += prefixWord[j];
				}
				morphWords.add(suffixWord);
			}
			else {
				if (morphWords.size() > 0) {
				String newMorphWord = morphWords.get(morphWords.size() - 1);
				for (String pw : prefixWord) {
					newMorphWord = newMorphWord.concat(pw);
				}
				morphWords.set(morphWords.size() - 1, newMorphWord);
				iterator.previous();
				String[] oldMorphsArr = iterator.previous();
				String[] newMorphsArr = Arrays.copyOf(oldMorphsArr, oldMorphsArr.length + prefixWord.length);
				newMorphsArr[newMorphsArr.length - 1] = prefixWord[0];
				iterator.next();
				iterator.set(newMorphsArr);
				iterator.next();
				iterator.remove();
				}
				else {
					log.debug("Reference {} in document {} does not contain suffixed morphemes, hence skipping concatenation.", ref, docName);
				}
			}
		}
		this.morphWords = morphWords;
		return this;
	}

	/**
	 * Attaches any free delimiters to the respective morphemes, 
	 * based on whether and where to attach. 
	 *
	 * @param affix The affix delimiter string
	 * @param clitic The clitic delimiter string 
	 * @param attach Whether to attach delimiters
	 * @param attachToNext Whether to attach delimiters to the next morpheme per default
	 * @param morphs A list of morphemes
	 * 
	 * @see ToolboxTextImporterProperties#attachDelimiter()
	 * @see ToolboxTextImporterProperties#attachDelimiterToNext()
	 */
	private void attachDelimiters(String affix, String clitic, String liaison, boolean attach, boolean attachToNext, List<String> morphs) {
		// Attach delimiters if specified
		if (attach) {
			for (ListIterator<String> iterator = morphs.listIterator(); iterator.hasNext();) {
				int index = iterator.nextIndex();
				String tok = iterator.next();
				if (tok.equals(affix) || tok.equals(clitic)) {
					if (attachToNext) {
						String next = iterator.next();
						iterator.set(tok + next);
						iterator.previous();
						iterator.previous();
						iterator.remove();
						// Process annotations accordingly
						for (Entry<String, List<String>> anno : getAnnotations().entries()) {
							String delimValue = null;
							if (index >= anno.getValue().size()) {
								log.warn("Mismatch between no. of morphemes and no. of annotations on layer \"" + anno.getKey() + "\" in document \"" + getDocName() + "\", reference \"" + getRef() + "\". Ignoring annotation.");
							}
							else {
								delimValue = anno.getValue().get(index);
								if (delimValue.equals(affix) || delimValue.equals(clitic)) {
									String nextValue = anno.getValue().get(index + 1);
									String newNextValue = delimValue + nextValue;
									anno.getValue().remove(index);
									anno.getValue().set(index, newNextValue);
								}
							}
						}
					}
					else {
						iterator.previous();
						String previous = iterator.previous();
						iterator.set(previous + tok);
						iterator.next();
						iterator.next();
						iterator.remove();
						// Process annotations accordingly
						for (Entry<String, List<String>> anno : getAnnotations().entries()) {
							String delimValue = anno.getValue().get(index);
							if (delimValue.equals(affix) || delimValue.equals(clitic)) {
								String previousValue = null;
								if (index - 1 >= 0) {
									previousValue = anno.getValue().get(index -1);
								}
								else {
									// Attach to next instead
								}
								if (previousValue != null) {
									String newPreviousValue = previousValue + delimValue;
									anno.getValue().remove(index);
									anno.getValue().set(index - 1, newPreviousValue);
								}
							}
						}
					}
				}
				else if (tok.equals(liaison)) {
					String next = iterator.next();
					iterator.set(tok + next);
					iterator.previous();
					iterator.previous();
					iterator.remove();
					// Process annotations accordingly
					for (Entry<String, List<String>> anno : getAnnotations().entries()) {
						String delimValue = null;
						if (index >= anno.getValue().size()) {
							log.warn("Mismatch between no. of morphemes and no. of annotations on layer \"" + anno.getKey() + "\" in document \"" + getDocName() + "\", reference \"" + getRef() + "\". Ignoring annotation.");
						}
						else {
							delimValue = anno.getValue().get(index);
							if (delimValue.equals(affix) || delimValue.equals(clitic)) {
								String nextValue = anno.getValue().get(index + 1);
								String newNextValue = delimValue + nextValue;
								anno.getValue().remove(index);
								anno.getValue().set(index, newNextValue);
							}
						}
					}					
				}
			}
		}
	}

	/**
	 * @return the morphWords
	 */
	public final List<String> getMorphWords() {
		return morphWords;
	}

	/**
	 * @return the morphemesInMorphWordList
	 */
	public final ArrayList<String[]> getMorphemesInMorphWordList() {
		return morphemesInMorphWordList;
	}

}
