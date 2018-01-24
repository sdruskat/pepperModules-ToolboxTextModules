/**
 * Copyright 2016ff. Humboldt-Universität zu Berlin.
 * Exploitation rights belong exclusively to Humboldt-Universität zu Berlin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Stephan Druskat (toolboxtextmodules@sdruskat.net) - initial API and implementation
 */
package org.corpus_tools.peppermodules.toolbox.text;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.junit.Test;

public class ToolboxTextDocumentNameParserTest {
	
	/**
	 * // TODO Add description
	 * 
	 */
	@Test(expected = PepperModuleException.class)
	public final void testParseIdFailNoFile() {
		ToolboxTextDocumentNameParser.parseId(1L, "", new File(""), false);
	}

	/**
	 * // TODO Add description
	 * 
	 */
	@Test(expected = PepperModuleException.class)
	public final void testParseIdFailIO() {
		File file = new File(this.getClass().getClassLoader().getResource("exporter/saltProject.salt").getFile());
		ToolboxTextDocumentNameParser.parseId(-10L, "", file, false);
	}
	
	/**
	 * // TODO Add description
	 * 
	 */
	@Test
	public final void testParseIdDefaultName() {
		File file = new File(this.getClass().getClassLoader().getResource("importer/no-id-name.txt").getFile());
		String name = ToolboxTextDocumentNameParser.parseId(15L, "", file, false);
		assertThat(name, is("Document at offset 15"));
	}

}
