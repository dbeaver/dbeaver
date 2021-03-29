/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorActivator;

import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.stream.Collectors;

public class SQLReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {
    private static final Log log = Log.getLog(SQLReconcilingStrategy.class);

    private final NavigableSet<SQLScriptElementImpl> cache = new TreeSet<>();

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
        if (DirtyRegion.INSERT.equals(dirtyRegion.getType())) {
            reconcile(subRegion.getOffset(), subRegion.getLength());
        } else {
            reconcile(subRegion.getOffset(), 0);
        }
    }

    @Override
    public void reconcile(IRegion partition) {
        reconcile(partition.getOffset(), partition.getLength());
    }

    @Override
    public void initialReconcile() {
        reconcileAllDocument();
    }

    private Set<SQLScriptElementImpl> getSavedCollapsedPositions() {
        IResource resource = getResource();
        if (resource == null) {
            return Collections.emptySet();
        }
        QualifiedName name = getQualifiedName();
        String data;
        try {
            data = resource.getPersistentProperty(name);
        } catch (CoreException e) {
            log.warn("Core Exception caught while reading saved collapsed folding positions", e);
            return Collections.emptySet();
        }
        if (data == null) {
            return Collections.emptySet();
        }

        Set<SQLScriptElementImpl> collapsedPositions = new HashSet<>();
        String[] positions = data.split(";");
        for (String position: positions) {
            String[] integers = position.split(",");
            if (integers.length != 2) {
                log.warn("Position with illegal format read while reading saved collapsed folding positions. position=" + position);
                continue;
            }
            int offset = CommonUtils.toInt(integers[0], -1);
            int length = CommonUtils.toInt(integers[1], -1);
            if (offset == -1 || length == -1) {
                log.warn("Position with offset or/and length read while reading saved collapsed folding positions. position=" + position);
                continue;
            }
            SQLScriptElementImpl scriptPosition = new SQLScriptElementImpl(offset, length, new ProjectionAnnotation());
            collapsedPositions.add(scriptPosition);
        }

        return collapsedPositions;
    }

    //format: "offset_1,length_1;offset_2,length_2;...offset_n,length_n"
    public void saveState() {
        IResource resource = getResource();
        ProjectionAnnotationModel annotationModel = editor.getAnnotationModel();
        if (resource == null || annotationModel == null) {
            return;
        }
        StringJoiner stringJoiner = new StringJoiner(";");
        for (SQLScriptElementImpl position: cache) {
            ProjectionAnnotation annotation = position.getAnnotation();
            if (annotation != null && annotation.isCollapsed()) {
                stringJoiner.add(position.getOffset() + "," + position.getLength());
            }
        }
        String value;
        if (stringJoiner.length() == 0) {
            value = null;
        } else {
            value = stringJoiner.toString();
        }
        try {
            resource.setPersistentProperty(getQualifiedName(), value);
        } catch (CoreException e) {
            log.warn("Core Exception caught while writing saved collapsed folding positions", e);
        }
    }

    @Nullable
    private IResource getResource() {
        return EditorUtils.getFileFromInput(editor.getEditorInput());
    }

    @NotNull
    private static QualifiedName getQualifiedName() {
        return new QualifiedName(SQLEditorActivator.PLUGIN_ID, SQLReconcilingStrategy.class.getName() + ".collapsedFoldingAnnotations");
    }

    public void onDataSourceChange() {
        if (document == null) {
            return;
        }
        reconcileAllDocument();
    }

    private void reconcileAllDocument() {
        if (!editor.isFoldingEnabled()) {
            return;
        }
        ProjectionAnnotationModel model = editor.getAnnotationModel();
        if (model == null) {
            return;
        }
        cache.clear();
        model.removeAllAnnotations();

        List<SQLScriptElement> parsedQueries = extractQueries(0, document.getLength());
        if (parsedQueries == null) {
            return;
        }

        Map<Annotation, SQLScriptElementImpl> additions = new HashMap<>();
        Set<SQLScriptElementImpl> savedCollapsedPositions = getSavedCollapsedPositions();
        for (SQLScriptElement scriptElement: parsedQueries) {
            if (!deservesFolding(scriptElement)) {
                continue;
            }
            SQLScriptElementImpl scriptPosition = getExpandedScriptPosition(scriptElement);
            ProjectionAnnotation annotation = new ProjectionAnnotation();
            scriptPosition.setAnnotation(annotation);
            additions.put(annotation, scriptPosition);
            if (savedCollapsedPositions.contains(scriptPosition)) {
                annotation.markCollapsed();
            }
        }
        model.modifyAnnotations(null, additions, null);
        cache.addAll(additions.values());
    }

    private void reconcile(int damagedRegionOffset, int damagedRegionLength) {
        if (!editor.isFoldingEnabled()) {
            return;
        }
        ProjectionAnnotationModel model = editor.getAnnotationModel();
        if (model == null) {
            return;
        }

        SQLScriptElementImpl leftBound = cache.lower(new SQLScriptElementImpl(damagedRegionOffset, damagedRegionLength));
        if (leftBound != null) {
            leftBound = cache.lower(leftBound);
        }
        SQLScriptElementImpl rightBound = cache.ceiling(new SQLScriptElementImpl(damagedRegionOffset + damagedRegionLength, 0));
        if (leftBound == null) {
            damagedRegionOffset = 0;
        } else {
            damagedRegionOffset = leftBound.getOffset() + leftBound.getLength();
        }
        if (rightBound == null) {
            damagedRegionLength = document.getLength();
        } else {
            damagedRegionLength = rightBound.getOffset() + rightBound.getLength() - damagedRegionOffset;
        }

        List<SQLScriptElement> parsedQueries = extractQueries(damagedRegionOffset, damagedRegionLength);
        if (parsedQueries == null) {
            return;
        }

        if (rightBound != null && !parsedQueries.isEmpty()) {
            SQLScriptElement rightmostParsedQuery = parsedQueries.get(parsedQueries.size() - 1);
            if (!rightBound.equals(getExpandedScriptPosition(rightmostParsedQuery))) {
                parsedQueries = extractQueries(damagedRegionOffset, document.getLength());
                if (parsedQueries == null) {
                    return;
                }
                rightBound = null;
            }
        }

        Collection<SQLScriptElementImpl> cachedQueries;
        if (leftBound == null && rightBound == null) {
            cachedQueries = Collections.unmodifiableNavigableSet(cache);
        } else if (leftBound == null) {
            cachedQueries = Collections.unmodifiableNavigableSet(cache.headSet(rightBound, true));
        } else if (rightBound == null) {
            cachedQueries = Collections.unmodifiableNavigableSet(cache.tailSet(leftBound, false));
        } else {
            cachedQueries = Collections.unmodifiableNavigableSet(cache.subSet(leftBound, false, rightBound, true));
        }

        List<SQLScriptElementImpl> parsedElements = parsedQueries.stream()
                .filter(this::deservesFolding)
                .map(this::getExpandedScriptPosition)
                .collect(Collectors.toList());
        Map<Annotation, SQLScriptElementImpl> additions = new HashMap<>();
        for (SQLScriptElementImpl element: parsedElements) {
            if (!cachedQueries.contains(element)) {
                element.setAnnotation(new ProjectionAnnotation());
                additions.put(element.getAnnotation(), element);
            }
        }
        Collection<SQLScriptElementImpl> deletedPositions = cachedQueries.stream()
                .filter(element -> !parsedElements.contains(element))
                .collect(Collectors.toList());
        Annotation[] deletions = deletedPositions.stream()
                .map(SQLScriptElementImpl::getAnnotation)
                .toArray(Annotation[]::new);
        model.modifyAnnotations(deletions, additions, null);
        cache.removeAll(deletedPositions);
        cache.addAll(additions.values());
    }

    @Nullable
    private List<SQLScriptElement> extractQueries(int offset, int length) {
        return editor.extractScriptQueries(offset, length, false, true, false);
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
    private int expandQueryLength(SQLScriptElement element) { //todo simplify
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

    @NotNull
    private SQLScriptElementImpl getExpandedScriptPosition(@NotNull SQLScriptElement element) {
        return new SQLScriptElementImpl(element.getOffset(), expandQueryLength(element));
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

        SQLScriptElementImpl(int offset, int length, @NotNull ProjectionAnnotation annotation) {
            super(offset, length);
            this.annotation = annotation;
        }

        @Nullable
        public ProjectionAnnotation getAnnotation() {
            return annotation;
        }

        public void setAnnotation(@Nullable ProjectionAnnotation annotation) {
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
            if (o instanceof Position) {
                Position p = (Position) o;
                return equals(p.getOffset(), p.getLength());
            }
            if (o instanceof SQLScriptElement) {
                SQLScriptElement e = (SQLScriptElement) o;
                return equals(e.getOffset(), e.getLength());
            }
            return false;
        }

        private boolean equals(int offset, int length) {
            return getOffset() == offset && getLength() == length;
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
