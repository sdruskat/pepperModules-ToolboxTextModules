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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.peppermodules.toolbox.text.AbstractToolboxTextMapper;
import org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter;
import org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporterProperties;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * This class can be extended to avoid re-duplication of the methods for
 * compiling a list of trimmed marker lines from the
 * {@link #trimmedInputString}.
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 *
 */
public abstract class AbstractBlockMapper extends AbstractToolboxTextMapper {
	
	private static final Logger log = LoggerFactory.getLogger(AbstractBlockMapper.class);
	
	protected final SDocumentGraph graph;
	private final String trimmedInputString;
	protected final ToolboxTextImporterProperties properties;
	protected final List<String> lines = new ArrayList<>();
	protected final ListMultimap<String, String> markerContentMap = ArrayListMultimap.create();

	/**
	 * @param properties 
	 * @param graph
	 * @param trimmedInputString
	 */
	public AbstractBlockMapper(PepperModuleProperties properties, SDocumentGraph graph, String trimmedInputString) {
		this.graph = graph;
		this.trimmedInputString = trimmedInputString;
		if (!(properties instanceof ToolboxTextImporterProperties)) {
			this.properties = null;
			throw new PepperModuleException("The wrong type of PepperModuleProperties have been passed to the mapper: " + properties.getClass().getSimpleName() + "!\n"
					+ "They must always be of type " + ToolboxTextImporterProperties.class.getSimpleName() + "!");
		}
		else {
			this.properties = (ToolboxTextImporterProperties) properties;
		}
		prepare();
	}

	/**
	 * Prepares the {@link #trimmedInputString} for mapping:
	 * 
	 * <ul>
	 * <li>Reads the {@link #trimmedInputString} line by line.</li>
	 * <li>Trims the lines.</li>
	 * <li>Adds the lines to the {@link #lines} {@link List}:
	 * <ul>
	 * <li>If the lines starts with a marker ("\"), add it</li>
	 * <li>If it doesn't, add it to the last line in {@link #lines}</li>
	 * </ul>
	 * </li>
	 * <li>Removes all empty and <code>null</code> lines</li>
	 * </ul>
	 */
	private void prepare() {
		// Break input string up into lines
		try (BufferedReader br = new BufferedReader(new StringReader(trimmedInputString))) {
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("\\")) {
					// Check whether the line contains more than a marker
					if (line.split(" ", 2).length > 1) {
						lines.add(line);
					}
					else {
						log.info("Ignoring the following line in candidacy to be included in '" + graph.getDocument().getName() + "' as it contains only a marker: '" + line + "'!");
					}
				}
				/*
				 * The first line cannot NOT start with \, as defined by the
				 * parsing mechanisms towards refMaps, etc. The latter looks for
				 * "\" + id marker and uses the offset of the found instance.
				 * Hence, rawLines must always have a size > 0 once a line that
				 * doesn't start with "\" is hit. Hence, no null checks needed.
				 */
				else {
					// Concatenate all lines not starting with \\, i.e.,
					// markers, to line before
					int lastIndex = lines.size() - 1;
					String lastLine = lines.get(lastIndex);
					lines.set(lastIndex, lastLine + " " + line);
				}
			}
		}
		catch (IOException e) {
			throw new PepperModuleException("Error preparing the String " + trimmedInputString + " for mapping!", e);
		}
		// Drop empty lines
		lines.removeAll(Arrays.asList("", null));
		// Sanity check: No duplicate marker lines allowed!
		Set<String> existingMarkers = new HashSet<>();
		Set<String> duplicateMarkers = new HashSet<>();
		for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();) {
			String l = iterator.next();
			String marker;
			if (!existingMarkers.add(marker = l.split(" ", 2)[0])) {
				/* 
				 * Both the unitref marker and the lines that can contain annotations to
				 * be applied to unitrefs can occur more than once in a block, hence
				 * test first if the marker under scrutiny belongs to either group before
				 * attempting to re-work lines.
				 */
				boolean hasUnitrefAnnoMarkers = properties.getUnitrefAnnotationMarkers() != null;
				String unitrefMarker = properties.getUnitrefDefinitionMarker();
				/* 
				 * If no unitref annotation markers have been defined, set the boolean 
				 * depending on the unitref marker only, else use both the latter and
				 * whether the marker is defined as a unitref annotation marker.
				 */
				boolean doProcessMarker = hasUnitrefAnnoMarkers ? 
						(!marker.substring(1).equals(unitrefMarker) && 
								!Arrays.asList(properties.getUnitrefAnnotationMarkers().split(ToolboxTextImporter.COMMA_DELIM_SPLIT_REGEX)).contains(marker.substring(1))) : 
						(!marker.substring(1).equals(unitrefMarker)) ;
				if (doProcessMarker) {
					if (properties.mergeDuplicateMarkers()) {
						log.info("Found more than one line marked with '" + marker + "':\n\"" + l + "\"\nAttempting to concatenate all lines with the same marker in the next step.");
						duplicateMarkers.add(marker);
					}
					else {
						log.warn("Found more than one line marked with '" + marker + "':\n\"" + l + "\"\nDropping all but the first line marked with it.");
						iterator.remove();
					}
				}
			}
		}
		// Merge duplicate marker lines if selected via property
		if (properties.mergeDuplicateMarkers() && !duplicateMarkers.isEmpty()) {
			ListIterator<String> iterator = lines.listIterator();
			for (String duplicateMarker : duplicateMarkers) {
				int firstLineIndex = -1;
				boolean firstLineSet = false;
				while (iterator.hasNext()) {
					int currentIndex = iterator.nextIndex();
					String line = iterator.next();
					if (line.startsWith(duplicateMarker + " ")) {
						if (!firstLineSet) {
							firstLineIndex = currentIndex;
							firstLineSet = true;
						}
						else {
							String oldLine = lines.get(firstLineIndex);
							lines.set(firstLineIndex, oldLine.concat(" ").concat(line.split(" ", 2)[1]));
							iterator.remove();
						}
					}
				}
			}
		}
		// Build markerContentMap
		for (String line : lines) {
			String[] split = line.split(" ", 2);
			markerContentMap.put(split[0].substring(1), split[1]);
		}
	}
	
	/**
	 * TODO: Description
	 *
	 */
	public abstract void map();

}
