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
package org.corpus_tools.peppermodules.toolbox.text;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CountingInputStream;

/**
 * A parser for Toolbox files which detects document and phrase
 * segmentations (`\id`s and `\ref`s in the Toolbox world). 
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 *
 */
class ToolboxTextSegmentationParser {
	
	private static final Logger logger = LoggerFactory.getLogger(ToolboxTextSegmentationParser.class);
	
	private final File file;
	private final String idMarker;
	private final String refMarker;
	private final String morphMarker;
	private final int idMarkerLength;
	private final int refMarkerLength;
	private final int morphMarkerLength;
	private final List<Long> idOffsets = new ArrayList<>();
	private final Map<Long, List<Long>> refMap = new HashMap<>();
	private final Map<Long, Boolean> idStructureMap = new HashMap<>();



	ToolboxTextSegmentationParser(File corpusFile, String idMarker, String refMarker, String morphMarker) {
		this.file = corpusFile;
		this.idMarker = idMarker;
		this.refMarker = refMarker;
		this.morphMarker = morphMarker;
		this.idMarkerLength = idMarker.length();
		this.refMarkerLength = refMarker.length();
		this.morphMarkerLength = morphMarker.length();
	}
	
	void parse() {
		int longestMarkerLength = Math.max(idMarkerLength, Math.max(refMarkerLength, morphMarkerLength));
		try (CountingInputStream stream = new CountingInputStream(new BufferedInputStream(new FileInputStream(file)));
				ByteArrayOutputStream bos = new ByteArrayOutputStream(longestMarkerLength);) {
			int currentByte;
			long currentIdOffset = -1;
			boolean hasMorphology = false;
			while ((currentByte = stream.read()) > 0) {
				long currentOffset = stream.getCount() - 1;
				if (currentByte == '\\') { // Most likely hit a marker
					while ((currentByte = stream.read()) > -1) {
						/*
						 * If the stream hits a whitespace, it must be the end
						 * of the marker, so break out of this iteration.
						 */
						if (Character.isWhitespace((char) currentByte)) {
							break;
						}
						else {
							bos.write(currentByte);
						}
					}
					if (bos.toString().equals(idMarker)) {
						currentIdOffset = currentOffset;
						// Write hasMorphology information for document
						if (!idOffsets.isEmpty()) {
							Collections.sort(idOffsets);
							Long lastOffset = idOffsets.get(idOffsets.size() - 1);
							idStructureMap.put(lastOffset, hasMorphology);
						}
						// Add \id offset, prepare for \ref recording and reset hasMorphology
						idOffsets.add(currentIdOffset);
						refMap.put(currentIdOffset, new ArrayList<Long>());
						hasMorphology = false;
					}
					else if (bos.toString().equals(refMarker)) {
						if (refMap.get(currentIdOffset) == null) {
							refMap.put(-1L, new ArrayList<Long>());
							refMap.get(-1L).add(currentOffset);
						}
						else {
							refMap.get(currentIdOffset).add(currentOffset);
						}
					}
					else if (bos.toString().equals(morphMarker)) {
						hasMorphology = true;
					}
					bos.reset();
				}
			}
			// Write hasMorphology one last time
			if (!idOffsets.isEmpty()) {
				Collections.sort(idOffsets);
				Long lastOffset = idOffsets.get(idOffsets.size() - 1);
				idStructureMap.put(lastOffset, hasMorphology);
			}
			else {
				logger.error("\n\nrefmap\n{}", refMap);
				logger.error("\n\n-1l\n{}", refMap.get(-1L));
				logger.error("\n\n0\n{}", refMap.get(-1L).get(0));
				logger.error("\n\nmorph\n{}", hasMorphology);
				idStructureMap.put(refMap.get(-1L).get(0), hasMorphology);
			}
		} catch (IOException e) {
			throw new PepperModuleException("Could not read corpus file " + file.getAbsolutePath(), e);
		}
	}

	/**
	 * @return the refMap
	 */
	Map<Long, List<Long>> getRefMap() {
		return refMap;
	}

	/**
	 * @return the idOffsets
	 */
	List<Long> getIdOffsets() {
		return idOffsets;
	}
	
	/**
	 * @return the \id structure map
	 */
	Map<Long, Boolean> getIdStructureMap() {
		return idStructureMap;
	}

}
