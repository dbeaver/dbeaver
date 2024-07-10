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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.parser.*;
import org.jkiss.dbeaver.utils.ListNode;

import java.util.*;

/**
 * Incremental token predicate evaluator responsible for efficient answering for the question,
 * whether any of presented dialect-specific conditions met during parsing of the sequence of tokens.
 * Tries to distribute the work across the parsing process in a way to reduce the amount of data for analysis on each step.
 */
public class SQLTokenPredicateEvaluator {

    static protected final Log log = Log.getLog(SQLTokenPredicateEvaluator.class);

    /**
     * Complete set of conditions under considerations
     */
    private final SQLTokenPredicateSet predicatesSet;
    /**
     * A number of tokens representing prefix of the SQL statement under analysis
     */
    private final Deque<TokenEntry> statementPrefixTokens;
    /**
     * A number of tokens representing suffix of the SQL statement under analysis
     */
    private final Deque<TokenEntry> statementSuffixTokens;
    /**
     * A number of conditions considered matching according to the judgement based on the accumulated statement prefix (see {@link #statementPrefixPredicates});
     * Statement suffix still should be analyzed to be sure if any of them really matched (see {@link #evaluatePredicates()}).
     */
    private final Set<SQLTokenPredicate> plausibleConditions = new HashSet<>();
    private final Set<SQLTokenPredicate> plausiblePrefixOnlyConditions = new HashSet<>();
    /**
     * A number of nodes describing the prefix conditions under consideration for a next token to judge about.
     * Effectively reduces the amount of conditions to check during each step of analysis by accumulating only the prefix-matched conditions in the {@link #plausibleConditions}
     * Completely replaced on each step of analysis while there are any token entry specified to look at (see {@link #captureToken(TokenEntry)}).
     */
    private ListNode<TrieNode<TokenEntry, SQLTokenPredicate>> statementPrefixPredicates;

    public SQLTokenPredicateEvaluator(@NotNull SQLTokenPredicateSet predicatesSet) {
        this.predicatesSet = predicatesSet;
        this.statementPrefixTokens = new ArrayDeque<>(predicatesSet.getMaxPrefixLength());
        this.statementSuffixTokens = new ArrayDeque<>(predicatesSet.getMaxSuffixLength());
        this.statementPrefixPredicates = ListNode.of(predicatesSet.getPrefixTreeRoot());
    }

    /**
     * Captures given token entry accumulating information about the SQL statement under analysis and
     * performs incremental part of the work on its recognition by the dialect-specific feature conditions
     * @param entry
     */
    public void captureToken(@NotNull TokenEntry entry) {
        // accumulating statement prefix until there is no more prefix conditions to judge on at this position
        if (statementPrefixTokens.size() <= predicatesSet.getMaxPrefixLength()) {
            if (statementPrefixTokens.size() < predicatesSet.getMaxPrefixLength()) {
                statementPrefixTokens.add(entry);
            }
            ListNode<TrieNode<TokenEntry, SQLTokenPredicate>> accumulator = null;
            for (var node = statementPrefixPredicates; node != null; node = node.next) {
                Set<SQLTokenPredicate> currentlyMatchedHeads = node.data.getValues();
                if (currentlyMatchedHeads.size() > 0) {
                    // accumulating conditions considered matched according to the already captured part of the statement prefix
                    // longer prefixes would be discovered only during the next token analysis among the reduced set of conditions
                    for (SQLTokenPredicate matchedByPrefix : currentlyMatchedHeads) {
                        if (matchedByPrefix.getMaxSuffixLength() == 0) {
                            plausiblePrefixOnlyConditions.add(matchedByPrefix);
                        } else {
                            plausibleConditions.add(matchedByPrefix);
                        }
                    }
                }
                // accumulating reduced subset of the next to look at conditions according to the token being consumed
                accumulator = node.data.accumulateSubnodesByTerm(entry, accumulator);
            }
            // incrementally reducing total set of plausibly matching conditions according to the token being consumed
            statementPrefixPredicates = accumulator;
        }
        // accumulating statement suffix dropping the unnecessary part of token's sequence fading out of range of analyzable suffix under conditions
        if (predicatesSet.getMaxSuffixLength() > 0) {
            if (statementSuffixTokens.size() >= predicatesSet.getMaxSuffixLength()) {
                statementSuffixTokens.removeFirst();
            }
            statementSuffixTokens.add(entry);
        }
    }

    /**
     * Checks for a presence of conditions matching accumulated prefix and suffix.
     * Suffix is already incrementally matched during its accumulation, so here we're just matching the suffix predicates.
     * If sets of conditions being met are not disjoint, then some conditions are actually matched by both the prefix and suffix.
     * @return
     */
    @Nullable
    public SQLParserActionKind evaluatePredicates() {
        // We are inspecting the whole set of suffix predicates here by matching them using trie, so no simple brute force at all.
        // It can also be optimized even further by using Aho–Corasick algorithm (which is actually an evolution of trie),
        // if we can associate condition objects with each suffix key token sequence to match.
        Set<SQLTokenPredicate> tailConditionsMatched = plausibleConditions.size() > 0 ? predicatesSet.matchSuffix(statementSuffixTokens) : Collections.emptySet();
        // check out the intersection of conditions matched by the prefix and suffix
        tailConditionsMatched.retainAll(plausibleConditions);

        if (tailConditionsMatched.size() + plausiblePrefixOnlyConditions.size() > 1) {
            log.warn("Ambiguous token predicates match");
        } else if (tailConditionsMatched.size() > 0) {
            return tailConditionsMatched.iterator().next().getActionKind();
        } else if (plausiblePrefixOnlyConditions.size() > 0){
            SQLTokenPredicate matchedByPrefix = plausiblePrefixOnlyConditions.iterator().next();
            plausiblePrefixOnlyConditions.clear();
            return matchedByPrefix.getActionKind();
        }
        return null;
    }

    /**
     * Drops the accumulated state of the predicate evaluator to prepare it for the next SQL statement analysis.
     */
    public void reset() {
        statementPrefixTokens.clear();
        statementSuffixTokens.clear();
        plausibleConditions.clear();
        plausiblePrefixOnlyConditions.clear();
        statementPrefixPredicates = ListNode.of(predicatesSet.getPrefixTreeRoot());
    }
}

