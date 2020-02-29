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
package org.jkiss.dbeaver.ui.editors.sql;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.text.parser.TPWordDetector;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL occurrences highlighter
 */
class SQLOccurrencesHighlighter {

    static protected final Log log = Log.getLog(SQLOccurrencesHighlighter.class);

    private final SQLEditorBase editor;
    private EditorSelectionChangedListener selectionChangedListener;
    private Annotation[] occurrenceAnnotations = null;
    private boolean markOccurrencesUnderCursor;
    private boolean markOccurrencesForSelection;
    private OccurrencesFinderJob occurrencesFinderJob;
    private OccurrencesFinderJobCanceler occurrencesFinderJobCanceler;

    private final Object LOCK_OBJECT = new Object();

    SQLOccurrencesHighlighter(SQLEditorBase editor) {
        this.editor = editor;
        this.markOccurrencesUnderCursor = DBWorkbench.getPlatform().getPreferenceStore().getBoolean(SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR);
        this.markOccurrencesForSelection = DBWorkbench.getPlatform().getPreferenceStore().getBoolean(SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION);
    }

    public boolean isEnabled() {
        return markOccurrencesUnderCursor || this.markOccurrencesForSelection;
    }

    /////////////////////////////////////////////////////////////////
    // Occurrences highlight

    private void updateOccurrenceAnnotations(ITextSelection selection) {
        if (this.occurrencesFinderJob != null) {
            this.occurrencesFinderJob.cancel();
        }

        if (this.markOccurrencesUnderCursor || this.markOccurrencesForSelection) {
            if (selection != null) {
                IDocument document = editor.getViewer().getDocument();
                if (document != null) {
                    // Get full word
                    TPWordDetector wordDetector = new TPWordDetector();
                    int startPos = selection.getOffset();
                    int endPos = startPos + selection.getLength();
                    try {
                        int documentLength = document.getLength();
                        while (startPos > 0 && wordDetector.isWordPart(document.getChar(startPos - 1))) {
                            startPos--;
                        }
                        while (endPos < documentLength && wordDetector.isWordPart(document.getChar(endPos))) {
                            endPos++;
                        }
                    } catch (BadLocationException e) {
                        log.debug("Error detecting current word: " + e.getMessage());
                    }
                    String wordSelected = null;
                    String wordUnderCursor = null;
                    if (markOccurrencesUnderCursor) {
                        try {
                            wordUnderCursor = document.get(startPos, endPos - startPos).trim();
                        } catch (BadLocationException e) {
                            log.debug("Error detecting word under cursor", e);
                        }
                    }
                    if (markOccurrencesForSelection) {
                        wordSelected = selection.getText();
                        for (int i = 0; i < wordSelected.length(); i++) {
                            if (!wordDetector.isWordPart(wordSelected.charAt(i))) {
                                wordSelected = null;
                                break;
                            }
                        }
                    }

                    if (CommonUtils.isEmpty(wordSelected) || wordSelected.length() < 2) {
                        this.removeOccurrenceAnnotations();
                    } else {
                        OccurrencesFinder finder = new OccurrencesFinder(document, wordUnderCursor, wordSelected);
                        List<OccurrencePosition> positions = finder.perform();
                        if (!CommonUtils.isEmpty(positions)) {
                            this.occurrencesFinderJob = new OccurrencesFinderJob(positions);
                            this.occurrencesFinderJob.run(new NullProgressMonitor());
                        } else {
                            this.removeOccurrenceAnnotations();
                        }
                    }
                }
            }
        }
    }

    private void removeOccurrenceAnnotations() {
        IDocumentProvider documentProvider = editor.getDocumentProvider();
        if (documentProvider != null) {
            IAnnotationModel annotationModel = documentProvider.getAnnotationModel(editor.getEditorInput());
            if (annotationModel != null && this.occurrenceAnnotations != null) {
                synchronized (LOCK_OBJECT) {
                    this.updateAnnotationModelForRemoves(annotationModel);
                }

            }
        }
    }

    private void updateAnnotationModelForRemoves(IAnnotationModel annotationModel) {
        if (annotationModel instanceof IAnnotationModelExtension) {
            ((IAnnotationModelExtension) annotationModel).replaceAnnotations(this.occurrenceAnnotations, null);
        } else {
            int i = 0;

            for (int length = this.occurrenceAnnotations.length; i < length; ++i) {
                annotationModel.removeAnnotation(this.occurrenceAnnotations[i]);
            }
        }

        this.occurrenceAnnotations = null;
    }

    void installOccurrencesFinder() {
        if (this.selectionChangedListener == null) {
            this.selectionChangedListener = new EditorSelectionChangedListener();
            this.selectionChangedListener.install(editor.getSelectionProvider());
        }

        if (editor.getSelectionProvider() != null) {
            ISelection selection = editor.getSelectionProvider().getSelection();
            if (selection instanceof ITextSelection) {
                this.updateOccurrenceAnnotations((ITextSelection) selection);
            }
        }

        if (this.occurrencesFinderJobCanceler == null) {
            this.occurrencesFinderJobCanceler = new SQLOccurrencesHighlighter.OccurrencesFinderJobCanceler();
            this.occurrencesFinderJobCanceler.install();
        }

    }

    private void uninstallOccurrencesFinder() {
        this.markOccurrencesUnderCursor = false;
        this.markOccurrencesForSelection = false;
        if (this.occurrencesFinderJob != null) {
            this.occurrencesFinderJob.cancel();
            this.occurrencesFinderJob = null;
        }

        if (this.occurrencesFinderJobCanceler != null) {
            this.occurrencesFinderJobCanceler.uninstall();
            this.occurrencesFinderJobCanceler = null;
        }

        this.removeOccurrenceAnnotations();
    }

    public boolean isMarkingOccurrences() {
        return this.markOccurrencesUnderCursor;
    }

    private void setMarkingOccurrences(boolean markUnderCursor, boolean markSelection) {
        if (markUnderCursor != this.markOccurrencesUnderCursor || markSelection != this.markOccurrencesForSelection) {
            this.markOccurrencesUnderCursor = markUnderCursor;
            this.markOccurrencesForSelection = markSelection;
            if (this.markOccurrencesUnderCursor || this.markOccurrencesForSelection) {
                this.installOccurrencesFinder();
            } else {
                this.uninstallOccurrencesFinder();
            }
        }
    }

    public void dispose() {
        if (this.selectionChangedListener != null) {
            this.selectionChangedListener.uninstall(editor.getSelectionProvider());
            this.selectionChangedListener = null;
        }
    }

    void updateInput(IEditorInput input) {
        if (SQLEditorBase.isBigScript(input)) {
            uninstallOccurrencesFinder();
        } else {
            setMarkingOccurrences(
                DBWorkbench.getPlatform().getPreferenceStore().getBoolean(SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR),
                DBWorkbench.getPlatform().getPreferenceStore().getBoolean(SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION));
        }
    }

    boolean handlePreferenceStoreChanged(PropertyChangeEvent event) {
        String property = event.getProperty();
        if (SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR.equals(property) || SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION.equals(property)) {
            updateInput(editor.getEditorInput());
            return true;
        }
        return false;
    }

    private class EditorSelectionChangedListener implements ISelectionChangedListener {
        public void install(ISelectionProvider selectionProvider) {
            if (selectionProvider instanceof IPostSelectionProvider) {
                ((IPostSelectionProvider) selectionProvider).addPostSelectionChangedListener(this);
            } else if (selectionProvider != null) {
                selectionProvider.addSelectionChangedListener(this);
            }
        }

        void uninstall(ISelectionProvider selectionProvider) {
            if (selectionProvider instanceof IPostSelectionProvider) {
                ((IPostSelectionProvider) selectionProvider).removePostSelectionChangedListener(this);
            } else if (selectionProvider != null) {
                selectionProvider.removeSelectionChangedListener(this);
            }
        }

        public void selectionChanged(SelectionChangedEvent event) {
            ISelection selection = event.getSelection();
            if (selection instanceof ITextSelection) {
                SQLOccurrencesHighlighter.this.updateOccurrenceAnnotations((ITextSelection) selection);
            }
        }
    }

    class OccurrencesFinderJob extends Job {
        private boolean isCanceled = false;
        private IProgressMonitor progressMonitor;
        private List<OccurrencePosition> positions;

        OccurrencesFinderJob(List<OccurrencePosition> positions) {
            super("Occurrences Marker");
            this.positions = positions;
        }

        void doCancel() {
            this.isCanceled = true;
            this.cancel();
        }

        private boolean isCanceled() {
            return this.isCanceled || this.progressMonitor.isCanceled();
        }

        public IStatus run(IProgressMonitor progressMonitor) {
            this.progressMonitor = progressMonitor;
            if (!this.isCanceled()) {
                ITextViewer textViewer = editor.getViewer();
                if (textViewer != null) {
                    IDocument document = textViewer.getDocument();
                    if (document != null) {
                        IDocumentProvider documentProvider = editor.getDocumentProvider();
                        if (documentProvider != null) {
                            IAnnotationModel annotationModel = documentProvider.getAnnotationModel(editor.getEditorInput());
                            if (annotationModel != null) {
                                Map<Annotation, Position> annotationMap = new LinkedHashMap<>(this.positions.size());

                                for (OccurrencePosition position : this.positions) {
                                    if (this.isCanceled()) {
                                        break;
                                    }
                                    try {
                                        String message = document.get(position.offset, position.length);
                                        annotationMap.put(
                                            new Annotation(
                                                position.forSelection ?
                                                    SQLEditorContributions.OCCURRENCES_FOR_SELECTION_ANNOTATION_ID :
                                                    SQLEditorContributions.OCCURRENCES_UNDER_CURSOR_ANNOTATION_ID,
                                                false,
                                                message),
                                            position);
                                    } catch (BadLocationException ex) {
                                        //
                                    }
                                }

                                if (!this.isCanceled()) {
                                    synchronized (LOCK_OBJECT) {
                                        this.updateAnnotations(annotationModel, annotationMap);
                                    }
                                    return Status.OK_STATUS;
                                }
                            }
                        }
                    }
                }
            }
            return Status.CANCEL_STATUS;
        }

        private void updateAnnotations(IAnnotationModel annotationModel, Map<Annotation, Position> annotationMap) {
            if (annotationModel instanceof IAnnotationModelExtension) {
                ((IAnnotationModelExtension) annotationModel).replaceAnnotations(SQLOccurrencesHighlighter.this.occurrenceAnnotations, annotationMap);
            } else {
                SQLOccurrencesHighlighter.this.removeOccurrenceAnnotations();

                for (Map.Entry<Annotation, Position> mapEntry : annotationMap.entrySet()) {
                    annotationModel.addAnnotation(mapEntry.getKey(), mapEntry.getValue());
                }
            }

            SQLOccurrencesHighlighter.this.occurrenceAnnotations = annotationMap.keySet().toArray(new Annotation[0]);
        }
    }

    class OccurrencesFinderJobCanceler implements IDocumentListener, ITextInputListener {

        public void install() {
            ISourceViewer sourceViewer = editor.getViewer();
            if (sourceViewer != null) {
                StyledText text = sourceViewer.getTextWidget();
                if (text != null && !text.isDisposed()) {
                    sourceViewer.addTextInputListener(this);
                    IDocument document = sourceViewer.getDocument();
                    if (document != null) {
                        document.addDocumentListener(this);
                    }

                }
            }
        }

        void uninstall() {
            ISourceViewer sourceViewer = editor.getViewer();
            if (sourceViewer != null) {
                sourceViewer.removeTextInputListener(this);
            }

            IDocumentProvider documentProvider = editor.getDocumentProvider();
            if (documentProvider != null) {
                IDocument document = documentProvider.getDocument(editor.getEditorInput());
                if (document != null) {
                    document.removeDocumentListener(this);
                }
            }

        }

        public void documentAboutToBeChanged(DocumentEvent event) {
            if (SQLOccurrencesHighlighter.this.occurrencesFinderJob != null) {
                SQLOccurrencesHighlighter.this.occurrencesFinderJob.doCancel();
            }

        }

        public void documentChanged(DocumentEvent event) {
        }

        public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
            if (oldInput != null) {
                oldInput.removeDocumentListener(this);
            }
        }

        public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
            if (newInput != null) {
                newInput.addDocumentListener(this);
            }
        }
    }

    private static class OccurrencePosition extends Position {
        boolean forSelection;

        OccurrencePosition(int offset, int length, boolean forSelection) {
            super(offset, length);
            this.forSelection = forSelection;
        }
    }

    private static class OccurrencesFinder {
        private IDocument fDocument;
        private String wordUnderCursor;
        private String wordSelected;

        OccurrencesFinder(IDocument document, String wordUnderCursor, String wordSelected) {
            this.fDocument = document;
            this.wordUnderCursor = wordUnderCursor;
            this.wordSelected = wordSelected;
        }

        public List<OccurrencePosition> perform() {
            if (CommonUtils.isEmpty(wordUnderCursor) && CommonUtils.isEmpty(wordSelected)) {
                return null;
            }

            List<OccurrencePosition> positions = new ArrayList<>();

            try {
                if (CommonUtils.equalObjects(wordUnderCursor, wordSelected)) {
                    // Search only selected words
                    findPositions(wordUnderCursor, positions, true);
                } else {
                    findPositions(wordUnderCursor, positions, false);
                    if (!CommonUtils.isEmpty(wordSelected)) {
                        findPositions(wordSelected, positions, true);
                    }
                }

            } catch (BadLocationException e) {
                log.debug("Error finding occurrences: " + e.getMessage());
            }

            return positions;
        }

        private void findPositions(String searchFor, List<OccurrencePosition> positions, boolean forSelection) throws BadLocationException {
            FindReplaceDocumentAdapter findReplaceDocumentAdapter = new FindReplaceDocumentAdapter(fDocument);
            for (int offset = 0; ; ) {
                IRegion region = findReplaceDocumentAdapter.find(offset, searchFor, true, false, !forSelection, false);
                if (region == null) {
                    break;
                }
                positions.add(
                    new OccurrencePosition(region.getOffset(), region.getLength(), forSelection)
                );
                offset = region.getOffset() + region.getLength();
            }
        }

    }

}
