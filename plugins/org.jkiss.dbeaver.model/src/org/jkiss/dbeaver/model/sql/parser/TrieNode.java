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
import org.jkiss.dbeaver.utils.ListNode;

import java.util.Set;

/**
 * Represents an information about the node of the trie
 * @param <T> type of the key term used as path item in the trie structure
 * @param <V> type of the value associated with the path in the trie
 */
public interface TrieNode<T, V> {
    /**
     * @return a set of values associated with paths ended in this node
     */
    @NotNull
    Set<V> getValues();

    /**
     * Augments a list of trie nodes with some child nodes of this node each associated with a key term matching to the given term
     * @param term represents element of path (or text) used to decide about the direction in the trie structure
     * @param results initial state of list to augment
     * @return augmented list of initial state optionally combined with some child nodes of this node
     */
    @NotNull
    ListNode<TrieNode<T, V>> accumulateSubnodesByTerm(@NotNull T term, @NotNull ListNode<TrieNode<T, V>> results);
}
