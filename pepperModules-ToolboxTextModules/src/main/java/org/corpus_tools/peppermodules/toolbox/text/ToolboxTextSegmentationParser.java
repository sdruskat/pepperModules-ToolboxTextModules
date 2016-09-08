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

import java.io.File;

/**
 * TODO Description
 *
 * @author Stephan Druskat
 *
 */
public class ToolboxTextSegmentationParser {

	private final File file;
	private final String idMarker;
	private final String refMarker;
	private final int idMarkerLength;
	private final int refMarkerLength;

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
		
	}

}
