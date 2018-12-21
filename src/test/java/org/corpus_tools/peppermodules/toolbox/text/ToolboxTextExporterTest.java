/*******************************************************************************
 * Copyright (c) 2016, 2018ff. Stephan Druskat
 * Exploitation rights for this version belong exclusively to Humboldt-Universität zu Berlin
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
 *     Stephan Druskat (mail@sdruskat.net) - initial API and implementation
 *******************************************************************************/
package org.corpus_tools.peppermodules.toolbox.text;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.corpus_tools.pepper.common.CorpusDesc;
import org.corpus_tools.pepper.testFramework.PepperExporterTest;
import org.corpus_tools.peppermodules.toolbox.text.properties.ToolboxTextExporterProperties;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.common.SaltProject;
import org.corpus_tools.salt.core.SLayer;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ToolboxTextExporter}.
 *
 * @author Stephan Druskat <[mail@sdruskat.net](mailto:mail@sdruskat.net)>
 * 
 */
public class ToolboxTextExporterTest extends PepperExporterTest {

	/**
	 * Sets up the fixture.
	 */
	@Before
	public void setUp() {
		setFixture(new ToolboxTextExporter());
		addFormatWhichShouldBeSupported(ToolboxTextExporter.FORMAT_NAME, ToolboxTextExporter.FORMAT_VERSION);
	}

	/**
	 * Test conversion with a FLExText XML example.
	 */
	@Test
	public final void testConversionOnlyOriginalNames() {
		assertResults("exporter/conversion-only-name.properties", 
				"exporter/saltProject.salt", 
				"/flextext/test.txt", 
				"exporter/conversion-only-name.txt",
				false,
				false);
	}

	/**
	 * Test conversion with a FLExText XML example,
	 * mapping layer names also, so annotation markers
	 * with a layer should have a preceding
	 * `{layer_name}__` in front of the annotation name.
	 */
	@Test
	public final void testConversionLayerOriginalNames() {
		assertResults("exporter/layer-name.properties", 
				"exporter/saltProject.salt", 
				"/flextext/test.txt", 
				"exporter/layer-name.txt",
				true,
				false);
	}
	
	/**
	 * Test conversion with a FLExText XML example,
	 * mapping namespaces also, so annotation markers
	 * with a namespace should have a preceding
	 * `{namespace}_` in front of the annotation name.
	 */
	@Test
	public final void testConversionNamespaceOriginalNames() {
		assertResults("exporter/namespace-name.properties", 
				"exporter/saltProject.salt", 
				"/flextext/test.txt", 
				"exporter/namespace-name.txt",
				false,
				true);
	}
	
	/**
	 * Test conversion with a FLExText XML example,
	 * mapping layers **and** namespaces also, so annotation markers
	 * with a layer and a namespace should have a preceding
	 * `{layer_name}__{namespace}_` in front of the annotation name.
	 */
	@Test
	public final void testConversionLayerNamespaceOriginalNames() {
		assertResults("exporter/layer-namespace-name.properties", 
				"exporter/saltProject.salt", 
				"/flextext/test.txt", 
				"exporter/layer-namespace-name.txt",
				true,
				true);
	}
	
	/**
	 * Test conversion with a FLExText XML example,
	 * mapping layers **and** namespaces also, so annotation markers
	 * with a layer and a namespace should have a preceding
	 * `{layer_name}__{namespace}_` in front of the annotation name.
	 */
	@Test
	public final void testConversionLayerNamespaceMappedNames() {
		assertResults("exporter/layer-namespace-mapped-name.properties", 
				"exporter/saltProject.salt", 
				"/flextext/test.txt", 
				"exporter/layer-namespace-mapped-name.txt",
				true,
				true);
	}

	
	/**
	 * Test conversion with a generic Salt example
	 */
	@Test
	public void testConversionNormalization() {
		createTestProject();
		setProperties("exporter/normalization/1.properties");
		URI projectURI = URI
				.createFileURI(getTempPath("ToolboxTextExporter-testProject/saltProject.salt").getAbsolutePath());
		SaltProject saltProject = SaltFactory.createSaltProject();
		saltProject.loadSaltProject(projectURI);

		getFixture().setSaltProject(saltProject);
		getFixture().setCorpusDesc(new CorpusDesc().setCorpusPath(URI
				.createFileURI(getTempPath("ToolboxTextExporter-testProject/marker-normalization").getAbsolutePath())));
		start();

		String resultPath = null;
		File result = new File(URI.createFileURI(
				resultPath = getTempPath("ToolboxTextExporter-testProject/marker-normalization").getAbsolutePath()
						+ "/corpus/document.txt")
				.toFileString());
		assertTrue(result.exists());
		// Test the actual files
		try {
			List<String> lines = Files.readAllLines(Paths.get(resultPath));
			assertThat(lines.size(), is(11));
			assertThat(lines.get(3), is("\\doc ione"));
			assertThat(lines.get(5), is("\\phrase rone"));
			assertThat(lines.get(6), is("\\name refname"));
			assertThat(lines.get(7), is("\\lex Birthday pony"));
			assertThat(lines.get(8), is("\\lexanno wone wtwo"));
			assertThat(lines.get(9), is("\\morph m_birth m_day m_pony"));
			assertThat(lines.get(10), is("\\morphanno mone mtwo mthree"));
		}
		catch (IOException e) {
			fail("Could not read result file.");
		}
	}
	
	/**
	 * Test conversion with a generic Salt example
	 */
	@Test
	public void testConversionNormalizationFromFLExTextSource() {
		URI saltProjectURI = URI.createFileURI(this.getClass().getClassLoader().getResource("exporter/normalization/saltProject.salt").getFile());
		setProperties("exporter/normalization/2.properties");
		SaltProject saltProject = SaltFactory.createSaltProject();
		saltProject.loadSaltProject(saltProjectURI);

		getFixture().setSaltProject(saltProject);
		getFixture().setCorpusDesc(new CorpusDesc().setCorpusPath(URI
				.createFileURI(getTempPath("ToolboxTextExporter-testProject/normalization-flex").getAbsolutePath())));
		start();
		String resultDir = getTempPath("ToolboxTextExporter-testProject/normalization-flex/flextext/flextext.txt").getAbsolutePath();
		Path resultPath = Paths.get(resultDir);
		List<String> resultFile = null;
		try {
			resultFile = Files.readAllLines(resultPath);
		}
		catch (IOException e) {
			fail("Could not read file! " + e.getMessage());
		}
		assertThat(resultFile.size(), is(23));
		List<String> targetLines = Stream.of("\\_sh v3.0 400 Text", 
				"", 
				"\\docguid b9742267-dd19-4170-81fd-ed849c4fb834",
				"\\3 DUMMY",
				"\\2 DUMMY",
				"\\14 DUMMY",
				"\\6 DUMMY",
				"",
				"\\id flextext",
				"",
				"\\ref f1a628a1-7d76-4bb5-841a-8221b87398b4",
				"\\4 DUMMY",
				"\\18 DUMMY",
				"\\17 DUMMY",
				"\\tx DUMMY",
				"\\12 DUMMY",
				"\\wordguid ac646ce8-ca4b-4d51-bc8e-c66dd0c57ba3",
				"\\16 DUMMY",
				"\\mb DUMMY",
				"\\13 DUMMY",
				"\\19 DUMMY",
				"\\morphguid d7f713e5-e8cf-11d3-9764-00c04f186933",
				"\\type root").collect(Collectors.toList());
		resultFile.sort(Comparator.naturalOrder());
		targetLines.sort(Comparator.naturalOrder());
		
		for (int i = 0; i < resultFile.size(); i++) {
			assertThat(resultFile.get(i), is(targetLines.get(i)));
		}
	}
	
	
	/**
	 * Creates a test project with one "ref":
	 * - 1 Annotation: ref=one
	 * 
	 * Two tx tokens with one annotation each:
	 * - "Birthday": word=one
	 * - "pony": word=two
	 * 
	 * Three morph tokens with one annotation each:
	 * - "m_Birth": morph=one
	 * - "m_day": morph=two
	 * - "m_pony": morph=three
	 * 
	 * All tokens are tied up with one timeline.
	 */
	private SaltProject createTestProject() {
		SaltProject project = SaltFactory.createSaltProject();
		project.addCorpusGraph(SaltFactory.createSCorpusGraph());
		SCorpusGraph cg = project.getCorpusGraphs().get(0);
		SCorpus corp = cg.createCorpus(null, "corpus");
		SDocument doc = cg.createDocument(corp, "document");
		SDocumentGraph graph = doc.createDocumentGraph();
		STimeline tl = graph.createTimeline();
		STextualDS ds1 = graph.createTextualDS("Birthday pony");
		SToken lt1 = graph.createToken(ds1, 0, 8);
		lt1.createAnnotation(null, "word", "wone");
		createTimelineRelation(graph, tl, lt1, 0, 13);
		SToken lt2 = graph.createToken(ds1, 9, 13);
		lt2.createAnnotation(null, "word", "wtwo");
		createTimelineRelation(graph, tl, lt2, 9, 13);
		STextualDS ds2 = graph.createTextualDS("m_birth m_day m_pony");
		SToken mt1 = graph.createToken(ds2, 0, 7);
		mt1.createAnnotation(null, "morph", "mone");
		createTimelineRelation(graph, tl, mt1, 0, 7);
		SToken mt2 = graph.createToken(ds2, 8, 13);
		mt2.createAnnotation(null, "morph", "mtwo");
		createTimelineRelation(graph, tl, mt2, 8, 13);
		SToken mt3 = graph.createToken(ds2, 14, 20);
		mt3.createAnnotation(null, "morph", "mthree");
		createTimelineRelation(graph, tl, mt3, 14, 20);
		SSpan refSpan = graph.createSpan(lt1, lt2, mt1, mt2, mt3);
		refSpan.createAnnotation(null, "ref", "rone");
		refSpan.createAnnotation("ns", "name", "refname");
		
		// Add the required spans and layers
		SSpan idSpan = graph.createSpan(lt1, lt2);
		idSpan.createAnnotation(null, "id", "ione");
		
		SLayer refSpanLayer = SaltFactory.createSLayer();
		refSpanLayer.setName("ref");
		refSpanLayer.addNode(refSpan);
		graph.addLayer(refSpanLayer);
		
		SLayer idSpanLayer = SaltFactory.createSLayer();
		idSpanLayer.setName("id");
		idSpanLayer.addNode(idSpan);
		graph.addLayer(idSpanLayer);
		
		SLayer txTokenLayer = SaltFactory.createSLayer();
		txTokenLayer.setName("tx");
		txTokenLayer.addNode(lt1);
		txTokenLayer.addNode(lt2);
		graph.addLayer(txTokenLayer);
		
		SLayer mbTokenLayer = SaltFactory.createSLayer();
		mbTokenLayer.setName("mb");
		mbTokenLayer.addNode(mt1);
		mbTokenLayer.addNode(mt2);
		mbTokenLayer.addNode(mt3);
		graph.addLayer(mbTokenLayer);
		
		
		project.saveSaltProject(URI.createFileURI(getTempPath("ToolboxTextExporter-testProject").getAbsolutePath()));
		return project;
	}

	private void createTimelineRelation(SDocumentGraph graph, STimeline tl, SToken t, int i, int j) {
		STimelineRelation tr = SaltFactory.createSTimelineRelation();
		tr.setSource(t);
		tr.setTarget(tl);
		tr.setStart(i);
		tr.setEnd(j);
		graph.addRelation(tr);
	}


	private String getFile(String fileName) {
		return this.getClass().getClassLoader().getResource(fileName).getFile();
	}

	private void setProperties(String fileName) {
		ToolboxTextExporterProperties properties = new ToolboxTextExporterProperties();
		properties.setPropertyValues(new File(getFile(fileName)));
		getFixture().setProperties(properties);
	}

	private void assertResults(String propertiesPath, String saltProjectPath, String resultPathString, String toolboxCorpusFileString, boolean mapLayer, boolean mapNamespace) {
		setProperties(propertiesPath);
		URI projectURI = URI
				.createFileURI(this.getClass().getClassLoader().getResource(saltProjectPath).getFile());
		SaltProject saltProject = SaltFactory.createSaltProject();
		saltProject.loadSaltProject(projectURI);
		getFixture().setSaltProject(saltProject);
		getFixture().setCorpusDesc(new CorpusDesc()
				.setCorpusPath(URI.createFileURI(getTempPath("ToolboxTextExporter").getAbsolutePath())));
		start();
		String resultPath = null, testPath = null;
		File result = new File(URI
				.createFileURI(resultPath = getTempPath("ToolboxTextExporter").getAbsolutePath() + resultPathString)
				.toFileString());
		File toolboxCorpusFile = new File(
				testPath = this.getClass().getClassLoader().getResource(toolboxCorpusFileString).getFile());
		assertTrue(result.exists());
		assertTrue(toolboxCorpusFile.exists());
		// Test the actual files
		List<String> testFileContent = null, resultFileContent = null;
		try {
			testFileContent = Files.readAllLines(Paths.get(testPath), Charset.defaultCharset());
			resultFileContent = Files.readAllLines(Paths.get(resultPath), Charset.defaultCharset());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		assertThat(((ToolboxTextExporterProperties) getFixture().getProperties()).mapLayer(), is(mapLayer));
		assertThat(((ToolboxTextExporterProperties) getFixture().getProperties()).mapNamespace(), is(mapNamespace));

		assertTrue(testFileContent.size() == resultFileContent.size());
		resultFileContent.sort(Comparator.naturalOrder());
		testFileContent.sort(Comparator.naturalOrder());
		
		for (int i = 0; i < resultFileContent.size(); i++) {
			assertThat(resultFileContent.get(i), is(testFileContent.get(i)));
		}
	}
	
}
