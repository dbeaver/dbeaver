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
package org.jkiss.dbeaver.model.sql.parser.tokens.predicates;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.parser.TrieNode;
import org.jkiss.dbeaver.utils.ListNode;

import java.util.*;

/**
 * Trie also known as prefix-tree implementation.
 * Useful to optimize lookup operations over sets of lazily-discoverable paths in the key-space represented with internal tree data structure.
 * @param <T> type of the key term used as path item in the trie structure
 * @param <V> type of the value associated with the path in the trie
 */
public class Trie<T, V> {

    private final Comparator<T> strongComparer;
    private final TrieLookupComparator<T> lookupPartialComparer;
    private TreeNode root = new TreeNode(null);

    /**
     * Node of the tree data structure implementing trie data item
     */
    private class TreeNode implements TrieNode<T, V> {
        /**
         * key of the node used to discover it while navigating from its parent node
         */
        public final T term;
        /**
         * a set of values associated with paths terminating in this node
         */
        public final Set<V> values;
        /**
         * some details about what is the most effective lookup strategy available to discover children nodes by a certain key
         */
        public boolean isStronglyOrdered = true;
        public boolean isPartiallyOrdered = true;
        /**
         * ordered list of keys used to discover children nodes for the paths laying through this node
         */
        public final List<T> childKeys;
        /**
         * list of children nodes corresponding the order of their keys in {@link #childKeys}
         */
        public final List<TreeNode> childNodes;
        /**
         * map of children nodes by the corresponding strongly-comparable key
         * due to hashset having ~O(1) vs tree or binary search over {@link #childKeys} having ~O(log(n))
         * have some clashes with custom comparators, so commenting it out for the time being
         */
        // public Map<T, TreeNode> childNodesByKey;

        public TreeNode(@Nullable T term) {
            this.term = term;
            this.values = new HashSet<>();
            this.childKeys = new ArrayList<>();
            this.childNodes = new ArrayList<>();
            // this.childNodesByKey = new HashMap<>();
        }

        @NotNull
        public Set<V> getValues() {
            return this.values;
        }

        /**
         * Builds up child node (or retrieves already existing one) strongly associated with the given term specification
         * @param term used as key to discover the node during further lookup operations
         * @return node in the trie data structure
         */
        @NotNull
        public TreeNode addOrCreateChild(@NotNull T term) {
            int index = Collections.binarySearch(this.childKeys, term, Trie.this.strongComparer);
            if (index >= 0) {
                return this.childNodes.get(index);
            } else {
                TreeNode newNode = new TreeNode(term);
                index = ~index;
                this.childKeys.add(index, term);
                this.childNodes.add(index, newNode);
                if (!lookupPartialComparer.isStronglyComparable(term)) {
                    this.isStronglyOrdered = false;
                    // this.childNodesByKey = null;
                }
                if (!lookupPartialComparer.isPartiallyComparable(term)) {
                    this.isPartiallyOrdered = false;
                    // this.childNodesByKey = null;
                }
                // if (this.childNodesByKey != null) {
                //     this.childNodesByKey.put(term, newNode);
                // }
                return newNode;
            }
        }

        @Override
        @Nullable
        public ListNode<TrieNode<T, V>> accumulateSubnodesByTerm(@NotNull T term, @NotNull ListNode<TrieNode<T, V>> results) {
            ListNode<TrieNode<T, V>> accumulatedResults;
            // use the best suitable strategy to lookup for children nodes by the given term
            if (this.isStronglyOrdered && lookupPartialComparer.isStronglyComparable(term)) {
                // TreeNode child = this.childNodesByKey.get(term);
                // if (child != null) {
                //    results = ListNode.push(results, child);
                // }
                // all the nodes and the key term are strongly distinguishable by comparison
                int index = Collections.binarySearch(this.childKeys, term, Trie.this.strongComparer);
                if (index >= 0) {
                    accumulatedResults = ListNode.push(results, this.childNodes.get(index));
                } else {
                    accumulatedResults = results;
                }
            } else if (this.isPartiallyOrdered && lookupPartialComparer.isPartiallyComparable(term)) {
                // nodes are ordered in common, but some of them (or the key term) couldn't be strongly distinguished by comparison
                // though we still can find a pivot item and then look around it while partial equality being hold
                accumulatedResults = this.accumulatePartiallyComparableSubnodes(term, results);
            } else {
                // nodes vs given key comparison does not respect any kind of consistent ordering
                accumulatedResults = this.accumulateNonComparableSubnodes(term, results);
            }
            return accumulatedResults;
        }


        @Nullable
        private ListNode<TrieNode<T, V>> accumulateNonComparableSubnodes(@NotNull T term, @NotNull ListNode<TrieNode<T, V>> results) {
            TrieLookupComparator<T> comparer = Trie.this.lookupPartialComparer;
            ListNode<TrieNode<T, V>> accumulatedResults = results;
            for (int i = 0; i < this.childKeys.size(); i++) {
                if (comparer.match(this.childKeys.get(i), term)) {
                    accumulatedResults = ListNode.push(accumulatedResults, this.childNodes.get(i));
                }
            }
            return accumulatedResults;
        }

        @NotNull
        private ListNode<TrieNode<T, V>> accumulatePartiallyComparableSubnodes(@NotNull T term, @NotNull ListNode<TrieNode<T, V>> results) {
            TrieLookupComparator<T> comparer = Trie.this.lookupPartialComparer;
            ListNode<TrieNode<T, V>> accumulatedResults = results;
            int index = Collections.binarySearch(this.childKeys, term, comparer);
            if (index >= 0) {
                if (comparer.match(this.childKeys.get(index), term)) {
                    accumulatedResults = ListNode.push(accumulatedResults, this.childNodes.get(index));
                }
                for (int i = index + 1; i < this.childKeys.size() && comparer.compare(this.childKeys.get(i), term) == 0; i++) {
                    if (comparer.match(this.childKeys.get(i), term)) {
                        accumulatedResults = ListNode.push(accumulatedResults, this.childNodes.get(i));
                    }
                }
                for (int i = index - 1; i >= 0 && comparer.compare(this.childKeys.get(i), term) == 0; i--) {
                    if (comparer.match(this.childKeys.get(i), term)) {
                        accumulatedResults = ListNode.push(accumulatedResults, this.childNodes.get(i));
                    }
                }
            }
            return accumulatedResults;
        }
    }

    public Trie(@NotNull Comparator<T> strongComparer, @NotNull TrieLookupComparator<T> lookupComparer) {
        this.strongComparer = strongComparer;
        this.lookupPartialComparer = lookupComparer;
    }

    @NotNull
    public TrieNode<T, V> getRoot() {
        return root;
    }

    public void add(@NotNull Iterable<T> key, @NotNull V value) {
        this.add(key.iterator(), value);
    }

    public void add(@NotNull Iterator<T> key, @NotNull V value) {
        TreeNode node = root;
        while (key.hasNext()) {
            T term = key.next();
            node = node.addOrCreateChild(term);
        }
        node.values.add(value);
    }

    @Nullable
    private ListNode<Set<V>> lookupImpl(@NotNull Iterator<T> key) {
        if (!key.hasNext()) {
            if (root.values.size() > 0) {
                return ListNode.of(root.values);
            } else {
                return null;
            }
        }

        // walk down the tree while terms of the key path sequence match the corresponding keys of the nodes
        // till the end of the key sequence or boundary of the data structure, where no more child nodes could be matched
        ListNode<TrieNode> activeNodes = ListNode.of(root); // - nodes to lookup down the tree by the current term from the key sequence
        ListNode<Set<V>> results = null; // - total accumulated values from all the nodes met along the key sequence path

        do {
            T term = key.next();
            ListNode<TrieNode> nextNodes = null;
            // inspect active nodes on the current level of the tree data structure
            for (ListNode<TrieNode> currNode = activeNodes; currNode != null; currNode = currNode.next) {
                TrieNode node = currNode.data;
                // accumulate values from the current node
                Set<V> values = node.getValues();
                if (values.size() > 0) {
                    results = ListNode.push(results, values);
                }
                // obtain children nodes by the current term for further inspection
                nextNodes = node.accumulateSubnodesByTerm(term, nextNodes);
            }
            activeNodes = nextNodes;
            // proceed to the next level of the tree if there is something to look at
        } while (activeNodes != null && key.hasNext());

        // accumulate values from the deepest level being reached
        for (ListNode<TrieNode> currNode = activeNodes; currNode != null; currNode = currNode.next) {
            results = ListNode.push(results, currNode.data.getValues());
        }

        return results;
    }

    /**
     * Lookup the trie in search of value items associated with key terms along the given key path sequence
     * @param key path sequence of terms to look for
     * @return a set of values
     */
    @NotNull
    public Set<V> collectValuesOnPath(@NotNull Iterator<T> key) {
        ListNode<Set<V>> values = this.lookupImpl(key);
        if (values == null) {
            return Collections.emptySet();
        }

        Set<V> result = new HashSet<>();
        for (ListNode<Set<V>> currNode = values; currNode != null; currNode = currNode.next) {
            result.addAll(currNode.data);
        }

        return result;
    }
}
