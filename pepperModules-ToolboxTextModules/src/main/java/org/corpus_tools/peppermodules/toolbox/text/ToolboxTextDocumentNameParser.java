/*******************************************************************************
 * Copyright (c) 2017 Stephan Druskat
 * Exploitation rights belong exclusively to Humboldt-Universität zu Berlin
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.regex.Pattern;

import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A parser for document names taken from \id marker lines in a Toolbox text file.
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 *
 */
class ToolboxTextDocumentNameParser {
	
	private static final Logger logger = LoggerFactory.getLogger(ToolboxTextDocumentNameParser.class);
	
	/**
	 * Parses a document name from a Toolbox text file:
	 * Reads a line from a specific offset, drops the \id
	 * marker and return a trimmed {@link String}.
	 * 
	 * @param corpusFile 
	 * @param string 
	 * @param idOffset 
	 *
	 * @return the document name
	 */
	static String parseId(Long offset, String idMarker, File file, boolean normalizeDocNames) {
		String documentName = null;
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			raf.seek(offset);
			String rawLine = raf.readLine();
			String[] markerAndLine = rawLine.split("\\\\" + idMarker);
			if (markerAndLine.length == 2) {
				documentName = markerAndLine[1].trim();
				if (normalizeDocNames) {
					String d1 = documentName.replaceAll(" ", "-");
					String d2 = d1.replaceAll("\\.", "_");
					String d3 = d2.replaceAll("\\n", "_");
					String d4 = d3.replaceAll(":", "_");
					String d5 = d4.replaceAll(",", "_");
					String d6 = d5.replaceAll("-", "_");
					String d7 = d6.replaceAll(Pattern.quote("("), "");
					String d8 = d7.replaceAll(Pattern.quote(")"), "");
					documentName = d8;
				}
			}
			else {
				String defaultName = "Document at offset " + offset.intValue();
				logger.info("The \\id marked line at offset " + offset + " does either not contain any contents, runs on over more than one line, or could not be parsed into a valid document name. Falling back to default name: " + defaultName + "!");
				documentName = defaultName;
			}
		}
		catch (FileNotFoundException e) {
			throw new PepperModuleException("Could not read the file " + file.getAbsolutePath() + "!", e);
		}
		catch (IOException e) {
			throw new PepperModuleException("Failed to jump to offset " + offset + " in file " + file.getAbsolutePath() + "!", e);
		}
		return documentName;
	}

}
