/**
 * 
 */
package org.corpus_tools.peppermodules.toolbox.text;

import static org.junit.Assert.*; 

import java.io.File;

import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.core.SMetaAnnotation;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * TODO Description
 *
 * @author Stephan Druskat
 *
 */
public class IdBasedToolboxTextMapperTest {
	
	private IdBasedToolboxTextMapper fixture = null;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		File file = new File(this.getClass().getClassLoader().getResource("ids.txt").getFile());
		String path = file.getAbsolutePath();
		IdBasedToolboxTextMapper mapper = new IdBasedToolboxTextMapper(33L, URI.createFileURI(path));
		SDocument doc = SaltFactory.createSDocument();
		mapper.setDocument(doc);
		setFixture(mapper);
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.IdBasedToolboxTextMapper#mapSDocument()}.
	 */
	@Test
	public void testMapSDocument() {
		getFixture().mapSDocument();
		
	}

	/**
	 * Test method for {@link org.corpus_tools.peppermodules.toolbox.text.IdBasedToolboxTextMapper#mapSCorpus()}.
	 */
	@Test
	public void testMapSCorpus() {
		SCorpus c = SaltFactory.createSCorpus();
		SaltFactory.createIdentifier(c, "corpus");
		getFixture().setCorpus(c);
		getFixture().mapSCorpus();
		assertEquals(2, getFixture().getCorpus().getMetaAnnotations().size());
		assertNotNull(getFixture().getCorpus().getMetaAnnotation("toolbox::_sh"));
		assertEquals("v3.0 Test", getFixture().getCorpus().getMetaAnnotation("toolbox::_sh").getValue());
		assertNotNull(getFixture().getCorpus().getMetaAnnotation("toolbox::info"));
		assertEquals("Some info", getFixture().getCorpus().getMetaAnnotation("toolbox::info").getValue());
	}

	/**
	 * @return the fixture
	 */
	private IdBasedToolboxTextMapper getFixture() {
		return fixture;
	}

	/**
	 * @param fixture the fixture to set
	 */
	private void setFixture(IdBasedToolboxTextMapper fixture) {
		this.fixture = fixture;
	}

}
