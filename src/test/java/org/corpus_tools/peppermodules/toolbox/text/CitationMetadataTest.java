package org.corpus_tools.peppermodules.toolbox.text;

import java.io.File;
import java.io.IOException;

import org.corpus_tools.peppermodules.toolbox.text.utils.Author;
import org.corpus_tools.peppermodules.toolbox.text.utils.CitationMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class CitationMetadataTest {

	private CitationMetadata fixture;

	@Before
	public void setUp() throws Exception {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try {
			fixture = mapper.readValue(new File("CITATION.cff"), CitationMetadata.class);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void test() {
		System.err.println(fixture.getMessage());
		System.err.println("cff-version: " + fixture.getCffVersion());
		System.err.println("year: " + fixture.getDateReleased().getYear());
		System.err.println("doi: " + fixture.getDoi());
		System.err.println("title: " + fixture.getTitle());
		System.err.println("version: " + fixture.getVersion());
		for (Author author : fixture.getAuthors()) {
			System.err.println("author " + (fixture.getAuthors().indexOf(author) + 1) + ": " + author.getGivenNames()
					+ " " + author.getFamilyNames() + " (" + author.getOrcid() + ")");
		}
	}

	/**
	 * @return the fixture
	 */
	public final CitationMetadata getFixture() {
		return fixture;
	}

	/**
	 * @param fixture
	 *            the fixture to set
	 */
	public final void setFixture(CitationMetadata fixture) {
		this.fixture = fixture;
	}

}
