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
import org.eclipse.jgit.annotations.Nullable;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

import java.util.*;
import java.util.stream.Collectors;

public class SQLReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {
    private final Collection<SQLScriptElementImpl> cache = new TreeSet<>();

    private final SQLEditorBase editor;

    private IDocument document;

    public SQLReconcilingStrategy(SQLEditorBase editor) {
        this.editor = editor;
    }

    @Override
    public void setDocument(IDocument document) {
        this.document = document;
    }

    @Override
    public void setProgressMonitor(IProgressMonitor monitor) {
        //todo use monitor
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
    public void initialReconcile() {
        reconcile();
    }

    private void reconcile() {
        if (!editor.isFoldingEnabled()) {
            return;
        }
        ProjectionAnnotationModel model = editor.getAnnotationModel();
        if (model == null) {
            return;
        }
        Collection<SQLScriptElement> queries = editor.extractScriptQueries(0, document.getLength(), false, true, false);
        if (queries == null) {
            return;
        }
        reconcile(model, queries);
    }

    private void reconcile(ProjectionAnnotationModel model, Collection<SQLScriptElement> parsedQueries) {
        Collection<SQLScriptElementImpl> parsedElements = parsedQueries.stream()
                .filter(this::deservesFolding)
                .map(element -> new SQLScriptElementImpl(element.getOffset(), expandQueryLength(element)))
                .collect(Collectors.toSet());
        Map<Annotation, SQLScriptElementImpl> additions = new HashMap<>();
        parsedElements.forEach(element -> {
            if (!cache.contains(element)) {
                element.setAnnotation(new ProjectionAnnotation());
                additions.put(element.getAnnotation(), element);
            }
        });
        Collection<SQLScriptElementImpl> deletedPositions = cache.stream()
                .filter(element -> !parsedElements.contains(element))
                .collect(Collectors.toList());
        Annotation[] deletions = deletedPositions.stream()
                .map(SQLScriptElementImpl::getAnnotation)
                .toArray(Annotation[]::new);
        model.modifyAnnotations(deletions, additions, null);
        cache.removeAll(deletedPositions);
        cache.addAll(additions.values());
    }

    private boolean deservesFolding(SQLScriptElement element) {
        int numberOfLines = getNumberOfLines(element);
        if (numberOfLines == 1) {
            return false;
        }
        if (element.getOffset() + element.getLength() != document.getLength() && expandQueryLength(element) == element.getLength()) {
            return numberOfLines > 2;
        }
        return true;
    }

    private int getNumberOfLines(SQLScriptElement element) {
        try {
            return document.getLineOfOffset(element.getOffset() + element.getLength()) - document.getLineOfOffset(element.getOffset()) + 1;
        } catch (BadLocationException e) {
            throw new SQLReconcilingStrategyException(e);
        }
    }

    //expands query to the end of the line if there are only whitespaces after it. Returns desired length.
    private int expandQueryLength(SQLScriptElement element) {
        int position = element.getOffset() + element.getLength();
        while (position < document.getLength()) {
            char c = unsafeGetChar(position);
            if (c == '\n') {
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

    private static class SQLReconcilingStrategyException extends RuntimeException {
        private SQLReconcilingStrategyException(Throwable cause) {
            super(cause);
        }
    }

    private static class SQLScriptElementImpl extends Position implements SQLScriptElement, Comparable<SQLScriptElementImpl> {
        @Nullable
        private ProjectionAnnotation annotation;

        SQLScriptElementImpl(int offset, int length) {
            super(offset, length);
        }

        @Nullable
        public ProjectionAnnotation getAnnotation() {
            return annotation;
        }

        public void setAnnotation(ProjectionAnnotation annotation) {
            this.annotation = annotation;
        }

        @Override
        public int compareTo(@NotNull SQLScriptElementImpl o) {
            int diff = getOffset() - o.getOffset();
            if (diff != 0) {
                return diff;
            }
            return getLength() - o.getLength();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Position)) {
                return false;
            }
            Position p = (Position) o;
            return getOffset() == p.getOffset() && getLength() == p.getLength();
        }

        @Override
        public int hashCode() {
            return Objects.hash(getOffset(), getLength());
        }

        @NotNull
        @Override
        public String getOriginalText() {
            return "";
        }

        @NotNull
        @Override
        public String getText() {
            return "";
        }

        @Override
        public Object getData() {
            return "";
        }

        @Override
        public void setData(Object data) {
            //do nothing
        }

        @Override
        public void reset() {
            //do nothing
        }
    }
}
