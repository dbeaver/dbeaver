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
package org.jkiss.dbeaver.model.sql.semantics.completion;

import org.jkiss.dbeaver.model.sql.completion.CompletionProposalBase;

import java.util.Comparator;

public class SQLCompletionProposalComparator implements Comparator<CompletionProposalBase> {

    private final boolean sortAlphabetically;

    private final boolean isSearchInsideNames;

    public SQLCompletionProposalComparator(boolean sortAlphabetically, boolean isSearchInsideNames) {
        this.sortAlphabetically = sortAlphabetically;
        this.isSearchInsideNames = isSearchInsideNames;
    }

    @Override
    public int compare(CompletionProposalBase p1, CompletionProposalBase p2) {
        if (this.isSearchInsideNames) {
            return this.compareWhenFuzzySearch(p1, p2);
        } else {
            return this.compareWhenPrefixSearch(p1, p2);
        }
    }

    private int compareWhenFuzzySearch(CompletionProposalBase p1, CompletionProposalBase p2) {
        // by match score, by text, by kind

        int score1 = p1.getProposalScore();
        int score2 = p2.getProposalScore();
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

        return Integer.compare(p1.getProposalTypeSorterScore(), p2.getProposalTypeSorterScore());
    }


    private int compareWhenPrefixSearch(CompletionProposalBase p1, CompletionProposalBase p2) {
        // all the unmatched completely filtered out already, so
        // by kind, by text

        int krc = Integer.compare(p1.getProposalTypeSorterScore(), p2.getProposalTypeSorterScore());
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
}
