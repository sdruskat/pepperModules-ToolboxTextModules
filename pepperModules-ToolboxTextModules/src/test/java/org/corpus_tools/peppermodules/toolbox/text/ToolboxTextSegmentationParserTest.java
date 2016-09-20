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

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ToolboxTextSegmentationParser}.
 *
 * @author Stephan Druskat
 *
 */
public class ToolboxTextSegmentationParserTest {
	
	private ToolboxTextSegmentationParser fixture = null;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		File file = new File(this.getClass().getClassLoader().getResource("ids.txt").getFile());
		ToolboxTextSegmentationParser parser = new ToolboxTextSegmentationParser(file, "id", "ref");
		setFixture(parser);
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextSegmentationParser#parse()}.
	 */
	@Test
	public void testParse() {
		getFixture().parse();
		assertNotNull(getFixture().getIdOffsets());
		assertNotNull(getFixture().getRefMap());
		assertArrayEquals(new Long[] {32L, 117L, 202L}, getFixture().getIdOffsets().toArray(new Long[getFixture().getIdOffsets().size()]));
		assertArrayEquals(new Long[] {61L, 89L}, getFixture().getRefMap().get(32L).toArray(new Long[2]));
		assertArrayEquals(new Long[] {146L, 174L}, getFixture().getRefMap().get(117L).toArray(new Long[2]));
		assertArrayEquals(new Long[] {231L, 259L}, getFixture().getRefMap().get(202L).toArray(new Long[2]));
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextSegmentationParser#parse()}.
	 */
	@Test
	public void testParseDocWithoutIds() {
		File file = new File(this.getClass().getClassLoader().getResource("no-ids.txt").getFile());
		ToolboxTextSegmentationParser parser = new ToolboxTextSegmentationParser(file, "id", "ref");
		setFixture(parser);
		getFixture().parse();
		assertNotNull(getFixture().getIdOffsets());
		assertNotNull(getFixture().getRefMap());
		assertEquals(0, getFixture().getIdOffsets().size());
		assertArrayEquals(new Long[] {32L, 60L, 88L, 116L, 144L, 172L}, getFixture().getRefMap().get(-1L).toArray(new Long[6]));
	}

	/**
	 * @return the fixture
	 */
	private ToolboxTextSegmentationParser getFixture() {
		return fixture;
	}

	/**
	 * @param fixture the fixture to set
	 */
	private void setFixture(ToolboxTextSegmentationParser fixture) {
		this.fixture = fixture;
	}

}
