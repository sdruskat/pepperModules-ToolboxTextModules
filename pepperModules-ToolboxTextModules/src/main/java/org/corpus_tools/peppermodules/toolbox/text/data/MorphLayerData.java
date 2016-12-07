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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ListMultimap;

/**
 * TODO Description
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 *
 */
public class MorphLayerData extends LayerData {
	
	private static final Logger log = LoggerFactory.getLogger(MorphLayerData.class);
	
	private List<String> morphWords = new ArrayList<>();
	private Map<String, ArrayList<String>> morphWordMorphemesMap;

	/**
	 * @param markerContentMap
	 * @param marker
	 * @param originalPrimaryData
	 * @param annoMarkers
	 * @param ref 
	 * @param string 
	 * @param fixErrors 
	 * @param missingAnnoString 
	 * @param b 
	 */
	public MorphLayerData(ListMultimap<String, String> markerContentMap, String marker, String originalPrimaryData, List<String> annoMarkers, boolean segmented, String missingAnnoString, boolean fixErrors, String docName, String ref) {
		super(markerContentMap, marker, originalPrimaryData, annoMarkers, segmented, missingAnnoString, fixErrors, docName, ref);
	}
	
	@Override
	public MorphLayerData compile() {
		return (MorphLayerData) super.compile();
	}

	public MorphLayerData compileMorphWords(String affix, String clitic, boolean attach, boolean attachToNext) {
		List<String> morphs = getPrimaryData();
		attachDelimiters(affix, clitic, attach, attachToNext, morphs);
		// Count "words" and map words back to morphemes contained
		morphWordMorphemesMap = new HashMap<>();
		ArrayList<String> prefixedmorphWords = new ArrayList<>();
		ArrayList<String> morphWords = new ArrayList<>();
		// Append prefixes first
		for (ListIterator<String> iterator = morphs.listIterator(); iterator.hasNext();) {
			String tok = iterator.next();
			if (tok.endsWith(affix) || tok.endsWith(clitic)) {
				if (attach) {
					String next = null;
					try {
					next = iterator.next();
					String val = tok.concat(next);
					prefixedmorphWords.add(val);
					mapMorphWordToMorphemes(tok, next, val);
					}
					catch (NoSuchElementException e) {
						log.warn("Cannot attach affix/clitic delimited morpheme \'" + tok + "\' because there is no other morpheme following it (document: \"" + getDocName() + "\", reference: \"" + getRef() + "\")! Ignoring this morpheme.");
					}
				}
				else {
					if (!tok.equals(affix) && !tok.equals(clitic)) {
						String next = iterator.next();
						String val = tok.concat(next);
						prefixedmorphWords.add(val);
						mapMorphWordToMorphemes(tok, next, val);
					}
					else {
						// As they should not be attached, add delimiter to list
						prefixedmorphWords.add(tok);
						if (morphWordMorphemesMap.get(tok) == null) {
							morphWordMorphemesMap.put(tok, new ArrayList<>(Arrays.asList(new String[]{tok})));
						}
					}
				}
			}
			else {
				prefixedmorphWords.add(tok);
				if (morphWordMorphemesMap.get(tok) == null) {
					morphWordMorphemesMap.put(tok, new ArrayList<>(Arrays.asList(new String[]{tok})));
				}
			}
		}
		// Append suffixes
		for (ListIterator<String> iterator = prefixedmorphWords.listIterator(); iterator.hasNext();) {
			String tok = iterator.next();
			if (tok.startsWith(affix) || tok.startsWith(clitic)) {
				if (attach) {
					int lastIndex = morphWords.size() - 1;
					String lastTokInList = morphWords.get(lastIndex);
					morphWords.remove(lastIndex);
					morphWords.add(lastTokInList + tok);
					mapMorphWordToMorphemes(lastTokInList, tok, lastTokInList + tok);
				}
				else {
					if (!tok.equals(affix) && !tok.equals(clitic)) {
						int lastIndex = morphWords.size() - 1;
						String lastTokInList = morphWords.get(lastIndex);
						morphWords.remove(lastIndex);
						morphWords.add(lastTokInList + tok);
						mapMorphWordToMorphemes(lastTokInList, tok, lastTokInList + tok);
					}
					else {
						// As they should not be attached, add delimiter to list
						morphWords.add(tok);
						if (morphWordMorphemesMap.get(tok) == null) {
							morphWordMorphemesMap.put(tok, new ArrayList<>(Arrays.asList(new String[]{tok})));
						}
					}
				}
			}
			else {
				morphWords.add(tok);
				if (morphWordMorphemesMap.get(tok) == null) {
					morphWordMorphemesMap.put(tok, new ArrayList<>(Arrays.asList(new String[]{tok})));
				}
			}
		}
		// Remove all entries from the morphWord-morphemes map that aren't actually morph words
		for (Iterator<String> iterator = morphWordMorphemesMap.keySet().iterator(); iterator.hasNext();) {
			String key = iterator.next();
			if (!morphWords.contains(key)) {
				iterator.remove();
			}
		}
		this.morphWords = morphWords;
		return this;
	}

	/**
	 * TODO: Description
	 *
	 * @param affix
	 * @param clitic
	 * @param attach
	 * @param attachToNext
	 * @param morphs
	 */
	private void attachDelimiters(String affix, String clitic, boolean attach, boolean attachToNext, List<String> morphs) {
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
							String delimValue = anno.getValue().get(index);
							if (delimValue.equals(affix) || delimValue.equals(clitic)) {
								String nextValue = anno.getValue().get(index + 1);
								String newNextValue = delimValue + nextValue;
								anno.getValue().remove(index);
								anno.getValue().set(index, newNextValue);
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
			}
		}
	}

	/**
	 * TODO: Description
	 *
	 * @param morphWordMorphemesMap
	 * @param m1
	 * @param m2
	 * @param morphWord
	 */
	private void mapMorphWordToMorphemes(String m1, String m2, String morphWord) {
		ArrayList<String> wordMorphemes;
		if ((wordMorphemes = morphWordMorphemesMap.get(m1)) != null) {
			wordMorphemes.add(m2);
		}
		else if ((wordMorphemes = morphWordMorphemesMap.get(m2)) != null) {
			wordMorphemes.add(m1);
		}
		else {
			wordMorphemes = new ArrayList<>();
			wordMorphemes.add(m1);
			wordMorphemes.add(m2);
		}
		morphWordMorphemesMap.put(morphWord, wordMorphemes);
	}
	
	/**
	 * @return the morphWords
	 */
	public final List<String> getMorphWords() {
		return morphWords;
	}

	/**
	 * @return the morphWordMorphemesMap
	 */
	public final Map<String, ArrayList<String>> getMorphWordMorphemesMap() {
		return morphWordMorphemesMap;
	}

}