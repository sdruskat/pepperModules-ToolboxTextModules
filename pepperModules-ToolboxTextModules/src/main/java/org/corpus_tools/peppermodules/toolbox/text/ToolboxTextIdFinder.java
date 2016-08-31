/*******************************************************************************
 * Copyright 2016 Stephan Druskat
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.graph.Identifier;
import org.eclipse.emf.common.util.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CountingInputStream;

/**
 * TODO Description
 *
 * @author Stephan Druskat <mail@sdruskat.net>
 *
 */
public class ToolboxTextIdFinder {
	
	private final File corpusFile;
	private final SCorpusGraph corpusGraph;
	private final SCorpus parent;
	private final LinkedHashMap<Identifier, URI> resourceMap = new LinkedHashMap<>();
	private final String idMarker;
	private static final Logger logger = LoggerFactory.getLogger(ToolboxTextIdFinder.class);
	
	/**
	 * @param corpusFile
	 * @param corpusGraph
	 * @param subCorpus
	 * @param idMarker 
	 */
	public ToolboxTextIdFinder(File corpusFile, SCorpusGraph corpusGraph, SCorpus subCorpus, String idMarker) {
		this.corpusFile = corpusFile;
		this.corpusGraph = corpusGraph;
		this.parent = subCorpus;
		this.idMarker = idMarker;
	}

	/**
	 * TODO: Description
	 */
	public void parse() {
		Set<Long> offsetSet = new HashSet<>();
		// Get the char-based length of the \id marker
		int idMarkerLength = idMarker.length();
		// Use a CIS to memorize position, and put a buffered FIS inside (buffered for performance reasons)
		try (CountingInputStream str = new CountingInputStream(new BufferedInputStream(new FileInputStream(corpusFile)))) {
			int b;
			while ((b = str.read()) > 0) {
				if (b == '\\') {
					// Remember the actual pointer position
					long offset = str.getCount() - 1;
					// Start putting the next idMarkerLength bytes into a byte array and compare with idMarker
					ByteArrayOutputStream bos = new ByteArrayOutputStream(idMarkerLength);
					for (int i = 0; i < idMarkerLength; i++) {
						b = str.read();
						bos.write(b);
					}
					if (bos.toString().equals(idMarker)) {
						offsetSet.add(offset);
					}
				}
			}
		}
		catch (IOException e) {
			throw new PepperModuleException("Cannot read file " + corpusFile.getName() + "!");
		}
		for (Long offset : offsetSet) {
			try (BufferedInputStream bis2 = new BufferedInputStream(new FileInputStream(corpusFile))) {
				bis2.skip(offset);
				StringBuilder sb = new StringBuilder();
				char c;
				while ((c = (char) bis2.read()) != '\n') {
					sb.append(c);
				}
				logger.info(sb.toString());
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	/**
	 * @return the resourceMap
	 */
	private LinkedHashMap<Identifier, URI> getResourceMap() {
		return resourceMap;
	}

}
