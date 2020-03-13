/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
 */
package org.jkiss.dbeaver.ui.editors.json;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.dbeaver.ui.editors.text.FileRefDocumentProvider;

/*
 * JSONTextEditor
 */
public class JSONTextEditor extends BaseTextEditor {

	private final static char[] PAIRS= { '{', '}', '[', ']' };

	private DefaultCharacterPairMatcher pairsMatcher = new DefaultCharacterPairMatcher(PAIRS);

	private ProjectionSupport projectionSupport;

	public JSONTextEditor() {
		super();
		setDocumentProvider(new FileRefDocumentProvider());
	}

	@Override
	public void dispose() {
		if (pairsMatcher != null) {
			pairsMatcher.dispose();
			pairsMatcher = null;
		}

		super.dispose();
	}

	@Override
	public void doRevertToSaved() {
		super.doRevertToSaved();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		super.doSave(monitor);
	}

	@Override
	public void doSaveAs() {
		super.doSaveAs();
	}

	@Override
	public void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		setupDocument();
	}

	private void setupDocument() {
		IDocument document = getDocument();
		if (document != null) {
			IDocumentPartitioner partitioner =
				new FastPartitioner(
					new JSONPartitionScanner(),
					new String[]{
						JSONPartitionScanner.JSON_STRING});
			partitioner.connect(document);
			((IDocumentExtension3) document).setDocumentPartitioner(JSONPartitionScanner.JSON_PARTITIONING, partitioner);
		}
	}


	@Override
	public void createPartControl(Composite parent) {
		setSourceViewerConfiguration(new JSONSourceViewerConfiguration(this));
		super.createPartControl(parent);
	}

	@Override
	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {

		ISourceViewer viewer = new ProjectionViewer(parent, ruler, null, false, styles);

		return viewer;
	}

}
