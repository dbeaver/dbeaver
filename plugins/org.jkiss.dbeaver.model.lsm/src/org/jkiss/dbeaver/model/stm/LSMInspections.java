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
package org.jkiss.dbeaver.model.stm;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.tree.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.*;
import org.jkiss.dbeaver.utils.ListNode;
import org.jkiss.utils.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LSMInspections {

    @NotNull
    private static final Set<String> knownReservedWords = new HashSet<>(BasicSQLDialect.INSTANCE.getReservedWords());

    @NotNull
    private static final Set<Integer> reachabilityTestRules = Set.of(
        SQLStandardParser.RULE_tableName,
        SQLStandardParser.RULE_columnReference,
        SQLStandardParser.RULE_identifier,
        SQLStandardParser.RULE_columnName
    );

    @NotNull
    private static final Set<Integer> knownReservedWordsExcludeRules = Stream.of(reachabilityTestRules, Set.of(
        SQLStandardParser.RULE_nonReserved,
        SQLStandardParser.RULE_anyUnexpected,
        SQLStandardParser.RULE_aggregateExprParam,
        SQLStandardParser.RULE_anyWord,
        SQLStandardParser.RULE_correlationName,
        SQLStandardParser.RULE_tableHintKeywords
    )).flatMap(Set::stream).collect(Collectors.toUnmodifiableSet());

    @NotNull
    private static Pair<STMTreeNode, Boolean> findChildBeforeOrAtPosition(@NotNull STMTreeNode node, int position) {
        STMTreeNode nodeBefore = null;
        Interval nodeBeforeRange = null;
        for (int i = 0; i < node.getChildCount(); i++) {
            STMTreeNode cn = node.getStmChild(i);
            Interval range = cn.getRealInterval();
            if (range.a <= position && range.b >= position) {
                return Pair.of(cn, true);
            } else if (range.a < position && (nodeBeforeRange == null || nodeBeforeRange.a < range.a)) {
                nodeBefore = cn;
                nodeBeforeRange = range;
            } else {
                break;
            }
        }
        return Pair.of(nodeBefore, false);
    }

    @NotNull
    public static SyntaxInspectionResult prepareOffquerySyntaxInspection() {
        ATN atn = SQLStandardParser._ATN;
        ListNode<Integer> emptyStack = ListNode.of(null);
        ATNState initialState = atn.states.get(atn.ruleToStartState[SQLStandardParser.RULE_sqlQueries].stateNumber);
        return inspectAbstractSyntaxAtState(emptyStack, initialState);
    }

    @Nullable
    public static SyntaxInspectionResult prepareAbstractSyntaxInspection(@NotNull STMTreeNode root, int position) {
        STMTreeNode subroot = root;
        ATN atn = SQLStandardParser._ATN;

        Interval range = subroot.getRealInterval();
        if (position < range.a) {
            return prepareOffquerySyntaxInspection();
        } else {
            // TODO collect position-prepending identifier based on subtree path and get rid of prepareTerms()
            // position >= range.a && position <= range.b
            Pair<STMTreeNode, Boolean> p = Pair.of(subroot, true);
            while (!(p.getFirst() instanceof STMTreeTermNode term) && p.getFirst() != null) {
                p = findChildBeforeOrAtPosition(subroot = p.getFirst(), position);
            }

            ATNState initialState;
            if (p.getSecond() == null) {
                // subroot itself contains given position, use its rule start state
                initialState = atn.states.get(subroot.getAtnState());
            } else if (p.getSecond()) {
                // containing term found, we need its start state
                initialState = atn.states.get(p.getFirst().getAtnState());
            } else {
                // previous node found, use its rule end state
                STMTreeNode node = p.getFirst();
                if (node instanceof STMTreeTermNode tn) {
                    initialState = atn.states.get(tn.getAtnState()).getTransitions()[0].target;
                } else if (node instanceof STMTreeRuleNode rn) {
                    initialState = atn.ruleToStopState[rn.getRuleContext().getRuleIndex()];
                } else {
                    STMTreeTermNode tn = findLastTerm(root);
                    if (tn != null) {
                        subroot = tn;
                        initialState = atn.states.get(tn.getAtnState()).getTransitions()[0].target;
                    } else {
                        throw new IllegalStateException("TODO/WTF");
                    }
                }
                // TODO watch for state context rule  
            }

            return inspectAbstractSyntaxAtTreeState(subroot, initialState);
        }            
    }

    @Nullable
    private static STMTreeTermNode findLastTerm(@NotNull STMTreeNode root) {
        ListNode<STMTreeNode> stack = ListNode.of(root);
        while (ListNode.hasAny(stack)) {
            STMTreeNode node = stack.data;
            stack = stack.next;

            if (node instanceof STMTreeTermNode term) {
                return term;
            } else {
                for (int i = 0; i < node.getChildCount(); i++) {
                    stack = ListNode.push(stack, node.getStmChild(i));
                }
            }
        }
        return null;
    }

    @NotNull
    public static List<STMTreeTermNode> prepareTerms(@NotNull STMTreeNode root) {
        List<STMTreeTermNode> terms = new ArrayList<>();
        ListNode<STMTreeNode> stack = ListNode.of(root);
        while (ListNode.hasAny(stack)) {
            STMTreeNode node = stack.data;
            stack = stack.next;

            if (node instanceof STMTreeTermNode term) {
                terms.add(term);
            } else {
                for (int i = node.getChildCount() - 1; i >= 0; i--) {
                    stack = ListNode.push(stack, node.getStmChild(i));
                }
            }
        }
        return terms;
    }

    @Nullable
    private static SyntaxInspectionResult inspectAbstractSyntaxAtTreeState(@NotNull STMTreeNode node, @NotNull ATNState initialState) {
        ListNode<Integer> stack = ListNode.of(null);
        {
            var path = new LinkedList<RuleNode>();
            for (STMTreeNode n = node instanceof TerminalNode ? node.getStmParent() : node;
                 n instanceof RuleNode rn;
                 n = n.getStmParent()) {
                path.addFirst(rn);
            }
            for (RuleNode rn : path) {
                stack = ListNode.push(stack, rn.getRuleContext().getRuleIndex());
            }
        }

        int atnStateIndex = node.getAtnState();
        if (atnStateIndex < 0) { 
            return null;  // TODO error node met, consider using previous valid node 
        } else {
            return inspectAbstractSyntaxAtState(stack, initialState);
        }
    }

    public static class SyntaxInspectionResult {
        @NotNull
        public final Set<Integer> predictedTokensIds;
        @NotNull
        public final Set<String> predictedWords;
        @NotNull
        private final Map<Integer, Boolean> reachabilityTests;

        public final boolean expectingTableReference;
        public final boolean expectingColumnReference;
        public final boolean expectingIdentifier;

        public SyntaxInspectionResult(
            @NotNull Set<Integer> predictedTokenIds,
            @NotNull Set<String> predictedWords,
            @NotNull Map<Integer, Boolean> reachabilityTests,
            boolean expectingTableReference,
            boolean expectingColumnReference,
            boolean expectingIdentifier
        ) {
            this.predictedTokensIds = predictedTokenIds;
            this.predictedWords = predictedWords;
            this.reachabilityTests = reachabilityTests;
            this.expectingTableReference = expectingTableReference;
            this.expectingColumnReference = expectingColumnReference;
            this.expectingIdentifier = expectingIdentifier;
        }

        @NotNull
        public Map<String, Boolean> getReachabilityByName() {
            return this.reachabilityTests.entrySet().stream()
                .collect(Collectors.toMap(e -> SQLStandardParser.ruleNames[e.getKey()], Map.Entry::getValue));
        }
    }

    @NotNull
    private static SyntaxInspectionResult inspectAbstractSyntaxAtState(@NotNull ListNode<Integer> stack, @NotNull ATNState initialState) {
        Set<String> predictedWords = new HashSet<>();
        Set<Integer> predictedTokenIds = new HashSet<>();

        Map<Integer, Boolean> reachabilityTests = new HashMap<>(reachabilityTestRules.size());
        reachabilityTestRules.forEach(n -> reachabilityTests.put(n, false));
        Collection<Transition> tt = collectFollowingTerms(stack, initialState, knownReservedWordsExcludeRules, reachabilityTests);

        IntervalSet transitionTokens = getTransitionTokens(tt);

        for (Interval interval : transitionTokens.getIntervals()) {
            int a = interval.a;
            int b = interval.b;
            for (int v = a; v <= b; v++) {
                String word = SQLStandardParser.VOCABULARY.getDisplayName(v);
                if (word != null && knownReservedWords.contains(word)) {
                    predictedTokenIds.add(v);
                    predictedWords.add(word);
                }
            }
        }

        return new SyntaxInspectionResult(
            predictedTokenIds,
            predictedWords,
            reachabilityTests,
            reachabilityTests.get(SQLStandardParser.RULE_tableName),
            reachabilityTests.get(SQLStandardParser.RULE_columnReference),
            reachabilityTests.get(SQLStandardParser.RULE_identifier)
        );
    }

    @NotNull
    private static IntervalSet getTransitionTokens(@NotNull Collection<Transition> transitions) {
        IntervalSet tokens = new IntervalSet();
        for (Transition transition : transitions) {
            switch (transition.getSerializationType()) {
                case Transition.ATOM:  {
                    tokens.add(((AtomTransition) transition).label);
                    break;
                }
                case Transition.RANGE: {
                    RangeTransition t = (RangeTransition) transition;
                    tokens.add(t.from, t.to);
                    break;
                }
                case Transition.SET:
                    tokens.addAll(((SetTransition) transition).set);
                    break;
                case Transition.NOT_SET: 
                case Transition.WILDCARD:
                    // matches "anything" so don't consider them
                    break;
                case Transition.EPSILON:
                case Transition.RULE: 
                    // is not responsible for matching, so ignore them
                case Transition.PREDICATE:
                case Transition.ACTION:
                case Transition.PRECEDENCE:
                    // doesn't describe matching in terms of tokens, so ignore them
                default:
                    throw new UnsupportedOperationException("Unrecognized ATN transition type.");
            }
        }
        return tokens;
    }

    @NotNull
    private static Collection<Transition> collectFollowingTerms(
        @NotNull  ListNode<Integer> stateStack,
        @NotNull ATNState initialState, Set<Integer> exceptRules,
        @NotNull Map<Integer, Boolean> reachabilityTest
    ) {
        HashSet<ATNState> visited = new HashSet<>();
        HashSet<Transition> results = new HashSet<>();
        LinkedList<Pair<ATNState, ListNode<Integer>>> q = new LinkedList<>();
        q.addLast(Pair.of(initialState, stateStack));

        /* TODO roll back through the prepending terms until only one forward branch available for a given sequence of terms
         * to cover situations when we have ambiguous terms like '(' and so all the possible terms are not visible
         * from the given initial state, because there may be more valid states for an ambiguous term
         * in the given context outside of the specific tree
         */

        while (q.size() > 0) {
            Pair<ATNState, ListNode<Integer>> pair = q.removeLast();
            ATNState state = pair.getFirst();
            ListNode<Integer> stack = pair.getSecond();

            for (Transition transition : state.getTransitions()) {
                switch (transition.getSerializationType()) {
                    case Transition.ATOM, Transition.RANGE, Transition.SET, Transition.NOT_SET, Transition.WILDCARD ->
                        results.add(transition);
                    case Transition.RULE, Transition.EPSILON, Transition.PREDICATE, Transition.ACTION, Transition.PRECEDENCE -> {
                        ListNode<Integer> transitionStack;
                        switch (state.getStateType()) {
                            case ATNState.RULE_STOP -> {
                                if (stack != null && stack.data != null && stack.next != null && stack.next.data != null
                                    && transition.target.ruleIndex == stack.next.data
                                ) {
                                    transitionStack = stack.next; // pop
                                } else {
                                    continue;
                                }
                            }
                            case ATNState.RULE_START -> {
                                reachabilityTest.computeIfPresent(state.ruleIndex, (k, v) -> true);
                                if (exceptRules.contains(state.ruleIndex)) {
                                    continue;
                                } else {
                                    transitionStack = ListNode.push(stack, state.ruleIndex);
                                }
                            }
                            default -> transitionStack = stack;
                        }

                        if (visited.add(transition.target)) {
                            q.addLast(Pair.of(transition.target, transitionStack));
                        }
                    }
                    default -> throw new UnsupportedOperationException("Unrecognized ATN transition type.");
                }
            }
        }
        return results;
    }
}
