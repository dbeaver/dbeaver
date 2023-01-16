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
package org.jkiss.dbeaver.model.sql.parser;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.utils.ListNode;

import java.util.Collections;
import java.util.Deque;
import java.util.Set;

/**
 * Represents empty set of connection-specific dialect features which require special handling during SQL parsing
 * (Used as default implementation for SQL dialects without such special features)
 */
public class EmptyTokenPredicateSet implements SQLTokenPredicateSet {

    public static final EmptyTokenPredicateSet INSTANCE = new EmptyTokenPredicateSet();

    private static final TrieNode<TokenEntry, SQLTokenPredicate> EMPTY_NODE = new TrieNode<>() {
        @Override
        @NotNull
        public Set<SQLTokenPredicate> getValues() {
            return Collections.emptySet();
        }

        @Override
        public ListNode<TrieNode<TokenEntry, SQLTokenPredicate>> accumulateSubnodesByTerm(@NotNull TokenEntry term, @NotNull ListNode<TrieNode<TokenEntry, SQLTokenPredicate>> results) {
            return results;
        }
    };

    private EmptyTokenPredicateSet() {

    }

    @Override
    public int getMaxPrefixLength() {
        return 0;
    }

    @Override
    public int getMaxSuffixLength() {
        return 0;
    }

    @Override
    public boolean anyMatches(Deque<TokenEntry> prefix, Deque<TokenEntry> suffix) {
        return false;
    }

    @Override
    @NotNull
    public TrieNode<TokenEntry, SQLTokenPredicate> getPrefixTreeRoot() {
        return EMPTY_NODE;
    }

    @Override
    @NotNull
    public Set<SQLTokenPredicate> matchSuffix(Deque<TokenEntry> suffix) {
        return Collections.emptySet();
    }
}
