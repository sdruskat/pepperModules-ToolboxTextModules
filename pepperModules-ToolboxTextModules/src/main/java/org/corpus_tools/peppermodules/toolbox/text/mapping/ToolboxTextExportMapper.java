package org.corpus_tools.peppermodules.toolbox.text.mapping;

import java.util.List;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.peppermodules.toolbox.text.AbstractToolboxTextMapper;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.util.SaltUtil;

public class ToolboxTextExportMapper extends AbstractToolboxTextMapper {
	/**
	 * Stores each document-structure to location given by
	 * {@link #getResourceURI()}.
	 */
	@Override
	public DOCUMENT_STATUS mapSDocument() {
		// workaround to deal with a bug in Salt
		SCorpusGraph sCorpusGraph = getDocument().getGraph();

		SaltUtil.save_DOT(getDocument(), getResourceURI());

		// workaround to deal with a bug in Salt
		if (getDocument().getGraph() == null) {
			getDocument().setGraph(sCorpusGraph);
		}

		addProgress(1.0);
		return (DOCUMENT_STATUS.COMPLETED);
	}

	/**
	 * Storing the corpus-structure once
	 */
	@Override
	public DOCUMENT_STATUS mapSCorpus() {
		List<SNode> roots = getCorpus().getGraph().getRoots();
		if ((roots != null) && (!roots.isEmpty())) {
			if (getCorpus().equals(roots.get(0))) {
				SaltUtil.save_DOT(getCorpus().getGraph(), getResourceURI());
			}
		}

		return (DOCUMENT_STATUS.COMPLETED);
	}
}