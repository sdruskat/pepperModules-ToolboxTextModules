# [Pepper](http://corpus-tools.org/pepper) modules for the [SIL Toolbox](https://software.sil.org/toolbox/) interlinear text format

## How to cite

If you use the Toolbox Text Modules for Pepper in your work, please cite it with the metadata given below!

- **Author:** Stephan Druskat (ORCiD id: https://orcid.org/0000-0003-4925-7248)
- **Year:** 2018
- **Title:** pepperModules-ToolboxTextModules
- **Version:** 1.0.0
- **DOI:** [TODO](doi link)
- **Release date:** 2018-01-24

Note that this metadata is also provided in machine-readable form in the 
[Citation File Format](https://citation-file-format.github.io/), in the file
[CITATION.cff](CITATION.cff).

## General information

[Pepper](http://corpus-tools.org) is a conversion framework for linguistic data. 
*pepperModules-ToolboxTextModules* is a plugin for *Pepper* and provides an
importer and exporter for the **Toolbox Interlinear Text Format**, i.e., the text-based
export format from [SIL Toolbox](https://software.sil.org/toolbox/). The format
is used frequently for persisting language documentation data. For examples of 
Toolbox interlinear text files, see for example [this directory in the GitHub
repository teropa/nlp](https://github.com/teropa/nlp/tree/f89bc5a15b516208de1cfe2abe9692b7ed9a43e2/resources/corpora/toolbox/rotokas).

With the *pepperModules-ToolboxTextModules*, the data stored in Toolbox 
interlinear text files can be transferred to another format. This way, the data 
can be re-used for other
purposes (such as adding different annotation types), or visualized and analyzed,
e.g., in [ANNIS](http://corpus-tools.org/annis), a search and visualization 
platform for linguistic data. For a list of available format converters for Pepper,
see the [list of known Pepper modules](http://corpus-tools.org/pepper/knownModules.html).

Note that there is also a 
[Pepper module for the Toolbox XML format](https://github.com/korpling/pepperModules-ToolboxModules)
which is not related to this project.

## Context

The development of *pepperModules-ToolboxTextModules* has been initiated in the
[MelaTAMP research project](https://hu.berlin/melatamp).

## Requirements

*pepperModules-ToolboxTextModules* requires at Pepper >= 3.1.1-SNAPSHOT, as it
relies on default property values which have been introduced in this version.
A kickstarter (i.e., standalone) version of Pepper including the correct version
can be obtained from the [snapshot releases repository for Pepper](https://korpling.german.hu-berlin.de/saltnpepper/pepper/download/snapshot/):
[Pepper_2018.01.26-SNAPSHOT.zip](https://korpling.german.hu-berlin.de/saltnpepper/pepper/download/snapshot/Pepper_2018.01.26-SNAPSHOT.zip). 
This is the earliest version of Pepper including the required
functionalities for *pepperModules-ToolboxTextModules*, newer versions will do
just as well.

## Usage

- Download [Pepper_2018.01.26-SNAPSHOT.zip](https://korpling.german.hu-berlin.de/saltnpepper/pepper/download/snapshot/Pepper_2018.01.26-SNAPSHOT.zip)
(or newer) and extract it to a directory of your choice
- Download the latest *pepperModules-ToolboxTextModules* zip from 
[releases](https://github.com/sdruskat/pepperModules-ToolboxTextModules/releases)
and extract it to a directory of your choice
- Add the path to the directory containing `pepperModules-ToolboxTextModules-1.0.0.jar`
to the Pepper configuration file:
	- Open `{Pepper directory}/pepper/conf/pepper.properties`
	- Remove the comment hash (`#`) from the line
	`#pepper.dropin.paths=` and add the path
- Start Pepper with the respective command (`pepperStart.sh`
on Linux/Mac, `pepperStart.bat` on Win)
- Check that the Toolbox text modules have been resolved by displaying the list
of available modules in Pepper with the `l` command
- Start a conversion. You can use the interactive wizard (`c`), or run a pre-defined
workflow (`c {path to workflow file}`).

### Pepper workflow file

Pepper conversions are defined in Pepper workflow files, see the 
[Pepper User Guide](http://corpus-tools.org/pepper/userGuide.html#workflow_file).

The available properties for the Toolbox Text Modules are detailed in the 
following sections.

## Importer

### Properties

Note that required values with a default value do not have to be specified in the
workflow file when the default value should be used.

- **`fileExtensions` (String) (required)**: The file extensions that corpus files can have as a 
comma-separated list.

   Default value: `txt`

- **`idMarker` (String)**: The Toolbox marker that precedes lines with IDs, without the 
preceding backslash.

   Default value: `id`

- **`refMarker` (String) (required)**: The marker used for references, i.e., usually "ref" or "id".

   Default value: `ref`

- **`lexicalMarker` (String) (required)**: The Toolbox marker that precedes lines with source text 
(usually "words") without the preceding backslash.

   Default value: `tx`

- **`morphologyMarker` (String)**: The Toolbox marker that precedes lines with morphological 
information, without the preceding backslash.

   Default value: `mb`

- **`lexAnnotationMarkers` (String)**: All Toolbox markers which precede lines with 
annotations of source text segments (usually "words"), without the preceding 
backslashes, and as a comma-separated list.

- **`morphologyAnnotationMarkers` (String)**: All Toolbox markers which precede lines with 
annotations of morphemes, without the preceding backslashes, and as a 
comma-separated list.

   Default value: `ge,ps`	

- **`attachDelimiter` (String)**: Whether detached morphology delimiters (as in "item 
- item" or similar) should be attached to the previous or subsequent item, as a 
two-item comma-separated list, where the first item signifies whether the 
delimiter should be attached at all (if `true` it will be attached), and the 
second item signifies whether the delimiter should be attached to the 
**subsequent** item (if `true` it will be attached to the subsequent item, 
making the latter a suffix).

   Default value: `true,true`

- **`morphemeDelimiters` (String)**: The morpheme delimiters used in the Toolbox files as a 
comma-separated two-point list where the first element is the **affix** 
delimiter, and the second element is the **clitics** delimiter.

Default value: `-,=`

- **`liaisonDelimiter` (String)**: The morpheme delimiter used in the Toolbox files to mark 
"words" represented on the morphological layer that are contracted into words on
the lexical layer, e.g., Saliba `tane = ta wane`. This delimiter can be used for 
cases where the importer may otherwise not have enough information to figure out 
that the lexical word should contain the "morphological word".

   It will be **dropped after parsing** and will not show up in either the Salt model
or any further model transformations.  

   The marker is only picked up when used to **suffix the second to nth word**, 
i.e. for the Saliba example above, `ta _wane` (property default is the 
underscore `_`) will be mapped as two items on the morphological layer which are 
ruled by one item on the lexical layer:  

   ```
     lex: | tane      |
          |-----------|
   morph: | ta | wane |
   ```

   Default value: `_`

- **`subrefDefinitionMarker` (String)**: The marker used to define *subref*s.

   Default value: `subref`

- **`subrefAnnotationMarkers` (String)**: The marker which precedes lines with 
annotations that can potentially span subranges of the complete morphological 
data source. For details about *subref*s see the respective 
[MelaTAMP wiki page](https://wikis.hu-berlin.de/melatamp/Clause_segmentation_and_annotation).

- **`mergeDuplMarkers` (Boolean) (required)**: Whether lines with the same marker in the same block 
should be merged into one line.

   `true`: Subsequent lines marked with {marker} are concatenated to the first 
   line marked with {marker}.

   `false`: All lines but the first line marked with {marker} are dropped.

   Default value: `true`

- **`recordErrors` (Boolean) (required)**: Whether the importer should record errors.

   `true` (default): Errors in the data model will be recorded, i.e., annotations
	on an error layer (called `err`) will be added for each line which
	seems to contain an error. Additionally, another annotation will be added
	to discrete layers, recording the original faulty line.
	 
	`false`: Errors will not be recorded. 

	Default value: `true`

- **`normalizeMarkers` (Boolean) (required)**: Whether annotation namespace-name combinations for the 
default layers should be normalized to Toolbox standards (after the default values
for *ref*s, *subref*s, *lexical* and *morphological* markers). 

	Default value: `false`

- **`normalizeDocNames` (Boolean) (required)**: Whether special characters and whitespaces 
in document names should be replaced with default characters.

   Default value: `true`

- **`fixInterl11n` (Boolean) (required)**: Whether the importer should fix interlinearization.

   `true` (default): Interlinearization error in the data model will be fixed as 
   follows.
   
	- For **discrepancies between the number of lexical and morphological
	tokens**, morphological tokens will either be added to until their 
	number is equal to that of lexical tokens (using the property `missingAnnoString`),
	or all tokens at indices >
	index of the last lexical token will be dropped.
	 
	- For **discrepancies between the number of tokens and their annotations**
	as defined by `lexAnnotationMarkers` and 
	`morphologyAnnotationMarkers`, annotations will either be
	added to until their number is equal to that of the token layer they
	refer to, or all tokens at indices > index of last token they refer
	to will be dropped.
	 
	`false`: Interlinearization errors will not be fixed. For missing morphological tokens
	or annotations, nothing will be inserted. Morphological tokens and
	annotations at indices > last index of lexical token, or last index
	of token layer they refer to will, respectively, be concatenated to the last element
	on their line, and separated by whitespaces.
	 
	**NOTE:** If the property is set to `false`, unfixed interl11n errors may
	cause an exception to be thrown during runtime!

	Default value: `true`
	
- **`missingAnnoString` (String) (required)**: A String used to fill interlinearization gaps.

   Default value: `***`

## Exporter

### Properties

Note that required values with a default value do not have to be specified in the
workflow file when the default value should be used.

- **`refSpanLayer` (String) (required)**: The Salt layer that contains the spans to be mapped to Toolbox *ref*s.

   Default value: `ref`

- **`idSpanLayer` (String) (required)**: The Salt layer that contains the spans to be mapped to Toolbox *id*s.

   Default value: `id`

- **`txTokenLayer` (String) (required)**: The Salt layer that contains the tokens to be mapped to Toolbox' *tx* lines.

   Default value: `tx`

- **`mbTokenLayer` (String)**: The Salt layer that contains the tokens to be mapped to Toolbox' *mb* lines.

   Default value: `mb`
	
- **`idIdentifierAnnotation` (String) (required)**: The annotation (`namespace::name`) 
that contains the identifiers of *id*s.

- **`refIdentifierAnnotation` (String) (required)**: The annotation (`namespace::name`) 
that contains the identifiers of *ref*s.
	
- **`txMaterialAnnotations` (String)**: Comma-separated list of annotations which contain primary data, i.e., 
lexical material which will already be mapped to tokens but still exists as 
annotation and should thus be left out during export to annotations (as they will
already be mapped to \tx).
	
- **`mbMaterialAnnotations` (String)**: Comma-separated list of annotations which contain primary data, i.e., 
morphological material which will already be mapped to tokens but still exists as 
annotation and should thus be left out during export to annotations (as they will
already be mapped to \mb).
	
- **`spaceReplacement` (String) (required)**: String to replace whitespaces in annotation 
values with, as these whitespaces may break the item count in Toolbox 
interlinearization.

   Default value: `-`	

- **`mdfMap` (String)**: A map whose keys are annotations with the pattern 
`namespace::name`, and whose values are MDF markers.

   In the export process, the value for the defined {@link SAnnotation} will
   be mapped to the line marked with the respective MDF pattern.

	- Example: `morph::gloss:ge` will map an annotation `morph::gloss:myglossvalue`
	to `\ge myglossvalue`.

	The map is defined in the format `namespace::name:marker,namespace::name:marker,...`.


- **`customMarkers` (String)**: A map whose keys are annotations with the pattern 
`namespace::name`, and whose values are custom markers supplementing the existing
MDF markers.

   In the export process, the value for the defined {@link SAnnotation} will
   be mapped to the line marked with the respective MDF pattern.

## Contribute

Contributions are welcome! When contributing to this repository, please first 
discuss the change you wish to make via a 
[new issue](https://github.com/sdruskat/pepperModules-ToolboxTextModules/issues/new), 
before making a change.

## Contributors

An overview of contributors to this project can be found 
[here](https://github.com/sdruskat/pepperModules-ToolboxTextModules/graphs/contributors).

## License

```
Copyright (c) 2016ff. Stephan Druskat.
Exploitation rights belong exclusively to Humboldt-Universität zu Berlin.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```