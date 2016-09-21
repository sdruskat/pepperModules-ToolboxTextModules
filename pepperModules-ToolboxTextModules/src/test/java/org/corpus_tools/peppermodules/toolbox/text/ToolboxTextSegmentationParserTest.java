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
	 * 
	 * Tests against a default minimum example, where a Toolbox file has a header, and 3 \ids with 2 \refs each.
	 */
	@Test
	public void testParse() {
		getFixture().parse();
		assertNotNull(getFixture().getIdOffsets());
		assertNotNull(getFixture().getRefMap());
		assertEquals(3, getFixture().getIdOffsets().size());
		assertArrayEquals(new Long[] {32L, 117L, 202L}, getFixture().getIdOffsets().toArray(new Long[getFixture().getIdOffsets().size()]));
		assertArrayEquals(new Long[] {61L, 89L}, getFixture().getRefMap().get(32L).toArray(new Long[2]));
		assertArrayEquals(new Long[] {146L, 174L}, getFixture().getRefMap().get(117L).toArray(new Long[2]));
		assertArrayEquals(new Long[] {231L, 259L}, getFixture().getRefMap().get(202L).toArray(new Long[2]));
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextSegmentationParser#parse()}.
	 * 
	 * Tests against a minimum example, where there are no \ids and 6 \refs.
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
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextSegmentationParser#parse()}.
	 * 
	 * Tests against a minimum example, where there is only 1 \id containing 6 \refs.
	 */
	@Test
	public void testParseDocWithOneId() {
		File file = new File(this.getClass().getClassLoader().getResource("one-id.txt").getFile());
		ToolboxTextSegmentationParser parser = new ToolboxTextSegmentationParser(file, "id", "ref");
		setFixture(parser);
		getFixture().parse();
		assertNotNull(getFixture().getIdOffsets());
		assertNotNull(getFixture().getRefMap());
		assertEquals(1, getFixture().getIdOffsets().size());
		assertArrayEquals(new Long[] {61L, 89L, 117L, 145L, 173L, 201L}, getFixture().getRefMap().get(32L).toArray(new Long[6]));
	}
	
	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextSegmentationParser#parse()}.
	 * 
	 * Tests against a minimum example, where there are 2 \refs before the first \id (i.e., sentences belonging to no
	 * document), and 1 \id in the middle of the file without any \refs before the next \id (i.e., an empty document), 
	 * and 1 \id at the very end of the file without any following \refs (ie., another empty document).
	 */
	@Test
	public void testParseDocWithIdAndRefOrphans() {
		File file = new File(this.getClass().getClassLoader().getResource("orphan-ids-and-refs.txt").getFile());
		ToolboxTextSegmentationParser parser = new ToolboxTextSegmentationParser(file, "id", "ref");
		setFixture(parser);
		getFixture().parse();
		assertNotNull(getFixture().getIdOffsets());
		assertNotNull(getFixture().getRefMap());
		assertEquals(5, getFixture().getIdOffsets().size());
		assertEquals(6, getFixture().getRefMap().size());
		assertArrayEquals(new Long[] {100L, 185L, 270L, 305L, 390L}, getFixture().getIdOffsets().toArray(new Long[getFixture().getIdOffsets().size()]));
		assertArrayEquals(new Long[] {32L, 66L}, getFixture().getRefMap().get(-1L).toArray(new Long[2]));
		assertArrayEquals(new Long[] {129L, 157L}, getFixture().getRefMap().get(100L).toArray(new Long[2]));
		assertArrayEquals(new Long[] {214L, 242L}, getFixture().getRefMap().get(185L).toArray(new Long[2]));
		assertTrue(getFixture().getRefMap().get(270L).isEmpty());
		assertArrayEquals(new Long[] {334L, 362L}, getFixture().getRefMap().get(305L).toArray(new Long[2]));
		assertTrue(getFixture().getRefMap().get(390L).isEmpty());
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
