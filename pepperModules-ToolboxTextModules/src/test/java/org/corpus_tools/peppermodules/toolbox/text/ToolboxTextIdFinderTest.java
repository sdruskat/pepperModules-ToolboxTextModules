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

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ToolboxTextIdFinder}.
 *
 * @author Stephan Druskat
 *
 */
public class ToolboxTextIdFinderTest {
	
	private ToolboxTextIdFinder fixture = null;
	private File corpusFile;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		corpusFile = new File(this.getClass().getClassLoader().getResource("ids.txt").getFile());
		String idMarker = "id";
		ToolboxTextIdFinder finder = new ToolboxTextIdFinder(corpusFile, idMarker);
		setFixture(finder);
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextIdFinder#parse()}.
	 */
	@Test
	public void testParse() {
		Map<String, Long> map = getFixture().parse();
		assertEquals(3, map.entrySet().size());
		assertTrue(map.containsKey("ID1"));
		assertTrue(map.containsKey("ID2"));
		assertTrue(map.containsKey("ID3"));
		assertEquals(Long.valueOf("32"), Long.valueOf(map.get("ID1")));
		assertEquals(Long.valueOf("117"), Long.valueOf(map.get("ID2")));
		assertEquals(Long.valueOf("202"), Long.valueOf(map.get("ID3")));
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextIdFinder#getResourceHeader()}.
	 */
	@Test
	public void testGetResourceHeader() {
		getFixture().parse();
		assertEquals(URI.createFileURI(corpusFile.getAbsolutePath()), getFixture().getResourceHeader().getResource());
	}

	/**
	 * @return the fixture
	 */
	private ToolboxTextIdFinder getFixture() {
		return fixture;
	}

	/**
	 * @param fixture the fixture to set
	 */
	private void setFixture(ToolboxTextIdFinder fixture) {
		this.fixture = fixture;
	}

}
