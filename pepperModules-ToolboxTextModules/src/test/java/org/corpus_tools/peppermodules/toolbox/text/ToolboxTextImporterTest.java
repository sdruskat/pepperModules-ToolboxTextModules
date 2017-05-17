/*******************************************************************************
 * Copyright (c) 2017 Stephan Druskat
 * Exploitation rights belong exclusively to Humboldt-Universität zu Berlin
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
import static org.junit.Assert.assertNull;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ToolboxTextImporter}.
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 *
 */
public class ToolboxTextImporterTest extends PepperImporterTest {
	
	private static final String DOC_INFO_ANNO = "A sample \"standard\" corpus in Toolbox text format. It includes use cases for most phenomena the importer tests against, such as clitics and affixes, subrefs, meta annotations, etc.";
	private static final String DOC_NO = "Document_no__";
	private static final String TOOLBOX = "toolbox";
	private Logger rootLogger;
	@SuppressWarnings("rawtypes")
	private Appender mockAppender;
	
	@Captor
	private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

	/**
	 * @throws java.lang.Exception
	 */
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		this.setFixture(new ToolboxTextImporter());
		this.addFormatWhichShouldBeSupported(new FormatDesc().setFormatName("toolbox-text").setFormatVersion("3.0"));
		this.getFixture().getCorpusDesc().getFormatDesc().setFormatName("toolbox-text").setFormatVersion("3.0");
		getFixture().getSaltProject().createCorpusGraph();
		getFixture().setProperties(new ToolboxTextImporterProperties());
		
		// Logging
		rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
	    mockAppender = mock(Appender.class);
	    when(mockAppender.getName()).thenReturn("MOCK");
	    rootLogger.addAppender(mockAppender);
	    rootLogger.setLevel(Level.WARN);
	}
	
	@SuppressWarnings("unchecked")
	@After
	public void teardown() {
		rootLogger.detachAppender(mockAppender);
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
		assertEquals(0, getDocument("ID4_no_further_info").getMetaAnnotations().size());
		assertEquals(1, getDocument("ID1").getMetaAnnotations().size());
		assertEquals(1, getDocument("ID2").getMetaAnnotations().size());
		assertEquals(1, getDocument("ID3").getMetaAnnotations().size());
		assertEquals("Info on ID1", getDocument("ID1").getMetaAnnotations().iterator().next().getValue_STEXT());
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a "standard" example, which covers most phenomena 
	 * the importer tests against, such as clitics and affixes, 
	 * subrefs, meta annotations, etc.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testParseStandardDocument() {
		setTestFile("test.txt");
		setProperties("test.properties");
		start();
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
					assertThat(ds.getText(), is(anyOf(is("Word1 -Tuple Tuple- Word2 Triple-= Word3 FreedashTuple Word4 FreecliticTuple Subref sentence one Subref sentence one Subref sentence two with one-to-four ref Subref sentence two with one-to-four ref Subref with some random text just like that Subref with some random text just like that Subref with some random text just like that"), 
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
				assertEquals(18, graph.getSpans().size());
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
						assertEquals(57, l.getNodes().size());
						break;
						
					case "mb":
						assertEquals(49, l.getNodes().size());
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
						assertEquals(5, l.getNodes().size());
						break;
						
					case "mb":
						assertEquals(5, l.getNodes().size());
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
						assertEquals(5, l.getNodes().size());
						break;
						
					case "mb":
						assertEquals(5, l.getNodes().size());
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
				SAnnotation aut = null;
				switch (docNumber) {
				case "1":
					switch (s.getName()) {
					case "Reference no. 1":
						assertEquals(3, s.getAnnotations().size());
						for (SAnnotation a : s.getAnnotations()) {
							assertThat(a.getName(), is(anyOf(is("ll"), is("ref"), is("met"))));
						}
						assertNotNull((aut = s.getAnnotation("toolbox::ll")));
						assertThat(aut.getValue_STEXT(), is("A reference testing composites, clitics, and such"));
						assertNotNull((aut = s.getAnnotation("toolbox::ref")));
						assertThat(aut.getValue_STEXT(), is(s.getName()));
						assertNotNull((aut = s.getAnnotation("toolbox::met")));
						assertThat(aut.getValue_STEXT(), is("Some meta information about the first sentence"));
						break;
					case "Subref sentence schema 1 (line-level) to mb":
						if (!s.getName().equals("subref")) {
							assertEquals(3, s.getAnnotations().size());
							for (SAnnotation a : s.getAnnotations()) {
								assertThat(a.getName(), is(anyOf(is("ll"), is("ref"), is("met"))));
							}
							assertNotNull((aut = s.getAnnotation("toolbox::ll")));
							assertThat(aut.getValue_STEXT(), is("uref one-mb"));
							assertNotNull((aut = s.getAnnotation("toolbox::ref")));
							assertThat(aut.getValue_STEXT(), is(s.getName()));
							assertNotNull((aut = s.getAnnotation("toolbox::met")));
							assertThat(aut.getValue_STEXT(), is("A sentence with a line-level Subref"));
						}
						break;
					case "Subref sentence schema 1 (line-level) to tx":
						if (!s.getName().equals("subref")) {
							assertEquals(3, s.getAnnotations().size());
							for (SAnnotation a : s.getAnnotations()) {
								assertThat(a.getName(), is(anyOf(is("ll"), is("ref"), is("met"))));
							}
							assertNotNull((aut = s.getAnnotation("toolbox::ll")));
							assertThat(aut.getValue_STEXT(), is("uref one-tx"));
							assertNotNull((aut = s.getAnnotation("toolbox::ref")));
							assertThat(aut.getValue_STEXT(), is(s.getName()));
							assertNotNull((aut = s.getAnnotation("toolbox::met")));
							assertThat(aut.getValue_STEXT(), is("A sentence with a line-level Subref"));
						}
						break;
					case "Subref sentence schema 2 (undefined global) with existing mb line":
						if (!s.getName().equals("subref")) {
							assertEquals(3, s.getAnnotations().size());
							for (SAnnotation a : s.getAnnotations()) {
								assertThat(a.getName(), is(anyOf(is("ll"), is("ref"), is("met"))));
							}
							assertNotNull((aut = s.getAnnotation("toolbox::ll")));
							assertThat(aut.getValue_STEXT(), is("uref two-with"));
							assertNotNull((aut = s.getAnnotation("toolbox::ref")));
							assertThat(aut.getValue_STEXT(), is(s.getName()));
							assertNotNull((aut = s.getAnnotation("toolbox::met")));
							assertThat(aut.getValue_STEXT(), is("Sentence with a single global Subref (morph-level) from m23 to m26 (incl.) on \\ur and \\ur2"));
						}
						break;
					case "Subref sentence schema 2 (undefined global) without mb line":
						if (!s.getName().equals("subref")) {
							assertEquals(3, s.getAnnotations().size());
							for (SAnnotation a : s.getAnnotations()) {
								assertThat(a.getName(), is(anyOf(is("ll"), is("ref"), is("met"))));
							}
							assertNotNull((aut = s.getAnnotation("toolbox::ll")));
							assertThat(aut.getValue_STEXT(), is("uref two-without"));
							assertNotNull((aut = s.getAnnotation("toolbox::ref")));
							assertThat(aut.getValue_STEXT(), is(s.getName()));
							assertNotNull((aut = s.getAnnotation("toolbox::met")));
							assertThat(aut.getValue_STEXT(), is("Sentence with a single global Subref (lex-level) from t24 to t27 (incl.) on \\ur and \\ur2"));
						}
						break;
					case "Subref sentence schema 3 (defined global) to mb":
						if (!s.getName().equals("subref")) {
							assertEquals(3, s.getAnnotations().size());
							for (SAnnotation a : s.getAnnotations()) {
								assertThat(a.getName(), is(anyOf(is("ll"), is("ref"), is("met"))));
							}
							assertNotNull((aut = s.getAnnotation("toolbox::ll")));
							assertThat(aut.getValue_STEXT(), is("uref three-mb"));
							assertNotNull((aut = s.getAnnotation("toolbox::ref")));
							assertThat(aut.getValue_STEXT(), is(s.getName()));
							assertNotNull((aut = s.getAnnotation("toolbox::met")));
							assertThat(aut.getValue_STEXT(), is("Sentence with two global Subrefs (morph-level) m26-m28 and m30-m31 on \\ur"));
						}
						break;
					case "Subref sentence schema 3 (defined global) to tx":
						if (!s.getName().equals("subref")) {
							assertEquals(3, s.getAnnotations().size());
							for (SAnnotation a : s.getAnnotations()) {
								assertThat(a.getName(), is(anyOf(is("ll"), is("ref"), is("met"))));
							}
							assertNotNull((aut = s.getAnnotation("toolbox::ll")));
							assertThat(aut.getValue_STEXT(), is("uref three-tx"));
							assertNotNull((aut = s.getAnnotation("toolbox::ref")));
							assertThat(aut.getValue_STEXT(), is(s.getName()));
							assertNotNull((aut = s.getAnnotation("toolbox::met")));
							assertThat(aut.getValue_STEXT(), is("Sentence with two global Subrefs (lex-level) t38-t40 and t43-t44 on \\ur"));
						}
						break;
					case "Subref sentence schema 4 (defined global with related line)":
						if (!s.getName().equals("subref")) {
							assertEquals(3, s.getAnnotations().size());
							for (SAnnotation a : s.getAnnotations()) {
								assertThat(a.getName(), is(anyOf(is("ll"), is("ref"), is("met"))));
							}
							assertNotNull((aut = s.getAnnotation("toolbox::ll")));
							assertThat(aut.getValue_STEXT(), is("uref four"));
							assertNotNull((aut = s.getAnnotation("toolbox::ref")));
							assertThat(aut.getValue_STEXT(), is(s.getName()));
							assertNotNull((aut = s.getAnnotation("toolbox::met")));
							assertThat(aut.getValue_STEXT(), is("Sentence with two global Subrefs (one lex-level, one morph-level) m26-m28 and m30-m31 on \\ur"));
						}
						break;

					default:
						break;
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
				assertThat(graph.getSpans().size(), is(18));
				int subrefCheck = 0;
				for (SSpan span : graph.getSpans()) {
					if (span.getName().equals("Reference no. 1")) {
						assertEquals(9, graph.getOverlappedTokens(span).size());
						for (SToken t : graph.getOverlappedTokens(span)) {
							assertThat(graph.getText(t),
									anyOf(is("Word1"), is("-Tuple"), is("Tuple-"), is("Word2"), is("Triple-="),
											is("Word3"), is(""), is("FreedashTuple"), is("Word4"),
											is("FreecliticTuple")));
						}
					}
					else if (span.getName().equals("Subref sentence schema 1 (line-level) to mb")) {
						assertEquals(3, graph.getOverlappedTokens(span).size());
						for (SToken t : graph.getOverlappedTokens(span)) {
							assertThat(graph.getText(t),
									anyOf(is("Subref"), is("sentence"), is("one")));
						}
					}
					else if (span.getName().equals("Subref sentence schema 1 (line-level) to tx")) {
						assertEquals(3, graph.getOverlappedTokens(span).size());
						for (SToken t : graph.getOverlappedTokens(span)) {
							assertThat(graph.getText(t),
									anyOf(is("Subref"), is("sentence"), is("one")));
						}
					}
					else if (span.getName().equals("Subref sentence schema 2 (undefined global) with existing mb line")) {
						assertEquals(6, graph.getOverlappedTokens(span).size());
						for (SToken t : graph.getOverlappedTokens(span)) {
							assertThat(graph.getText(t),
									anyOf(is("Subref"), is("sentence"), is("two"), is("with"), is("one-to-four"), is("ref")));
						}
					}
					else if (span.getName().equals("Subref sentence schema 2 (undefined global) without mb line")) {
						assertEquals(6, graph.getOverlappedTokens(span).size());
						for (SToken t : graph.getOverlappedTokens(span)) {
							assertThat(graph.getText(t),
									anyOf(is("Subref"), is("sentence"), is("two"), is("with"), is("one-to-four"), is("ref")));
						}
					}
					else if (span.getName().equals("Subref sentence schema 3 (defined global) to mb")) {
						assertEquals(8, graph.getOverlappedTokens(span).size());
						for (SToken t : graph.getOverlappedTokens(span)) {
							assertThat(graph.getText(t),
									anyOf(is("Subref"), is("with"), is("some"), is("random"), is("text"), is("just"), is("like"), is("that")));
						}
					}
					else if (span.getName().equals("Subref sentence schema 3 (defined global) to tx")) {
						assertEquals(8, graph.getOverlappedTokens(span).size());
						for (SToken t : graph.getOverlappedTokens(span)) {
							assertThat(graph.getText(t),
									anyOf(is("Subref"), is("with"), is("some"), is("random"), is("text"), is("just"), is("like"), is("that")));
						}
					}
					else if (span.getName().equals("Subref sentence schema 4 (defined global with related line)")) {
						assertEquals(8, graph.getOverlappedTokens(span).size());
						for (SToken t : graph.getOverlappedTokens(span)) {
							assertThat(graph.getText(t),
									anyOf(is("Subref"), is("with"), is("some"), is("random"), is("text"), is("just"), is("like"), is("that")));
						}
					}
					// Subrefs
					else if (span.getName().equals("subref")) {
						for (SAnnotation a : span.getAnnotations()) {
							if (a.getValue_STEXT().equals("Subref m29-m31")) {
								subrefCheck++;
								assertEquals(3, graph.getOverlappedTokens(span).size());
								for (SToken t : graph.getOverlappedTokens(span)) {
									assertThat(graph.getText(t),
											anyOf(is("m30"), is("m31"), is("m29")));
								}
							}
							else if (a.getValue_STEXT().equals("Subref m33-m34")) {
								subrefCheck++;
								assertEquals(2, graph.getOverlappedTokens(span).size());
								for (SToken t : graph.getOverlappedTokens(span)) {
									assertThat(graph.getText(t),
											anyOf(is("m33"), is("m34")));
								}
							}
							else if (a.getValue_STEXT().equals("Subref t38-t40")) {
								subrefCheck++;
								assertEquals(3, graph.getOverlappedTokens(span).size());
								for (SToken t : graph.getOverlappedTokens(span)) {
									assertThat(graph.getText(t),
											anyOf(is("with"), is("some"), is("random")));
								}
							}
							else if (a.getValue_STEXT().equals("Subref t42-t43")) {
								subrefCheck++;
								assertEquals(2, graph.getOverlappedTokens(span).size());
								for (SToken t : graph.getOverlappedTokens(span)) {
									assertThat(graph.getText(t),
											anyOf(is("just"), is("like")));
								}
							}
							else if (a.getValue_STEXT().equals("Subref t46-t48")) {
								subrefCheck++;
								assertEquals(3, graph.getOverlappedTokens(span).size());
								for (SToken t : graph.getOverlappedTokens(span)) {
									assertThat(graph.getText(t),
											anyOf(is("with"), is("some"), is("random")));
								}
							}
							else if (a.getValue_STEXT().equals("Subref m41-m42")) {
								subrefCheck++;
								assertEquals(2, graph.getOverlappedTokens(span).size());
								for (SToken t : graph.getOverlappedTokens(span)) {
									assertThat(graph.getText(t),
											anyOf(is("m41"), is("m42")));
								}
							}
							else if (a.getValue_STEXT().equals("Subref to morphemes m17 and m18 in a span")) {
								subrefCheck++;
								assertEquals(2, graph.getOverlappedTokens(span).size());
								for (SToken t : graph.getOverlappedTokens(span)) {
									assertThat(graph.getText(t),
											anyOf(is("m17"), is("m18"), is("m29")));
								}
							}
							else if (a.getValue_STEXT().equals("Subref to lexicals t15 and t16 in a span")) {
								subrefCheck++;
								assertEquals(2, graph.getOverlappedTokens(span).size());
								for (SToken t : graph.getOverlappedTokens(span)) {
									assertThat(graph.getText(t),
											anyOf(is("sentence"), is("one")));
								}
							}
							else if (a.getValue_STEXT().equals("Subref to morphemes m23-m26 in a span")) {
								subrefCheck++;
								assertEquals(4, graph.getOverlappedTokens(span).size());
								for (SToken t : graph.getOverlappedTokens(span)) {
									assertThat(graph.getText(t),
											anyOf(is("m23"), is("m24"), is("m25"), is("m26")));
								}
							}
							else if (a.getValue_STEXT().equals("Subref to lexicals \"sentence\"-\"one-to-four\" in a span")) {
								subrefCheck++;
								assertEquals(4, graph.getOverlappedTokens(span).size());
								for (SToken t : graph.getOverlappedTokens(span)) {
									assertThat(graph.getText(t),
											anyOf(is("sentence"), is("two"), is("with"), is("one-to-four")));
								}
							}
						}
					}
				}
				assertThat(subrefCheck, is(10));
				break;

			case "2":
			case "3":
			case "4":
				assertThat(graph.getSpans().size(), is(2));
				break;

			case "5":
				assertThat(graph.getSpans().size(), is(1));
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
		checkLog("orphan-ids-and-refs.txt: Found \\refs that do not belong to any \\ids! Those will not be processed.", Level.WARN);
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
	 * Tests against a real data example.
	 */
	@Test
	public void testParseNoTxLineReal() {
		setTestFile("no-tx-line-real.txt");
		setProperties("no-tx-line-real.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocumentGraph graph = getGraph("ID1");
		assertEquals(1, graph.getTextualDSs().size());
		assertThat(graph.getTextualDSs().get(0).getText(), is (""));
		assertThat(graph.getTokens().size(), is(0));
		assertThat(graph.getSpans().size(), is(0));
		assertThat(graph.getRelations().size(), is(0));
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
		SDocumentGraph graph = getGraph("Document_no__1");
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
	 * Tests against a minimum example, where there is an annotation line just containing a marker, and no annotation.
	 */
	@Test
	public void testLinesWithoutAnnotation() {
		setTestFile("lines-without-annotation.txt");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocumentGraph graph = getGraph("Document_no__1");
		SAnnotation geAnno = graph.getAnnotation("toolbox::ge");
		assertNull((geAnno));
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
		assertThat(graph.getNodesByName("unitref").size(), is(1));
		SNode subref = graph.getNodesByName("unitref").get(0);
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
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#SIMPLE}.
	 */
	@Test
	public void testSubRefSIMPLE_NO_ANNOTATION_VALUE() {
		rootLogger.setLevel(Level.DEBUG);
		setTestFile("subref_SIMPLE_NO_ANNOTATION_VALUE.txt");
		setProperties("subref_SIMPLE.properties");
		start();
		checkLog("No value for annotation with key \"sr\" in document 'Document_no__1', reference 'subref sentence schema 1 (line-level) with no defined target line'. Ignoring ...", Level.DEBUG);
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
		List<SNode> subrefNodes = graph.getNodesByName("unitref");
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
	public void testSubRefSIMPLE_TARGETED_MB_INDEX_EXCEEDED() {
		setTestFile("subref_SIMPLE_TARGETED_INDEX_EXCEEDED.txt");
		setProperties("subref_SIMPLE_TARGETED.properties");
		start();
		checkLog("The maximum of subref range 2..4 in document 'Document_no__1', reference 'subref sentence schema 1 (line-level) to mb' is larger than the highest token index. Please fix source data! Ignoring this annotation ...", Level.WARN);
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
		assertThat(graph.getNodesByName("unitref").size(), is(1));
		SNode srNode = graph.getNodesByName("unitref").get(0);
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
	public void testSubRefUNIDENTIFIED_GLOBAL_MB_INDEX_EXCEEDED() {
		setTestFile("subref_UNIDENTIFIED_GLOBAL_INDEX_EXCEEDED.txt");
		setProperties("subref_UNIDENTIFIED_GLOBAL.properties");
		start();
		checkLog("Document 'Document_no__1', reference 'subref sentence schema 2 (undefined global) with existing mb line': The indices defined in the global subdef are outside of the index range of the target tokens. Please fix the source data! Ignoring this subref ...", Level.WARN);
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#UNIDENTIFIED_GLOBAL},
	 * with several other types mixed in (which would be overriden by the global definition).
	 */
	@Test
	public void testSubRefUNIDENTIFIED_GLOBAL_MB_MIXED() {
		setTestFile("subref_UNIDENTIFIED_GLOBAL_mixed.txt");
		setProperties("subref_UNIDENTIFIED_GLOBAL.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(12));
		// All but the global definition is ignored, hence only 2 spans
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
		assertThat(srNode.getAnnotation("toolbox::sr").getValue_STEXT(), is("UNIDENTIFIED_GLOBAL m23-m26"));
		assertThat(srNode.getAnnotation("toolbox::sr2").getValue_STEXT(), is("UNIDENTIFIED_GLOBAL m23-m26"));
		assertThat(srNode.getLayers().size(), is(1));
		assertThat(srNode.getLayers().iterator().next().getName(), is("mb"));
		checkLog("Illegal subref definition in ref 'subref sentence schema 2 (undefined global) with existing mb line and a mixture of other subref definitions', document 'Document_no__1'!\nThere can only be exactly one unidentified global subref definition per ref! Cancelling definition overwrite.", Level.WARN);
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
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#UNIDENTIFIED_GLOBAL_TARGETED},
	 * with several other types mixed in (which would be overriden by the global definition).
	 */
	@Test
	public void testSubRefUNIDENTIFIED_GLOBAL_TARGETED_TX_MIXED() {
		setTestFile("subref_UNIDENTIFIED_GLOBAL_TARGETED_mixed.txt");
		setProperties("subref_UNIDENTIFIED_GLOBAL.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(10));
		// All but the global definition is ignored, hence only 2 spans
		assertThat(graph.getSpans().size(), is(2));
		assertThat(graph.getNodesByName("subref").size(), is(1));
		SNode srNode = graph.getNodesByName("subref").get(0);
		assertThat(srNode, instanceOf(SSpan.class));
		assertThat(graph.getOverlappedTokens(srNode).size(), is(4));
		for (SToken tok : graph.getOverlappedTokens(srNode)) {
			assertThat(graph.getText(tok), anyOf(is("L2"), is("L3"), is("L4"), is("L5")));
		}
		assertThat(srNode.getAnnotations().size(), is(2));
		assertNotNull(srNode.getAnnotation("toolbox::sr"));
		assertNotNull(srNode.getAnnotation("toolbox::sr2"));
		assertThat(srNode.getAnnotation("toolbox::sr").getValue_STEXT(), is("UNIDENTIFIED_GLOBAL_TARGETED_TX_mixed L2-L5"));
		assertThat(srNode.getAnnotation("toolbox::sr2").getValue_STEXT(), is("UNIDENTIFIED_GLOBAL_TARGETED_TX_mixed L2-L5"));
		assertThat(srNode.getLayers().size(), is(1));
		assertThat(srNode.getLayers().iterator().next().getName(), is("tx"));
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#IDENTIFIED_GLOBAL}.
	 */
	@Test
	public void testSubRefIDENTIFIED_GLOBAL_MB() {
		setTestFile("subref_IDENTIFIED_GLOBAL.txt");
		setProperties("subref_UNIDENTIFIED_GLOBAL.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(16));
		assertThat(graph.getSpans().size(), is(3));
		List<SNode> subrefNodes = graph.getNodesByName("subref");
		assertThat(subrefNodes.size(), is(2));
		SNode srNode1 = null, srNode2 = null;
		for (SNode subref : subrefNodes) {
			for (SAnnotation anno : subref.getAnnotations()) {
				if (anno.getQName().equals("toolbox::sr") && anno.getValue_STEXT().equals("IDENTIFIED_GLOBAL m29-m31")) {
					srNode1 = subref;
				}
				else if (anno.getQName().equals("toolbox::sr") && anno.getValue_STEXT().equals("IDENTIFIED_GLOBAL m33-m34")) {
					srNode2 = subref;
				}
			}
		}
		assertNotNull(srNode1);
		assertNotNull(srNode2);
		assertThat(srNode1 == srNode2, is(false));
		assertThat(srNode1, instanceOf(SSpan.class));
		assertThat(srNode2, instanceOf(SSpan.class));
		assertThat(graph.getOverlappedTokens(srNode1).size(), is(3));
		assertThat(graph.getOverlappedTokens(srNode2).size(), is(2));
		for (SToken tok : graph.getOverlappedTokens(srNode1)) {
			assertThat(graph.getText(tok), anyOf(is("m29"), is("m30"), is("m31")));
		}
		for (SToken tok : graph.getOverlappedTokens(srNode2)) {
			assertThat(graph.getText(tok), anyOf(is("m33"), is("m34")));
		}
		assertThat(srNode1.getAnnotations().size(), is(1));
		assertThat(srNode2.getAnnotations().size(), is(1));
		assertThat(srNode1.getAnnotations().iterator().next().getQName(), is("toolbox::sr"));
		assertThat(srNode1.getAnnotation("toolbox::sr").getValue_STEXT(), is("IDENTIFIED_GLOBAL m29-m31"));
		assertThat(srNode2.getAnnotations().iterator().next().getQName(), is("toolbox::sr"));
		assertThat(srNode2.getAnnotation("toolbox::sr").getValue_STEXT(), is("IDENTIFIED_GLOBAL m33-m34"));
		assertThat(srNode1.getLayers().size(), is(1));
		assertThat(srNode1.getLayers().iterator().next().getName(), is("mb"));
		assertThat(srNode2.getLayers().size(), is(1));
		assertThat(srNode2.getLayers().iterator().next().getName(), is("mb"));
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#IDENTIFIED_GLOBAL}.
	 */
	@Test
	public void testSubRefIDENTIFIED_GLOBAL_MB_DUPLICATE_ANNO() {
		setTestFile("subref_IDENTIFIED_GLOBAL_DUPLICATE_ANNOTATION.txt");
		setProperties("subref_UNIDENTIFIED_GLOBAL.properties");
		start();
		checkLog("Duplicate annotation in 'Document_no__1'-'subref sentence schema 3 (defined global) to mb'! There already exists an annotation with the key \"sr\". This might be an error in the source data. If it is not, please file a bug report.", Level.WARN);
	}
	
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#IDENTIFIED_GLOBAL}.
	 */
	@Test
	public void testSubRefIDENTIFIED_GLOBAL_TX() {
		setTestFile("subref_IDENTIFIED_GLOBAL_tx.txt");
		setProperties("subref_UNIDENTIFIED_GLOBAL.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(8));
		assertThat(graph.getSpans().size(), is(3));
		List<SNode> subrefNodes = graph.getNodesByName("subref");
		assertThat(subrefNodes.size(), is(2));
		SNode srNode1 = null, srNode2 = null;
		for (SNode subref : subrefNodes) {
			for (SAnnotation anno : subref.getAnnotations()) {
				if (anno.getQName().equals("toolbox::sr") && anno.getValue_STEXT().equals("IDENTIFIED_GLOBAL L1-L3")) {
					srNode1 = subref;
				}
				else if (anno.getQName().equals("toolbox::sr") && anno.getValue_STEXT().equals("IDENTIFIED_GLOBAL L5-L6")) {
					srNode2 = subref;
				}
			}
		}
		assertNotNull(srNode1);
		assertNotNull(srNode2);
		assertThat(srNode1 == srNode2, is(false));
		assertThat(srNode1, instanceOf(SSpan.class));
		assertThat(srNode2, instanceOf(SSpan.class));
		assertThat(graph.getOverlappedTokens(srNode1).size(), is(3));
		assertThat(graph.getOverlappedTokens(srNode2).size(), is(2));
		for (SToken tok : graph.getOverlappedTokens(srNode1)) {
			assertThat(graph.getText(tok), anyOf(is("L1"), is("L2"), is("L3")));
		}
		for (SToken tok : graph.getOverlappedTokens(srNode2)) {
			assertThat(graph.getText(tok), anyOf(is("L5"), is("L6")));
		}
		assertThat(srNode1.getAnnotations().size(), is(1));
		assertThat(srNode2.getAnnotations().size(), is(1));
		assertThat(srNode1.getAnnotations().iterator().next().getQName(), is("toolbox::sr"));
		assertThat(srNode1.getAnnotation("toolbox::sr").getValue_STEXT(), is("IDENTIFIED_GLOBAL L1-L3"));
		assertThat(srNode2.getAnnotations().iterator().next().getQName(), is("toolbox::sr"));
		assertThat(srNode2.getAnnotation("toolbox::sr").getValue_STEXT(), is("IDENTIFIED_GLOBAL L5-L6"));
		assertThat(srNode1.getLayers().size(), is(1));
		assertThat(srNode1.getLayers().iterator().next().getName(), is("tx"));
		assertThat(srNode2.getLayers().size(), is(1));
		assertThat(srNode2.getLayers().iterator().next().getName(), is("tx"));
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#IDENTIFIED_GLOBAL_TARGETED}.
	 */
	@Test
	public void testSubRefIDENTIFIED_GLOBAL_TARGETED_MB() {
		setTestFile("subref_IDENTIFIED_GLOBAL_TARGETED.txt");
		setProperties("subref_UNIDENTIFIED_GLOBAL.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(16));
		assertThat(graph.getSpans().size(), is(4));
		List<SNode> subrefNodes = graph.getNodesByName("subref");
		assertThat(subrefNodes.size(), is(3));
		SNode subref1 = null, subref2 = null, subref3 = null;
		for (SNode subref : subrefNodes) {
			for (SAnnotation anno : subref.getAnnotations()) {
				if (anno.getQName().equals("toolbox::sr")) {
					if (anno.getValue_STEXT().equals("IDENTIFIED_GLOBAL_TARGETED L1-L3")) {
						subref1 = subref;
					}
					else if (anno.getValue_STEXT().equals("IDENTIFIED_GLOBAL_TARGETED m5-m6")) {
						subref2 = subref;
					}
					else if (anno.getValue_STEXT().equals("IDENTIFIED_GLOBAL_TARGETED m1-m3")) {
						subref3 = subref;
					}
				}
			}
		}
		assertNotNull(subref1);
		assertNotNull(subref2);
		assertNotNull(subref3);
		assertThat(subref1 == subref2 || subref1 == subref3 || subref2 == subref3, is(false));
		assertThat(subref1, instanceOf(SSpan.class));
		assertThat(subref2, instanceOf(SSpan.class));
		assertThat(subref3, instanceOf(SSpan.class));
		assertThat(graph.getOverlappedTokens(subref1).size(), is(3));
		assertThat(graph.getOverlappedTokens(subref2).size(), is(2));
		assertThat(graph.getOverlappedTokens(subref3).size(), is(3));
		for (SToken tok : graph.getOverlappedTokens(subref1)) {
			assertThat(graph.getText(tok), anyOf(is("L1"), is("L2"), is("L3")));
		}
		for (SToken tok : graph.getOverlappedTokens(subref2)) {
			assertThat(graph.getText(tok), anyOf(is("m5"), is("m6")));
		}
		for (SToken tok : graph.getOverlappedTokens(subref3)) {
			assertThat(graph.getText(tok), anyOf(is("m1"), is("m2"), is("m3")));
		}
		assertThat(subref1.getAnnotations().size(), is(1));
		assertThat(subref2.getAnnotations().size(), is(1));
		assertThat(subref3.getAnnotations().size(), is(1));
		assertThat(subref1.getAnnotations().iterator().next().getQName(), is("toolbox::sr"));
		assertThat(subref2.getAnnotations().iterator().next().getQName(), is("toolbox::sr"));
		assertThat(subref3.getAnnotations().iterator().next().getQName(), is("toolbox::sr"));
		assertThat(subref1.getAnnotation("toolbox::sr").getValue_STEXT(), is("IDENTIFIED_GLOBAL_TARGETED L1-L3"));
		assertThat(subref2.getAnnotation("toolbox::sr").getValue_STEXT(), is("IDENTIFIED_GLOBAL_TARGETED m5-m6"));
		assertThat(subref3.getAnnotation("toolbox::sr").getValue_STEXT(), is("IDENTIFIED_GLOBAL_TARGETED m1-m3"));
		assertThat(subref1.getLayers().size(), is(1));
		assertThat(subref2.getLayers().size(), is(1));
		assertThat(subref3.getLayers().size(), is(1));
		assertThat(subref1.getLayers().iterator().next().getName(), is("tx"));
		assertThat(subref2.getLayers().iterator().next().getName(), is("mb"));
		assertThat(subref3.getLayers().iterator().next().getName(), is("mb"));		
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#DISCONTINUOUS_TARGETED}.
	 */
	@Test
	public void testSubRefDISCONTINUOUS_TARGETED_MB() {
		setTestFile("subref_DISCONTINUOUS_TARGETED.txt");
		setProperties("subref_UNIDENTIFIED_GLOBAL.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(16));
		assertThat(graph.getSpans().size(), is(2));
		List<SNode> subrefNodes = graph.getNodesByName("subref");
		assertThat(subrefNodes.size(), is(1));
		SNode subref = subrefNodes.get(0);
		assertNotNull(subref);
		assertThat(subref, instanceOf(SSpan.class));
		assertThat(graph.getOverlappedTokens(subref).size(), is(5));
		for (SToken tok : graph.getOverlappedTokens(subref)) {
			assertThat(graph.getText(tok), anyOf(is("m37"), is("m38"), is("m39"), is("m41"), is("m42")));
		}
		assertThat(subref.getAnnotations().size(), is(1));
		assertThat(subref.getAnnotations().iterator().next().getQName(), is("toolbox::sr"));
		assertThat(subref.getAnnotation("toolbox::sr").getValue_STEXT(), is("subref m37-m39 & m41-m42"));
		assertThat(subref.getLayers().size(), is(1));
		assertThat(subref.getLayers().iterator().next().getName(), is("mb"));
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#DISCONTINUOUS_TARGETED}.
	 */
	@Test
	public void testSubRefDISCONTINUOUS_TARGETED_TX() {
		setTestFile("subref_DISCONTINUOUS_TARGETED_tx.txt");
		setProperties("subref_UNIDENTIFIED_GLOBAL.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(8));
		assertThat(graph.getSpans().size(), is(2));
		List<SNode> subrefNodes = graph.getNodesByName("subref");
		assertThat(subrefNodes.size(), is(1));
		SNode subref = subrefNodes.get(0);
		assertNotNull(subref);
		assertThat(subref, instanceOf(SSpan.class));
		assertThat(graph.getOverlappedTokens(subref).size(), is(5));
		for (SToken tok : graph.getOverlappedTokens(subref)) {
			assertThat(graph.getText(tok), anyOf(is("L2"), is("L3"), is("L5"), is("L6"), is("L7")));
		}
		assertThat(subref.getAnnotations().size(), is(1));
		assertThat(subref.getAnnotations().iterator().next().getQName(), is("toolbox::sr"));
		assertThat(subref.getAnnotation("toolbox::sr").getValue_STEXT(), is("DISCONTINUOUS_TARGETED L2-L3 & L5-L7"));
		assertThat(subref.getLayers().size(), is(1));
		assertThat(subref.getLayers().iterator().next().getName(), is("tx"));
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#NON_SUBREF}.
	 */
	@Test
	public void testSubRefNON_SUBREF() {
		setTestFile("subref_NON_SUBREF.txt");
		setProperties("subref_UNIDENTIFIED_GLOBAL.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(6));
		assertThat(graph.getSpans().size(), is(1));
		assertThat(graph.getNodesByName("fullref").size(), is(1));
		SNode ref = graph.getNodesByName("fullref").get(0);
		assertThat(graph.getOverlappedTokens(ref).size(), is(3));
		for (SToken tok : graph.getOverlappedTokens(ref)) {
			assertThat(graph.getText(tok), anyOf(is("subref"), is("sentence"), is("one")));
		}
		assertThat(ref.getAnnotations().size(), is(3));
		assertNotNull(ref.getAnnotation("toolbox::sr"));
		assertThat(ref.getAnnotation("toolbox::sr").getValue_STEXT(), is("Full-ref annotation"));
		assertThat(ref.getLayers().size(), is(2));
		for (SLayer l : ref.getLayers()) {
			assertThat(l.getName(), anyOf(is("ref"), is("tx")));
		}
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#NON_SUBREF}.
	 */
	@Test
	public void testSubRefNON_SUBREF_LONG_SR() {
		setTestFile("subref_NON_SUBREF_long_sr.txt");
		setProperties("subref_UNIDENTIFIED_GLOBAL.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(6));
		assertThat(graph.getSpans().size(), is(1));
		assertThat(graph.getNodesByName("fullref").size(), is(1));
		SNode ref = graph.getNodesByName("fullref").get(0);
		assertThat(graph.getOverlappedTokens(ref).size(), is(3));
		for (SToken tok : graph.getOverlappedTokens(ref)) {
			assertThat(graph.getText(tok), anyOf(is("subref"), is("sentence"), is("one")));
		}
		assertThat(ref.getAnnotations().size(), is(3));
		assertNotNull(ref.getAnnotation("toolbox::sr"));
		assertThat(ref.getAnnotation("toolbox::sr").getValue_STEXT(), is("Full-ref annotation with an annotation line that is longer than 5 units if split with \\\\s+"));
		assertThat(ref.getLayers().size(), is(2));
		for (SLayer l : ref.getLayers()) {
			assertThat(l.getName(), anyOf(is("ref"), is("tx")));
		}
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#IDENTIFIED_GLOBAL}.
	 */
	@Test
	public void testSubRefIDENTIFIED_GLOBAL_REAL1() {
		setTestFile("subref_IDENTIFIED_GLOBAL_real_1.txt");
		setProperties("subref_IDENTIFIED_GLOBAL_real.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(16));
		assertThat(graph.getSpans().size(), is(3));
		List<SNode> subrefNodes = graph.getNodesByName("unitref");
		assertThat(subrefNodes.size(), is(2));
		SNode srNode1 = null, srNode2 = null;
		for (SNode subref : subrefNodes) {
			for (SAnnotation anno : subref.getAnnotations()) {
				if (anno.getQName().equals("toolbox::clause") && anno.getValue_STEXT().equals("assertion")) {
					srNode1 = subref;
				}
				else if (anno.getQName().equals("toolbox::clause") && anno.getValue_STEXT().equals("proposition")) {
					srNode2 = subref;
				}
			}
		}
		assertNotNull(srNode1);
		assertNotNull(srNode2);
		assertThat(srNode1 == srNode2, is(false));
		assertThat(srNode1, instanceOf(SSpan.class));
		assertThat(srNode2, instanceOf(SSpan.class));
		assertThat(graph.getOverlappedTokens(srNode1).size(), is(3));
		assertThat(graph.getOverlappedTokens(srNode2).size(), is(6));
		for (SToken tok : graph.getOverlappedTokens(srNode1)) {
			assertThat(graph.getText(tok), anyOf(is("m0"), is("m1"), is("m2")));
		}
		for (SToken tok : graph.getOverlappedTokens(srNode2)) {
			assertThat(graph.getText(tok), anyOf(is("m3"), is("-m4"), is("m5"), is("m6"), is("-m7"), is("m8")));
		}
		assertThat(srNode1.getAnnotations().size(), is(1));
		assertThat(srNode2.getAnnotations().size(), is(1));
		assertThat(srNode1.getAnnotations().iterator().next().getQName(), is("toolbox::clause"));
		assertThat(srNode1.getAnnotation("toolbox::clause").getValue_STEXT(), is("assertion"));
		assertThat(srNode2.getAnnotations().iterator().next().getQName(), is("toolbox::clause"));
		assertThat(srNode2.getAnnotation("toolbox::clause").getValue_STEXT(), is("proposition"));
		assertThat(srNode1.getLayers().size(), is(1));
		assertThat(srNode1.getLayers().iterator().next().getName(), is("mb"));
		assertThat(srNode2.getLayers().size(), is(1));
		assertThat(srNode2.getLayers().iterator().next().getName(), is("mb"));
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#IDENTIFIED_GLOBAL}.
	 */
	@Test
	public void testSubRefIDENTIFIED_GLOBAL_REAL2() {
		setTestFile("subref_IDENTIFIED_GLOBAL_real_2.txt");
		setProperties("subref_IDENTIFIED_GLOBAL_real.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(40));
		assertThat(graph.getSpans().size(), is(2));
		List<SNode> subrefNodes = graph.getNodesByName("unitref");
		assertThat(subrefNodes.size(), is(1));
		SNode subref = null;
		for (SNode sr : subrefNodes) {
			for (SAnnotation anno : sr.getAnnotations()) {
				if (anno.getQName().equals("toolbox::clause") && anno.getValue_STEXT().equals("assertion")) {
					subref = sr;
				}
			}
		}
		assertNotNull(subref);
		assertThat(subref, instanceOf(SSpan.class));
		assertThat(graph.getOverlappedTokens(subref).size(), is(5));
		for (SToken tok : graph.getOverlappedTokens(subref)) {
			assertThat(graph.getText(tok), anyOf(is("m16-"), is("m17"), is("m18"), is("m19"), is("m20")));
		}
		assertThat(subref.getAnnotations().size(), is(1));
		assertThat(subref.getAnnotations().iterator().next().getQName(), is("toolbox::clause"));
		assertThat(subref.getAnnotation("toolbox::clause").getValue_STEXT(), is("assertion"));
		assertThat(subref.getLayers().size(), is(1));
		assertThat(subref.getLayers().iterator().next().getName(), is("mb"));
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#IDENTIFIED_GLOBAL}.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSubRefIDENTIFIED_GLOBAL_REAL3() {
		setTestFile("subref_IDENTIFIED_GLOBAL_real_3.txt");
		setProperties("subref_IDENTIFIED_GLOBAL_real.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getTokens().size(), is(greaterThan(0)));
		assertThat(graph.getTokens().size(), is(24));
		assertThat(graph.getSpans().size(), is(2));
		List<SNode> subrefNodes = graph.getNodesByName("unitref");
		assertThat(subrefNodes.size(), is(1));
		SNode subref = null;
		for (SNode sr : subrefNodes) {
			for (SAnnotation anno : sr.getAnnotations()) {
				if (anno.getQName().equals("toolbox::clause") && anno.getValue_STEXT().equals("proposition")) {
					subref = sr;
				}
			}
		}
		assertNotNull(subref);
		assertThat(subref, instanceOf(SSpan.class));
		assertThat(graph.getOverlappedTokens(subref).size(), is(11));
		for (SToken tok : graph.getOverlappedTokens(subref)) {
			assertThat(graph.getText(tok), anyOf(is("m2"), is("m3"), is("m4"), is("m5"), is("m6-"), is("m7"), is("m8"), is("m9"), is("m10"), is("-m11"), is("m12")));
		}
		assertThat(subref.getAnnotations().size(), is(1));
		assertThat(subref.getAnnotations().iterator().next().getQName(), is("toolbox::clause"));
		assertThat(subref.getAnnotation("toolbox::clause").getValue_STEXT(), is("proposition"));
		assertThat(subref.getLayers().size(), is(1));
		assertThat(subref.getLayers().iterator().next().getName(), is("mb"));
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#IDENTIFIED_GLOBAL}
	 * which is bound to fail due to an `to` index which is > token index.
	 */
	@Test(expected=AssertionError.class)
	public void testSubRefIDENTIFIED_GLOBAL_REAL1_FAIL() {
		setTestFile("subref_IDENTIFIED_GLOBAL_real_1_fail.txt");
		setProperties("subref_IDENTIFIED_GLOBAL_real.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getSpans().size(), is(3));
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#IDENTIFIED_GLOBAL}
	 * which is bound to fail due to an `to` index which is > token index.
	 */
	@Test(expected=AssertionError.class)
	public void testSubRefIDENTIFIED_GLOBAL_REAL2_FAIL() {
		setTestFile("subref_IDENTIFIED_GLOBAL_real_2_fail.txt");
		setProperties("subref_IDENTIFIED_GLOBAL_real.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getSpans().size(), is(2));
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a subref of type {@link org.corpus_tools.peppermodules.toolbox.text.mapping.SubrefMapper.SUBREF_TYPE#IDENTIFIED_GLOBAL}
	 * which is bound to fail due to an `to` index which is > token index.
	 */
	@Test(expected=AssertionError.class)
	public void testSubRefIDENTIFIED_GLOBAL_REAL3_FAIL() {
		setTestFile("subref_IDENTIFIED_GLOBAL_real_3_fail.txt");
		setProperties("subref_IDENTIFIED_GLOBAL_real.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getSpans().size(), is(2));
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
		setProperties("duplicate-markers.properties");
		start();
		SDocumentGraph graph = getNonEmptyCorpusGraph().getDocuments().get(0).getDocumentGraph();
		assertEquals(1, graph.getDocument().getMetaAnnotations().size());
		assertEquals("Some more info about document no. 4 Duplicate 1 Duplicate 2", graph.getDocument().getMetaAnnotation("toolbox::moreidinfo").getValue_STEXT());
		assertEquals("A sentence another sentence", graph.getTextualDSs().get(0).getText());
		assertEquals("m1m2m3m4", graph.getTextualDSs().get(1).getText());
		for (SNode node : graph.getLayerByName("tx").get(0).getNodes()) {
			if (node instanceof SToken) {
				assertEquals(1, node.getAnnotations().size());
				assertThat(node.getAnnotation("toolbox::ta").getValue_STEXT(), anyOf(is("A"), is("word"), is("another")));
			}
		}
		for (SNode node : graph.getLayerByName("mb").get(0).getNodes()) {
			if (!(node instanceof STextualDS)) {
				assertEquals(1, node.getAnnotations().size());
				assertThat(node.getAnnotation("toolbox::ge").getValue_STEXT(), anyOf(is("M1"), is("M2"), is("M3"), is("M4")));
			}
		}
		SSpan refSpan = null;
		assertNotNull(refSpan = graph.getSpans().get(0));
		SAnnotation subrefAnno = null; 
		assertNotNull(subrefAnno = refSpan.getAnnotation("toolbox::nt"));
		assertThat(subrefAnno.getValue_STEXT(), is("This is the first part of a note, and this is the second."));
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where the markers should be normalized back to defaults
	 */
	@Test
	public void testNormalizeMarkersTrue() {
		setTestFile("normalized-markers.txt");
		setProperties("normalized-markers.properties");
		start();
		SDocumentGraph graph = getNonEmptyCorpusGraph().getDocuments().get(0).getDocumentGraph();
		assertThat(graph.getTextualDSs().size(), is(2));
		boolean containsDS = false;
		for (SNode n : graph.getLayerByName("tx").get(0).getNodes()) {
			if (n instanceof STextualDS) {
				containsDS = true;
				assertThat(((STextualDS) n).getText(), is ("One Two Three Four"));
			}
		}
		assertThat(containsDS, is(true));
		containsDS = false;
		for (SNode n : graph.getLayerByName("mb").get(0).getNodes()) {
			if (n instanceof STextualDS) {
				containsDS = true;
				assertThat(((STextualDS) n).getText(), is ("m1-m2m3m4-m5m6"));
			}
		}
		for (SToken t : graph.getTokens()) {
			assertThat(t.getLayers().size(), is(1));
			assertThat(t.getLayers().iterator().next().getName(), anyOf(is("tx"), is("mb")));
		}
		assertTrue(containsDS);
		assertThat(graph.getDocument().getName(), is("Document_no__1"));
		assertThat(graph.getSpans().size(), is(2));
		assertThat(graph.getNodesByName("Reference no. 1").get(0).getAnnotation("toolbox::ref").getValue_STEXT(), is("Reference no. 1"));
		assertThat(graph.getNodesByName("subref").get(0).getAnnotation("toolbox::test").getValue_STEXT(), is("Test"));
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where the markers should be normalized back to defaults
	 */
	@Test
	public void testNormalizeMarkersTrueErr() {
		setTestFile("normalized-markers-err.txt");
		setProperties("normalized-markers.properties");
		start();
		SDocumentGraph graph = getNonEmptyCorpusGraph().getDocuments().get(0).getDocumentGraph();
		assertThat(graph.getTextualDSs().size(), is(2));
		boolean containsDS = false;
		for (SNode n : graph.getLayerByName("tx").get(0).getNodes()) {
			if (n instanceof STextualDS) {
				containsDS = true;
				assertThat(((STextualDS) n).getText(), is ("One Two Three Four"));
			}
		}
		assertThat(containsDS, is(true));
		containsDS = false;
		for (SNode n : graph.getLayerByName("mb").get(0).getNodes()) {
			if (n instanceof STextualDS) {
				containsDS = true;
				assertThat(((STextualDS) n).getText(), is ("m1-m2m3m4-m5m6"));
			}
		}
		for (SToken t : graph.getTokens()) {
			assertThat(t.getLayers().size(), is(1));
			assertThat(t.getLayers().iterator().next().getName(), anyOf(is("tx"), is("mb")));
		}
		assertTrue(containsDS);
		assertThat(graph.getDocument().getName(), is("Document_no__1"));
		assertThat(graph.getSpans().size(), is(2));
		assertThat(graph.getNodesByName("Reference no. 1").get(0).getAnnotation("toolbox::ref").getValue_STEXT(), is("Reference no. 1"));
		assertThat(graph.getNodesByName("subref").get(0).getAnnotation("toolbox::test").getValue_STEXT(), is("Test"));
		SSpan ref = (SSpan) graph.getNodesByName("Reference no. 1").get(0);
		assertNotNull(ref.getAnnotation("toolbox::err"));
		assertThat(ref.getAnnotation("toolbox::err").getValue_STEXT(), is("mb-p"));
		assertNotNull(ref.getAnnotation("toolbox::mb-p"));
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where the markers should be normalized back to defaults
	 */
	@Test
	public void testNormalizeMarkersTrueAnnoErr() {
		setTestFile("normalized-markers-annoerr.txt");
		setProperties("normalized-markers.properties");
		start();
		SDocumentGraph graph = getNonEmptyCorpusGraph().getDocuments().get(0).getDocumentGraph();
		assertThat(graph.getTextualDSs().size(), is(2));
		boolean containsDS = false;
		for (SNode n : graph.getLayerByName("tx").get(0).getNodes()) {
			if (n instanceof STextualDS) {
				containsDS = true;
				assertThat(((STextualDS) n).getText(), is ("One Two Three Four"));
			}
		}
		assertThat(containsDS, is(true));
		containsDS = false;
		for (SNode n : graph.getLayerByName("mb").get(0).getNodes()) {
			if (n instanceof STextualDS) {
				containsDS = true;
				assertThat(((STextualDS) n).getText(), is ("m1-m2m3m4-m5m6"));
			}
		}
		for (SToken t : graph.getTokens()) {
			assertThat(t.getLayers().size(), is(1));
			assertThat(t.getLayers().iterator().next().getName(), anyOf(is("tx"), is("mb")));
		}
		assertTrue(containsDS);
		assertThat(graph.getDocument().getName(), is("Document_no__1"));
		assertThat(graph.getSpans().size(), is(2));
		assertThat(graph.getNodesByName("Reference no. 1").get(0).getAnnotation("toolbox::ref").getValue_STEXT(), is("Reference no. 1"));
		assertThat(graph.getNodesByName("subref").get(0).getAnnotation("toolbox::test").getValue_STEXT(), is("Test"));
		SSpan ref = (SSpan) graph.getNodesByName("Reference no. 1").get(0);
		assertNotNull(ref.getAnnotation("toolbox::err"));
		assertThat(ref.getAnnotation("toolbox::err").getValue_STEXT(), is("ge-m"));
		assertNotNull(ref.getAnnotation("toolbox::ge-m"));
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where the markers should *not* be normalized back to defaults
	 */
	@Test
	public void testNormalizeMarkersDefault() {
		setTestFile("unnormalized-markers.txt");
		setProperties("unnormalized-markers.properties");
		start();
		SDocumentGraph graph = getNonEmptyCorpusGraph().getDocuments().get(0).getDocumentGraph();
		assertThat(graph.getTextualDSs().size(), is(2));
		boolean containsDS = false;
		for (SNode n : graph.getLayerByName("xt").get(0).getNodes()) {
			if (n instanceof STextualDS) {
				containsDS = true;
				assertThat(((STextualDS) n).getText(), is ("One Two Three Four"));
			}
		}
		assertThat(containsDS, is(true));
		containsDS = false;
		for (SNode n : graph.getLayerByName("bm").get(0).getNodes()) {
			if (n instanceof STextualDS) {
				containsDS = true;
				assertThat(((STextualDS) n).getText(), is ("m1-m2m3m4-m5m6"));
			}
		}
		for (SToken t : graph.getTokens()) {
			assertThat(t.getLayers().size(), is(1));
			assertThat(t.getLayers().iterator().next().getName(), anyOf(is("xt"), is("bm")));
		}
		assertTrue(containsDS);
		assertThat(graph.getDocument().getName(), is("Document_no__1"));
		assertThat(graph.getSpans().size(), is(2));
		assertThat(graph.getNodesByName("Reference no. 1").get(0).getAnnotation("toolbox::fer").getValue_STEXT(), is("Reference no. 1"));
		assertThat(graph.getNodesByName("customref").get(0).getAnnotation("toolbox::test").getValue_STEXT(), is("Test"));
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where the markers should *not* be normalized back to defaults
	 */
	@Test
	public void testNormalizeMarkersDefaultWithErrors() {
		setTestFile("unnormalized-markers-err.txt");
		setProperties("unnormalized-markers.properties");
		start();
		SDocumentGraph graph = getNonEmptyCorpusGraph().getDocuments().get(0).getDocumentGraph();
		assertThat(graph.getTextualDSs().size(), is(2));
		boolean containsDS = false;
		for (SNode n : graph.getLayerByName("xt").get(0).getNodes()) {
			if (n instanceof STextualDS) {
				containsDS = true;
				assertThat(((STextualDS) n).getText(), is ("One Two Three Four"));
			}
		}
		assertThat(containsDS, is(true));
		containsDS = false;
		for (SNode n : graph.getLayerByName("bm").get(0).getNodes()) {
			if (n instanceof STextualDS) {
				containsDS = true;
				assertThat(((STextualDS) n).getText(), is ("m1-m2m3m4-m5m6"));
			}
		}
		for (SToken t : graph.getTokens()) {
			assertThat(t.getLayers().size(), is(1));
			assertThat(t.getLayers().iterator().next().getName(), anyOf(is("xt"), is("bm")));
		}
		assertTrue(containsDS);
		assertThat(graph.getDocument().getName(), is("Document_no__1"));
		assertThat(graph.getSpans().size(), is(2));
		assertThat(graph.getNodesByName("Reference no. 1").get(0).getAnnotation("toolbox::fer").getValue_STEXT(), is("Reference no. 1"));
		assertThat(graph.getNodesByName("customref").get(0).getAnnotation("toolbox::test").getValue_STEXT(), is("Test"));
		SSpan ref = (SSpan) graph.getNodesByName("Reference no. 1").get(0);
		assertNotNull(ref.getAnnotation("toolbox::err"));
		assertThat(ref.getAnnotation("toolbox::err").getValue_STEXT(), is("bm-p"));
		assertNotNull(ref.getAnnotation("toolbox::bm-p"));
	}

	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where the markers should *not* be normalized back to defaults
	 */
	@Test
	public void testNormalizeMarkersDefaultWithAnnoErrors() {
		setTestFile("unnormalized-markers-annoerr.txt");
		setProperties("unnormalized-markers.properties");
		start();
		SDocumentGraph graph = getNonEmptyCorpusGraph().getDocuments().get(0).getDocumentGraph();
		assertThat(graph.getTextualDSs().size(), is(2));
		boolean containsDS = false;
		for (SNode n : graph.getLayerByName("xt").get(0).getNodes()) {
			if (n instanceof STextualDS) {
				containsDS = true;
				assertThat(((STextualDS) n).getText(), is ("One Two Three Four"));
			}
		}
		assertThat(containsDS, is(true));
		containsDS = false;
		for (SNode n : graph.getLayerByName("bm").get(0).getNodes()) {
			if (n instanceof STextualDS) {
				containsDS = true;
				assertThat(((STextualDS) n).getText(), is ("m1-m2m3m4-m5m6"));
			}
		}
		for (SToken t : graph.getTokens()) {
			assertThat(t.getLayers().size(), is(1));
			assertThat(t.getLayers().iterator().next().getName(), anyOf(is("xt"), is("bm")));
		}
		assertTrue(containsDS);
		assertThat(graph.getDocument().getName(), is("Document_no__1"));
		assertThat(graph.getSpans().size(), is(2));
		assertThat(graph.getNodesByName("Reference no. 1").get(0).getAnnotation("toolbox::fer").getValue_STEXT(), is("Reference no. 1"));
		assertThat(graph.getNodesByName("customref").get(0).getAnnotation("toolbox::test").getValue_STEXT(), is("Test"));
		SSpan ref = (SSpan) graph.getNodesByName("Reference no. 1").get(0);
		assertNotNull(ref.getAnnotation("toolbox::err"));
		assertThat(ref.getAnnotation("toolbox::err").getValue_STEXT(), is("ge-m"));
		assertNotNull(ref.getAnnotation("toolbox::ge-m"));
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
	
	/*
	 * ██████╗ ██╗   ██╗ ██████╗ ███████╗
	 * ██╔══██╗██║   ██║██╔════╝ ██╔════╝
	 * ██████╔╝██║   ██║██║  ███╗███████╗
	 * ██╔══██╗██║   ██║██║   ██║╚════██║
	 * ██████╔╝╚██████╔╝╚██████╔╝███████║
	 * ╚═════╝  ╚═════╝  ╚═════╝ ╚══════╝
	 */
	
	/**
	 * Two lines with the same marker included in subrefAnnotationMarkers
	 * throw an exception because they're not concatenated.
	 * 
	 * @see SuprefMapper#mapSimpleCandidates
	 */
	@Test
	public void test1() {
		setTestFile("bugs/1.txt");
		setProperties("bugs/1.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		SSpan refSpan = null;
		assertNotNull(refSpan = graph.getSpans().get(0));
		SAnnotation a = null;
		assertNotNull(a = refSpan.getAnnotation("toolbox::nt"));
		assertThat(a.getValue_STEXT(), is("\"blokem\" as second element in a complex verb construction expresses an action resulting in something getting in the way or: the sea goes (went) in his way / the sea goes (went), thereby getting in his way"));
	}
	
	/**
	 * Wrong subref identification
	 */
	@Test
	public void test2() {
		setTestFile("bugs/2.txt");
		setProperties("bugs/2.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		assertThat(graph.getSpans().size(), is(5));
	}
    
	/**
	 * Single token subrefs
	 *
	 * `\subref tx 6 6`
	 */
	@Test
	public void test3() {
		setTestFile("bugs/3.txt");
		setProperties("bugs/3.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		SNode subrefNode = null;
		assertNotNull(subrefNode = graph.getNodesByName("unitref").get(0));
		assertThat(subrefNode, instanceOf(SSpan.class));
		SSpan subref = (SSpan) subrefNode;
		List<SToken> tokens = null;
		assertThat((tokens = graph.getOverlappedTokens(subref)).size(), is(1));
		assertThat(graph.getText(tokens.get(0)), is("festafir."));
	}
	
	/**
	 * Multiple defined targeted subrefs in one ref
	 */
	@Test
	public void test4() {
		setTestFile("bugs/4.txt");
		setProperties("bugs/4.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		for (SNode n : graph.getNodes()) {
			if (n instanceof STextualDS) {
				assertThat(((STextualDS) n).getText(), anyOf(is("T1 T2 T3 T4 T5 T6 T7"), is("m1-m2m3m4m5-m6-m7m8m9m10")));
			}
		}
		SLayer txLayer, mbLayer;
		assertThat(graph.getLayerByName("tx").size(), is(1));
		assertThat(graph.getLayerByName("mb").size(), is(1));
		assertNotNull(txLayer = graph.getLayerByName("tx").get(0));
		assertNotNull(mbLayer = graph.getLayerByName("mb").get(0));
		assertThat(txLayer.getNodes().size(), is(10));
		assertThat(mbLayer.getNodes().size(), is(12));
		int spancount = 0;
		for (SNode txn : txLayer.getNodes()) {
			if (txn instanceof SSpan) {
				spancount++;
				assertThat(graph.getText(txn), anyOf(is("T1 T2 T3"), is("T5 T6")));
				assertThat(txn.getAnnotations().size(), is(1));
				assertThat(txn.getAnnotations().iterator().next().getValue_STEXT(), anyOf(is("test1"), is("test2")));
			}
		}
		assertThat(spancount, is(2));
		spancount = 0;
		for (SNode mbn : mbLayer.getNodes()) {
			if (mbn instanceof SSpan) {
				spancount++;
				assertThat(graph.getText(mbn), is("m1-m2"));
				assertThat(mbn.getAnnotations().size(), is(1));
				assertThat(mbn.getAnnotations().iterator().next().getValue_STEXT(), is("test4"));
			}
		}
		assertThat(spancount, is(1));
		checkLog("Subref 4-10 in segment 'Testref' in document \"4\" could not be resolved, as one or more subref token indices were outside of the range of token indices.", Level.WARN);
		checkLog("Subref 6-7 in segment 'Testref' in document \"4\" could not be resolved, as one or more subref token indices were outside of the range of token indices.", Level.WARN);
	}
	
	/**
	 * Multiple defined (untargeted) subrefs in one ref
	 */
	@Test
	public void test5() {
		setTestFile("bugs/5.txt");
		setProperties("bugs/5.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		for (SNode n : graph.getNodes()) {
			if (n instanceof STextualDS) {
				assertThat(((STextualDS) n).getText(), anyOf(is("T1 T2 T3 T4 T5 T6 T7"), is("m1-m2m3m4m5-m6-m7m8m9m10")));
			}
		}
		SLayer txLayer, mbLayer;
		assertThat(graph.getLayerByName("tx").size(), is(1));
		assertThat(graph.getLayerByName("mb").size(), is(1));
		assertNotNull(txLayer = graph.getLayerByName("tx").get(0));
		assertNotNull(mbLayer = graph.getLayerByName("mb").get(0));
		assertThat(txLayer.getNodes().size(), is(8));
		assertThat(mbLayer.getNodes().size(), is(13));
		int spancount = 0;
		for (SNode mbn : mbLayer.getNodes()) {
			if (mbn instanceof SSpan) {
				spancount++;
				assertThat(graph.getText(mbn), anyOf(is("m1-m2m3"), is("m5-m6")));
				assertThat(mbn.getAnnotations().size(), is(1));
				assertThat(mbn.getAnnotations().iterator().next().getValue_STEXT(), anyOf(is("test1"), is("test2")));
			}
		}
		assertThat(spancount, is(2));
		checkLog("Subref 4-10 in segment 'Test ref' in document \"5\" could not be resolved, as one or more subref token indices were outside of the range of token indices.", Level.WARN);
		checkLog("Subref 11-19 in segment 'Test ref' in document \"5\" could not be resolved, as one or more subref token indices were outside of the range of token indices.", Level.WARN);
	}
	
	/**
	 * Erratic global targeted subref
	 */
	@Test
	public void test6() {
		setTestFile("bugs/6.txt");
		setProperties("bugs/6.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		for (SNode n : graph.getNodes()) {
			if (n instanceof STextualDS) {
				assertThat(((STextualDS) n).getText(), anyOf(is("T1 T2 T3 T4 T5 T6 T7"), is("m1-m2m3m4m5-m6-m7m8m9m10")));
			}
		}
		SLayer txLayer, mbLayer;
		assertThat(graph.getLayerByName("tx").size(), is(1));
		assertThat(graph.getLayerByName("mb").size(), is(1));
		assertNotNull(txLayer = graph.getLayerByName("tx").get(0));
		assertNotNull(mbLayer = graph.getLayerByName("mb").get(0));
		assertThat(txLayer.getNodes().size(), is(8));
		assertThat(mbLayer.getNodes().size(), is(11));
		int spancount = 0;
		for (SNode mbn : mbLayer.getNodes()) {
			if (mbn instanceof SSpan) {
				spancount++;
			}
		}
		assertThat(spancount, is(0));
		checkLog("Document '6', reference 'Test ref x': The indices defined in the global subdef are outside of the index range of the target tokens. Please fix the source data! Ignoring this subref ...", Level.WARN);
	}
	
	/**
	 * Erratic simple (targeted and untargeted) subrefs
	 */
	@Test
	public void test7() {
		setTestFile("bugs/7.txt");
		setProperties("bugs/7.properties");
		start();
		assertEquals(1, getNonEmptyCorpusGraph().getDocuments().size());
		SDocument doc = getNonEmptyCorpusGraph().getDocuments().get(0);
		SDocumentGraph graph = doc.getDocumentGraph();
		for (SNode n : graph.getNodes()) {
			if (n instanceof STextualDS) {
				assertThat(((STextualDS) n).getText(), anyOf(is("T1 T2 T3 T4 T5 T6 T7"), is("m1-m2m3m4m5-m6-m7m8m9m10")));
			}
		}
		SLayer txLayer, mbLayer;
		assertThat(graph.getLayerByName("tx").size(), is(1));
		assertThat(graph.getLayerByName("mb").size(), is(1));
		assertNotNull(txLayer = graph.getLayerByName("tx").get(0));
		assertNotNull(mbLayer = graph.getLayerByName("mb").get(0));
		assertThat(txLayer.getNodes().size(), is(9));
		assertThat(mbLayer.getNodes().size(), is(12));
		int spancount = 0;
		for (SNode txn : txLayer.getNodes()) {
			if (txn instanceof SSpan) {
				spancount++;
				assertThat(graph.getText(txn), is("T1 T2 T3 T4 T5"));
				assertThat(txn.getAnnotations().size(), is(1));
				assertThat(txn.getAnnotations().iterator().next().getValue_STEXT(), is("TestTX"));
			}
		}
		assertThat(spancount, is(1));
		spancount = 0;
		for (SNode mbn : mbLayer.getNodes()) {
			if (mbn instanceof SSpan) {
				spancount++;
				assertThat(graph.getText(mbn), is("m1-m2m3m4m5-"));
				assertThat(mbn.getAnnotations().size(), is(1));
				assertThat(mbn.getAnnotations().iterator().next().getValue_STEXT(), is("TestMB"));
			}
		}
		assertThat(spancount, is(1));
		checkLog("The maximum of subref range 9..10 in document '7', reference 'Test ref y' is larger than the highest token index. Please fix source data! Ignoring this annotation ...", Level.WARN);
		checkLog("The maximum of subref range 13..19 in document '7', reference 'Test ref y' is larger than the highest token index. Please fix source data! Ignoring this annotation ...", Level.WARN);
	}

	/**
	 * Verifies whether the log contains a specific message.
	 *
	 * @param string
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void checkLog(final String string, final Level level) {
		verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
		      @Override
		      public boolean matches(final Object argument) {
		    	  if (level == null) {
		    		  return ((LoggingEvent) argument).getFormattedMessage().contains(string);
		    	  }
		    	  else {
		    		  return ((LoggingEvent) argument).getFormattedMessage().contains(string) && ((LoggingEvent) argument).getLevel().equals(level);
		    	  }
		      }
		    }));
	}

    
}
