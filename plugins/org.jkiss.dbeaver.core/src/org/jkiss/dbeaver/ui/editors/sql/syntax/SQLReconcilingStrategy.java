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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

import java.util.*;

/**
 * SQLReconcilingStrategy
 */
public class SQLReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension
{
    static protected final Log log = Log.getLog(SQLReconcilingStrategy.class);

    private SQLEditorBase editor;
    private IDocument document;

    private int regionOffset;
    private int regionLength;

    public SQLEditorBase getEditor()
    {
        return editor;
    }

    public void setEditor(SQLEditorBase editor)
    {
        this.editor = editor;
    }

    @Override
    public void setDocument(IDocument document)
    {
        this.document = document;
    }

    @Override
    public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion)
    {
        regionOffset = dirtyRegion.getOffset();
        regionLength = dirtyRegion.getLength();
        calculatePositions();
    }

    @Override
    public void reconcile(IRegion partition)
    {
        regionOffset = partition.getOffset();
        regionLength = partition.getLength();
        calculatePositions();
    }

    @Override
    public void setProgressMonitor(IProgressMonitor monitor)
    {
    }

    @Override
    public void initialReconcile()
    {
        regionOffset = 0;
        regionLength = document.getLength();
        calculatePositions();
    }

    private List<SQLScriptPosition> parsedPositions = new ArrayList<>();

    protected void calculatePositions()
    {
        ProjectionAnnotationModel annotationModel = editor.getAnnotationModel();
        if (annotationModel == null) {
            return;
        }
        List<SQLScriptElement> queries = editor.extractScriptQueries(regionOffset, regionLength, false, true);

        Annotation[] removedAnnotations = null;
        Map<Annotation, Position> addedAnnotations = null;

        {
            List<SQLScriptPosition> removedPositions = new ArrayList<>();
            for (SQLScriptPosition sp : parsedPositions) {
                if (sp.getOffset() >= regionOffset && sp.getOffset() <= regionOffset + regionLength) {
                    removedPositions.add(sp);
                }
            }
            if (!removedPositions.isEmpty()) {
                parsedPositions.removeAll(removedPositions);
                removedAnnotations = new Annotation[removedPositions.size()];
                for (int i = 0; i < removedPositions.size(); i++) {
                    removedAnnotations[i] = removedPositions.get(i).getFoldingAnnotation();
                }
            }
        }

        try {
            List<SQLScriptPosition> addedPositions = new ArrayList<>();
            int documentLength = document.getLength();
            for (SQLScriptElement se : queries) {
                int queryOffset = se.getOffset();
                int queryLength = se.getLength();
                // Expand query to the end of line
                for (int i = queryOffset + queryLength; i < documentLength; i++) {
                    char ch = document.getChar(i);
                    if (Character.isWhitespace(ch)) {
                        queryLength++;
                    }
                    if (ch == '\n') {
                        break;
                    }
                }
                addedPositions.add(new SQLScriptPosition(queryOffset, queryLength, new ProjectionAnnotation()));
            }
            parsedPositions.addAll(addedPositions);

            if (!addedPositions.isEmpty()) {
                addedAnnotations = new HashMap<>();
                for (SQLScriptPosition pos : addedPositions) {
                    addedAnnotations.put(pos.getFoldingAnnotation(), pos);
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
        if (removedAnnotations != null || addedAnnotations != null) {
            annotationModel.modifyAnnotations(
                removedAnnotations,
                addedAnnotations,
                null);
        }
    }

}

