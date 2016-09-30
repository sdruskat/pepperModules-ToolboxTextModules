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

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

/**
 * Unit tests for {@link ToolboxTextDocumentNameParser}.
 *
 * @author Stephan Druskat
 *
 */
public class ToolboxTextDocumentNameParserTest {

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextDocumentNameParser#parseId(java.lang.Long, java.lang.String, java.io.File)}.
	 */
	@Test
	public void testParseExistingID() {
		File file = getFile("ids.txt");
		assertEquals("ID1", ToolboxTextDocumentNameParser.parseId(32L, "id", file));
		assertEquals("ID3", ToolboxTextDocumentNameParser.parseId(202L, "id", file));
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextDocumentNameParser#parseId(java.lang.Long, java.lang.String, java.io.File)}.
	 */
	@Test
	public void testParseFaultyID() {
		File file = getFile("faulty-id-name.txt");
		assertEquals("Document at offset 32", ToolboxTextDocumentNameParser.parseId(32L, "id", file));
		assertEquals("ID3", ToolboxTextDocumentNameParser.parseId(198L, "id", file));
	}
	
	private File getFile(String fileName) {
		return new File(this.getClass().getClassLoader().getResource(fileName).getFile());
	}

}
