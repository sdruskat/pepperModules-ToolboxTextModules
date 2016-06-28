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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.STextualDS;
import org.eclipse.emf.common.util.URI;
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
		ToolboxTextMapper mapper = new ToolboxTextMapper();
		File file = new File(this.getClass().getClassLoader().getResource("test.txt").getFile());
		String path = file.getAbsolutePath();
		mapper.setResourceURI(URI.createFileURI(path));
		ToolboxTextImporterProperties properties = new ToolboxTextImporterProperties();
		mapper.setProperties(properties);
		SDocument doc = SaltFactory.createSDocument();
		mapper.setDocument(doc);
		setFixture(mapper);
	}
	
	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextMapper#mapSDocument()}.
	 */
	@Test
	public void testMapSDocument() {
		getFixture().mapSDocument();
		assertNotNull(getFixture().getDocument().getDocumentGraph());
		SDocumentGraph graph = getFixture().getDocument().getDocumentGraph();
		assertTrue(graph == getFixture().getGraph());
		assertEquals(3, graph.getLayers().size());
		assertNotNull(graph.getLayerByName("ref"));
		assertNotNull(graph.getLayerByName("tx"));
		assertNotNull(graph.getLayerByName("mb"));
		assertEquals(3, getFixture().getDocument().getMetaAnnotations().size());
		assertNotNull(getFixture().getDocument().getMetaAnnotation("toolbox::_sh"));
		assertNotNull(getFixture().getDocument().getMetaAnnotation("toolbox::id"));
		assertNotNull(getFixture().getDocument().getMetaAnnotation("toolbox::info"));
		// Textual DSs
		assertEquals(2, graph.getTextualDSs().size());
		for (STextualDS ds : graph.getTextualDSs()) {
			assertNotNull(ds.getText());
			assertTrue(ds.getText().length() > 0);
			if (ds.getName().equals("lexical-ds")) {
				assertEquals("Wort Kompositum Wort Dreifachwort Wort Wort Wort Doppelwort Doppelwortmitfreiemdash Wort", ds.getText());
			}
			else if (ds.getName().equals("morphology-ds")) {
				assertEquals("m1 m2 -m3 m4 m5- m6= m7 m8 m9 m10 m11 -m12 m13 -m14 m15", ds.getText());
			}
			else {
				fail("TextualDS with name other than \"mb\" or \"tx\" found: " + ds.getName());
			}
		}
		// Tokens
		// Timeline
		// Annotations
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
