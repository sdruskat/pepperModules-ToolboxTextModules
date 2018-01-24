package org.corpus_tools.peppermodules.toolbox.text;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.corpus_tools.pepper.common.CorpusDesc;
import org.corpus_tools.pepper.testFramework.PepperExporterTest;
import org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SaltProject;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

public class ToolboxTextExporterTest extends PepperExporterTest {

	/**
	 * // TODO Add description
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		setFixture(new ToolboxTextExporter());
		getFixture().getProperties().setPropertyValue("idSpanLayer", "paragraphs");
		getFixture().getProperties().setPropertyValue("refSpanLayer", "phrases");
		getFixture().getProperties().setPropertyValue("txTokenLayer", "words");
		getFixture().getProperties().setPropertyValue("mbTokenLayer", "morphemes");
		getFixture().getProperties().setPropertyValue("idIdentifierAnnotation", "paragraph::guid");
		getFixture().getProperties().setPropertyValue("refIdentifierAnnotation", "phrase::guid");
		getFixture().getProperties().setPropertyValue("ignoreTxAnnotations", "mmg-fonipa-x-emic::word_txt,mmg-fonipa-x-emic::word_punct");
		getFixture().getProperties().setPropertyValue("ignoreMbAnnotations", "mmg-fonipa-x-emic::morpheme_txt,mmg-fonipa-x-emic::morpheme_punct");
		getFixture().getProperties().setPropertyValue(ToolboxTextExporterProperties.CUSTOM_MARKERS, "en::phrase_gls:ft, morpheme::type:ty, nonamespace:nna, nonamespacetoken:nnat");
		getFixture().getProperties().setPropertyValue(ToolboxTextExporterProperties.MDF_MAP, "en::morph_gls:ge, fictional::word_txt:lt, en::word_gls:we, second::phrase_gls:xn");
		addFormatWhichShouldBeSupported(ToolboxTextExporter.FORMAT_NAME, ToolboxTextExporter.FORMAT_VERSION);
	}

	/**
	 * // TODO Add description
	 * 
	 */
	@Test
	public final void testConversion() {
		URI projectURI = URI.createFileURI(this.getClass().getClassLoader().getResource("exporter/saltProject.salt").getFile());
		SaltProject saltProject = SaltFactory.createSaltProject();
		saltProject.loadSaltProject(projectURI);
		getFixture().setSaltProject(saltProject);
		getFixture().setCorpusDesc(new CorpusDesc().setCorpusPath(URI.createFileURI(getTempPath("ToolboxTextExporter").getAbsolutePath())));	
		start();
		String resultPath = null, testPath = null;
		File result = new File(URI.createFileURI(resultPath = getTempPath("ToolboxTextExporter").getAbsolutePath() + "/flextext/test.txt").toFileString());
		File toolboxCorpusFile = new File(testPath = this.getClass().getClassLoader().getResource("exporter/1.txt").getFile());
		assertTrue(result.exists());
		assertTrue(toolboxCorpusFile.exists());
		// Test the actual files
		List<String> testFileContent = null, resultFileContent = null;
		try {
			testFileContent = Files.readAllLines(Paths.get(testPath), Charset.defaultCharset());
			resultFileContent = Files.readAllLines(Paths.get(resultPath), Charset.defaultCharset());
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertTrue(testFileContent.size() == resultFileContent.size());
		List<String> trDiff = diffFiles(testFileContent, resultFileContent);
		List<String> rtDiff = diffFiles(resultFileContent, testFileContent);
		assertTrue(rtDiff.isEmpty());
		assertTrue(trDiff.isEmpty());
		
	}

	private static List<String> diffFiles(final List<String> firstFileContent, final List<String> secondFileContent) {
		final List<String> diff = new ArrayList<String>();
		for (final String line : firstFileContent) {
			if (!secondFileContent.contains(line)) {
				diff.add((firstFileContent.indexOf(line) + 1) + " " + line);
			}
		}
		return diff;
	}
	
}
