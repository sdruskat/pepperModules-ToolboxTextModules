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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.corpus_tools.salt.common.SDocumentGraph;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ToolboxTextMapper}.
 *
 * @author Stephan Druskat <mail@sdruskat.net>
 *
 */
public class ToolboxTextMapperTest {
	
	private ToolboxTextMapper fixture = null;

	/**
	 * Set up the fixture.
	 *
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		setFixture(new ToolboxTextMapper());
		getFixture().setProperties(new ToolboxTextImporterProperties());
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextMapper#processRefs(java.util.Map)}.
	 */
	@Test
	public void testProcessRefs() {
		Map<String, List<String>> block = new HashMap<>();
		block.put("ref", Arrays.asList(new String[]{"Ref"}));
		block.put("tx", Arrays.asList(new String[]{"This", "is", "a", "birthday", "pony"}));
		block.put("ta", Arrays.asList(new String[]{"Dies", "ist", "ein", "Geburtstag", "Pony"}));
		block.put("mb", Arrays.asList(new String[]{"Th", "-is", "is", "a", "bi-", "rth-", "day", "po", "=ny"}));
		block.put("ma", Arrays.asList(new String[]{"TH", "-IS", "BE", "DET", "BI-", "RTH-", "TEMP", "PO", "=CLIT"}));
//		Map<String, Integer> result = getFixture().processRefs(block);
//		assertEquals(Integer.valueOf(2), result.get("This"));
//		assertEquals(Integer.valueOf(1), result.get("is"));
//		assertEquals(Integer.valueOf(1), result.get("a"));
//		assertEquals(Integer.valueOf(3), result.get("birthday"));
//		assertEquals(Integer.valueOf(2), result.get("pony"));
	}

	/**
	 * @return the fixture
	 */
	private ToolboxTextMapper getFixture() {
		return fixture;
	}

	/**
	 * @param fixture the fixture to set
	 */
	private void setFixture(ToolboxTextMapper fixture) {
		this.fixture = fixture;
	}

}
