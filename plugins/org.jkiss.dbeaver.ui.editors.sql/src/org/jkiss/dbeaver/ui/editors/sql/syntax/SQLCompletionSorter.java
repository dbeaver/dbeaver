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

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalSorter;

/**
 * Completion sorter
 */
public class SQLCompletionSorter implements ICompletionProposalSorter {

    @Override
    public int compare(ICompletionProposal p1, ICompletionProposal p2) {
        if (p1 instanceof SQLCompletionProposal && p2 instanceof SQLCompletionProposal) {
            int score1 = ((SQLCompletionProposal) p1).getProposalScore();
            int score2 = ((SQLCompletionProposal) p2).getProposalScore();
            if (score1 > 0 && score2 > 0) {
                return score2 - score1;
            }
        }
        return 0;
    }
}