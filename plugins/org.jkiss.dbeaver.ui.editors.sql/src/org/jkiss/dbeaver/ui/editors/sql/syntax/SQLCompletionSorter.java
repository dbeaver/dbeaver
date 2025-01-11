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

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalSorter;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryCompletionProposal;

import java.util.Comparator;

/**
 * Completion sorter
 */
public class SQLCompletionSorter implements ICompletionProposalSorter {

    private final SQLEditorBase editor;

    private boolean sortAlphabetically;

    public SQLCompletionSorter(SQLEditorBase editor) {
        this.editor = editor;
    }

    @Override
    public int compare(ICompletionProposal p1, ICompletionProposal p2) {
        int a = getScore(p1);
        int b = getScore(p2);
        if (a > 0 || b > 0) {
            int rc = a - b;
            if (rc != 0) {
                return rc;
            }
        }
        if (sortAlphabetically) {
            return p1.getDisplayString().compareToIgnoreCase(p2.getDisplayString());
        } else {
            return 0;
        }
    }

    private static int getScore(ICompletionProposal p) {
        if (p instanceof SQLCompletionProposal cp) {
            return cp.getProposalScore();
        } else if (p instanceof SQLQueryCompletionProposal qcp) {
            return qcp.getProposalScore();
        }
        return 0;
    }

    public void refreshSettings() {
        this.sortAlphabetically = this.editor.getActivePreferenceStore().getBoolean(SQLPreferenceConstants.PROPOSAL_SORT_ALPHABETICALLY);
    }
}