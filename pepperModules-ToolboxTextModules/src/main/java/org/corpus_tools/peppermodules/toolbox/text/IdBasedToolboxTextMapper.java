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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SDocument;
import org.eclipse.emf.common.util.URI;
import com.google.common.io.CountingInputStream;

/**
 * TODO Description
 *
 * @author Stephan Druskat <mail@sdruskat.net>
 *
 */
public class IdBasedToolboxTextMapper extends AbstractToolboxTextMapper {
	
	private final Long headerEnd;
	private final Long offset;
	private final File corpusFile;
	private final Long nextOffset;
	
	/**
	 * Private constructor setting fields.
	 * 
	 * @param offset
	 * @param headerEnd
	 * @param resourceURI
	 * @param nextOffset
	 */
	private IdBasedToolboxTextMapper(Long offset, Long headerEnd, URI resourceURI, Long nextOffset) {
		this.offset = offset;
		this.nextOffset = nextOffset;
		this.headerEnd = headerEnd;
		this.setResourceURI(resourceURI);
		corpusFile = new File(resourceURI.toFileString());
	}
	
	/**
	 * Constructor for {@link SDocument}s, which takes parameters for the
	 * offset, the resource's {@link URI} and the offset of the next
	 * section to be mapped onto an {@link SDocument}.
	 * 
	 * @param offset
	 * @param resourceURI
	 * @param nextOffset
	 */
	public IdBasedToolboxTextMapper(Long offset, URI resourceURI, Long nextOffset) {
		this(offset, null, resourceURI, nextOffset);
	}

	/**
	 * Constructor for {@link SCorpus}, which takes parameters for the
	 * header end offset and the reource's {@link URI}.
	 * 
	 * @param headerEnd
	 * @param resource
	 */
	public IdBasedToolboxTextMapper(Long headerEnd, URI resource) {
		this(0l, headerEnd, resource, null);
	}

	/* (non-Javadoc)
	 * @see org.corpus_tools.pepper.impl.PepperMapperImpl#mapSCorpus()
	 */
	@Override
	public DOCUMENT_STATUS mapSCorpus() {
		List<String> headerLines = new ArrayList<>();
		
		/* 
		 * Stream the file contents up to headerEnd - 1 (which will be the backslash for the first \id marker),
		 * and write the read bytes into a BOS. If an '\n' is encountered, add the contents of the BOS to the
		 * list of header lines. Once the streaming is completed, map the header lines to meta annotations on the corpus.
		 * 
		 * NOTE: Doesn't work for OS not using '\n' in EOLs, e.g., Mac OS <= v9!
		 */
		try (CountingInputStream str = new CountingInputStream(new BufferedInputStream(new FileInputStream(corpusFile)));
				ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			int b;
			while (str.getCount() < (headerEnd - 1)) {
				b = str.read();
				if ((char) b != '\n') {
					bos.write(b);
				}
				else {
					headerLines.add(bos.toString().trim());
					bos.reset();
				}
			}
		}
		catch (IOException e) {
			throw new PepperModuleException("Could not read corpus file " + corpusFile.getAbsolutePath() + "!", e);
		}
		headerLines.removeAll(Arrays.asList("", null));
		for (String headerLine : headerLines) {
			String rawMarker = headerLine.split("\\s+")[0];
			String marker = rawMarker.substring(1);
			String line = headerLine.substring(rawMarker.length(), headerLine.length()).trim();
			getCorpus().createMetaAnnotation(SALT_NAMESPACE_TOOLBOX, marker, line);
		}
		return (DOCUMENT_STATUS.COMPLETED);
	}
	
	@Override
	public DOCUMENT_STATUS mapSDocument() {
		// Compile a list of lines in the section of the file that is delimited by offset and next offset
		List<String> lines = new ArrayList<>();
		try (CountingInputStream str = new CountingInputStream(new BufferedInputStream(new FileInputStream(corpusFile)));
				ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			str.skip(offset);
			int b;
			while (str.getCount() < nextOffset) {
				b = str.read();
				if ((char) b != '\n') {
					bos.write(b);
				}
				else {
					lines.add(bos.toString().trim());
					bos.reset();
				}
			}
		}
		catch (IOException e) {
			throw new PepperModuleException("Cannot read corpus file " + corpusFile.getAbsolutePath() + "!", e);
		}
		
		// Drop empty lines
		lines.removeAll(Arrays.asList("", null));
		
		return (DOCUMENT_STATUS.COMPLETED);
	}


}
