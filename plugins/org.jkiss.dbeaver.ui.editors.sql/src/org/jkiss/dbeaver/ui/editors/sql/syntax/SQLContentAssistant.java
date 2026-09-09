/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;

/**
 * SQL Completion proposal
 */
public class SQLContentAssistant extends ContentAssistant {

    private final SQLEditorBase editor;

    private SQLCompletionSorterUI sorter;

    private IDocument completionDocument;
    private ISelectionProvider completionSelectionProvider;
    private int completionRegionStart = -1;
    private int completionRegionEnd = -1;

    private final IDocumentListener completionDocumentListener = new IDocumentListener() {
        @Override
        public void documentAboutToBeChanged(DocumentEvent event) {
            int changeStart = event.getOffset();
            int changeEnd = changeStart + event.getLength();
            if (changeStart < completionRegionStart ||
                changeStart > completionRegionEnd ||
                changeEnd > completionRegionEnd
            ) {
                hide();
            } else {
                completionRegionEnd += (event.getText() == null ? 0 : event.getText().length()) - event.getLength();
            }
        }

        @Override
        public void documentChanged(DocumentEvent event) {
            // do nothing
        }
    };
    private final ISelectionChangedListener completionSelectionListener = event -> {
        if (event.getSelection() instanceof ITextSelection selection) {
            int selectionStart = selection.getOffset();
            int selectionEnd = selectionStart + selection.getLength();
            if (selectionStart < completionRegionStart || selectionEnd > completionRegionEnd) {
                hide();
            }
        }
    };

    public SQLContentAssistant(SQLEditorBase editor) {
        super(); // Sync. Maybe we should make it async
        this.editor = editor;
        enableColoredLabels(true);
    }

    public void setCompletionRegionOffset(int offset) {
        clearCompletionRegion();
        IDocument document = this.editor.getDocument();
        if (document == null) {
            return;
        }

        this.completionDocument = document;
        this.completionRegionStart = offset;
        this.completionRegionEnd = offset;
        this.completionDocument.addDocumentListener(this.completionDocumentListener);
        if (this.editor.getTextViewer() != null) {
            this.completionSelectionProvider = this.editor.getTextViewer().getSelectionProvider();
            if (this.completionSelectionProvider != null) {
                this.completionSelectionProvider.addSelectionChangedListener(this.completionSelectionListener);
            }
        }
    }

    public void clearCompletionRegion() {
        if (this.completionDocument != null) {
            this.completionDocument.removeDocumentListener(this.completionDocumentListener);
            this.completionDocument = null;
        }
        if (this.completionSelectionProvider != null) {
            this.completionSelectionProvider.removeSelectionChangedListener(this.completionSelectionListener);
            this.completionSelectionProvider = null;
        }
        this.completionRegionStart = -1;
        this.completionRegionEnd = -1;
    }

    public void setSorter(SQLCompletionSorterUI sorter) {
        this.sorter = sorter;
        super.setSorter(sorter);
    }

    public void assistSessionStarted(ContentAssistEvent event) {
        if (this.sorter != null) {
            this.sorter.refreshSettings();
        }
    }

    @Override
    protected AutoAssistListener createAutoAssistListener() {
        return new SQLAutoAssistListener();
    }

    private class SQLAutoAssistListener extends AutoAssistListener {
        @Override
        protected void showAssist(int showStyle) {
            if (showStyle == 1 && !(SQLEditorUtils.isSQLSyntaxParserApplied(editor.getEditorInput())
                && editor.getActivePreferenceStore().getBoolean(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION))
            ) {
                return;
            }
            SQLCompletionProcessor.setSimpleMode(true);
            try {
                super.showAssist(showStyle);
            } finally {
                SQLCompletionProcessor.setSimpleMode(false);
            }
        }
    }

    @Override
    public String showContextInformation() {
        SQLCompletionProcessor.setLookupTemplates(true);
        try {
            return super.showPossibleCompletions();
        } finally {
            SQLCompletionProcessor.setLookupTemplates(false);
        }
    }
}
