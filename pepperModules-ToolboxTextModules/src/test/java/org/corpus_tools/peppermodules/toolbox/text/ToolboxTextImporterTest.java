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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.corpus_tools.pepper.common.CorpusDesc;
import org.corpus_tools.pepper.common.FormatDesc;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.pepper.testFramework.PepperImporterTest;
import org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper;
import org.corpus_tools.salt.SALT_TYPE;
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
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNot;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ToolboxTextImporter}.
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 *
 */
public class ToolboxTextImporterTest extends PepperImporterTest {

	private static final String DOC_INFO_ANNO = "A sample \"standard\" corpus in Toolbox text format. It includes use cases for most phenomena the importer tests against, such as clitics and affixes, subrefs, meta annotations, etc.";
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
//		assertTrue(getFixture().isMonolithic());
		SCorpusGraph corpusGraph = getNonEmptyCorpusGraph();
		
		// Test corpora
		runCorpusTests(corpusGraph, "no-ids.txt");
		
		// Test single document
		assertEquals(1, corpusGraph.getDocuments().size());
		assertEquals("no-ids", corpusGraph.getDocuments().get(0).getName());
		SDocumentGraph graph = getGraph("no-ids");
		assertEquals(2, graph.getTextualDSs().size());
		assertEquals("Wort1 Wort2 Wort3 Wort4 Wort5 Wort6", graph.getTextualDSs().get(0).getText());
		assertEquals("m1m2m3m4m5m6", graph.getTextualDSs().get(1).getText());
		assertEquals(12, getGraph("no-ids").getTokens().size());
		assertEquals(6, getGraph("no-ids").getSpans().size());
		for (SSpan span : getGraph("no-ids").getSpans()) {
			assertEquals(1, graph.getOverlappedTokens(span).size());
		}
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
		assertEquals(4, getNonEmptyCorpusGraph().getDocuments().size());
		for (int i = 0; i < getNonEmptyCorpusGraph().getDocuments().size(); i++) {
			SDocumentGraph graph = getNonEmptyCorpusGraph().getDocuments().get(i).getDocumentGraph();
			assertEquals(0, graph.getTokens().size());
			assertEquals(0, graph.getSpans().size());
		}
		assertEquals(0, getDocument("ID4 (no further info)").getMetaAnnotations().size());
		assertEquals(1, getDocument("ID1").getMetaAnnotations().size());
		assertEquals(1, getDocument("ID2").getMetaAnnotations().size());
		assertEquals(1, getDocument("ID3").getMetaAnnotations().size());
		assertEquals("Info on ID1", getDocument("ID1").getMetaAnnotations().iterator().next().getValue_STEXT());
	}
	
//	/**
//	 * Test method for
//	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
//	 * 
//	 * Tests against a "standard" example, which covers most phenomena 
//	 * the importer tests against, such as clitics and affixes, 
//	 * subrefs, meta annotations, etc.
//	 */
//	@Test
//	public void testParseStandardDocument() {
//		setTestFile("test.txt");
//		setProperties("test.properties");
//		start();
////		assertEquals((Long) 246L, getFixture().getHeaderEndOffset());
////		assertFalse(getFixture().isMonolithic());
//		SCorpusGraph corpusGraph = getNonEmptyCorpusGraph();
//
//		// Test corpora
//		runCorpusTests(corpusGraph, "test.txt");
//		
//		// Test documents
//		assertEquals(5, corpusGraph.getDocuments().size());
//		for (SDocument doc : corpusGraph.getDocuments()) {
//			assertThat(doc.getName(), anyOf(is(DOC_NO + "1"), is(DOC_NO + "2"), is(DOC_NO + "3"), is(DOC_NO + "4"), is(DOC_NO + "5")));
//			assertEquals(2, doc.getMetaAnnotations().size());
//			for (SAnnotation anno : doc.getAnnotations()) {
//				assertThat(anno.getQName(), anyOf(is(TOOLBOX + "::idinfo"), is(TOOLBOX + "::moreidinfo")));
//				if (anno.getQName().equals(TOOLBOX + "::idinfo")) {
//					assertEquals("Some ".toLowerCase() + doc.getName().toLowerCase() + " info".toLowerCase(), anno.getValue_STEXT().toLowerCase());
//				}
//				else {
//					assertEquals("Some more info about ".toLowerCase() + doc.getName().toLowerCase(), anno.getValue_STEXT().toLowerCase());
//				}
//			}
//		}
//		for (SDocument doc : corpusGraph.getDocuments()) {
//			// General document tests TODO: Factor out to method?
//			assertNotNull(doc.getDocumentGraph());
//			SDocumentGraph graph = doc.getDocumentGraph();
//			assertEquals(2, graph.getTextualDSs().size());
//			// Document-respective tests
//			String docNumber = doc.getId().substring(doc.getId().length() - 1);
//			// FIXME switch on docNumber
//			// Document-level
//			assertEquals(DOC_NO + docNumber, doc.getName());
//			assertNotNull(doc.getMetaAnnotation(TOOLBOX + "::idinfo"));
//			assertNotNull(doc.getMetaAnnotation(TOOLBOX + "::moreidinfo"));
//			switch (docNumber) {
//			case "4":
//				assertEquals("Some document no. " + docNumber + " info followed by an empty marker", doc.getMetaAnnotation(TOOLBOX + "::idinfo").getValue_STEXT());
//				assertEquals("Some more info about document no. " + docNumber + " Duplicate 1 Duplicate 2", doc.getMetaAnnotation(TOOLBOX + "::moreidinfo").getValue_STEXT());
//				break;
//
//			default:
//				assertEquals("Some document no. " + docNumber + " info", doc.getMetaAnnotation(TOOLBOX + "::idinfo").getValue_STEXT());
//				assertEquals("Some more info about document no. " + docNumber, doc.getMetaAnnotation(TOOLBOX + "::moreidinfo").getValue_STEXT());
//				break;
//			}
//			// Data sources
//			for (STextualDS ds : graph.getTextualDSs()) {
//				switch (docNumber) {
//				case "1":
//					assertThat(ds.getText(), is(anyOf(is("Word1 -Tuple Tuple- Word2 Triple-= Word3 FreedashTuple Word4 FreecliticTuple SubRef sentence one SubRef sentence one SubRef sentence two with one-to-four ref SubRef sentence two with one-to-four ref SubRef with some random text just like that SubRef with some random text just like that SubRef with some random text just like that"), 
//							is("m1m2-m3m4-m5m6m7-m8=m9m10m11-m12m13m14=m15m16m17m18m19m20m21m22m23m24m25m26m27m28m29m30m31m32m33m34m35m36m37m38m39m40m41m42m43"))));
//					break;
//
//				case "2":
//					assertThat(ds.getText(), is(anyOf(is("A sentence A sentence"),
//							is("m1m2m1m2"))));
//					break;
//
//				case "3":
//					assertThat(ds.getText(), is(anyOf(is("A sentence A sentence"),
//							is("m1m2m1m2"))));
//					break;
//
//				case "4":
//					assertThat(ds.getText(), is(anyOf(is("A sentence A sentence"),
//							is("m1m2m1m2"))));
//					break;
//					
//				case "5":
//					assertThat(ds.getText(), is(anyOf(is("One Two Three Four"),
//							is("m1m2m3m4"))));
//					break;
//
//				default:
//					fail();
//					break;
//				}
//			}
//			// Tokens, spans, layers
//			switch (docNumber) {
//			case "1":
//				assertEquals(94, graph.getTokens().size());
//				assertEquals(8, graph.getSpans().size());
//				assertEquals(3, graph.getLayers().size());
//				break;
//
//			case "2":
//			case "3":
//			case "4":
//				assertEquals(8, graph.getTokens().size());
//				assertEquals(2, graph.getSpans().size());
//				assertEquals(3, graph.getLayers().size());
//				break;
//
//			case "5":
//				assertEquals(8, graph.getTokens().size());
//				assertEquals(1, graph.getSpans().size());
//				assertEquals(3, graph.getLayers().size());
//				break;
//
//			default:
//				fail();
//				break;
//			}
//			// Layers
//			for (SLayer l : graph.getLayers()) {
//				switch (docNumber) {
//				case "1":
//					switch (l.getName()) {
//					case "tx":
//						assertEquals(51, l.getNodes().size());
//						break;
//						
//					case "mb":
//						assertEquals(43, l.getNodes().size());
//						break;
//						
//					case "ref":
//						assertEquals(8, l.getNodes().size());
//						break;
//
//					default:
//						fail();
//						break;
//					}
//					break;
//
//				case "2":
//				case "3":
//				case "4":
//					switch (l.getName()) {
//					case "tx":
//						assertEquals(4, l.getNodes().size());
//						break;
//						
//					case "mb":
//						assertEquals(4, l.getNodes().size());
//						break;
//						
//					case "ref":
//						assertEquals(2, l.getNodes().size());
//						break;
//
//					default:
//						fail();
//						break;
//					}
//					break;
//
//				case "5":
//					switch (l.getName()) {
//					case "tx":
//						assertEquals(4, l.getNodes().size());
//						break;
//						
//					case "mb":
//						assertEquals(4, l.getNodes().size());
//						break;
//						
//					case "ref":
//						assertEquals(1, l.getNodes().size());
//						break;
//
//					default:
//						fail();
//						break;
//					}
//					break;
//
//				default:
//					fail();
//					break;
//				}
//			}
//			// Ref-level annotations
//			for (SSpan s : graph.getSpans()) {
//				switch (docNumber) {
//				case "1":
//					assertEquals(3, s.getAnnotations().size());
//					for (SAnnotation a : s.getAnnotations()) {
//						assertThat(a.getName(), is(anyOf(is("ll"), is("ref"), is("met"))));
//					}
//					break;
//
//				case "2":
//				case "3":
//				case "4":
//				case "5":
//					assertEquals(2, s.getAnnotations().size());
//					for (SAnnotation a : s.getAnnotations()) {
//						assertThat(a.getName(), is(anyOf(is("ll"), is("ref"))));
//					}
//					break;
//
//				default:
//					fail();
//					break;
//				}
//			}
//			// Token annotations
//			String[] lexAnnos, morphAnnos;
//			List<SToken> sortedLexToks = new ArrayList<>();
//			List<SToken> sortedMorphToks = new ArrayList<>();
//			switch (docNumber) {
//			case "1":
//				lexAnnos = new String[] { "WordOne", "TupleOne", "TupleTwo", "WordTwo", "Triple", "WordThree", "TupleThree", "WordFour", "TupleFour", "t11", "t12", "t13", "t14", "t15", "t16", "t17", "t18", "t19", "t20", "t21", "t22", "t23", "t24", "t25", "t26", "t27", "t28", "t29", "t30", "t31", "t32", "t33", "t34", "t35", "t36", "t37", "t38", "t39", "t40", "t41", "t42", "t43", "t44", "t45", "t46", "t47", "t48", "t49", "t50", "t51", "t52" };
//				morphAnnos = new String[] { "M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", "M10", "M11", "M12", "M13", "M14", "M15", "M16", "M17", "M18", "M19", "M20", "M21", "M22", "M23", "M24", "M25", "M26", "M27", "M28", "M29", "M30", "M31", "M32", "M33", "M34", "M35", "M36", "M37", "M38", "M39", "M40", "M41", "M42", "M43" };
//				for (SToken sortedTok : graph.getSortedTokenByText()) {
//					if (graph.getLayerByName("tx").get(0).getNodes().contains(sortedTok)) {
//						sortedLexToks.add(sortedTok);
//					}
//					else if (graph.getLayerByName("mb").get(0).getNodes().contains(sortedTok)) {
//						sortedMorphToks.add(sortedTok);
//					}
//				}
//				for (int j = 0; j < sortedLexToks.size(); j++) {
//					assertEquals(lexAnnos[j], sortedLexToks.get(j).getAnnotation("toolbox::ta").getValue());
//				}
//				for (int j = 0; j < sortedMorphToks.size(); j++) {
//					assertEquals(morphAnnos[j], sortedMorphToks.get(j).getAnnotation("toolbox::ge").getValue());
//				}
//				break;
//
//			case "2":
//			case "3":
//			case "4":
//				lexAnnos = new String[] { "A", "word", "A", "word"};
//				morphAnnos = new String[] { "M1", "M2", "M1", "M2"};
//				for (SToken sortedTok : graph.getSortedTokenByText()) {
//					if (graph.getLayerByName("tx").get(0).getNodes().contains(sortedTok)) {
//						sortedLexToks.add(sortedTok);
//					}
//					else if (graph.getLayerByName("mb").get(0).getNodes().contains(sortedTok)) {
//						sortedMorphToks.add(sortedTok);
//					}
//				}
//				for (int j = 0; j < sortedLexToks.size(); j++) {
//					assertEquals(lexAnnos[j], sortedLexToks.get(j).getAnnotation("toolbox::ta").getValue());
//				}
//				for (int j = 0; j < sortedMorphToks.size(); j++) {
//					assertEquals(morphAnnos[j], sortedMorphToks.get(j).getAnnotation("toolbox::ge").getValue());
//				}
//				break;
//
//			case "5":
//				lexAnnos = new String[] { "1", "2", "3", "4"};
//				morphAnnos = new String[] { "M1", "M2", "M3", "M4"};
//				for (SToken sortedTok : graph.getSortedTokenByText()) {
//					if (graph.getLayerByName("tx").get(0).getNodes().contains(sortedTok)) {
//						sortedLexToks.add(sortedTok);
//					}
//					else if (graph.getLayerByName("mb").get(0).getNodes().contains(sortedTok)) {
//						sortedMorphToks.add(sortedTok);
//					}
//				}
//				for (int j = 0; j < sortedLexToks.size(); j++) {
//					assertEquals(lexAnnos[j], sortedLexToks.get(j).getAnnotation("toolbox::ta").getValue());
//				}
//				for (int j = 0; j < sortedMorphToks.size(); j++) {
//					assertEquals(morphAnnos[j], sortedMorphToks.get(j).getAnnotation("toolbox::ge").getValue());
//				}
//				break;
//
//			default:
//				fail();
//				break;
//			}
//			// Spanning
//			switch (docNumber) {
//			case "1":
//				for (SSpan span : graph.getSpans()) {
//					if (span.getName().equals("Reference no. 1")) {
//						assertEquals(9, graph.getOverlappedTokens(span).size());
//						for (SToken tok : graph.getOverlappedTokens(span)) {
////							FIXME CONTINUE IMPLEMENTATION graph.getTimeline().get
//						}
//					}
//				}
//				break;
//
//			case "2":
//			case "3":
//			case "4":
//				
//				break;
//
//			case "5":
//				break;
//
//			default:
//				break;
//			}
//		}
//	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there are 0 \ids and 0 \refs
	 */
	@Test
	public void testParseOrphanRefs() {
		setTestFile("orphan-ids-and-refs.txt");
		start();
		assertEquals(5, getNonEmptyCorpusGraph().getDocuments().size());
		assertEquals("Wort1 Wort2", getGraph("ID1").getTextualDSs().get(0).getText());
		assertEquals("m1m2", getGraph("ID1").getTextualDSs().get(1).getText());
		assertEquals(4, getGraph("ID1").getTokens().size());
		assertEquals(2, getGraph("ID1").getSpans().size());
		assertEquals("Wort3 Wort4", getGraph("ID2").getTextualDSs().get(0).getText());
		assertEquals("m3m4", getGraph("ID2").getTextualDSs().get(1).getText());
		assertEquals(4, getGraph("ID2").getTokens().size());
		assertEquals(2, getGraph("ID2").getSpans().size());
		assertEquals("Wort3 Wort4", getGraph("ID2").getTextualDSs().get(0).getText());
		assertEquals("m3m4", getGraph("ID2").getTextualDSs().get(1).getText());
		assertEquals("Wort5 Wort6", getGraph("ID3").getTextualDSs().get(0).getText());
		assertEquals("m5m6", getGraph("ID3").getTextualDSs().get(1).getText());
		assertEquals(4, getGraph("ID3").getTokens().size());
		assertEquals(2, getGraph("ID3").getSpans().size());
		assertEquals(1, getDocument("ORPHANID1").getMetaAnnotations().size());
		assertEquals("Info on ORID1", getDocument("ORPHANID1").getMetaAnnotations().iterator().next().getValue_STEXT());
		assertEquals(0, getGraph("ORPHANID1").getTokens().size());
		assertEquals(0, getGraph("ORPHANID1").getSpans().size());
		assertEquals(1, getDocument("ORPHANID2").getMetaAnnotations().size());
		assertEquals("Info on ORID2", getDocument("ORPHANID2").getMetaAnnotations().iterator().next().getValue_STEXT());
		assertEquals(0, getGraph("ORPHANID2").getTokens().size());
		assertEquals(0, getGraph("ORPHANID2").getSpans().size());
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
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocumentGraph graph = getGraph("empty-refs");
		assertEquals(2, graph.getTextualDSs().size());
		assertEquals("Wort1", graph.getTextualDSs().get(0).getText());
		assertEquals("m1", graph.getTextualDSs().get(1).getText());
		assertEquals(2, graph.getTokens().size());
		assertEquals(1, graph.getSpans().size());
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
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocumentGraph graph = getGraph("ID1");
		assertEquals(1, graph.getTextualDSs().size());
		assertEquals("Word Word", graph.getTextualDSs().get(0).getText());
		assertEquals(2, graph.getTokens().size());
		assertEquals(2, graph.getSpans().size());
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
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocumentGraph graph = getGraph("id-without-morph-line");
		assertEquals(1, graph.getTextualDSs().size());
		assertEquals("This is a birthday pony ! ( Google this ? )", graph.getTextualDSs().get(0).getText());
		assertEquals(11, graph.getTokens().size());
		assertEquals(1, graph.getSpans().size());
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
		setProperties("test-mixed-with-without-mb.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocumentGraph graph = getGraph("Document no. 1");
		assertEquals(2, graph.getTextualDSs().size());
		assertEquals("Word1 Word2 Word3 Word4 Word5 Word6", graph.getTextualDSs().get(0).getText());
		assertEquals("m1m2m5m6", graph.getTextualDSs().get(1).getText());
		assertEquals(10, graph.getTokens().size());
		assertEquals(3, graph.getSpans().size());
		for (SSpan span : graph.getSpans()) {
			assertThat(span.getName(), anyOf(is("Sentence 2 *without* mb line"), is("Sentence 1 with mb line")));
			assertEquals(2, span.getAnnotations().size());
		}
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#SIMPLE}.
	 */
	@Test
	public void testSubRefSIMPLE() {
		setTestFile("subref_SIMPLE.txt");
		setProperties("subref_SIMPLE.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(6));
		assertThat(graph.getSpans().size(), is(2));
		assertThat(graph.getNodesByName("subref").size(), is(1));
		SNode subref = graph.getNodesByName("subref").get(0);
		assertThat(subref, instanceOf(SSpan.class));
		assertThat(subref.getAnnotations().size(), is(1));
		for (SToken tok : graph.getOverlappedTokens(subref)) {
			assertThat(graph.getText(tok), anyOf(is("m2"), is("m3")));
		}
		assertThat(subref.getAnnotations().iterator().next().getQName(), is("toolbox::sr"));
		assertThat(subref.getAnnotations().iterator().next().getValue_STEXT(), is("SIMPLE m2-m3"));
		assertThat(subref.getLayers().size(), is(1));
		assertThat(subref.getLayers().iterator().next().getName(), is("mb"));
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#SIMPLE_TARGETED}.
	 */
	@Test
	public void testSubRefSIMPLE_TARGETED_MB() {
		setTestFile("subref_SIMPLE_TARGETED.txt");
		setProperties("subref_SIMPLE_TARGETED.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(7));
		assertThat(graph.getSpans().size(), is(3));
		List<SNode> subrefNodes = graph.getNodesByName("subref"); 
		assertThat(subrefNodes.size(), is(2));
		SNode xtNode = null, srNode = null;
		for (SNode subref : subrefNodes) {
			for (SAnnotation anno : subref.getAnnotations()) {
				if (anno.getQName().equals("toolbox::xt")) {
					xtNode = subref;
				}
				else if (anno.getQName().equals("toolbox::sr")) {
					srNode = subref;
				}
			}
		}
		assertNotNull(xtNode);
		assertNotNull(srNode);
		assertThat(xtNode == srNode, is(false));
		assertThat(xtNode, instanceOf(SSpan.class));
		assertThat(srNode, instanceOf(SSpan.class));
		assertThat(graph.getOverlappedTokens(xtNode).size(), is(2));
		assertThat(graph.getOverlappedTokens(srNode).size(), is(2));
		for (SToken tok : graph.getOverlappedTokens(xtNode)) {
			assertThat(graph.getText(tok), anyOf(is("m18"), is("-m19")));
		}
		for (SToken tok : graph.getOverlappedTokens(srNode)) {
			assertThat(graph.getText(tok), anyOf(is("m17"), is("m18")));
		}
		assertThat(xtNode.getAnnotations().size(), is(1));
		assertThat(srNode.getAnnotations().size(), is(1));
		assertThat(xtNode.getAnnotations().iterator().next().getQName(), is("toolbox::xt"));
		assertThat(xtNode.getAnnotation("toolbox::xt").getValue_STEXT(), is("SIMPLE_TARGETED m18-m19"));
		assertThat(srNode.getAnnotations().iterator().next().getQName(), is("toolbox::sr"));
		assertThat(srNode.getAnnotation("toolbox::sr").getValue_STEXT(), is("SIMPLE_TARGETED m17-m18"));
		assertThat(srNode.getLayers().size(), is(1));
		assertThat(srNode.getLayers().iterator().next().getName(), is("mb"));
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#SIMPLE_TARGETED}.
	 */
	@Test
	public void testSubRefSIMPLE_TARGETED_TX() {
		setTestFile("subref_SIMPLE_TARGETED_tx.txt");
		setProperties("subref_SIMPLE_TARGETED.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(6));
		assertThat(graph.getSpans().size(), is(2));
		assertThat(graph.getNodesByName("subref").size(), is(1));
		SNode srNode = graph.getNodesByName("subref").get(0);
		assertThat(srNode, instanceOf(SSpan.class));
		assertThat(graph.getOverlappedTokens(srNode).size(), is(2));
		for (SToken tok : graph.getOverlappedTokens(srNode)) {
			assertThat(graph.getText(tok), anyOf(is("L2"), is("L3")));
		}
		assertThat(srNode.getAnnotations().size(), is(1));
		assertThat(srNode.getAnnotations().iterator().next().getQName(), is("toolbox::sr"));
		assertThat(srNode.getAnnotation("toolbox::sr").getValue_STEXT(), is("SIMPLE_TARGETED L2-L3"));
		assertThat(srNode.getLayers().size(), is(1));
		assertThat(srNode.getLayers().iterator().next().getName(), is("tx"));
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#UNIDENTIFIED_GLOBAL}.
	 */
	@Test
	public void testSubRefUNIDENTIFIED_GLOBAL_MB() {
		setTestFile("subref_UNIDENTIFIED_GLOBAL.txt");
		setProperties("subref_UNIDENTIFIED_GLOBAL.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(12));
		assertThat(graph.getSpans().size(), is(2));
		assertThat(graph.getNodesByName("subref").size(), is(1));
		SNode srNode = graph.getNodesByName("subref").get(0);
		assertThat(srNode, instanceOf(SSpan.class));
		assertThat(graph.getOverlappedTokens(srNode).size(), is(4));
		for (SToken tok : graph.getOverlappedTokens(srNode)) {
			assertThat(graph.getText(tok), anyOf(is("m23"), is("m24"), is("m25"), is("m26")));
		}
		// FIXME: Salt pull request for graphElement.hasAnnotation(String qName)
		// and getAnnotations().contains(String annotationString)
		assertThat(srNode.getAnnotations().size(), is(2));
		assertNotNull(srNode.getAnnotation("toolbox::sr"));
		assertNotNull(srNode.getAnnotation("toolbox::sr2"));
		assertThat(srNode.getAnnotation("toolbox::sr").getValue_STEXT(), is("UNIDENTIFIED_GLOBAL m23-m26"));
		assertThat(srNode.getAnnotation("toolbox::sr2").getValue_STEXT(), is("UNIDENTIFIED_GLOBAL m23-m26"));
		assertThat(srNode.getLayers().size(), is(1));
		assertThat(srNode.getLayers().iterator().next().getName(), is("mb"));
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#UNIDENTIFIED_GLOBAL}.
	 */
	@Test
	public void testSubRefUNIDENTIFIED_GLOBAL_TX() {
		setTestFile("subref_UNIDENTIFIED_GLOBAL_tx.txt");
		setProperties("subref_UNIDENTIFIED_GLOBAL.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(6));
		assertThat(graph.getSpans().size(), is(2));
		assertThat(graph.getNodesByName("subref").size(), is(1));
		SNode srNode = graph.getNodesByName("subref").get(0);
		assertThat(srNode, instanceOf(SSpan.class));
		assertThat(graph.getOverlappedTokens(srNode).size(), is(4));
		for (SToken tok : graph.getOverlappedTokens(srNode)) {
			assertThat(graph.getText(tok), anyOf(is("L2"), is("L3"), is("L4"), is("L5")));
		}
		// FIXME: Salt pull request for graphElement.hasAnnotation(String qName)
		// and getAnnotations().contains(String annotationString)
		assertThat(srNode.getAnnotations().size(), is(2));
		assertNotNull(srNode.getAnnotation("toolbox::sr"));
		assertNotNull(srNode.getAnnotation("toolbox::sr2"));
		assertThat(srNode.getAnnotation("toolbox::sr").getValue_STEXT(), is("UNIDENTIFIED_GLOBAL L2-L5"));
		assertThat(srNode.getAnnotation("toolbox::sr2").getValue_STEXT(), is("UNIDENTIFIED_GLOBAL L2-L5"));
		assertThat(srNode.getLayers().size(), is(1));
		assertThat(srNode.getLayers().iterator().next().getName(), is("tx"));
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#UNIDENTIFIED_GLOBAL_TARGETED}.
	 */
	@Test
	public void testSubRefUNIDENTIFIED_GLOBAL_TARGETED_MB() {
		setTestFile("subref_UNIDENTIFIED_GLOBAL_TARGETED.txt");
		setProperties("subref_UNIDENTIFIED_GLOBAL.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(12));
		assertThat(graph.getSpans().size(), is(2));
		assertThat(graph.getNodesByName("subref").size(), is(1));
		SNode srNode = graph.getNodesByName("subref").get(0);
		assertThat(srNode, instanceOf(SSpan.class));
		assertThat(graph.getOverlappedTokens(srNode).size(), is(4));
		for (SToken tok : graph.getOverlappedTokens(srNode)) {
			assertThat(graph.getText(tok), anyOf(is("m23"), is("m24"), is("m25"), is("m26")));
		}
		assertThat(srNode.getAnnotations().size(), is(2));
		assertNotNull(srNode.getAnnotation("toolbox::sr"));
		assertNotNull(srNode.getAnnotation("toolbox::sr2"));
		assertThat(srNode.getAnnotation("toolbox::sr").getValue_STEXT(), is("UNIDENTIFIED_GLOBAL_TARGETED m23-m26"));
		assertThat(srNode.getAnnotation("toolbox::sr2").getValue_STEXT(), is("UNIDENTIFIED_GLOBAL_TARGETED m23-m26"));
		assertThat(srNode.getLayers().size(), is(1));
		assertThat(srNode.getLayers().iterator().next().getName(), is("mb"));
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#UNIDENTIFIED_GLOBAL_TARGETED}.
	 */
	@Test
	public void testSubRefUNIDENTIFIED_GLOBAL_TARGETED_TX() {
		setTestFile("subref_UNIDENTIFIED_GLOBAL_TARGETED_tx.txt");
		setProperties("subref_UNIDENTIFIED_GLOBAL.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(6));
		assertThat(graph.getSpans().size(), is(2));
		assertThat(graph.getNodesByName("subref").size(), is(1));
		SNode srNode = graph.getNodesByName("subref").get(0);
		assertThat(srNode, instanceOf(SSpan.class));
		assertThat(graph.getOverlappedTokens(srNode).size(), is(4));
		for (SToken tok : graph.getOverlappedTokens(srNode)) {
			assertThat(graph.getText(tok), anyOf(is("sentence"), is("two"), is("with"), is("one-to-four")));
		}
		assertThat(srNode.getAnnotations().size(), is(2));
		assertNotNull(srNode.getAnnotation("toolbox::sr"));
		assertNotNull(srNode.getAnnotation("toolbox::sr2"));
		assertThat(srNode.getAnnotation("toolbox::sr").getValue_STEXT(), is("UNIDENTIFIED_GLOBAL_TARGETED_tx m23-m26"));
		assertThat(srNode.getAnnotation("toolbox::sr2").getValue_STEXT(), is("UNIDENTIFIED_GLOBAL_TARGETED_tx m23-m26"));
		assertThat(srNode.getLayers().size(), is(1));
		assertThat(srNode.getLayers().iterator().next().getName(), is("tx"));
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there is 1 liaison delimiter
	 */
	@Test
	public void testLiaison() {
		setTestFile("liaison.txt");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertEquals("Word Contraction Word", graph.getTextualDSs().get(0).getText());
		assertEquals("m1contractionm2", graph.getTextualDSs().get(1).getText());
		assertEquals(7, graph.getTokens().size());
		assertEquals(1, graph.getSpans().size());
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
		setProperties("test-no-mb-lines.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertEquals(1, graph.getTextualDSs().size());
		assertEquals("Word1 Word2 Word3 Word4 Word5 Word6", graph.getTextualDSs().get(0).getText());
		assertEquals(6, graph.getTokens().size());
		assertEquals(3, graph.getSpans().size());
		for (SToken token: graph.getTokens()) {
			assertEquals(1, token.getAnnotations().size());
		}
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

//	/**
//	 * Test method for
//	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
//	 * 
//	 * Tests cases where the number of annotations for lex or morph does not equal the number of tokens.
//	 */
//	@Test
//	public void testParseAnnosUnequalTok() {
//		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_LEX_ANNOTATION_MARKERS, "ta, tb");
//		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_MORPH_ANNOTATION_MARKERS, "ge");
////		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_FIX_ERRORS, false);
////		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_RECORD_ERRORS, false);
//		setTestFile("annos-unequal-tok.txt");
//		start();
//		fail("Needs to be implemented further!");
//	}

//	/**
//	 * Test method for
//	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
//	 * 
//	 * FIXME Tests against a minimum example, where there are 4 \id, one of which has a \ref **without** a morph line
//	 */
//	@Test
//	public void testDelimAttachment() {
//		setTestFile("delims.txt");
//		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_ATTACH_DETACHED_MORPHEME_DELIMITER, "true, false");
//		start();
//		fail("Needs to be implemented further!");
//	}

//	/**
//	 * Test method for
//	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
//	 * 
//	 * FIXME Tests against a minimum example, where there are 4 \id, one of which has a \ref **without** a morph line
//	 */
//	@Test
//	public void testMorphLexInterlinearization() {
////		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_ATTACH_DETACHED_MORPHEME_DELIMITER, "false, false");
//		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_MORPH_ANNOTATION_MARKERS, "ma");
//		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_LEX_ANNOTATION_MARKERS, "ta");
////		getFixture().getProperties().setPropertyValue(ToolboxTextImporterProperties.PROP_FIX_INTERL11N, false);
//		setTestFile("interlinearization.txt");
//		start();
//		fail("Needs to be implemented further!");
//	}

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
	private void setProperties(String fileName) {
		ToolboxTextImporterProperties properties = new ToolboxTextImporterProperties();
		properties.setPropertyValues(new File(getFile(fileName)));
		getFixture().setProperties(properties);
	}
	
	private SDocumentGraph getGraph(String name) {
		return getDocument(name).getDocumentGraph();
	}

	private SDocument getDocument(String name) {
		for (SDocument doc : getNonEmptyCorpusGraph().getDocuments()) {
			if (doc.getName().equals(name)) {
				return doc;
			}
		}
		return null;
	}
}
