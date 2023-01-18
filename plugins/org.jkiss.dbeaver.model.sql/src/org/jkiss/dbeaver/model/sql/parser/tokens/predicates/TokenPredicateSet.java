/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.parser.tokens.predicates;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.parser.SQLTokenPredicate;
import org.jkiss.dbeaver.model.sql.parser.SQLTokenPredicateSet;
import org.jkiss.dbeaver.model.sql.parser.TokenEntry;
import org.jkiss.dbeaver.model.sql.parser.TrieNode;

import java.util.*;

/**
 * A set of conditions about sequences of terms expressed in form of predicate pairs about term sequence prefix and suffix
 */
public class TokenPredicateSet implements SQLTokenPredicateSet {
    private final List<TokenPredicatesCondition> conditions = new ArrayList<>();
    private final Trie<TokenEntry, SQLTokenPredicate> conditionsByPrefix = new Trie<>(ExactTokenEntryComparator.INSTANCE, TokenEntryMatchingComparator.INSTANCE);
    private final Trie<TokenEntry, SQLTokenPredicate> conditionsBySuffix = new Trie<>(ExactTokenEntryComparator.INSTANCE, TokenEntryMatchingComparator.INSTANCE);
    private int maxHeadLength = 0;
    private int maxTailLength = 0;

    @Override
    @NotNull
    public TrieNode<TokenEntry, SQLTokenPredicate> getPrefixTreeRoot() {
        return conditionsByPrefix.getRoot();
    }

    public int getMaxPrefixLength() {
        return maxHeadLength;
    }

    public int getMaxSuffixLength() {
        return maxTailLength;
    }

    /**
     * Inserts a new condition in the set
     * @param cond condition to insert
     */
    public void add(@NotNull TokenPredicatesCondition cond) {
        this.conditions.add(cond);
        cond.getPrefixes().forEach(h -> conditionsByPrefix.add(h.iterator(), cond));
        cond.getSuffixes().forEach(t -> conditionsBySuffix.add(new ArrayDeque<TokenEntry>(t).descendingIterator(), cond));
        maxHeadLength = Math.max(maxHeadLength, cond.maxPrefixLength);
        maxTailLength = Math.max(maxTailLength, cond.maxSuffixLength);
    }

    @NotNull
    public static TokenPredicateSet of(@NotNull TokenPredicatesCondition... conditions) {
        TokenPredicateSet set = new TokenPredicateSet();
        for (TokenPredicatesCondition cond: conditions) {
            set.add(cond);
        }
        return set;
    }

    @NotNull
    public Set<SQLTokenPredicate> matchSuffix(@NotNull Deque<TokenEntry> suffix) {
        return conditionsBySuffix.collectValuesOnPath(suffix.descendingIterator());
    }

    public boolean anyMatches(@NotNull Deque<TokenEntry> prefix, @NotNull Deque<TokenEntry> suffix) {
        Set<SQLTokenPredicate> matchedConds = conditionsBySuffix.collectValuesOnPath(suffix.descendingIterator());
        if (matchedConds.isEmpty()) {
            return false;
        } else {
            Set<SQLTokenPredicate> matchedHeadConds = conditionsByPrefix.collectValuesOnPath(prefix.iterator());
            matchedConds.retainAll(matchedHeadConds);
            return matchedConds.size() > 0;
        }
    }
}

