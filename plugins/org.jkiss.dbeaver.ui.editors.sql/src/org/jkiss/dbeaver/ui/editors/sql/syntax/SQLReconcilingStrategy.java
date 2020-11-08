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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

import java.util.*;

/**
 * SQLReconcilingStrategy
 */
public class SQLReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {
    private static final Log log = Log.getLog(SQLReconcilingStrategy.class);

    private Collection<Annotation> oldAnnotations = new ArrayList<>();

    private SQLEditorBase editor;

    private IDocument document;

    private IProgressMonitor monitor;

    public SQLEditorBase getEditor() {
        return editor;
    }

    public void setEditor(SQLEditorBase editor) {
        this.editor = editor;
    }

    @Override
    public void setDocument(IDocument document) {
        this.document = document;
    }

    @Override
    public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
        reconcile();
    }

    @Override
    public void reconcile(IRegion partition) {
        reconcile();
    }

    @Override
    public void setProgressMonitor(IProgressMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void initialReconcile() {
        reconcile();
    }

    private void logDebug(int... values) { //fixme delete
        for (int i: values) {
            log.debug("i=+" + i + ", ch=" + unsafeGetChar(i));
        }
    }

    private void reconcile() {
        if (!editor.isFoldingEnabled()) {
            return;
        }
        ProjectionAnnotationModel model = editor.getAnnotationModel();
        if (editor == null) {
            return;
        }
        Iterable<SQLScriptElement> queries = getQueries();
        Map<Annotation, Position> newAnnotations = new HashMap<>();
        Collection<Annotation> annotations = new ArrayList<>();
        for (SQLScriptElement element: queries) {
            int numberOfLines = getNumberOfLines(element);
            if (numberOfLines == 1) {
                continue;
            }
            int newLength = calcLengthOfExpandedQueryToTheEndOfTheLine(element); //fixme needs rename
            if (numberOfLines == 2 && newLength == element.getLength() && element.getOffset() + element.getLength() != document.getLength()) {
                continue;
            }
            Annotation annotation = new ProjectionAnnotation();
            Position position = new Position(element.getOffset(), newLength);
            newAnnotations.put(annotation, position);
            annotations.add(annotation);
        }
        model.modifyAnnotations(oldAnnotations.toArray(new Annotation[0]), newAnnotations, null);
        oldAnnotations = annotations;
    }

    private int getNumberOfLines(SQLScriptElement element) {
        try {
            return document.getLineOfOffset(element.getOffset() + element.getLength()) - document.getLineOfOffset(element.getOffset()) + 1;
        } catch (BadLocationException e) {
            throw new SQLReconcilingStrategyException(e);
        }
    }

    //todo rename
    private int calcLengthOfExpandedQueryToTheEndOfTheLine(SQLScriptElement element) {
        int position = element.getOffset() + element.getLength();
        while (position < document.getLength()) {
            char c = unsafeGetChar(position);
            if (c == '\n') { //fixme really '\n'?
                if (position + 1 < document.getLength()) {
                    position++;
                    break;
                }
            }
            if (Character.isWhitespace(c)) {
                position++;
            } else {
                return element.getLength();
            }
        }
        return position - element.getOffset();
    }

    private char unsafeGetChar(int index) {
        try {
            return document.getChar(index);
        } catch (BadLocationException e) {
            throw new SQLReconcilingStrategyException(e);
        }
    }

    private Iterable<SQLScriptElement> getQueries() {
        List<SQLScriptElement> queries = unsafeGetQueries();
        if (queries == null) {
            editor.reloadParserContext();
        }
        return unsafeGetQueries();
    }

    @Nullable
    private List<SQLScriptElement> unsafeGetQueries() {
        return editor.extractScriptQueries(0, document.getLength(), false, true, false);
    }

    private static class SQLReconcilingStrategyException extends RuntimeException {
        private SQLReconcilingStrategyException(Throwable cause) {
            super(cause);
        }
    }

//    protected void calculatePositions(IRegion partition)
//    {
//        if (!editor.isFoldingEnabled()) {
//            return;
//        }
//
//        List<Annotation> removedAnnotations = null;
//        Map<Annotation, Position> addedAnnotations = null;
//
//        if (partition == null) {
//            regionOffset = 0;
//            regionLength = document.getLength();
//        } else {
//            regionOffset = partition.getOffset();
//            regionLength = partition.getLength();
//
//            for (int i = 0; i < parsedPositions.size(); i++) {
//                SQLScriptPosition sp = parsedPositions.get(i);
//                if (sp.getOffset() <= regionOffset + regionLength && sp.getOffset() + sp.getLength() >= regionOffset + regionLength) {
//                    SQLScriptPosition startPos = i > 0 ? parsedPositions.get(i - 1) : sp;
//                    SQLScriptPosition endPos = i < parsedPositions.size() - 1 ? parsedPositions.get(i + 1) : sp;
//                    regionOffset = i == 0 ? 0 : startPos.getOffset();
//                    regionLength = endPos.getOffset() + endPos.getLength() + regionLength;
//                    break;
//                }
//            }
//
///*
//            if (partition instanceof DirtyRegion) {
//                // Modify positions
//                DirtyRegion dirtyRegion = (DirtyRegion)partition;
//                // Shift parsed positions
//                boolean insert = DirtyRegion.INSERT.equals(dirtyRegion.getType());
//                for (SQLScriptPosition sp : parsedPositions) {
//                    if (dirtyRegion.getOffset() > sp.getOffset() && dirtyRegion.getOffset() < sp.getOffset() + sp.getLength()) {
//                        // In this position
//                        if (insert) {
//                            sp.setLength(sp.getLength() + dirtyRegion.getLength());
//                        } else {
//                            sp.setLength(sp.getLength() - dirtyRegion.getLength());
//                        }
//                    } else if (sp.getOffset() >= dirtyRegion.getOffset()) {
//                        // After this position
//                        if (insert) {
//                            sp.setOffset(sp.getOffset() + dirtyRegion.getLength());
//                        } else {
//                            sp.setOffset(sp.getOffset() - dirtyRegion.getLength());
//                        }
//                    }
//                }
//            }
//*/
//        }
//        ProjectionAnnotationModel annotationModel = editor.getAnnotationModel();
//        if (annotationModel == null) {
//            return;
//        }
//        List<SQLScriptElement> queries;
//        try {
//            queries = editor.extractScriptQueries(regionOffset, document.getLength() - regionOffset, false, true, false);
//        } catch (Exception e) {
//            log.error("Error parsing script queries", e);
//            return;
//        }
//
//        {
//            List<SQLScriptPosition> removedPositions = new ArrayList<>();
//            for (SQLScriptPosition sp : parsedPositions) {
//                if (sp.getOffset() >= regionOffset/* && sp.getOffset() <= regionOffset + regionLength*/) {
//                    removedPositions.add(sp);
//                }
//            }
//            if (!removedPositions.isEmpty()) {
//                parsedPositions.removeAll(removedPositions);
//                removedAnnotations = new ArrayList<>();
//                for (SQLScriptPosition removedPosition : removedPositions) {
//                    if (removedPosition.isMultiline()) {
//                        removedAnnotations.add(removedPosition.getFoldingAnnotation());
//                    }
//                }
//            }
//        }
//
//        try {
//            List<SQLScriptPosition> addedPositions = new ArrayList<>();
//            int documentLength = document.getLength();
//            for (SQLScriptElement se : queries) {
//                int queryOffset = se.getOffset();
//                int queryLength = se.getLength();
//
//                boolean isMultiline = document.getLineOfOffset(queryOffset) != document.getLineOfOffset(queryOffset + queryLength);
//
//                // Expand query to the end of line
//                for (int i = queryOffset + queryLength; i < documentLength; i++) {
//                    char ch = document.getChar(i);
//                    if (Character.isWhitespace(ch)) {
//                        queryLength++;
//                    }
//                    if (ch == '\n') {
//                        break;
//                    }
//                }
//                addedPositions.add(new SQLScriptPosition(queryOffset, queryLength, isMultiline, new ProjectionAnnotation()));
//            }
//            if (!addedPositions.isEmpty()) {
//                final int firstQueryPos = addedPositions.get(0).getOffset();
//                int posBeforeFirst = 0;
//                for (int i = 0; i < parsedPositions.size(); i++) {
//                    SQLScriptPosition sp = parsedPositions.get(i);
//                    if (sp.getOffset() >= firstQueryPos) {
//                        break;
//                    }
//                    posBeforeFirst = i;
//                }
//                parsedPositions.addAll(posBeforeFirst, addedPositions);
//
//                addedAnnotations = new HashMap<>();
//                for (SQLScriptPosition pos : addedPositions) {
//                    if (pos.isMultiline()) {
//                        addedAnnotations.put(pos.getFoldingAnnotation(), pos);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            log.error(e);
//        }
//        if (removedAnnotations != null || !CommonUtils.isEmpty(addedAnnotations)) {
//            annotationModel.modifyAnnotations(
//                removedAnnotations == null ? null : removedAnnotations.toArray(new Annotation[removedAnnotations.size()]),
//                addedAnnotations,
//                null);
//        }
//    }
}
