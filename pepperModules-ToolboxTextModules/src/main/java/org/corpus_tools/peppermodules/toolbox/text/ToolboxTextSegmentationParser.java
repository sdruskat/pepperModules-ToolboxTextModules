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
package org.corpus_tools.peppermodules.toolbox.text;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CountingInputStream;

/**
 * TODO Description
 *
 * @author Stephan Druskat
 *
 */
public class ToolboxTextSegmentationParser {
	
	Logger logger = LoggerFactory.getLogger(ToolboxTextSegmentationParser.class);

	private final File file;
	private final String idMarker;
	private final String refMarker;
	private final int idMarkerLength;
	private final int refMarkerLength;
	private final List<Long> idOffsets = new ArrayList<>();
	private final Map<Long, List<Long>> refMap = new HashMap<>();

	public ToolboxTextSegmentationParser(File corpusFile, String idMarker, String refMarker) {
		this.file = corpusFile;
		this.idMarker = idMarker;
		this.refMarker = refMarker;
		this.idMarkerLength = idMarker.length();
		this.refMarkerLength = refMarker.length();
	}
	
	/**
	 * 
	 */
	public void parse() {
		logger.warn("Parsing");
		try (CountingInputStream stream = new CountingInputStream(new BufferedInputStream(new FileInputStream(file)));
				ByteArrayOutputStream idBos = new ByteArrayOutputStream(idMarkerLength);
				ByteArrayOutputStream refBos = new ByteArrayOutputStream(idMarkerLength)) {
			int currentByte;
			int longerMarkerLength = idMarkerLength >= refMarkerLength ? idMarkerLength : refMarkerLength;
			long currentIdOffset = -1;
			while ((currentByte = stream.read()) > 0) {
				long currentOffset = stream.getCount() - 1;
				if (currentByte == '\\') {
					for (int i = 0; i < longerMarkerLength + 1; i++) {
						currentByte = stream.read();
						if (i <= idMarkerLength) {
							idBos.write(currentByte);
						}
						if (i <= refMarkerLength) {
							refBos.write(currentByte);
						}
					}
					if (idBos.toString().equals(idMarker + ' ')) {
						currentIdOffset = currentOffset;
						idOffsets.add(currentIdOffset);
						refMap.put(currentIdOffset, new ArrayList<Long>());
					}
					else if (refBos.toString().equals(refMarker + ' ')) {
						if (refMap.get(currentIdOffset) == null) {
							refMap.put(-1L, new ArrayList<Long>());
							refMap.get(-1L).add(currentOffset);
						}
						else {
							refMap.get(currentIdOffset).add(currentOffset);
						}
					}
					idBos.reset();
					refBos.reset();
				}
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

}
