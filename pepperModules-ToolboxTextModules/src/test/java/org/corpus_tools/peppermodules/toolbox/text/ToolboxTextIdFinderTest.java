/**
 * 
 */
package org.corpus_tools.peppermodules.toolbox.text;

import static org.junit.Assert.*;

import java.io.File;

import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

/**
 * TODO Description
 *
 * @author Stephan Druskat
 *
 */
public class ToolboxTextIdFinderTest {
	
	private ToolboxTextIdFinder fixture = null;
	private File corpusFile;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		corpusFile = new File(this.getClass().getClassLoader().getResource("ids.txt").getFile());
		SCorpusGraph corpusGraph = SaltFactory.createSCorpusGraph();
		SCorpus subCorpus = SaltFactory.createSCorpus();
		String idMarker = "id";
		ToolboxTextIdFinder finder = new ToolboxTextIdFinder(corpusFile, corpusGraph, subCorpus, idMarker);
		setFixture(finder);
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextIdFinder#parse()}.
	 */
	@Test
	public void testParse() {
		getFixture().parse();
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.ToolboxTextIdFinder#getResourceHeader()}.
	 */
	@Test
	public void testGetResourceHeader() {
		getFixture().parse();
		System.err.println(getFixture().getResourceHeader());
		assertEquals(URI.createFileURI(corpusFile.getAbsolutePath()), getFixture().getResourceHeader().getResource());
	}

	/**
	 * @return the fixture
	 */
	private ToolboxTextIdFinder getFixture() {
		return fixture;
	}

	/**
	 * @param fixture the fixture to set
	 */
	private void setFixture(ToolboxTextIdFinder fixture) {
		this.fixture = fixture;
	}

}
