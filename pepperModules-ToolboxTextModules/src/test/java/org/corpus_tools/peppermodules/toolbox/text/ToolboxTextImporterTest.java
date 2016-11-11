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

import org.corpus_tools.pepper.common.CorpusDesc;
import org.corpus_tools.pepper.common.FormatDesc;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.pepper.testFramework.PepperImporterTest;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
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
 * @author Stephan Druskat
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
		assertEquals(4, corpusGraph.getDocuments().size());
		for (SDocument doc : corpusGraph.getDocuments()) {
			assertThat(doc.getName(), anyOf(is(DOC_NO + "1"), is(DOC_NO + "2"), is(DOC_NO + "3"), is(DOC_NO + "4")));
			assertEquals(2, doc.getAnnotations().size());
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
			if (docNumber.equals("1")) {
				
			}
			else {
				// Document-level
				assertEquals(DOC_NO + docNumber, doc.getName());
				assertNotNull(doc.getAnnotation(TOOLBOX + "::idinfo"));
				assertNotNull(doc.getAnnotation(TOOLBOX + "::moreidinfo"));
				assertEquals("Some document no. " + docNumber + " info", doc.getAnnotation(TOOLBOX + "::idinfo").getValue_STEXT());
				assertEquals("Some more info about document no. " + docNumber, doc.getAnnotation(TOOLBOX + "::moreidinfo").getValue_STEXT());
				if (docNumber.equals("3")) {
					assertNotNull(doc.getAnnotation(TOOLBOX + "::docmet"));
					assertEquals("Some randomly put document meta annotation", doc.getAnnotation(TOOLBOX + "::docmet").getValue_STEXT());
				}
				
				// Data sources
				for (STextualDS ds : graph.getTextualDSs()) {
					assertThat(ds.getText(), is(anyOf(is("A sentence A sentence"), is("A word A word"))));
				}
				
				// Nodes
				assertEquals(8, graph.getTokens().size());
				assertEquals(2, graph.getSpans().size());
				assertEquals(3, graph.getLayers().size()); // ref, lex, morph
				
				// Layers
				for (SLayer l : graph.getLayers()) {
					if (l.getId().equals("ref")) {
						assertEquals(2, l.getNodes().size());
					}
					else {
						assertEquals(4, l.getNodes().size());
						for (SNode n : l.getNodes()) {
							assertTrue(n instanceof SToken);
						}
						if (l.getId().equals("lex")) {
						}
						else {
							
						}
					}
				}
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
