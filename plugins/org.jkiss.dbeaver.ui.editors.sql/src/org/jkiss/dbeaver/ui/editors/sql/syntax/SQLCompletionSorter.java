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
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionProposal;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLEditorQueryCompletionProposal;

/**
 * Completion sorter
 */
public class SQLCompletionSorter implements ICompletionProposalSorter {

    private final SQLEditorBase editor;

    private boolean sortAlphabetically;

    private boolean isSearchInsideNames;

    public SQLCompletionSorter(SQLEditorBase editor) {
        this.editor = editor;
    }

    @Override
    public int compare(ICompletionProposal p1, ICompletionProposal p2) {
        if (this.isSearchInsideNames) {
            return this.compareWhenFuzzySearch(p1, p2);
        } else {
            return this.compareWhenPrefixSearch(p1, p2);
        }
    }

    private int compareWhenFuzzySearch(ICompletionProposal p1, ICompletionProposal p2) {
        // by match score, by text, by kind

        int score1 = getScore(p1);
        int score2 = getScore(p2);
        if (score1 == Integer.MAX_VALUE && score2 == Integer.MAX_VALUE) {
            return this.compareWhenPrefixSearch(p1, p2);
        }

        if (score1 > 0 || score2 > 0) {
            int rc = -Integer.compare(score1, score2);
            if (rc != 0) {
                return rc;
            }
        }
        if (sortAlphabetically) {
            int rc = p1.getDisplayString().compareToIgnoreCase(p2.getDisplayString());
            if (rc != 0) {
                return rc;
            }
        } else {
            return 0;
        }

        return compareProposalKind(p1, p2);
    }

    private int compareWhenPrefixSearch(ICompletionProposal p1, ICompletionProposal p2) {
        // all the unmatched completely filtered out already, so
        // by kind, by text

        int krc = compareProposalKind(p1, p2);
        if (krc != 0) {
            return krc;
        }

        if (sortAlphabetically) {
            int rc = p1.getDisplayString().compareToIgnoreCase(p2.getDisplayString());
            if (rc != 0) {
                return rc;
            }
        }

        return 0;
    }

    private static int compareProposalKind(ICompletionProposal a, ICompletionProposal b) {
        int aOrd = a instanceof SQLQueryCompletionProposal x ? x.getItemKind().sortOrder :
                   a instanceof SQLCompletionProposal x ? x.getProposalType().sortOrder : Integer.MAX_VALUE;
        int bOrd = b instanceof SQLQueryCompletionProposal y ? y.getItemKind().sortOrder :
                   b instanceof SQLCompletionProposal y ? y.getProposalType().sortOrder : Integer.MAX_VALUE;
        return Integer.compare(aOrd, bOrd);
    }

    private static int getScore(ICompletionProposal p) {
        if (p instanceof SQLCompletionProposal cp) {
            return cp.getProposalScore();
        } else if (p instanceof SQLEditorQueryCompletionProposal qcp) {
            return qcp.getProposalScore();
        }
        return 0;
    }

    public void refreshSettings() {
        DBPPreferenceStore prefStore = this.editor.getActivePreferenceStore();
        this.sortAlphabetically = prefStore.getBoolean(SQLPreferenceConstants.PROPOSAL_SORT_ALPHABETICALLY);
        this.isSearchInsideNames = prefStore.getBoolean(SQLPreferenceConstants.PROPOSALS_MATCH_CONTAINS);
    }
}