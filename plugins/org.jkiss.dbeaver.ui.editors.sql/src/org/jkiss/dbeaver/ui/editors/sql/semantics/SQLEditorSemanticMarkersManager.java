/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.antlr.v4.runtime.misc.Interval;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.ui.AbstractUIJob;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SQLEditorSemanticMarkersManager {

    private static final Log log = Log.getLog(SQLEditorSemanticMarkersManager.class);

    @NotNull
    private final ITextInputListener textInputListener = new ITextInputListener() {

        @Override
        public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
            refresh();
        }

        @Override
        public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
        }
    };

    private final SQLDocumentSyntaxContextListener syntaxContextListener = new SQLDocumentSyntaxContextListener() {
        @Override
        public void onScriptItemIntroduced(@NotNull SQLDocumentScriptItemSyntaxContext item) {
            synchronized (syncRoot) {
                queuedOperations.put(item, true);
                scheduleRefreshJob();
            }
        }

        @Override
        public void onScriptItemInvalidated(@NotNull SQLDocumentScriptItemSyntaxContext item) {
            synchronized (syncRoot) {
                queuedOperations.put(item, false);
                scheduleRefreshJob();
            }
        }

        @Override
        public void onAllScriptItemsInvalidated() {
            scheduleClearAllProblems();
        }
    };

    @NotNull
    private final AbstractUIJob refreshJob = new AbstractUIJob("SQL editor error markers refresh") {
        @NotNull
        @Override
        protected IStatus runInUIThread(@NotNull DBRProgressMonitor monitor) {
            updateMarkers();
            return Status.OK_STATUS;
        }
    };

    @NotNull
    private final SQLEditorBase editor;
    @Nullable
    private volatile SQLDocumentSyntaxContext syntaxContext;
    @NotNull
    private final Object syncRoot = new Object();
    @NotNull
    private final Map<SQLDocumentScriptItemSyntaxContext, Boolean> queuedOperations = new HashMap<>();
    @NotNull
    private final Map<SQLDocumentScriptItemSyntaxContext, Deque<SQLSemanticErrorAnnotation>> annotations = new HashMap<>();
    private volatile boolean resetAnnotations = false;

    public SQLEditorSemanticMarkersManager(@NotNull SQLEditorBase editor) {
        this.editor = editor;
        this.setup();
    }

    private void updateMarkers() {
        final IResource resource = GeneralUtils.adapt(this.editor.getEditorInput(), IResource.class);
        final IAnnotationModel annotationModel = this.editor.getAnnotationModel();
        if (resource == null || annotationModel == null) {
            return;
        }

        Map.Entry<SQLDocumentScriptItemSyntaxContext, Boolean>[] entries;
        synchronized (syncRoot) {
            if (this.resetAnnotations) {
                try {
                    resource.deleteMarkers(SQLSemanticErrorAnnotation.MARKER_TYPE, false, IResource.DEPTH_ONE);
                } catch (CoreException e) {
                    log.error("Error deleting problem markers: " + e.getMessage());
                }
                this.resetAnnotations = false;
            }
            entries = this.queuedOperations.entrySet().toArray(new Map.Entry[0]);
            this.queuedOperations.clear();
        }

        for (Map.Entry<SQLDocumentScriptItemSyntaxContext, Boolean> entry : entries) {
            SQLDocumentScriptItemSyntaxContext scriptItem = entry.getKey();
            if (entry.getValue() && scriptItem.getProblems() != null) {
                Map<Integer, SQLSemanticErrorAnnotation> severestAnnotationsByLine = new HashMap<>();
                Deque<SQLSemanticErrorAnnotation> itemAnnotations = this.annotations.computeIfAbsent(scriptItem, c -> new LinkedList<>());
                int scriptItemPosition = scriptItem.getInitialPosition();
                for (SQLQueryRecognitionProblemInfo problemInfo : scriptItem.getProblems()) {
                    try {
                        Interval problemInterval = problemInfo.getInterval();
                        int problemOffset = scriptItemPosition + problemInterval.a;
                        int problemLine = this.editor.getDocument().getLineOfOffset(problemOffset);
                        // We associate position objects with annotations, but don't introduce this information into the markers, because
                        // otherwise eclipse breaks annotation location, margin marker's per-line icon rendering and aggregation.
                        final IMarker marker = resource.createMarker(SQLSemanticErrorAnnotation.MARKER_TYPE, Map.of(
                                IMarker.SEVERITY, problemInfo.getSeverity().markerSeverity,
                                IMarker.MESSAGE, problemInfo.getMessage(),
                                IMarker.TRANSIENT, true
                        ));
                        SQLSemanticErrorAnnotation annotation = new SQLSemanticErrorAnnotation(marker, problemInfo);
                        marker.setAttribute(SQLSemanticErrorAnnotation.MARKER_ATTRIBUTE_NAME, annotation);
                        Position position = new Position(problemOffset, problemInterval.length());
                        annotationModel.addAnnotation(annotation, position);
                        itemAnnotations.addLast(annotation);
                        severestAnnotationsByLine.compute(problemLine, (k, v) -> (v == null ||
                            annotation.getProblemMarkerSeverity() > v.getProblemMarkerSeverity()
                        ) ? annotation : v);
                    } catch (CoreException|BadLocationException e) {
                        log.error("Error creating problem marker", e);
                    }
                }
                for (SQLSemanticErrorAnnotation annotation: severestAnnotationsByLine.values()) {
                    annotation.setMarginMarkerVisible(true);
                }
            } else {
                Deque<SQLSemanticErrorAnnotation> itemAnnotations = this.annotations.remove(scriptItem);
                if (itemAnnotations != null) {
                    for (SQLSemanticErrorAnnotation annotation : itemAnnotations) {
                        IMarker marker = annotation.getMarker();
                        try {
                            marker.setAttribute(SQLSemanticErrorAnnotation.MARKER_ATTRIBUTE_NAME, null);
                        } catch (CoreException e) {
                            log.error("Error dissociating problem marker", e);
                        }
                        annotationModel.removeAnnotation(annotation);
                        try {
                            marker.delete();
                        } catch (CoreException e) {
                            log.error("Error deleting problem marker", e);
                        }
                    }
                }
            }
        }
    }

    public void refresh() {
        SQLDocumentSyntaxContext actualContext = this.editor.getSyntaxContext();
        synchronized (this.syncRoot) {
            SQLDocumentSyntaxContext currentContext = this.syntaxContext;
            boolean clearScheduled = false;
            if (actualContext != currentContext) {
                if (currentContext != null) {
                    this.cleanup();
                    clearScheduled = true;
                }
                if (actualContext != null) {
                    this.setup();
                }
            }
            {
                if (!clearScheduled) {
                    queuedOperations.clear();
                    resetAnnotations = true;
                }
                SQLDocumentSyntaxContext context = this.syntaxContext;
                if (context != null) {
                    for (SQLScriptItemAtOffset itemAtOffset : context.getScriptItems()) {
                        queuedOperations.put(itemAtOffset.item, true);
                    }
                }
                if (!clearScheduled) {
                    scheduleRefreshJob();
                }
            }
        }
    }

    private void scheduleClearAllProblems() {
        synchronized (syncRoot) {
            queuedOperations.clear();
            resetAnnotations = true;
            scheduleRefreshJob();
        }
    }

    private void scheduleRefreshJob() {
        this.refreshJob.schedule(500);
    }

    private void setup() {
        SQLDocumentSyntaxContext context = this.editor.getSyntaxContext();
        if (context != null) {
            this.syntaxContext = context;
            context.addListener(this.syntaxContextListener);
            this.scheduleRefreshJob();
        }
        TextViewer textViewer = this.editor.getTextViewer();
        if (textViewer != null) {
            textViewer.addTextInputListener(this.textInputListener);
        }
    }

    private void cleanup() {
        TextViewer textViewer = this.editor.getTextViewer();
        if (textViewer != null) {
            textViewer.removeTextInputListener(this.textInputListener);
        }

        SQLDocumentSyntaxContext context = this.syntaxContext;
        if (context != null) {
            context.removeListener(this.syntaxContextListener);
            this.syntaxContext = null;
        }
        this.scheduleClearAllProblems();
    }

    /**
     * Release associated resources
     */
    public void dispose() {
        this.cleanup();
    }

}
