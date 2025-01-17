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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionProposalSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;

/**
 * SQL Completion proposal
 */
public class SQLContentAssistant extends ContentAssistant {

    private final SQLEditorBase editor;

    private SQLCompletionSorter sorter;

    private int lastCompletionOffset = - 1;
    private volatile boolean restartRequested = false;

    public SQLContentAssistant(SQLEditorBase editor) {
        super(); // Sync. Maybe we should make it async
        this.editor = editor;
        enableColoredLabels(true);
    }

    public void setLastCompletionOffset(int lastCompletionOffset) {
        this.lastCompletionOffset = lastCompletionOffset;
        if (lastCompletionOffset == -1 && restartRequested) {
            restartRequested = false;
            UIUtils.asyncExec(() -> showPossibleCompletions());
        }
    }

    public void setSorter(SQLCompletionSorter sorter) {
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

        @Override
        public void verifyKey(VerifyEvent event) {
            if (lastCompletionOffset >= 0 && (
                event.character == SWT.BS ||
                (event.character == 0 && event.keyCode == SWT.ARROW_LEFT)
            ) && editor.getTextViewer() != null) {
                int pos = editor.getTextViewer().getSelectedRange().x;
                if ((pos - 1) < lastCompletionOffset) {
                    restartRequested = true;
                    hide();
                    return;
                }
            }

            super.verifyKey(event);
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
