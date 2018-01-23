package org.corpus_tools.peppermodules.toolbox.text;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.corpus_tools.pepper.common.CorpusDesc;
import org.corpus_tools.pepper.testFramework.PepperExporterTest;
import org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.common.SaltProject;
import org.corpus_tools.salt.core.SLayer;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

public class ToolboxTextExporterTest extends PepperExporterTest {

	private SLayer idSpanLayer;
	private SLayer refSpanLayer;
	private SLayer txTokenLayer;
	private SLayer mbTokenLayer;
	private SToken t1, t2, t3, t4, t5, t6, m1, m2, m3, m4, m5, m6, m7, m8;

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
		getFixture().getProperties().setPropertyValue(ToolboxTextExporterProperties.CUSTOM_MARKERS, "ft:en::phrase_gls, ty:morpheme::type");
		getFixture().getProperties().setPropertyValue(ToolboxTextExporterProperties.MDF_MAP, "ge:en::morph_gls, lt:fictional::word_txt, we:en::word_gls, xn:second::phrase_gls");
		addFormatWhichShouldBeSupported(ToolboxTextExporter.FORMAT_NAME, ToolboxTextExporter.FORMAT_VERSION);
	}

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
		boolean fileContentsEqual = false;
		try {
			fileContentsEqual = FileUtils.contentEquals(result, toolboxCorpusFile);
		}
		catch (IOException e) {
			fail("Couldn't read actual or test file.");
		}

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
		// REMOVE BELOW
		List<String> trDiff = diffFiles(testFileContent, resultFileContent);
		List<String> rtDiff = diffFiles(resultFileContent, testFileContent);
		System.out.println("LINES IN TEST FILE THAT AREN'T IN RESULT FILE (" + trDiff.size() + ")\n");
		for (String s : trDiff) {
			System.out.println(s);
		}
		System.out.println("-----------------");
		try {
			TimeUnit.SECONDS.sleep(1);
		}
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.err.println("LINES IN RESULT FILE THAT AREN'T IN TEST FILE (" + rtDiff.size() + ")\n");
		for (String s : rtDiff) {
			System.err.println(s);
		}
		System.err.println("\n\n\n--------------------------\n\n\n");
		for (String l : resultFileContent) {
			System.err.println(l);
		}
		// REMOVE ABOVE

		assertTrue(rtDiff.isEmpty());
		assertTrue(trDiff.isEmpty());
	}

	/**
	 * FIXME DELETE
	 * 
	 * @param firstFileContent
	 * @param secondFileContent
	 * @return
	 */
	private static List<String> diffFiles(final List<String> firstFileContent, final List<String> secondFileContent) {
		final List<String> diff = new ArrayList<String>();
		for (final String line : firstFileContent) {
			if (!secondFileContent.contains(line)) {
				diff.add((firstFileContent.indexOf(line) + 1) + " " + line);
			}
		}
		return diff;
	}

	/**
	 * FIXME DELETE
	 * 
	 * @return
	 */
	private SaltProject createSaltProject() {
		SaltProject saltProject = SaltFactory.createSaltProject();
		saltProject.setName("testProject");
		SCorpusGraph corpGraph = SaltFactory.createSCorpusGraph();
		corpGraph = createCorpusStructure(corpGraph);
		assertNotNull(corpGraph);
		List<SCorpus> corpora = null;
		assertThat((corpora = corpGraph.getCorpora()).size(), is(1));
		assertThat(corpGraph.getDocuments().size(), is(1));
		final SDocument document = corpGraph.getDocuments().get(0);
		assertThat(corpGraph.getCorpus(document), is(corpora.get(0)));
		saltProject.addCorpusGraph(corpGraph);
		createDocumentStructure(document);
		return saltProject;
	}

	/**
	 * FIXME DELETE
	 * 
	 * @param corpGraph
	 * @return
	 */
	private SCorpusGraph createCorpusStructure(SCorpusGraph corpGraph) {
		assertNotNull(corpGraph);
		// Corpus
		corpGraph.setId("corpusGraph");
		SCorpus corpusRoot = SaltFactory.createSCorpus();
		corpusRoot.setName("testCorpus");
		corpGraph.addNode(corpusRoot);
		// Document
		SDocument doc = null;
		doc = SaltFactory.createSDocument();
		doc.setName("testDoc");
		doc.createAnnotation("languages", "mmg-fonipa-x-emic", "encoding=null,vernacular=true,font=Times New Roman");
		doc.createAnnotation("languages", "nonsense", "encoding=null,vernacular=true,font=Times New Roman");
		doc.createAnnotation("languages", "en", "encoding=null,vernacular=true,font=Times New Roman");
		doc.createAnnotation("mmg-fonipa-x-emic", "source", "Stephan Druskat");
		doc.createAnnotation("nonsense", "title", "Börsday pony text");
		doc.createAnnotation("mmg-fonipa-x-emic", "title", "Brsd pn");
		doc.createAnnotation("en", "title", "Birthday pony");
		doc.createAnnotation("interlinear-text", "guid", "7997afd5-5554-4714-894e-2c3d5bdb6f09");
		corpGraph.addDocument(corpusRoot, doc);
		return corpGraph;
	}

	/**
	 * FIXME DELETE
	 * 
	 * @param document
	 */
	private void createDocumentStructure(SDocument document) {
		document.setDocumentGraph(SaltFactory.createSDocumentGraph());
		createLayers(document);
		createPrimaryTexts(document);
		createTokens(document);
//		createSpans(document);
	}

	/**
	 * FIXME DELETE
	 * 
	 * @param document
	 */
	private void createLayers(SDocument document) {
		SDocumentGraph graph = document.getDocumentGraph();
		idSpanLayer = SaltFactory.createSLayer();
		idSpanLayer.setName("paragraphs");
		refSpanLayer = SaltFactory.createSLayer();
		refSpanLayer.setName("phrases");
		txTokenLayer = SaltFactory.createSLayer();
		txTokenLayer.setName("words");
		mbTokenLayer = SaltFactory.createSLayer();
		mbTokenLayer.setName("morphemes");
		graph.addLayer(idSpanLayer);
		graph.addLayer(refSpanLayer);
		graph.addLayer(txTokenLayer);
		graph.addLayer(mbTokenLayer);
	}

	/**
	 * FIXME DELETE
	 * 
	 * @param document
	 */
	private void createPrimaryTexts(SDocument document) {
		STextualDS textualDS = SaltFactory.createSTextualDS();
		textualDS.setName("lexicalDS");
		textualDS.setText("Wis iz 1 birdae powny.");
		STextualDS morphDS = SaltFactory.createSTextualDS();
		morphDS.setName("morphDS");
		morphDS.setText("Wis iz 1 bir-dae pow-ny.");
		document.getDocumentGraph().addNode(textualDS);
		document.getDocumentGraph().addNode(morphDS);
	}

	/**
	 * FIXME DELETE
	 * 
	 * @param document
	 */
	private void createTokens(SDocument document) {
		SDocumentGraph graph = document.getDocumentGraph();
		STextualDS lexicalDS = null, morphDS = null;
		SLayer txLayer = graph.getLayerByName("words").get(0);
		SLayer morphLayer = graph.getLayerByName("morphemes").get(0);
		for (STextualDS ds : graph.getTextualDSs()) {
			switch (ds.getName()) {
			case "morphDS":
				morphDS = ds;
				break;

			case "lexicalDS":
				lexicalDS = ds;
				break;

			default:
				break;
			}
		}
		// Lexical tokens
		t1 = createToken(0, 3, lexicalDS, document, txLayer);
		t2 = createToken(4, 6, lexicalDS, document, txLayer);
		t3 = createToken(7, 8, lexicalDS, document, txLayer);
		t4 = createToken(9, 15, lexicalDS, document, txLayer);
		t5 = createToken(16, 21, lexicalDS, document, txLayer);
		t6 = createToken(21, 22, lexicalDS, document, txLayer);
		assertThat(graph.getTokens().size(), is(6));
		// Morphological tokens
		m1 = createToken(0, 3, morphDS, document, morphLayer);
		m2 = createToken(4, 6, morphDS, document, morphLayer);
		m3 = createToken(7, 8, morphDS, document, morphLayer);
		m4 = createToken(9, 12, morphDS, document, morphLayer);
		m5 = createToken(12, 16, morphDS, document, morphLayer);
		m6 = createToken(17, 20, morphDS, document, morphLayer);
		m7 = createToken(20, 23, morphDS, document, morphLayer);
		m8 = createToken(23, 24, morphDS, document, morphLayer);
		assertThat(graph.getTokens().size(), is(14));
		
		// Lex token annotation
		t1.createAnnotation("en", "gls", "T1");
		t2.createAnnotation("en", "gls", "T2");
		t3.createAnnotation("en", "gls", "T3");
		t4.createAnnotation("en", "gls", "T4");
		t5.createAnnotation("en", "gls", "T5");
		t6.createAnnotation("en", "gls", "PUNCT");
		t1.createAnnotation("word", "guid", "aca7a378-e4af-4c73-8e8c-a9ac6afff52c");
		t2.createAnnotation("word", "guid", "c046b2f4-1a88-44b4-ab69-553c8c6fb375");
		t3.createAnnotation("word", "guid", "ff304ce5-a542-4793-a042-fe3015b0929a");
		t4.createAnnotation("word", "guid", "dc0c742f-011a-4b62-bf1b-fd9de95b80b0");
		t5.createAnnotation("word", "guid", "ba85f036-678d-4359-9fd5-0bbd7fb9d468");
		t6.createAnnotation("word", "guid", "d497002f-8af3-4e08-954b-10657a688849");
		    
		// Morph token annotation
		m1.createAnnotation("morpheme", "guid", "766cb387-6eb3-4847-aa7c-409dd8d76a06");
		m2.createAnnotation("morpheme", "guid", "8e1e1d60-58eb-4bb8-8fcb-1970b3965c9d");
		m3.createAnnotation("morpheme", "guid", "3d8c9252-4ac7-4430-82e5-09193fd914de");
		m4.createAnnotation("morpheme", "guid", "dfbef297-2d1f-4bc8-8bdd-0facc2c40515");
		m5.createAnnotation("morpheme", "guid", "ad547d0a-dec5-4530-ab1a-42b822d9f296");
		m6.createAnnotation("morpheme", "guid", "4d3aeffa-1323-4dc2-80d3-ed5d8bd6a8cb");
		m7.createAnnotation("morpheme", "guid", "4d2659e2-0ca3-4139-b8f6-01ea53d673a0");
		m8.createAnnotation("morpheme", "guid", "7a645be5-e514-4768-a9a6-bd1aee280577");
		m1.createAnnotation("en", "gls", "e1");
		m2.createAnnotation("en", "gls", "e2");
		m3.createAnnotation("en", "gls", "e3");
		m4.createAnnotation("en", "gls", "e4");
		m5.createAnnotation("en", "gls", "e5");
		m6.createAnnotation("en", "gls", "e6");
		m7.createAnnotation("en", "gls", "e7");
		m8.createAnnotation("en", "gls", "e8");
		m1.createAnnotation("", "", "");
	}

	/**
	 * FIXME DELETE
	 * 
	 * @param start
	 * @param end
	 * @param ds
	 * @param document
	 * @param layer
	 * @return
	 */
	private SToken createToken(int start, int end, STextualDS ds, SDocument document, SLayer layer) {
		SToken token = SaltFactory.createSToken();
		document.getDocumentGraph().addNode(token);
		if (layer != null) {
			layer.addNode(token);
		}
		STextualRelation textRel = SaltFactory.createSTextualRelation();
		textRel.setSource(token);
		textRel.setTarget(ds);
		textRel.setStart(start);
		textRel.setEnd(end);
		document.getDocumentGraph().addRelation(textRel);
		return (token);
	}

}
