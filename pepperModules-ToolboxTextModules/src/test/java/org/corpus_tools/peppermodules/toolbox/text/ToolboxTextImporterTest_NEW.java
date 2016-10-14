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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.corpus_tools.pepper.common.CorpusDesc;
import org.corpus_tools.pepper.common.FormatDesc;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.pepper.testFramework.PepperImporterTest;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SaltProject;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SMetaAnnotation;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ToolboxTextImporter}.
 *
 * @author Stephan Druskat
 *
 */
public class ToolboxTextImporterTest_NEW extends PepperImporterTest {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.setFixture(new ToolboxTextImporter());
		this.supportedFormatsCheck.add(new FormatDesc().setFormatName("toolbox-text").setFormatVersion("3.0"));
		this.getFixture().getCorpusDesc().getFormatDesc().setFormatName("toolbox-text").setFormatVersion("3.0");
		getFixture().getSaltProject().createCorpusGraph();
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
		runCorpusTests(corpusGraph);
		
		// Test single document
		assertEquals(1, corpusGraph.getDocuments().size());
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there are 3 \ids and n \refs,
	 * i.e., what will become a single corpus with a single document.
	 */
	@Test
	public void testParseStandardDocument() {
		getFixture().setCorpusDesc(new CorpusDesc().setCorpusPath(URI.createFileURI(getFile("ids.txt"))));
		start();
		assertEquals((Long) 32L, getFixture().getHeaderEndOffset());
		assertFalse(getFixture().isMonolithic());
		SCorpusGraph corpusGraph = getNonEmptyCorpusGraph();

		// Test corpora
		runCorpusTests(corpusGraph);
		
		// Test documents
		assertEquals(3, corpusGraph.getDocuments().size());
		for (SDocument doc : corpusGraph.getDocuments()) {
			assertThat(doc.getName(), is(anyOf(is("ID1"), is("ID2"), is("ID3"))));
			for (SAnnotation anno : doc.getAnnotations()) {
				System.err.println(anno.getQName());
				assertThat(anno.getQName(), is("toolbox::idinfo:Info on " + doc.getName()));
			}
		}
	}
	
	/**
	 * Test method for
	 * {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextImporter#importCorpusStructure(org.corpus_tools.salt.common.SCorpusGraph)}.
	 * 
	 * Tests against a minimum example, where there are 0 \ids and 0 \refs,
	 * i.e., an empty corpus.
	 */
	@Test
	public void testParseOrphanRefs() {
		getFixture().setCorpusDesc(new CorpusDesc().setCorpusPath(URI.createFileURI(getFile("orphan-ids-and-refs.txt"))));
		start();
	}

	private String getFile(String fileName) {
		return this.getClass().getClassLoader().getResource(fileName).getFile();
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
	private void runCorpusTests(SCorpusGraph corpusGraph) {
		assertEquals(1, corpusGraph.getCorpora().size());
		
		// Test single corpus
		for (SMetaAnnotation ma : corpusGraph.getCorpora().get(0).getMetaAnnotations()) {
			assertThat(ma.getQName() + ":" + ma.getValue_STEXT(), is(anyOf(is("toolbox::_sh:v3.0 Test"), is("toolbox::info:Some info"))));
		}
	}
	

}
