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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
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
public class IdBasedToolboxTextMapper extends PepperMapperImpl {
	
	private static final Logger logger = LoggerFactory.getLogger(IdBasedToolboxTextMapper.class);

	private final Long headerEnd;
	private final Long offset;
	private final File corpusFile;
	
	private static final String SALT_NAMESPACE_TOOLBOX = "toolbox";
	
	/**
	 * @param long1
	 * @param string
	 */
	public IdBasedToolboxTextMapper(Long offset, Long headerEnd, URI resourceURI) {
		this.offset = offset;
		this.headerEnd = headerEnd;
		this.setResourceURI(resourceURI);
		corpusFile = new File(resourceURI.toFileString());
	}
	
	/**
	 * @param offset
	 */
	public IdBasedToolboxTextMapper(Long offset, URI resourceURI) {
		this(offset, null, resourceURI);
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
		try (BufferedInputStream str = new BufferedInputStream(new FileInputStream(corpusFile));
				ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			str.skip(offset);
			int b;
			while ((b = str.read()) > 0) {
				System.err.println((char) b);
			}
		}
		catch (IOException e) {
			throw new PepperModuleException("Cannot read corpus file " + corpusFile.getAbsolutePath() + "!", e);
		}
		return (DOCUMENT_STATUS.COMPLETED);
	}


}
