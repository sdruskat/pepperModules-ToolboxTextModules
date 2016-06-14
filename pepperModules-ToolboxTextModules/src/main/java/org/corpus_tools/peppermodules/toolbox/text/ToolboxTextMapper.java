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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Generated;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.core.SMetaAnnotation;
import org.eclipse.emf.common.CommonPlugin;
import org.eclipse.emf.common.util.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Description
 *
 * @author Stephan Druskat <mail@sdruskat.net>
 *
 */
public class ToolboxTextMapper extends PepperMapperImpl {
	
	private static final Logger logger = LoggerFactory.getLogger(ToolboxTextMapper.class);

	private static final String DATE_LAST_MAPPING = "date-last-mapping";
	
	/**
	 * Represents a block within the Toolbox file, either the
	 * "header" block (starting with "\_sh") or a segment block
	 * (starting with "\ref").
	 */
	private static Map<String, List<String>> block = null;
	
	/**
	 * The last Toolbox marker (any "tag" starting with a backslash ("\")
	 * that content has been mapped to.
	 */
	private static String lastActiveMarker = null;
	
	/**
	 * Whether the header block of the file has already been mapped.
	 */
	private static boolean isHeaderBlockMapped = false;
	

	/**
	 * Adds a single metadate to the corpus, namely the current date (no time).
	 */
	@Override
	public DOCUMENT_STATUS mapSCorpus() {
		DateFormat dateFormat = new SimpleDateFormat("dd MM yyyy");
		Date date = new Date();
		SMetaAnnotation dateLastMappingAnno = getCorpus().getMetaAnnotation(DATE_LAST_MAPPING); 
		if (dateLastMappingAnno != null) {
			dateLastMappingAnno.setValue(dateFormat.format(date));
		}
		else {
			getCorpus().createMetaAnnotation(null, DATE_LAST_MAPPING, dateFormat.format(date));
		}

		return (DOCUMENT_STATUS.COMPLETED);
	}

	/**
	 * Reads the Toolbox file line-by-line and maps the information 
	 * to Salt elements.
	 * <strong>OVERRIDE THIS METHOD FOR CUSTOMIZATION</strong> <br/>
	 * This is the place for the real work. Here you have to do anything
	 * necessary, to map a corpus to Salt. These could be things like:
	 * reading a file, mapping the content, closing the file, cleaning up
	 * and so on. <br/>
	 * In our dummy implementation, we do not read a file, for not making
	 * the code too complex. We just show how to create a simple
	 * document-structure in Salt, in following steps:
	 * <ol>
	 * <li>creating primary data</li>
	 * <li>creating tokenization</li>
	 * <li>creating part-of-speech annotation for tokenization</li>
	 * <li>creating information structure annotation via spans</li>
	 * <li>creating anaphoric relation via pointing relation</li>
	 * <li>creating syntactic annotations</li>
	 * </ol>
	 */
	@Override
	public DOCUMENT_STATUS mapSDocument() {
		// FIXME: Add progress via addProgress(0.n)
		getDocument().setDocumentGraph(SaltFactory.createSDocumentGraph());
		URI resource = getResourceURI();
		File file = null;
		logger.debug("Importing the file {}.", resource);
		
		/**
		 * STEP 1: Prepare the file for reading. 
		 */
		if (getResourceURI() == null) {
			throw new PepperModuleException(this, "Cannot map Toolbox file, because the given resource URI is empty.");
		}
		else {
			if (resource.isFile()) {
				String path = resource.toFileString();
				file = new File(path);
			}
			else {
				throw new PepperModuleException(this, "Resource is not a file!");
			}
		}
		
		/**
		 * STEP 2:  
		 */
		if (getDocument().getDocumentGraph() == null) {
			getDocument().setDocumentGraph(SaltFactory.createSDocumentGraph());
		}

		/** 
		 * STEP 3: Map the file to document
		 * File MUST be in UTF-8 encoding, otherwise the backslash may not be recognized!
		 */
		BufferedReader br = null;
		block = new HashMap<>();
		try {
			br = new BufferedReader(new FileReader(file));
			String line = br.readLine();
			while (line != null) {
				line = line.trim();
				if (!line.isEmpty()) {
					if (!line.startsWith("\\" + ((ToolboxTextImporterProperties) getProperties()).getRefMarker())) {
						addLineToBlock(line);
					}
					else { // Hit a "\ref" or "\id" marker or similar
						if (!isHeaderBlockMapped) { // "\ref" hit is first instance of "\ret", active block must be the header block
							mapHeaderToModel(block);
						}
						else {
							mapRefToModel(block);
						}
						block.clear();
						addLineToBlock(line);
					}
				}
				line = br.readLine();
			}
			// Hit a line that is null, so map block to model
			// This is assuming that the Toolbox file contains at least one \ref block
			mapRefToModel(block);
			block = null;
		} catch (FileNotFoundException e) {
			throw new PepperModuleException(this, "Cannot read file '" + getResourceURI() + "', because of nested exception: ", e);
		} catch (IOException e) {
			throw new PepperModuleException(this, "Cannot read file '" + getResourceURI() + "', because of nested exception: ", e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					throw new PepperModuleException(this, "Cannot close file '" + getResourceURI() + "', because of nested exception: ", e);
				}
			}
		}
//		getDocument().getDocumentGraph().createTextualDS(sb.toString());
		
		/**
		 * STEP 2: Read header and write as document (meta?) annotations
		 */
		
		/**
		 * STEP 3: Iterate through \refs and write sentence, word and token annotations
		 */
		
		
		return (DOCUMENT_STATUS.COMPLETED);


	}

	/**
	 * TODO: Description
	 *
	 * @param lastLine
	 */
	private void addLineToBlock(String line) {
		if (line.startsWith("\\")) {
			// Remove backslash from marker, include everything up to but excluding the first whitespace
			String[] markerAndValues = line.split("\\s+");
			String marker = markerAndValues[0].substring(1, markerAndValues[0].length());
			ArrayList<String> valueList = new ArrayList<>();
			for (int i = 1; i < markerAndValues.length; i++) {
				valueList.add(markerAndValues[i]);
			}
			block.put(marker, valueList);
			lastActiveMarker = marker;
		}
		else {
			List<String> valueList = block.get(lastActiveMarker);
			int lastIndex = valueList.size() - 1;
			StringBuilder builder = new StringBuilder();
			String lastValue = valueList.get(lastIndex);
			builder.append(lastValue);
			builder.append(System.getProperty("line.separator").toString());
			builder.append(line);
			valueList.remove(lastIndex);
			valueList.add(lastIndex, builder.toString());
			block.put(lastActiveMarker, valueList);
		}
	}

	/**
	 * TODO: Description
	 *
	 * @param headerBlock
	 */
	private void mapHeaderToModel(Map<String, List<String>> headerBlock) {
		for (Entry<String, List<String>> entry : headerBlock.entrySet()) {
			for (String value : entry.getValue()) {
				SMetaAnnotation metaAnno = SaltFactory.createSMetaAnnotation();
				metaAnno.setName(entry.getKey());
				metaAnno.setValue(value);
				getDocument().addMetaAnnotation(metaAnno);
			}
		}
		isHeaderBlockMapped = true;
	
	}

	/**
	 * TODO: Description
	 *
	 * @param refBlock
	 */
	private void mapRefToModel(Map<String, List<String>> refBlock) {
		for (Entry<String, List<String>> entry : refBlock.entrySet()) {
			for (String value : entry.getValue()) {
				// TODO
			}
		}
		refBlock.clear();
	}

}
