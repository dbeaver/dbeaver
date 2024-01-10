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
package org.jkiss.dbeaver.model.sql.parser;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TokenPredicatesCondition;

import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Plain list of token predicate conditions for a comparison with trie-based predicates set
 */
public class TokenPredicatesList {
    private final List<TokenPredicatesCondition> conditions;
    private final int maxHeadLength;
    private final int maxTailLength;

    public TokenPredicatesList(List<TokenPredicatesCondition> conditions) {
        this.conditions = Collections.unmodifiableList(conditions);
        this.maxHeadLength = conditions.stream().mapToInt(c -> c.maxPrefixLength).max().orElse(0);
        this.maxTailLength = conditions.stream().mapToInt(c -> c.maxSuffixLength).max().orElse(0);
    }

    public int getMaxHeadLength() {
        return maxHeadLength;
    }

    public int getMaxTailLength() {
        return maxTailLength;
    }

    @NotNull
    public static TokenPredicatesList of(@NotNull TokenPredicatesCondition... conditions) {
        return new TokenPredicatesList(List.of(conditions));
    }

    public boolean anyMatches(@NotNull Deque<TokenEntry> head, @NotNull Deque<TokenEntry> tail) {
        for (TokenPredicatesCondition cond: conditions) {
            if (this.conditionMatches(cond, head, tail)) {
                return true;
            }
        }
        return false;
    }

    private boolean conditionMatches(@NotNull TokenPredicatesCondition cond, @NotNull Deque<TokenEntry> head, @NotNull Deque<TokenEntry> tail) {
        boolean tailMatch = false;
        for (List<TokenEntry> condTail : cond.getSuffixes()) {
            int condTailLen = condTail.size();
            if (condTailLen <= tail.size()) {
                boolean matched = true;
                Iterator<TokenEntry> it = tail.descendingIterator();
                for (int i = condTailLen - 1; i >= 0; i--) {
                    if (!it.next().matches(condTail.get(i))) {
                        matched = false;
                        break;
                    }
                }
                if (matched) {
                    tailMatch = true;
                    break;
                }
            }
        }

        if (!tailMatch) {
            return false;
        }

        boolean headMatch = false;
        for (List<TokenEntry> condHead : cond.getPrefixes()) {
            int condHeadLen = condHead.size();
            if (condHeadLen <= head.size()) {
                boolean matched = true;
                Iterator<TokenEntry> it = head.iterator();
                for (int i = 0; i < condHeadLen; i++) {
                    if (!it.next().matches(condHead.get(i))) {
                        matched = false;
                        break;
                    }
                }
                if (matched) {
                    headMatch = true;
                    break;
                }
            }
        }

        return headMatch;
    }
}


