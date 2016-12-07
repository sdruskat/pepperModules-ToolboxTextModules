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

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.corpus_tools.pepper.common.CorpusDesc;
import org.corpus_tools.pepper.common.FormatDesc;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.pepper.testFramework.PepperImporterTest;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.common.SaltProject;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SMetaAnnotation;
import org.corpus_tools.salt.core.SNode;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ToolboxTextImporter}.
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 *
 */
public class ToolboxTextImporterTest extends PepperImporterTest {

	private static final String DOC_INFO_ANNO = "A sample \"standard\" corpus in Toolbox text format. It includes use cases for most phenomena the importer tests against, such as clitics and affixes, unitrefs, meta annotations, etc.";
	private static final String DOC_NO = "Document no. ";
	private static final String TOOLBOX = "toolbox";

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.setFixture(new ToolboxTextImporter());
		this.supportedFormatsCheck.add(new FormatDesc().setFormatName("toolbox-text").setFormatVersion("3.0"));
		this.getFixture().getCorpusDesc().getFormatDesc().setFormatName("toolbox-text").setFormatVersion("3.0");
		getFixture().getSaltProject().createCorpusGraph();
		getFixture().setProperties(new ToolboxTextImporterProperties());
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#createPepperMapper(org.corpus_tools.salt.graph.Identifier)}.
	 */
	@Test
	public void testCreatePepperMapper() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there are 0 \ids and 0 \refs,
	 * i.e., an empty corpus.
	 */
	@Test(expected=PepperModuleException.class)
	public void testParseQuasiEmptyCorpus() {
		getFixture().setCorpusDesc(new CorpusDesc().setCorpusPath(URI.createFileURI(getFile("empty-corpus.txt"))));
		start();
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there are 0 \ids and n \refs,
	 * i.e., what will become a single corpus with a single document.
	 */
	@Test
	public void testParseMonolithicDocument() {
		getFixture().setCorpusDesc(new CorpusDesc().setCorpusPath(URI.createFileURI(getFile("no-ids.txt"))));
		start();
		assertEquals((Long) 32L, getFixture().getHeaderEndOffset());
		assertTrue(getFixture().isMonolithic());
		SCorpusGraph corpusGraph = getNonEmptyCorpusGraph();
		
		// Test corpora
		runCorpusTests(corpusGraph, "no-ids.txt");
		
		// Test single document
		assertEquals(1, corpusGraph.getDocuments().size());
		fail("Needs to be implemented further!");
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there are n \ids and 0 \refs,
	 * i.e., what will become a single corpus with n empty documents.
	 */
	@Test
	public void testParseDocumentWithJustIds() {
		getFixture().setCorpusDesc(new CorpusDesc().setCorpusPath(URI.createFileURI(getFile("just-ids.txt"))));
		start();
		fail("Needs to be implemented further!");
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a "standard" example, which covers most phenomena 
	 * the importer tests against, such as clitics and affixes, 
	 * unitrefs, meta annotations, etc.
	 */
	@Test
	public void testParseStandardDocument() {
		setTestFile("test.txt");
		setProperties("test.properties");
		start();
		assertEquals((Long) 246L, getFixture().getHeaderEndOffset());
		assertFalse(getFixture().isMonolithic());
		SCorpusGraph corpusGraph = getNonEmptyCorpusGraph();

		// Test corpora
		runCorpusTests(corpusGraph, "test.txt");
		
		// Test documents
		assertEquals(5, corpusGraph.getDocuments().size());
		for (SDocument doc : corpusGraph.getDocuments()) {
			assertThat(doc.getName(), anyOf(is(DOC_NO + "1"), is(DOC_NO + "2"), is(DOC_NO + "3"), is(DOC_NO + "4"), is(DOC_NO + "5")));
			assertEquals(2, doc.getMetaAnnotations().size());
			for (SAnnotation anno : doc.getAnnotations()) {
				assertThat(anno.getQName(), anyOf(is(TOOLBOX + "::idinfo"), is(TOOLBOX + "::moreidinfo")));
				if (anno.getQName().equals(TOOLBOX + "::idinfo")) {
					assertEquals("Some ".toLowerCase() + doc.getName().toLowerCase() + " info".toLowerCase(), anno.getValue_STEXT().toLowerCase());
				}
				else {
					assertEquals("Some more info about ".toLowerCase() + doc.getName().toLowerCase(), anno.getValue_STEXT().toLowerCase());
				}
			}
		}
		for (SDocument doc : corpusGraph.getDocuments()) {
			// General document tests TODO: Factor out to method?
			assertNotNull(doc.getDocumentGraph());
			SDocumentGraph graph = doc.getDocumentGraph();
			assertEquals(2, graph.getTextualDSs().size());
			// Document-respective tests
			String docNumber = doc.getId().substring(doc.getId().length() - 1);
			// FIXME switch on docNumber
			// Document-level
			assertEquals(DOC_NO + docNumber, doc.getName());
			assertNotNull(doc.getMetaAnnotation(TOOLBOX + "::idinfo"));
			assertNotNull(doc.getMetaAnnotation(TOOLBOX + "::moreidinfo"));
			switch (docNumber) {
			case "4":
				assertEquals("Some document no. " + docNumber + " info followed by an empty marker", doc.getMetaAnnotation(TOOLBOX + "::idinfo").getValue_STEXT());
				assertEquals("Some more info about document no. " + docNumber + " Duplicate 1 Duplicate 2", doc.getMetaAnnotation(TOOLBOX + "::moreidinfo").getValue_STEXT());
				break;

			default:
				assertEquals("Some document no. " + docNumber + " info", doc.getMetaAnnotation(TOOLBOX + "::idinfo").getValue_STEXT());
				assertEquals("Some more info about document no. " + docNumber, doc.getMetaAnnotation(TOOLBOX + "::moreidinfo").getValue_STEXT());
				break;
			}
			// Data sources
			for (STextualDS ds : graph.getTextualDSs()) {
				switch (docNumber) {
				case "1":
					assertThat(ds.getText(), is(anyOf(is("Word1 -Tuple Tuple- Word2 Triple-= Word3 FreedashTuple Word4 FreecliticTuple Unitref sentence one Unitref sentence one Unitref sentence two with one-to-four ref Unitref sentence two with one-to-four ref Unitref with some random text just like that Unitref with some random text just like that Unitref with some random text just like that"), 
							is("m1m2-m3m4-m5m6m7-m8=m9m10m11-m12m13m14=m15m16m17m18m19m20m21m22m23m24m25m26m27m28m29m30m31m32m33m34m35m36m37m38m39m40m41m42m43"))));
					break;

				case "2":
					assertThat(ds.getText(), is(anyOf(is("A sentence A sentence"),
							is("m1m2m1m2"))));
					break;

				case "3":
					assertThat(ds.getText(), is(anyOf(is("A sentence A sentence"),
							is("m1m2m1m2"))));
					break;

				case "4":
					assertThat(ds.getText(), is(anyOf(is("A sentence A sentence"),
							is("m1m2m1m2"))));
					break;
					
				case "5":
					assertThat(ds.getText(), is(anyOf(is("One Two Three Four"),
							is("m1m2m3m4"))));
					break;

				default:
					fail();
					break;
				}
			}
			// Tokens, spans, layers
			switch (docNumber) {
			case "1":
				assertEquals(94, graph.getTokens().size());
				assertEquals(8, graph.getSpans().size());
				assertEquals(3, graph.getLayers().size());
				break;

			case "2":
			case "3":
			case "4":
				assertEquals(8, graph.getTokens().size());
				assertEquals(2, graph.getSpans().size());
				assertEquals(3, graph.getLayers().size());
				break;

			case "5":
				assertEquals(8, graph.getTokens().size());
				assertEquals(1, graph.getSpans().size());
				assertEquals(3, graph.getLayers().size());
				break;

			default:
				fail();
				break;
			}
			// Layers
			for (SLayer l : graph.getLayers()) {
				switch (docNumber) {
				case "1":
					switch (l.getName()) {
					case "tx":
						assertEquals(51, l.getNodes().size());
						break;
						
					case "mb":
						assertEquals(43, l.getNodes().size());
						break;
						
					case "ref":
						assertEquals(8, l.getNodes().size());
						break;

					default:
						fail();
						break;
					}
					break;

				case "2":
				case "3":
				case "4":
					switch (l.getName()) {
					case "tx":
						assertEquals(4, l.getNodes().size());
						break;
						
					case "mb":
						assertEquals(4, l.getNodes().size());
						break;
						
					case "ref":
						assertEquals(2, l.getNodes().size());
						break;

					default:
						fail();
						break;
					}
					break;

				case "5":
					switch (l.getName()) {
					case "tx":
						assertEquals(4, l.getNodes().size());
						break;
						
					case "mb":
						assertEquals(4, l.getNodes().size());
						break;
						
					case "ref":
						assertEquals(1, l.getNodes().size());
						break;

					default:
						fail();
						break;
					}
					break;

				default:
					fail();
					break;
				}
			}
			// Ref-level annotations
			for (SSpan s : graph.getSpans()) {
				switch (docNumber) {
				case "1":
					assertEquals(3, s.getAnnotations().size());
					for (SAnnotation a : s.getAnnotations()) {
						assertThat(a.getName(), is(anyOf(is("ll"), is("ref"), is("met"))));
					}
					break;

				case "2":
				case "3":
				case "4":
				case "5":
					assertEquals(2, s.getAnnotations().size());
					for (SAnnotation a : s.getAnnotations()) {
						assertThat(a.getName(), is(anyOf(is("ll"), is("ref"))));
					}
					break;

				default:
					fail();
					break;
				}
			}
			// Token annotations
			String[] lexAnnos, morphAnnos;
			List<SToken> sortedLexToks = new ArrayList<>();
			List<SToken> sortedMorphToks = new ArrayList<>();
			switch (docNumber) {
			case "1":
				lexAnnos = new String[] { "WordOne", "TupleOne", "TupleTwo", "WordTwo", "Triple", "WordThree", "TupleThree", "WordFour", "TupleFour", "t11", "t12", "t13", "t14", "t15", "t16", "t17", "t18", "t19", "t20", "t21", "t22", "t23", "t24", "t25", "t26", "t27", "t28", "t29", "t30", "t31", "t32", "t33", "t34", "t35", "t36", "t37", "t38", "t39", "t40", "t41", "t42", "t43", "t44", "t45", "t46", "t47", "t48", "t49", "t50", "t51", "t52" };
				morphAnnos = new String[] { "M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", "M10", "M11", "M12", "M13", "M14", "M15", "M16", "M17", "M18", "M19", "M20", "M21", "M22", "M23", "M24", "M25", "M26", "M27", "M28", "M29", "M30", "M31", "M32", "M33", "M34", "M35", "M36", "M37", "M38", "M39", "M40", "M41", "M42", "M43" };
				for (SToken sortedTok : graph.getSortedTokenByText()) {
					if (graph.getLayerByName("tx").get(0).getNodes().contains(sortedTok)) {
						sortedLexToks.add(sortedTok);
					}
					else if (graph.getLayerByName("mb").get(0).getNodes().contains(sortedTok)) {
						sortedMorphToks.add(sortedTok);
					}
				}
				for (int j = 0; j < sortedLexToks.size(); j++) {
					assertEquals(lexAnnos[j], sortedLexToks.get(j).getAnnotation("toolbox::ta").getValue());
				}
				for (int j = 0; j < sortedMorphToks.size(); j++) {
					assertEquals(morphAnnos[j], sortedMorphToks.get(j).getAnnotation("toolbox::ge").getValue());
				}
				break;

			case "2":
			case "3":
			case "4":
				lexAnnos = new String[] { "A", "word", "A", "word"};
				morphAnnos = new String[] { "M1", "M2", "M1", "M2"};
				for (SToken sortedTok : graph.getSortedTokenByText()) {
					if (graph.getLayerByName("tx").get(0).getNodes().contains(sortedTok)) {
						sortedLexToks.add(sortedTok);
					}
					else if (graph.getLayerByName("mb").get(0).getNodes().contains(sortedTok)) {
						sortedMorphToks.add(sortedTok);
					}
				}
				for (int j = 0; j < sortedLexToks.size(); j++) {
					assertEquals(lexAnnos[j], sortedLexToks.get(j).getAnnotation("toolbox::ta").getValue());
				}
				for (int j = 0; j < sortedMorphToks.size(); j++) {
					assertEquals(morphAnnos[j], sortedMorphToks.get(j).getAnnotation("toolbox::ge").getValue());
				}
				break;

			case "5":
				lexAnnos = new String[] { "1", "2", "3", "4"};
				morphAnnos = new String[] { "M1", "M2", "M3", "M4"};
				for (SToken sortedTok : graph.getSortedTokenByText()) {
					if (graph.getLayerByName("tx").get(0).getNodes().contains(sortedTok)) {
						sortedLexToks.add(sortedTok);
					}
					else if (graph.getLayerByName("mb").get(0).getNodes().contains(sortedTok)) {
						sortedMorphToks.add(sortedTok);
					}
				}
				for (int j = 0; j < sortedLexToks.size(); j++) {
					assertEquals(lexAnnos[j], sortedLexToks.get(j).getAnnotation("toolbox::ta").getValue());
				}
				for (int j = 0; j < sortedMorphToks.size(); j++) {
					assertEquals(morphAnnos[j], sortedMorphToks.get(j).getAnnotation("toolbox::ge").getValue());
				}
				break;

			default:
				fail();
				break;
			}
			// Spanning
			switch (docNumber) {
			case "1":
				for (SSpan span : graph.getSpans()) {
					if (span.getName().equals("Reference no. 1")) {
						assertEquals(9, graph.getOverlappedTokens(span).size());
						for (SToken tok : graph.getOverlappedTokens(span)) {
//							graph.getTimeline().get
						}
					}
				}
				break;

			case "2":
			case "3":
			case "4":
				
				break;

			case "5":
				break;

			default:
				break;
			}
		}
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there are 0 \ids and 0 \refs
	 */
	@Test
	public void testParseOrphanRefs() {
		getFixture().setCorpusDesc(new CorpusDesc().setCorpusPath(URI.createFileURI(getFile("orphan-ids-and-refs.txt"))));
		start();
		fail("Needs to be implemented further!");
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there are 0 \ids, 1 standard \ref and 3 empty \refs.
	 */
	@Test
	public void testParseEmptyRefs() {
		setTestFile("empty-refs.txt");
		start();
		fail("Needs to be implemented further!");
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there are 1 \id, 2 standard \refs, 1 \ref without a \tx line and 1 \ref with an empty \tx line.
	 */
	@Test
	public void testParseNoTxLine() {
		setTestFile("no-tx-line.txt");
		start();
		fail("Needs to be implemented further!");
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there are 4 \id, one of which has a \ref **without** a morph line
	 */
	@Test
	public void testParseNoMbLine() {
		setTestFile("id-without-morph-line.txt");
		start();
		fail("Needs to be implemented further!");
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there are 3 \refs, one of which has **no** morph line
	 */
	@Test
	public void testMixedWithWithoutMbLines() {
		setTestFile("test-mixed-with-without-mb.txt");
		start();
		fail("Needs to be implemented further!");
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there are 3 \refs, one of which has **no** morph line
	 */
	@Test
	public void testRealData() {
		setTestFile("real-data.txt");
		setProperties("real-data.properties");
		start();
		assertEquals(214, getNonEmptyCorpusGraph().getDocuments().size());
		for (SDocument doc : getNonEmptyCorpusGraph().getDocuments()) {
			SDocumentGraph graph = doc.getDocumentGraph();
//			if (graph.getTokens().size() < 1) {
//				System.err.println(graph.getDocument().getName());
//			}
//			assertTrue(graph.getTokens().size() > 0);
		}
		try {
			Thread.sleep(10000);
		}
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		fail("Needs to be implemented further!");
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there are no morphology lines
	 */
	@Test
	public void testWithoutMbLines() {
		setTestFile("test-no-mb-lines.txt");
		start();
		fail("Needs to be implemented further!");
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there are duplicate lines in the refs
	 */
	@Test
	public void testDuplicateMarkers() {
		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_LEX_ANNOTATION_MARKERS, "ta");
		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_MORPH_ANNOTATION_MARKERS, "ge");
		setTestFile("duplicate-markers.txt");
		start();
		SDocumentGraph graph = getNonEmptyCorpusGraph().getDocuments().get(0).getDocumentGraph();
		assertEquals(1, graph.getDocument().getMetaAnnotations().size());
		assertEquals("Some more info about document no. 4 Duplicate 1 Duplicate 2", graph.getDocument().getMetaAnnotation("toolbox::moreidinfo").getValue_STEXT());
		assertEquals("A sentence another sentence", graph.getTextualDSs().get(0).getText());
		assertEquals("m1m2m3m4", graph.getTextualDSs().get(1).getText());
		for (SNode node : graph.getLayerByName("tx").get(0).getNodes()) {
			assertEquals(1, node.getAnnotations().size());
			assertThat(node.getAnnotation("toolbox::ta").getValue_STEXT(), anyOf(is("A"), is("word"), is("another")));
		}
		for (SNode node : graph.getLayerByName("mb").get(0).getNodes()) {
			assertEquals(1, node.getAnnotations().size());
			assertThat(node.getAnnotation("toolbox::ge").getValue_STEXT(), anyOf(is("M1"), is("M2"), is("M3"), is("M4")));
		}
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests cases where the number of annotations for lex or morph does not equal the number of tokens.
	 */
	@Test
	public void testParseAnnosUnequalTok() {
		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_LEX_ANNOTATION_MARKERS, "ta, tb");
		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_MORPH_ANNOTATION_MARKERS, "ge");
//		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_FIX_ERRORS, false);
//		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_RECORD_ERRORS, false);
		setTestFile("annos-unequal-tok.txt");
		start();
		fail("Needs to be implemented further!");
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * FIXME Tests against a minimum example, where there are 4 \id, one of which has a \ref **without** a morph line
	 */
	@Test
	public void testDelimAttachment() {
		setTestFile("delims.txt");
		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_ATTACH_DETACHED_MORPHEME_DELIMITER, "true, false");
		start();
		fail("Needs to be implemented further!");
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * FIXME Tests against a minimum example, where there are 4 \id, one of which has a \ref **without** a morph line
	 */
	@Test
	public void testMorphLexInterlinearization() {
//		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_ATTACH_DETACHED_MORPHEME_DELIMITER, "false, false");
		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_MORPH_ANNOTATION_MARKERS, "ma");
		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_LEX_ANNOTATION_MARKERS, "ta");
//		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_FIX_INTERL11N, false);
		setTestFile("interlinearization.txt");
		start();
		fail("Needs to be implemented further!");
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#getProperties()}.
	 */
	@Test
	public void testGetProperties() {
		assertTrue(getFixture().getProperties() instanceof ToolboxTextImporterProperties);
	}
	
	@Override
	public ToolboxTextImporter getFixture() {
		return (ToolboxTextImporter) super.getFixture();
	}

	/**
	 * Returns the first {@link SCorpusGraph} in the fixture's
	 * {@link SaltProject} that contains > 0 {@link SDocument}s.
	 *
	 * @return a non-empty corpus graph.
	 */
	private SCorpusGraph getNonEmptyCorpusGraph() {
		SaltProject project = getFixture().getSaltProject();
		SCorpusGraph corpusGraph = null;
		loop:
		for (SCorpusGraph cg : project.getCorpusGraphs()) {
			if (cg.getDocuments().size() > 0) {
				corpusGraph = project.getCorpusGraphs().get(0);
				break loop;
			}
		}
		return corpusGraph;
	}

	/**
	 * TODO: Description
	 * @param corpusGraph 
	 *
	 */
	private void runCorpusTests(SCorpusGraph corpusGraph, String fileName) {
		assertEquals(1, corpusGraph.getCorpora().size());

		// Test single corpus
		if (fileName.equals("test.txt")) {
			for (SMetaAnnotation ma : corpusGraph.getCorpora().get(0).getMetaAnnotations()) {
				assertThat(ma.getQName() + ":" + ma.getValue_STEXT(), anyOf(is("toolbox::_sh:v3.0 Test"), 
						is("toolbox::info:" + DOC_INFO_ANNO),
						is("toolbox::moreinfo:Some more info about the corpus")));
			}
		}
		else {
			for (SMetaAnnotation ma : corpusGraph.getCorpora().get(0).getMetaAnnotations()) {
				assertThat(ma.getQName() + ":" + ma.getValue_STEXT(), is(anyOf(is("toolbox::_sh:v3.0 Test"), is("toolbox::info:Some info"))));
			}
		}
	}

	private String getFile(String fileName) {
		return this.getClass().getClassLoader().getResource(fileName).getFile();
	}

	/**
	 * TODO: Description
	 *
	 * @param string
	 */
	private void setTestFile(String fileName) {
		getFixture().setCorpusDesc(new CorpusDesc().setCorpusPath(URI.createFileURI(getFile(fileName))));
	}

	/**
	 * TODO: Description
	 *
	 * @param string
	 */
	private void setProperties(String string) {
		ToolboxTextImporterProperties properties = new ToolboxTextImporterProperties();
		properties.setPropertyValues(new File(getFile("test.properties")));
		getFixture().setProperties(properties);
	}
	

}
