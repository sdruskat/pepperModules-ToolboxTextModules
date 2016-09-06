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
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
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
	private int genericDocumentNameCount = 0;
	private final Map<Identifier, Long> offsetMap = new HashMap<>();
	private ResourceHeaderend resourceHeader = null;
	
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
		// Get the char-based length of the \id marker
		int idMarkerLength = idMarker.length();
		// Use a CIS to memorize position, and put a buffered FIS inside (buffered for performance reasons)
		try (CountingInputStream str = new CountingInputStream(new BufferedInputStream(new FileInputStream(corpusFile))); 
				ByteArrayOutputStream bos = new ByteArrayOutputStream(idMarkerLength)) {
			int b;
			boolean isFirstIdOffsetWritten = false;
			while ((b = str.read()) > 0) {
				if (b == '\\') {
					// Remember the actual pointer position
					long offset = str.getCount() - 1;
					// Start putting the next idMarkerLength bytes into a byte array and compare with idMarker
					for (int i = 0; i < idMarkerLength; i++) {
						b = str.read();
						bos.write(b);
					}
					// If an \id marker is actually found, use the trimmed rest of the line as name for the newly created SDocument
					if (bos.toString().equals(idMarker)) {
						// If the first \id offset isn't recorded as end offset for the header yet, do so
						if (!isFirstIdOffsetWritten) {
							isFirstIdOffsetWritten = true;
							resourceHeader = new ResourceHeaderend(URI.createFileURI(corpusFile.getAbsolutePath()), offset);
						}
						// Read the full rest of the \id line to gain a name for the document
						StringBuilder sb = new StringBuilder();
						char c;
						while ((c = (char) str.read()) != '\n') {
							sb.append(c);
						}
						String idName = sb.toString().trim();
						if (idName == null || idName.isEmpty()) {
							logger.warn("The \\id line starting at byte offset " + str.getCount() + " is empty. The document will be given a generic name!");
							idName = "Document " + genericDocumentNameCount++;
						}
						SDocument doc = corpusGraph.createDocument(parent, idName);
						// Save all the important stuff for re-use in the importer
						resourceMap.put(doc.getIdentifier(), URI.createFileURI(corpusFile.getAbsolutePath()));
						offsetMap.put(doc.getIdentifier(), offset);
					}
					bos.reset();
				}
			}
		}
		catch (IOException e) {
			throw new PepperModuleException("Cannot read file " + corpusFile.getName() + "!");
		}
	}

	/**
	 * @return the resourceMap
	 */
	protected LinkedHashMap<Identifier, URI> getResourceMap() {
		return resourceMap;
	}

	/**
	 * @return the offsetMap
	 */
	protected Map<Identifier, Long> getOffsetMap() {
		return offsetMap;
	}

	/**
	 * TODO: Description
	 *
	 * @return
	 */
	public ResourceHeaderend getResourceHeader() {
		return resourceHeader;
	}
	
	/**
	 * TODO Description
	 *
	 * @author Stephan Druskat <mail@sdruskat.net>
	 *
	 */
	protected class ResourceHeaderend {
		private URI resource = null;
		private Long headerEnd = null;

		protected ResourceHeaderend(URI resource, Long headerEnd) {
			this.resource = resource;
			this.headerEnd = headerEnd;
		}

		/**
		 * @return the resource
		 */
		public URI getResource() {
			return resource;
		}

		/**
		 * @return the headerEnd
		 */
		public Long getHeader() {
			return headerEnd;
		}
	}
	
}
