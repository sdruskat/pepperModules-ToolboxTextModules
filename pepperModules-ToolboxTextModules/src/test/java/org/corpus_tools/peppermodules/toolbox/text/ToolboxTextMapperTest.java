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
import java.util.List;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit tests for {@link ToolboxTextMapper}.
 *
 * @author Stephan Druskat <mail@sdruskat.net>
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
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_LEX_ANNOTATION_MARKERS, "ta");
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_DOCUMENT_METADATA_MARKERS, "docmet");
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_REF_METADATA_MARKERS, "met");
		mapper.setProperties(properties);
		SDocument doc = SaltFactory.createSDocument();
		mapper.setDocument(doc);
		setFixture(mapper);
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextMapper#mapSDocument()}. Tests the setup of the document graph.
	 */
	@Test
	public void testMapSDocumentSetup() {
		getFixture().mapSDocument();
		assertNotNull(getFixture().getDocument().getDocumentGraph());
		SDocumentGraph graph = getFixture().getDocument().getDocumentGraph();
		assertTrue(graph == getFixture().getGraph());
		assertEquals(3, graph.getLayers().size());
		assertNotNull(graph.getLayerByName("ref"));
		assertNotNull(graph.getLayerByName("tx"));
		assertNotNull(graph.getLayerByName("mb"));
		assertEquals(4, getFixture().getDocument().getMetaAnnotations().size());
		assertNotNull(getFixture().getDocument().getMetaAnnotation("toolbox::_sh"));
		assertNotNull(getFixture().getDocument().getMetaAnnotation("toolbox::id"));
		assertNotNull(getFixture().getDocument().getMetaAnnotation("toolbox::info"));
		assertNotNull(getFixture().getDocument().getMetaAnnotation("toolbox::docmet"));
		assertEquals("v3.0 Test", getFixture().getDocument().getMetaAnnotation("toolbox::_sh").getValue());
		assertEquals("Some Info", getFixture().getDocument().getMetaAnnotation("toolbox::info").getValue());
		assertEquals("Some ID", getFixture().getDocument().getMetaAnnotation("toolbox::id").getValue());
		assertEquals("Some randomly put document meta annotation", getFixture().getDocument().getMetaAnnotation("toolbox::docmet").getValue());
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextMapper#mapSDocument()}. Tests the data sources in the document graph.
	 */
	@Test
	public void testMapSDocumentDS() {
		getFixture().mapSDocument();
		assertNotNull(getFixture().getDocument().getDocumentGraph());
		SDocumentGraph graph = getFixture().getDocument().getDocumentGraph();
		assertTrue(graph == getFixture().getGraph());
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
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextMapper#mapSDocument()}. Tests the data sources in the document graph.
	 */
	@Test
	public void testMapSDocumentDSSwappedDelimAttachment() {
		ToolboxTextImporterProperties properties = new ToolboxTextImporterProperties();
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_LEX_ANNOTATION_MARKERS, "ta");
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_ATTACH_DETACHED_MORPHEME_DELIMITER, "true,false");
		getFixture().setProperties(properties);
		getFixture().mapSDocument();
		assertNotNull(getFixture().getDocument().getDocumentGraph());
		SDocumentGraph graph2 = getFixture().getDocument().getDocumentGraph();
		assertTrue(graph2 == getFixture().getGraph());
		assertEquals(2, graph2.getTextualDSs().size());
		for (STextualDS ds : graph2.getTextualDSs()) {
			assertNotNull(ds.getText());
			assertTrue(ds.getText().length() > 0);
			if (ds.getName().equals("lexical-ds")) {
				assertEquals("Wort Kompositum Wort Dreifachwort Wort Wort Wort Doppelwort Doppelwortmitfreiemdash Wort", ds.getText());
			}
			else if (ds.getName().equals("morphology-ds")) {
				assertEquals("m1 m2 -m3 m4 m5- m6= m7 m8 m9 m10 m11 -m12 m13- m14 m15", ds.getText());
			}
			else {
				fail("TextualDS with name other than \"mb\" or \"tx\" found: " + ds.getName());
			}
		}
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextMapper#mapSDocument()}. Tests the tokens in the document graph.
	 */
	@Test
	public void testMapSDocumentTokens() {
		getFixture().mapSDocument();
		assertNotNull(getFixture().getDocument().getDocumentGraph());
		SDocumentGraph graph = getFixture().getDocument().getDocumentGraph();
		assertTrue(graph == getFixture().getGraph());
		List<SLayer> lexLayers = graph.getLayerByName("tx");
		List<SLayer> morphLayers = graph.getLayerByName("mb");
		assertEquals(1, lexLayers.size());
		assertEquals(1, morphLayers.size());
		List<SToken> lexTokens = new ArrayList<>();
		List<SToken> morphTokens = new ArrayList<>();
		for (SNode node : lexLayers.get(0).getNodes()) {
			if (node instanceof SToken) {
				lexTokens.add((SToken) node);
			}
		}
		for (SNode node : morphLayers.get(0).getNodes()) {
			if (node instanceof SToken) {
				morphTokens.add((SToken) node);
			}
		}
		String[] lexTokenTestSet = new String[] { "Wort", "Kompositum", "Wort", "Dreifachwort", "Wort", "Wort", "Wort", "Doppelwort", "Doppelwortmitfreiemdash", "Wort" };
		List<SToken> sortedLexTokens = graph.getSortedTokenByText(lexTokens);
		assertEquals(10, sortedLexTokens.size());
		for (int i = 0; i < lexTokenTestSet.length; i++) {
			assertEquals(lexTokenTestSet[i], graph.getText(sortedLexTokens.get(i)));
		}
		String[] morphTokenTestSet = new String[] { "m1", "m2", "-m3", "m4", "m5-", "m6=", "m7", "m8", "m9", "m10", "m11", "-m12", "m13", "-m14", "m15" };
		List<SToken> sortedMorphTokens = graph.getSortedTokenByText(morphTokens);
		assertEquals(15, sortedMorphTokens.size());
		for (int i = 0; i < morphTokenTestSet.length; i++) {
			assertEquals(morphTokenTestSet[i], graph.getText(sortedMorphTokens.get(i)));
		}
	}
	
	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextMapper#mapSDocument()}. Tests the tokens in the document graph.
	 */
	@Test
	public void testMapSDocumentTokensWithSwappedDelimAttachment() {
		ToolboxTextImporterProperties properties = new ToolboxTextImporterProperties();
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_LEX_ANNOTATION_MARKERS, "ta");
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_ATTACH_DETACHED_MORPHEME_DELIMITER, "true,false");
		getFixture().setProperties(properties);
		getFixture().mapSDocument();
		assertNotNull(getFixture().getDocument().getDocumentGraph());
		SDocumentGraph graph = getFixture().getDocument().getDocumentGraph();
		assertTrue(graph == getFixture().getGraph());
		List<SLayer> lexLayers = graph.getLayerByName("tx");
		List<SLayer> morphLayers = graph.getLayerByName("mb");
		assertEquals(1, lexLayers.size());
		assertEquals(1, morphLayers.size());
		List<SToken> lexTokens = new ArrayList<>();
		List<SToken> morphTokens = new ArrayList<>();
		for (SNode node : lexLayers.get(0).getNodes()) {
			if (node instanceof SToken) {
				lexTokens.add((SToken) node);
			}
		}
		for (SNode node : morphLayers.get(0).getNodes()) {
			if (node instanceof SToken) {
				morphTokens.add((SToken) node);
			}
		}
		String[] lexTokenTestSet = new String[] { "Wort", "Kompositum", "Wort", "Dreifachwort", "Wort", "Wort", "Wort", "Doppelwort", "Doppelwortmitfreiemdash", "Wort" };
		List<SToken> sortedLexTokens = graph.getSortedTokenByText(lexTokens);
		assertEquals(10, sortedLexTokens.size());
		for (int i = 0; i < lexTokenTestSet.length; i++) {
			assertEquals(lexTokenTestSet[i], graph.getText(sortedLexTokens.get(i)));
		}
		String[] morphTokenTestSet = new String[] { "m1", "m2", "-m3", "m4", "m5-", "m6=", "m7", "m8", "m9", "m10", "m11", "-m12", "m13-", "m14", "m15" };
		List<SToken> sortedMorphTokens = graph.getSortedTokenByText(morphTokens);
		assertEquals(15, sortedMorphTokens.size());
		for (int i = 0; i < morphTokenTestSet.length; i++) {
			assertEquals(morphTokenTestSet[i], graph.getText(sortedMorphTokens.get(i)));
		}
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextMapper#mapSDocument()}. Tests the annotations in the document graph.
	 */
	@Test
	public void testMapSDocumentAnnotations() {
		getFixture().mapSDocument();
		assertNotNull(getFixture().getDocument().getDocumentGraph());
		SDocumentGraph graph = getFixture().getDocument().getDocumentGraph();
		assertTrue(graph == getFixture().getGraph());
		List<SLayer> refLayers = graph.getLayerByName("ref");
		assertEquals(1, refLayers.size());
		List<SLayer> lexLayers = graph.getLayerByName("tx");
		List<SLayer> morphLayers = graph.getLayerByName("mb");
		assertEquals(1, lexLayers.size());
		assertEquals(1, morphLayers.size());
		List<String[]> lexAnnoMap = new ArrayList<>(10);
		List<String[]> morphAnnoMap = new ArrayList<>(15);
		List<SToken> lexTokens = new ArrayList<>();
		List<SToken> morphTokens = new ArrayList<>();
		for (SNode node : lexLayers.get(0).getNodes()) {
			if (node instanceof SToken) {
				lexTokens.add((SToken) node);
			}
		}
		for (SNode node : morphLayers.get(0).getNodes()) {
			if (node instanceof SToken) {
				morphTokens.add((SToken) node);
			}
		}
		// Lex annotations
		lexAnnoMap.add(new String[]{"Wort", "Eins"});
		lexAnnoMap.add(new String[]{"Kompositum", "Zwei"});
		lexAnnoMap.add(new String[]{"Wort", "Drei"});
		lexAnnoMap.add(new String[]{"Dreifachwort", "Vier"});
		lexAnnoMap.add(new String[]{"Wort", "Fünf"});
		lexAnnoMap.add(new String[]{"Wort", "Sechs"});
		lexAnnoMap.add(new String[]{"Wort", "Sieben"});
		lexAnnoMap.add(new String[]{"Doppelwort", "Acht"});
		lexAnnoMap.add(new String[]{"Doppelwortmitfreiemdash", "Neun"});
		lexAnnoMap.add(new String[]{"Wort", "Zehn"});
		List<SToken> sortedLexTokens = graph.getSortedTokenByText(lexTokens);
		assertEquals(10, sortedLexTokens.size());
		for (int i = 0; i < sortedLexTokens.size(); i++) {
			SToken token = sortedLexTokens.get(i);
			String tokenText = graph.getText(token);
			String annoText = (String) token.getAnnotation("toolbox", "ta").getValue();
			assertEquals(lexAnnoMap.get(i)[0], tokenText);
			assertEquals(lexAnnoMap.get(i)[1], annoText);
		}
			
		// Morph annotations
		morphAnnoMap.add(new String[]{"m1", "M1"});
		morphAnnoMap.add(new String[]{"m2", "M2"});
		morphAnnoMap.add(new String[]{"-m3", "M3"});
		morphAnnoMap.add(new String[]{"m4", "M4"});
		morphAnnoMap.add(new String[]{"m5-", "M5"});
		morphAnnoMap.add(new String[]{"m6=", "M6"});
		morphAnnoMap.add(new String[]{"m7", "M7"});
		morphAnnoMap.add(new String[]{"m8", "M8"});
		morphAnnoMap.add(new String[]{"m9", "M9"});
		morphAnnoMap.add(new String[]{"m10", "M10"});
		morphAnnoMap.add(new String[]{"m11", "M11"});
		morphAnnoMap.add(new String[]{"-m12", "M12"});
		morphAnnoMap.add(new String[]{"m13", "M13"});
		morphAnnoMap.add(new String[]{"-m14", "-M14"}); // From "-" + "m14"
		morphAnnoMap.add(new String[]{"m15", "M15"});
		List<SToken> sortedMorphTokens = graph.getSortedTokenByText(morphTokens);
		assertEquals(15, sortedMorphTokens.size());
		for (int i = 0; i < sortedMorphTokens.size(); i++) {
			SToken token = sortedMorphTokens.get(i);
			String tokenText = graph.getText(token);
			String annoText = (String) token.getAnnotation("toolbox", "ge").getValue();
			assertEquals(morphAnnoMap.get(i)[0], tokenText);
			assertEquals(morphAnnoMap.get(i)[1], annoText);
		}
		// Annotations on refs are already tested above, test only span meta refs here
	}
	
	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextMapper#mapSDocument()}. Tests the annotations in the document graph.
	 */
	@Test
	public void testMapSDocumentAnnotationsWithSwappedDelimAttachment() {
		ToolboxTextImporterProperties properties = new ToolboxTextImporterProperties();
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_LEX_ANNOTATION_MARKERS, "ta");
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_ATTACH_DETACHED_MORPHEME_DELIMITER, "true,false");
		getFixture().setProperties(properties);
		getFixture().mapSDocument();
		SDocumentGraph graph = getFixture().getDocument().getDocumentGraph();
		assertTrue(graph == getFixture().getGraph());
		List<SLayer> morphLayers = graph.getLayerByName("mb");
		assertEquals(1, morphLayers.size());
		List<String[]> morphAnnoMap = new ArrayList<>(15);
		List<SToken> morphTokens = new ArrayList<>();
		for (SNode node : morphLayers.get(0).getNodes()) {
			if (node instanceof SToken) {
				morphTokens.add((SToken) node);
			}
		}
		// Morph annotations
		morphAnnoMap.add(new String[]{"m1", "M1"});
		morphAnnoMap.add(new String[]{"m2", "M2"});
		morphAnnoMap.add(new String[]{"-m3", "M3"});
		morphAnnoMap.add(new String[]{"m4", "M4"});
		morphAnnoMap.add(new String[]{"m5-", "M5"});
		morphAnnoMap.add(new String[]{"m6=", "M6"});
		morphAnnoMap.add(new String[]{"m7", "M7"});
		morphAnnoMap.add(new String[]{"m8", "M8"});
		morphAnnoMap.add(new String[]{"m9", "M9"});
		morphAnnoMap.add(new String[]{"m10", "M10"});
		morphAnnoMap.add(new String[]{"m11", "M11"});
		morphAnnoMap.add(new String[]{"-m12", "M12"});
		morphAnnoMap.add(new String[]{"m13-", "M13-"});
		morphAnnoMap.add(new String[]{"m14", "M14"}); // From "-" + "m14"
		morphAnnoMap.add(new String[]{"m15", "M15"});
		List<SToken> sortedMorphTokens = graph.getSortedTokenByText(morphTokens);
		assertEquals(15, sortedMorphTokens.size());
		for (int i = 0; i < sortedMorphTokens.size(); i++) {
			SToken token = sortedMorphTokens.get(i);
			String tokenText = graph.getText(token);
			String annoText = (String) token.getAnnotation("toolbox", "ge").getValue();
			assertEquals(morphAnnoMap.get(i)[0], tokenText);
			assertEquals(morphAnnoMap.get(i)[1], annoText);
		}
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextMapper#mapSDocument()}. Tests the annotations in the document graph.
	 */
	@Test
	public void testMapSDocumentTimeline() {
		getFixture().mapSDocument();
		SDocumentGraph graph = getFixture().getDocument().getDocumentGraph();
		STimeline timeline = getFixture().getDocument().getDocumentGraph().getTimeline();
		assertNotNull(timeline);
		assertNotNull(timeline.getEnd());
		assertEquals(Integer.valueOf(16), timeline.getEnd());
		List<SToken> lexTokens = new ArrayList<>();
		List<SToken> morphTokens = new ArrayList<>();
		List<SLayer> lexLayers = graph.getLayerByName("tx");
		List<SLayer> morphLayers = graph.getLayerByName("mb");
		int[] lexTokensNoOfMorphs = new int[] {1, 2, 1, 3, 1, 1, 1, 2, 2, 1};
		for (SNode node : lexLayers.get(0).getNodes()) {
			if (node instanceof SToken) {
				lexTokens.add((SToken) node);
			}
		}
		for (SNode node : morphLayers.get(0).getNodes()) {
			if (node instanceof SToken) {
				morphTokens.add((SToken) node);
			}
		}
		List<SToken> sortedLexTokens = graph.getSortedTokenByText(lexTokens);
		for (int i = 0; i < sortedLexTokens.size(); i++) {
			SToken lexToken = sortedLexTokens.get(i);
			for (SRelation<?, ?> inRel : timeline.getInRelations()) {
				if (inRel.getSource() == lexToken && inRel.getTarget() == timeline) {
					STimelineRelation rel = (STimelineRelation) inRel;
					int length = lexTokensNoOfMorphs[i];
					assertEquals(length, rel.getEnd() - rel.getStart());
					System.out.println("L" + i + ": " + rel.getStart() + "-" + rel.getEnd());
				}
			}	
		}
		for (SToken morphToken : morphTokens) {
			for (SRelation<?, ?> inRel : timeline.getInRelations()) {
				if (inRel.getSource() == morphToken) {
					STimelineRelation rel = (STimelineRelation) inRel;
					assertEquals(1, rel.getEnd() - rel.getStart());
					System.out.println("M: " + rel.getStart() + "-" + rel.getEnd());
				}
			}
		}
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextMapper#mapSDocument()}. Tests the ref spans in the document graph.
	 */
	@Test
	public void testMapSDocumentRefSpans() {
		getFixture().mapSDocument();
		SDocumentGraph graph = getFixture().getDocument().getDocumentGraph();
		assertEquals(3, graph.getSpans().size());
		for (SSpan span : graph.getSpans()) {
			if (span.getName().equals("First sentence")) {
				assertEquals(3, graph.getOverlappedTokens(span).size());
				assertEquals(2, span.getAnnotations().size());
				assertEquals("This is a reference level annotation!", span.getAnnotation("toolbox::ll").getValue().toString());
				assertEquals("First sentence", span.getAnnotation("toolbox::ref").getValue().toString());
			}
			else if (span.getName().equals("Second sentence")) {
				assertEquals(5, graph.getOverlappedTokens(span).size());
				assertEquals(2, span.getAnnotations().size());
				assertEquals("This is yet another reference level annotation!", span.getAnnotation("toolbox::ll").getValue().toString());
				assertEquals("Second sentence", span.getAnnotation("toolbox::ref").getValue().toString());
			}
			else if (span.getName().equals("Third sentence")) {
				assertEquals(2, graph.getOverlappedTokens(span).size());
				assertEquals(2, span.getAnnotations().size());
				assertEquals("This is a third reference level annotation!", span.getAnnotation("toolbox::ll").getValue().toString());
				assertEquals("Third sentence", span.getAnnotation("toolbox::ref").getValue().toString());
			}
			else {
				fail("Found a ref that shouldn't be in the span list: " + span.getName());
			}
		}
	}
	
	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextMapper#mapSDocument()}. Tests the ref spans in the document graph.
	 */
	@Test
	public void testMapSDocumentRefSpansOverMorphs() {
		ToolboxTextImporterProperties properties = new ToolboxTextImporterProperties();
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_LEX_ANNOTATION_MARKERS, "ta");
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_MAP_REF_ANNOTATIONS_TO_LEXICAL_LAYER, "false");
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_REF_METADATA_MARKERS, "met");
		properties.setPropertyValue(ToolboxTextImporterProperties.PROP_DOCUMENT_METADATA_MARKERS, "docmet");
		getFixture().setProperties(properties);
		getFixture().mapSDocument();
		SDocumentGraph graph = getFixture().getDocument().getDocumentGraph();
		assertEquals(3, graph.getSpans().size());
		for (SSpan span : graph.getSpans()) {
			if (span.getName().equals("First sentence")) {
				assertEquals(4, graph.getOverlappedTokens(span).size());
				assertEquals(2, span.getAnnotations().size());
				assertEquals("This is a reference level annotation!", span.getAnnotation("toolbox::ll").getValue().toString());
				assertEquals("First sentence", span.getAnnotation("toolbox::ref").getValue().toString());
			}
			else if (span.getName().equals("Second sentence")) {
				assertEquals(8, graph.getOverlappedTokens(span).size());
				assertEquals(2, span.getAnnotations().size());
				assertEquals("This is yet another reference level annotation!", span.getAnnotation("toolbox::ll").getValue().toString());
				assertEquals("Second sentence", span.getAnnotation("toolbox::ref").getValue().toString());
			}
			else if (span.getName().equals("Third sentence")) {
				assertEquals(3, graph.getOverlappedTokens(span).size());
				assertEquals(2, span.getAnnotations().size());
				assertEquals("This is a third reference level annotation!", span.getAnnotation("toolbox::ll").getValue().toString());
				assertEquals("Third sentence", span.getAnnotation("toolbox::ref").getValue().toString());
			}
			else {
				fail("Found a ref that shouldn't be in the span list: " + span.getName());
			}
		}
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextMapper#mapSDocument()}. Tests the meta annotations in the document graph.
	 */
	@Test
	public void testMapSDocumentMetaAnnotations() {
		getFixture().mapSDocument();
		SDocumentGraph graph = getFixture().getDocument().getDocumentGraph();
		for (SSpan span : graph.getSpans()) {
			if (span.getName().equals("First sentence")) {
				assertEquals(1, span.getMetaAnnotations().size());
				assertEquals("Some meta information about the first sentence", span.getMetaAnnotation("toolbox::met").getValue().toString());
			}
			else if (span.getName().equals("Second sentence")) {
				assertEquals(1, span.getMetaAnnotations().size());
				assertEquals("Some meta info about the second sentence", span.getMetaAnnotation("toolbox::met").getValue().toString());
			}
			else if (span.getName().equals("Third sentence")) {
				assertEquals(1, span.getMetaAnnotations().size());
				assertEquals("Some meta info about the third sentence", span.getMetaAnnotation("toolbox::met").getValue().toString());
			}
			else {
				fail("Found a ref that shouldn't be in the span list: " + span.getName());
			}
		}
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
