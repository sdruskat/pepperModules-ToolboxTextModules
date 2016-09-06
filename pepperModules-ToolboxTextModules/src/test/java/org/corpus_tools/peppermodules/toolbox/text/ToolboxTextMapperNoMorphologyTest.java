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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SNode;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests testing the ID handling of {@link ToolboxTextMapper}.
 *
 * @author Stephan Druskat <mail@sdruskat.net>
 *
 */
public class ToolboxTextMapperNoMorphologyTest {
	
	private MonolithicToolboxTextMapper fixture = null;

	/**
	 * Set up the fixture.
	 *
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		MonolithicToolboxTextMapper mapper = new MonolithicToolboxTextMapper();
		File file = new File(this.getClass().getClassLoader().getResource("no-morphology.txt").getFile());
		String path = file.getAbsolutePath();
		mapper.setResourceURI(URI.createFileURI(path));
		ToolboxTextImporterProperties properties = new ToolboxTextImporterProperties();
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_FIX_ALIGNMENT, true);
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_CONTAINS_MORPHOLOGY, false);
		mapper.setProperties(properties);
		SDocument doc = SaltFactory.createSDocument();
		mapper.setDocument(doc);
		setFixture(mapper);
	}

	@Test
	public void testNoMorphology() {
		getFixture().mapSDocument();
		SDocumentGraph graph = getFixture().getGraph();
		SLayer lexLayer = graph.getLayerByName(getFixture().getProperties().getLexMarker()).get(0);
		List<SToken> tokenList = new ArrayList<>();
		for (SNode node : lexLayer.getNodes()) {
			if (node instanceof SToken) {
				tokenList.add((SToken) node);
			}
		}
		SLayer idLayer = graph.getLayerByName("toolbox-text-importer").get(0);
		List<SSpan> idSpans = new ArrayList<>();
		for (SNode node : idLayer.getNodes()) {
			if (node instanceof SSpan) {
				idSpans.add((SSpan) node);
			}
		}
		assertEquals(6, graph.getTokens().size());
		assertEquals(1, graph.getTextualDSs().size());
		for (SSpan span : idSpans) {
			if (span.getName().equals("ID1")) {
				List<String> overlappedTokens = new ArrayList<>(Arrays.asList("Wort1", "Wort2"));
				List<SToken> sortedTokens = graph.getSortedTokenByText(graph.getOverlappedTokens(span));
				assertEquals(2, sortedTokens.size());
				for (int i = 0; i < graph.getOverlappedTokens(span).size(); i++) {
					assertEquals(overlappedTokens.get(i), graph.getText(sortedTokens.get(i)));
				}
				assertEquals("Info on ID1", span.getAnnotation("toolbox::idinfo").getValue());
			}
			else if (span.getName().equals("ID2")) {
				List<String> overlappedTokens = new ArrayList<>(Arrays.asList("Wort3", "Wort4"));
				List<SToken> sortedTokens = graph.getSortedTokenByText(graph.getOverlappedTokens(span));
				assertEquals(2, sortedTokens.size());
				for (int i = 0; i < graph.getOverlappedTokens(span).size(); i++) {
					assertEquals(overlappedTokens.get(i), graph.getText(sortedTokens.get(i)));
				}
				assertEquals("Info on ID2", span.getAnnotation("toolbox::idinfo").getValue());
			}
			else if (span.getName().equals("ID3")) {
				List<String> overlappedTokens = new ArrayList<>(Arrays.asList("Wort5", "Wort6"));
				List<SToken> sortedTokens = graph.getSortedTokenByText(graph.getOverlappedTokens(span));
				assertEquals(2, sortedTokens.size());
				for (int i = 0; i < graph.getOverlappedTokens(span).size(); i++) {
					assertEquals(overlappedTokens.get(i), graph.getText(sortedTokens.get(i)));
				}
				assertEquals("Info on ID3", span.getAnnotation("toolbox::idinfo").getValue());
			}
		}
	}
	
	@Test(expected=PepperModuleException.class)
	public void testContainsNoMorph() {
		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_CONTAINS_MORPHOLOGY, true);
		getFixture().mapSDocument();
	}
	
	@Test
	public void testMapSDocumentTokens() {
		getFixture().mapSDocument();
		assertNotNull(getFixture().getDocument().getDocumentGraph());
		SDocumentGraph graph = getFixture().getDocument().getDocumentGraph();
		assertTrue(graph == getFixture().getGraph());
		List<SLayer> lexLayers = graph.getLayerByName("tx");
		assertEquals(1, lexLayers.size());
		List<SToken> lexTokens = new ArrayList<>();
		for (SNode node : lexLayers.get(0).getNodes()) {
			if (node instanceof SToken) {
				lexTokens.add((SToken) node);
			}
		}
		String[] lexTokenTestSet = new String[] { "Wort1", "Wort2", "Wort3", "Wort4", "Wort5", "Wort6"};
		List<SToken> sortedLexTokens = graph.getSortedTokenByText(lexTokens);
		assertEquals(6, sortedLexTokens.size());
		for (int i = 0; i < lexTokenTestSet.length; i++) {
			assertEquals(lexTokenTestSet[i], graph.getText(sortedLexTokens.get(i)));
		}
	}
	
	@Test
	public void testMeta() {
		getFixture().mapSDocument();
		assertEquals("Some info", getFixture().getDocument().getMetaAnnotation("toolbox::info").getValue());
	}

	/**
	 * @return the fixture
	 */
	private MonolithicToolboxTextMapper getFixture() {
		return fixture;
	}

	/**
	 * @param fixture the fixture to set
	 */
	private void setFixture(MonolithicToolboxTextMapper fixture) {
		this.fixture = fixture;
	}

}
