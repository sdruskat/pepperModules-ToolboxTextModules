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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.peppermodules.toolbox.text.mapping.DocumentHeaderMapper;
import org.corpus_tools.peppermodules.toolbox.text.mapping.RefMapper;
import org.corpus_tools.peppermodules.toolbox.text.utils.MappingIndices;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.core.SMetaAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Range;
import com.google.common.io.CountingInputStream;

/**
 * TODO Description
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 *
 */
public class ToolboxTextMapper extends AbstractToolboxTextMapper {
	
	private static final Logger logger = LoggerFactory.getLogger(ToolboxTextMapper.class);
	
	private final Long headerEndOffset;
	private final Map<Long, List<Long>> refMap;
	private final Range<Long> idRange;
	
	private final boolean hasMorphology;

	/**
	 * @param headerEndOffset
	 * @param refMap
	 * @param idRange
	 * @param hasMorphology 
	 */
	public ToolboxTextMapper(Long headerEndOffset, Map<Long, List<Long>> refMap, Range<Long> idRange, boolean hasMorphology) {
		this.idRange = idRange;
		this.refMap = refMap;
		this.headerEndOffset = headerEndOffset;
		this.hasMorphology = hasMorphology;
	}

	/**
	 * {@inheritDoc PepperMapper#setDocument(SDocument)}
	 * 
	 * OVERRIDE THIS METHOD FOR CUSTOMIZED MAPPING.
	 */
	@Override
	public DOCUMENT_STATUS mapSDocument() {
		final boolean eDM = getProperties().errorDetectionMode();
		SDocumentGraph graph = getDocument().getDocumentGraph();
		File file = new File(getResourceURI().toFileString());
		
		// Create a timeline to linearize lexical and morphological tokens
		if (!eDM && hasMorphology) {
			graph.createTimeline();
		}

 
		try (RandomAccessFile raf = new RandomAccessFile(file, "r"); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			int currentByte;
			// Whether this document is an orphan, i.e., contains no \refs
			boolean isOrphan = false;
			List<Long> refOffsets = refMap.get(idRange.lowerEndpoint());
			if (isMonolithic()) {
				/*
				 * If a document is monolithic, i.e., contains no \id markers,
				 * it cannot have a header. Hence, no header will be mapped, and
				 * instead of calling an instance of DocumentHeaderMapper, the
				 * name of the SDocument will be set to the file name sans extension.
				 */
				if (!eDM) {
					String fileName = file.getName();
					graph.getDocument().setName(fileName.substring(0, fileName.lastIndexOf('.')));
				}
				refOffsets = refMap.get(-1L);
			}
			else if (!eDM) {
				// The offset at which the header of this document ends
				long docHeaderEndOffset;
				/*
				 * If the list of \ref offsets for this document is not empty, i.e.,
				 * the document is not an orphan, the headerEndOffset is the offset 
				 * of the start of the first ref, i.e., the first offset in the list.
				 * Otherwise, it's the end of the idRange, i.e., its upper endpoint.
				 */
				if (refOffsets.size() == 0) {
					docHeaderEndOffset = idRange.upperEndpoint();
					isOrphan = true;
				}
				else {
					docHeaderEndOffset = refOffsets.get(0);
				}
				// Parse document header
				raf.seek(idRange.lowerEndpoint());
				while ((currentByte = raf.read()) > 0 && raf.getFilePointer() <= docHeaderEndOffset) {
					bos.write(currentByte);
				}
				DocumentHeaderMapper documentHeaderMapper = new DocumentHeaderMapper(getProperties(), graph, bos.toString().trim());
				documentHeaderMapper.map();
				bos.reset();
			}

			MappingIndices indices = new MappingIndices();
			
			// Parse refs if the document is not an orphan
			if (!isOrphan) {
				for (Long refOffset : refOffsets) {
					Long nextOffset;
					boolean isLast = false;
					if (refOffsets.indexOf(refOffset) == refOffsets.size() - 1) {
						nextOffset = idRange.upperEndpoint();
						isLast = true;
					}
					else {
						nextOffset = refOffsets.get(refOffsets.indexOf(refOffset) + 1);
					}
					raf.seek(refOffset);
					while ((currentByte = raf.read()) > 0 && raf.getFilePointer() <= nextOffset) {
						bos.write(currentByte);
					}
					RefMapper refMapper = new RefMapper(getProperties(), graph, bos.toString().trim(), hasMorphology, indices);
					refMapper.map();
					indices = refMapper.getIndices();
					bos.reset();
				}
			}
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return DOCUMENT_STATUS.COMPLETED;
	}

	/**
	 * {@inheritDoc PepperMapper#setCorpus(SCorpus)}
	 * 
	 * Streams the corpus file from 0 to the byte offset of the end
	 * of the header (i.e., the offset of the first \id or \ref) and
	 * writes all read bytes into a {@link ByteArrayOutputStream}.
	 * Every time the stream encounters a marker (starting with \),
	 * it calls {@link #getMarkerAndValueFromString(String)} passing
	 * the (non-empty) contents of the {@link ByteArrayOutputStream} 
	 * as parameter. The resulting two elements of the returned
	 * {@link String} array are then written to a new {@link SMetaAnnotation}
	 * on the {@link SDocument}, using the first element as annotation name,
	 * and the second element as annotation value, or the empty {@link String}
	 * if no second element exists. Consecutively, the 
	 * {@link ByteArrayOutputStream} is reset to take up the next line.
	 * 
	 */
	@Override
	public DOCUMENT_STATUS mapSCorpus() {
		final boolean eDM = getProperties().errorDetectionMode();
		File file = new File(getResourceURI().toFileString());
		headerParsing:
		try (CountingInputStream stream = new CountingInputStream(new BufferedInputStream(new FileInputStream(file)));
				ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			int currentByte;
			String[] markerAndValue = null;
			while ((currentByte = stream.read()) > 0 && stream.getCount() < headerEndOffset) {
				/* 
				 * If we hit a new marker, split the trimmed contents of bos into 
				 * marker and value and write them to a meta annotation, unless
				 * the marker is the \ref marker, which means that we have hit an
				 * orphan \ref before the first \id marker, in which case write and abort!
				 */
				if (currentByte == '\\' && bos.size() > 0) {
					markerAndValue = getMarkerAndValueFromString(bos.toString().trim());
					if (!markerAndValue[0].equals(getProperties().getRefMarker())) {
						if (!eDM) {
							getCorpus().createMetaAnnotation(SALT_NAMESPACE_TOOLBOX, markerAndValue[0], markerAndValue.length > 1 ? markerAndValue[1] : "");
						}
						bos.reset();
					}
					else {
						logger.warn("Found an orphan \\ref in the corpus header of \"" + file.getName() + "\" at byte " + stream.getCount() + ".\nWill neglect it and stop parsing the corpus header, and write the content that has already been parsed to the model.");
						// Break the whole try block
						break headerParsing;
					}
				}
				bos.write(currentByte);
			}
			// bos still contains the last marker line, so write that to the list of marker lines.
			if (!eDM) {
				markerAndValue = getMarkerAndValueFromString(bos.toString().trim());
				getCorpus().createMetaAnnotation(SALT_NAMESPACE_TOOLBOX, markerAndValue[0], markerAndValue.length > 1 ? markerAndValue[1] : "");
			}
		}
		catch (FileNotFoundException e) {
			throw new PepperModuleException("The corpus file " + getResourceURI().toFileString() + " has not been found.", e);
		}
		catch (IOException e) {
			throw new PepperModuleException("Error while parsing the corpus file " + getResourceURI().toFileString() + "!", e);
		}
		return DOCUMENT_STATUS.COMPLETED;
	}

	/**
	 * Takes a {@link String} parameter, and splits its substring from
	 * index 1 (dropping the \ of the marker) once at the first whitespace,
	 * the resulting {@link String} of which it returns.
	 * 
	 * Example: the {@link String} "<code>\id Some id or other</code>"
	 * returns <code>[id, Some id or other]</code>.
	 *
	 * @param line
	 * @return A {@link String} array with two elements, of which the first 
	 * is the marker {@link String} sans backslash, and of which the second 
	 * is the contents of the marked line
	 */
	private String[] getMarkerAndValueFromString(String line) {
		return line.substring(1).split(" ", 2);
	}

	/**
	 * Verifies whether the document to be mapped is the
	 * single document in a monolithic corpus, i.e., a
	 * corpus which contains only one document, which in
	 * turn contains all the \refs.
	 * 
	 * In this case, the Toolbox file will not contain any
	 * `\id`s but only `\ref`s.
	 *
	 * @return
	 */
	private boolean isMonolithic() {
		return refMap.size() == 1 && refMap.containsKey(-1L);
	}

	/**
	 * @return the properties
	 */
	@Override
	public ToolboxTextImporterProperties getProperties() {
		return (ToolboxTextImporterProperties) super.getProperties();
	}

}
