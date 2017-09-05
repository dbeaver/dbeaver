/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.xml;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditorExtension3;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.dbeaver.ui.editors.text.FileRefDocumentProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class XMLEditor extends BaseTextEditor {

    private ProjectionSupport projectionSupport;
    private Annotation[] oldAnnotations;
    private ProjectionAnnotationModel annotationModel;

    public XMLEditor() {
        configureInsertMode(ITextEditorExtension3.SMART_INSERT, false);
        setSourceViewerConfiguration(new XMLSourceViewerConfiguration(this));
        setDocumentProvider(new FileRefDocumentProvider());
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    protected void doSetInput(IEditorInput input) throws CoreException {
        super.doSetInput(input);
        setupDocument();
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);

        ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();

        projectionSupport = new ProjectionSupport(viewer, getAnnotationAccess(), getSharedColors());
        projectionSupport.install();

        //turn projection mode on
        viewer.doOperation(ProjectionViewer.TOGGLE);

        annotationModel = viewer.getProjectionAnnotationModel();
    }

    private void setupDocument() {
        IDocument document = getDocument();
        if (document != null) {
            IDocumentPartitioner partitioner =
                new FastPartitioner(
                    new XMLPartitionScanner(),
                    new String[]{
                        XMLPartitionScanner.XML_TAG,
                        XMLPartitionScanner.XML_COMMENT});
            partitioner.connect(document);
            ((IDocumentExtension3) document).setDocumentPartitioner(XMLPartitionScanner.XML_PARTITIONING, partitioner);
        }
    }

    protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles)
    {
        ISourceViewer viewer = new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);

        // ensure decoration support has been created and configured.
        getSourceViewerDecorationSupport(viewer);

        return viewer;
    }

    void updateFoldingStructure(List<Position> positions) {
        Annotation[] annotations = new Annotation[positions.size()];

        //this will hold the new annotations along
        //with their corresponding positions
        Map<Annotation, Position> newAnnotations = new HashMap<>();

        for (int i = 0; i < positions.size(); i++) {
            ProjectionAnnotation annotation = new ProjectionAnnotation();

            newAnnotations.put(annotation, positions.get(i));

            annotations[i] = annotation;
        }

        annotationModel.modifyAnnotations(oldAnnotations, newAnnotations, null);

        oldAnnotations = annotations;
    }

}
