/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalSorter;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.completion.CompletionProposalBase;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLCompletionProposalComparator;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;

/**
 * Completion sorter
 */
public class SQLCompletionSorterUI implements ICompletionProposalSorter {

    private final SQLEditorBase editor;

    private SQLCompletionProposalComparator sqlCompletionProposalComparator;

    public SQLCompletionSorterUI(SQLEditorBase editor) {
        this.editor = editor;
        DBPPreferenceStore prefStore = this.editor.getActivePreferenceStore();
        sqlCompletionProposalComparator = new SQLCompletionProposalComparator(
            prefStore.getBoolean(SQLPreferenceConstants.PROPOSAL_SORT_ALPHABETICALLY),
            prefStore.getBoolean(SQLPreferenceConstants.PROPOSALS_MATCH_CONTAINS));
    }

    @Override
    public int compare(ICompletionProposal p1, ICompletionProposal p2) {
        if (!(p1 instanceof CompletionProposalBase completionProposalBase1)
            || !(p2 instanceof CompletionProposalBase completionProposalBase2)) {
            return 0;
        }
        return sqlCompletionProposalComparator.compare(completionProposalBase1, completionProposalBase2);
    }

    public void refreshSettings() {
        DBPPreferenceStore prefStore = this.editor.getActivePreferenceStore();
        sqlCompletionProposalComparator = new SQLCompletionProposalComparator(
            prefStore.getBoolean(SQLPreferenceConstants.PROPOSAL_SORT_ALPHABETICALLY),
            prefStore.getBoolean(SQLPreferenceConstants.PROPOSALS_MATCH_CONTAINS));
    }
}