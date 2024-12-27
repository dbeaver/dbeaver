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
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.utils.ListNode;
import org.jkiss.utils.Pair;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class LSMInspections {

    private static final Pattern anyWordPattern = Pattern.compile("^\\w+$");

    public static boolean matchesAnyWord(String str) {
        return anyWordPattern.matcher(str).matches();
    }

    private static final Set<Integer> KNOWN_IDENTIFIER_PART_TOKENS = Set.of(
        SQLStandardLexer.Identifier,
        SQLStandardLexer.DelimitedIdentifier,
        SQLStandardLexer.Quotted
    );

    public static final Set<Integer> KNOWN_SEPARATOR_TOKENS = Set.of(
        SQLStandardLexer.EqualsOperator,
        SQLStandardLexer.NotEqualsOperator,
        SQLStandardLexer.RightParen,
        SQLStandardLexer.LeftParen,
        SQLStandardLexer.Comma,
        SQLStandardLexer.TypeCast,
        SQLStandardLexer.Colon,
        SQLStandardLexer.Semicolon,
        SQLStandardLexer.Ampersand,
        SQLStandardLexer.Asterisk,
        SQLStandardLexer.Solidus,
        SQLStandardLexer.ConcatenationOperator,
        SQLStandardLexer.Percent,
        SQLStandardLexer.DoublePeriod,
        SQLStandardLexer.GreaterThanOperator,
        SQLStandardLexer.GreaterThanOrEqualsOperator,
        SQLStandardLexer.LessThanOperator,
        SQLStandardLexer.LessThanOrEqualsOperator,
        SQLStandardLexer.LeftBracket,
        SQLStandardLexer.RightBracket,
        SQLStandardLexer.LeftBrace,
        SQLStandardLexer.RightBrace,
        SQLStandardLexer.MinusSign,
        SQLStandardLexer.PlusSign,
        SQLStandardLexer.QuestionMark,
        SQLStandardLexer.VerticalBar,
        SQLStandardLexer.Tilda
    );

    @NotNull
    private static final Set<String> knownReservedWords = new HashSet<>(BasicSQLDialect.INSTANCE.getReservedWords());

    @NotNull
    private static final Set<Integer> presenceTestRules = Set.of(
        SQLStandardParser.RULE_tableName,
        SQLStandardParser.RULE_columnReference,
        SQLStandardParser.RULE_identifier,
        SQLStandardParser.RULE_nonjoinedTableReference,
        SQLStandardParser.RULE_joinCondition
    );

    @NotNull
    private static final Set<Integer> reachabilityTestRules = Set.of(
        SQLStandardParser.RULE_tableName,
        SQLStandardParser.RULE_columnReference,
        SQLStandardParser.RULE_identifier,
        SQLStandardParser.RULE_columnName,
        SQLStandardParser.RULE_nonjoinedTableReference,
        SQLStandardParser.RULE_derivedColumn,
        SQLStandardParser.RULE_pattern
    );

    private static final Map<Integer, List<List<Integer>>> subtreeTests = Map.ofEntries(
        Map.entry(SQLStandardParser.RULE_columnReference, List.of(
            List.of(SQLStandardParser.RULE_anyUnexpected, SQLStandardParser.RULE_searchCondition),
            List.of(SQLStandardParser.RULE_anyUnexpected, SQLStandardParser.RULE_selectSublist)
        )),
        Map.entry(SQLStandardParser.RULE_tableName, List.of(
            List.of(SQLStandardParser.RULE_anyUnexpected, SQLStandardParser.RULE_tableReference)
        ))
    );

    @NotNull
    private static final Set<Integer> knownReservedWordsExcludeRules = Set.of(
        SQLStandardParser.RULE_tableName,
        SQLStandardParser.RULE_columnReference,
        SQLStandardParser.RULE_identifier,
        SQLStandardParser.RULE_columnName,

        SQLStandardParser.RULE_nonReserved,
        SQLStandardParser.RULE_anyUnexpected,
        SQLStandardParser.RULE_aggregateExprParam,
        SQLStandardParser.RULE_anyWord,
        SQLStandardParser.RULE_correlationName,
        SQLStandardParser.RULE_tableHintKeywords
    );

    @NotNull
    private final SQLDialect dialect;
    @NotNull
    private final STMTreeNode root;

    @NotNull
    private final List<STMTreeNode> allTerms;
    private final List<STMTreeTermNode> allNonErrorTerms;

    public LSMInspections(@NotNull SQLDialect dialect, @NotNull STMTreeNode root) {
        this.dialect = dialect;
        this.root = root;
        Pair<List<STMTreeNode>, List<STMTreeTermNode>> termLists = this.prepareTerms(root);
        this.allTerms = termLists.getFirst();
        this.allNonErrorTerms = termLists.getSecond();
    }

    private static final SyntaxInspectionResult offqueryInspectionResult = prepareOffquerySyntaxInspectionInternal();

    @NotNull
    public static SyntaxInspectionResult prepareOffquerySyntaxInspection() {
        return offqueryInspectionResult;
    }

    @NotNull
    public static SyntaxInspectionResult prepareOffquerySyntaxInspectionInternal() {
        ATN atn = SQLStandardParser._ATN;
        ListNode<Integer> emptyStack = ListNode.of(null);
        ATNState initialState = atn.states.get(atn.ruleToStartState[SQLStandardParser.RULE_sqlQueries].stateNumber);
        return inspectAbstractSyntaxAtState(null, emptyStack, initialState);
    }

    @Nullable
    public SyntaxInspectionResult prepareAbstractSyntaxInspection(int position) {
        STMTreeNode subroot = this.root;
        ATN atn = SQLStandardParser._ATN;

        Interval range = subroot.getRealInterval();
        if (position < range.a) {
            return prepareOffquerySyntaxInspection();
        } else {
            int index;
            STMTreeTermNode node;
            ATNState initialState;

            if (position > range.b) {
                if (this.allNonErrorTerms.size() > 0) {
                    index = this.allNonErrorTerms.size() - 1;
                    node = this.allNonErrorTerms.get(index);
                    subroot = node.getParentNode();
                    initialState = atn.states.get(node.getAtnState()).getTransitions()[0].target;
                } else {
                    return SyntaxInspectionResult.EMPTY;
                }
            } else {
                index = STMUtils.binarySearchByKey(this.allNonErrorTerms, t -> t.getRealInterval().a, position, Comparator.comparingInt(k -> k));
                if (index < 0) {
                    index = ~index - 1;
                }

                node = this.allNonErrorTerms.get(index);
                subroot = node.getParentNode();
                Interval nodeRange = node.getRealInterval();
                if (nodeRange.a <= position) {
                    if (nodeRange.b >= position) {
                        // containing term found
                        if (KNOWN_SEPARATOR_TOKENS.contains(node.symbol.getType())) {
                            // we need target state of the previous term
                            node = this.allNonErrorTerms.get(index - 1);
                            initialState = atn.states.get(node.getAtnState()).getTransitions()[0].target;
                            subroot = node.getParentNode();
                        } else {
                            // we need its start state
                            initialState = atn.states.get(node.getAtnState());
                        }
                    } else {
                        // otherwise position is after its end, so we need its end state
                        initialState = atn.states.get(node.getAtnState()).getTransitions()[0].target;
                    }
                } else if (index > 0) {
                    // use previous node, its rule end state
                    node = this.allNonErrorTerms.get(index - 1);
                    initialState = atn.states.get(node.getAtnState()).getTransitions()[0].target;
                    subroot = node.getParentNode();
                } else {
                    // subroot itself contains given position, use its rule start state
                    initialState = atn.states.get(subroot.getAtnState());
                }
            }

            return inspectAbstractSyntaxAtTreeState(subroot, initialState);
        }            
    }

    @NotNull
    public static Pair<List<STMTreeNode>, List<STMTreeTermNode>> prepareTerms(@NotNull STMTreeNode root) {
        List<STMTreeNode> allTerms = new ArrayList<>();
        List<STMTreeTermNode> allNonErrorTerms = new ArrayList<>();

        ListNode<STMTreeNode> stack = ListNode.of(root);
        while (ListNode.hasAny(stack)) {
            STMTreeNode node = stack.data;
            stack = stack.next;

            if (node instanceof STMTreeTermNode term) {
                allTerms.add(term);
                allNonErrorTerms.add(term);
            } else if (node instanceof STMTreeTermErrorNode err) {
                allTerms.add(err);
            } else {
                for (int i = node.getChildCount() - 1; i >= 0; i--) {
                    stack = ListNode.push(stack, node.getChildNode(i));
                }
            }
        }
        return Pair.of(allTerms, allNonErrorTerms);
    }

    @Nullable
    private static SyntaxInspectionResult inspectAbstractSyntaxAtTreeState(@NotNull STMTreeNode node, @NotNull ATNState initialState) {
        ListNode<Integer> stack = ListNode.of(null);
        {
            var path = new LinkedList<RuleNode>();
            for (STMTreeNode n = node instanceof TerminalNode ? node.getParentNode() : node;
                 n instanceof RuleNode rn;
                 n = n.getParentNode()) {
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
            return inspectAbstractSyntaxAtState(node, stack, initialState);
        }
    }

    public record SyntaxInspectionResult(
        @NotNull Set<Integer> predictedTokenIds,
        @NotNull Set<String> predictedWords,
        @NotNull Map<Integer, Boolean> reachabilityTests,
        boolean expectingTableReference,
        boolean expectingColumnName,
        boolean expectingColumnReference,
        boolean expectingIdentifier,
        boolean expectingTableSourceIntroduction,
        boolean expectingColumnIntroduction,
        boolean expectingValue,
        boolean expectingJoinCondition
    ) {

        public static final SyntaxInspectionResult EMPTY = new SyntaxInspectionResult(
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptyMap(),
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false
        );

        @NotNull
        public Map<String, Boolean> getReachabilityByName() {
            return this.reachabilityTests.entrySet().stream()
                .collect(Collectors.toMap(e -> SQLStandardParser.ruleNames[e.getKey()], Map.Entry::getValue));
        }
    }

    public record NameInspectionResult(
        ArrayDeque<STMTreeNode> nameNodes,
        boolean hasPeriod,
        STMTreeNode currentTerm,
        int positionToInspect
    ){
    }

    public NameInspectionResult collectNameNodes(int position) {
        ArrayDeque<STMTreeNode> nameNodes = new ArrayDeque<>();
        int index = STMUtils.binarySearchByKey(this.allTerms, t -> t.getRealInterval().a, position, Comparator.comparingInt(k -> k));
        if (index < 0) {
            index = ~index - 1;
        }

        int positionToInspect = position;
        boolean hasPeriod = false;
        STMTreeNode currentTerm = null;
        if (index >= 0) {
            STMTreeNode immTerm = allTerms.get(index);
            // position is actually considered to be right _after_ the term of interest,
            // so use we the previous one on the exact match
            if (immTerm.getRealInterval().a >= position) {
                if (index > 0) {
                    immTerm = allTerms.get(index - 1);
                    index--;
                } else  {
                    immTerm = null;
                }
            }
            if (immTerm != null && immTerm.getRealInterval().properlyContains(Interval.of(position - 1, position - 1))) {
                if (anyWordPattern.matcher(immTerm.getTextContent()).matches()) {
                    currentTerm = immTerm;
                }
                if (dialect.getReservedWords().contains(immTerm.getTextContent().toUpperCase())) {
                    positionToInspect = immTerm.getRealInterval().a;
                }
                if (immTerm instanceof STMTreeTermNode t && t.symbol.getType() == SQLStandardLexer.Period) {
                    hasPeriod = true;
                    index--; // skip identifier separator immediately before the cursor
                }
                for (int i = index; i >= 0; i--) {
                    STMTreeNode term = allTerms.get(i);
                    if (term instanceof STMTreeTermNode t && KNOWN_IDENTIFIER_PART_TOKENS.contains(t.symbol.getType())
                        || (term.getParentNode() != null && term.getParentNode().getNodeKindId() == SQLStandardParser.RULE_nonReserved)
                        || term instanceof STMTreeTermErrorNode
                    ) {
                        nameNodes.addFirst(term);
                        i--;
                        if (i < 0 || (allTerms.get(i) instanceof STMTreeTermNode t && t.symbol.getType() != SQLStandardLexer.Period)) {
                            break; // not followed by an identifier separator part
                        }
                    } else {
                        break; // not an identifier part
                    }
                }
            }
        }

        return new NameInspectionResult(nameNodes, hasPeriod, currentTerm, positionToInspect);
    }

    private static Map<Integer, Boolean> performPresenceTests(ListNode<Integer> stateStack) {
        Map<Integer, Boolean> presenceTests = new HashMap<>(presenceTestRules.size());
        presenceTestRules.forEach(n -> presenceTests.put(n, false));

        for (Integer s : stateStack) {
            presenceTests.computeIfPresent(s, (k, v) -> true);
        }

        performSubtreeTests(presenceTests, stateStack);

        return presenceTests;
    }


    @NotNull
    private static SyntaxInspectionResult inspectAbstractSyntaxAtState(
        @Nullable STMTreeNode node,
        @NotNull ListNode<Integer> stack,
        @NotNull ATNState initialState
    ) {
        Set<String> predictedWords = new HashSet<>();
        Set<Integer> predictedTokenIds = new HashSet<>();

        Map<Integer, Boolean> presenceTests = performPresenceTests(stack);

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

        boolean expectingTableName = reachabilityTests.get(SQLStandardParser.RULE_tableName) || presenceTests.get(SQLStandardParser.RULE_tableName);
        boolean expectingColumnName = reachabilityTests.get(SQLStandardParser.RULE_columnName);
        boolean expectingColumnReference = reachabilityTests.get(SQLStandardParser.RULE_columnReference) || presenceTests.get(SQLStandardParser.RULE_columnReference);
        return new SyntaxInspectionResult(
            predictedTokenIds,
            predictedWords,
            reachabilityTests,
            expectingTableName,
            expectingColumnName,
            expectingColumnReference,
            reachabilityTests.get(SQLStandardParser.RULE_identifier) || presenceTests.get(SQLStandardParser.RULE_identifier),
            expectingTableName && (reachabilityTests.get(SQLStandardParser.RULE_nonjoinedTableReference) ||
                presenceTests.get(SQLStandardParser.RULE_nonjoinedTableReference)),
            expectingColumnReference && reachabilityTests.get(SQLStandardParser.RULE_derivedColumn),
            reachabilityTests.get(SQLStandardParser.RULE_pattern),
            presenceTests.get(SQLStandardParser.RULE_joinCondition) && (
                node instanceof STMTreeTermNode term && term.getSymbol().getType() == SQLStandardLexer.ON
            )
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

    private static String collectStack(ListNode<Integer> stack) {
        return StreamSupport.stream(stack.spliterator(), false)
            .map(ss -> ss == null ? "<NULL>" : SQLStandardParser.ruleNames[ss])
            .collect(Collectors.joining(", "));
    }

    @NotNull
    private static Collection<Transition> collectFollowingTerms(
        @NotNull ListNode<Integer> stateStack,
        @NotNull ATNState initialState, Set<Integer> exceptRules,
        @NotNull Map<Integer, Boolean> reachabilityTests
    ) {
        HashSet<Pair<ATNState, ListNode<Integer>>> visited = new HashSet<>();
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
                                reachabilityTests.computeIfPresent(state.ruleIndex, (k, v) -> true);

                                transitionStack = ListNode.push(stack, state.ruleIndex);
                                performSubtreeTests(reachabilityTests, transitionStack);
                                if (exceptRules.contains(state.ruleIndex)) {
                                    continue;
                                }
                            }
                            default -> transitionStack = stack;
                        }

                        Pair<ATNState, ListNode<Integer>> nextState = Pair.of(transition.target, transitionStack);
                        if (visited.add(nextState)) {
                            q.addLast(nextState);
                        }
                    }
                    default -> throw new UnsupportedOperationException("Unrecognized ATN transition type.");
                }
            }
        }
        return results;
    }

    private static void performSubtreeTests(@NotNull Map<Integer, Boolean> reachabilityTest, ListNode<Integer> stack) {
        for (Map.Entry<Integer, List<List<Integer>>> subtreeTest : subtreeTests.entrySet()) {
            subtreeTests:
            for (List<Integer> subpath : subtreeTest.getValue()) {
                ListNode<Integer> stackItem = stack;

                for (Integer subpathNode : subpath) {
                    if (subpathNode.equals(stackItem.data)) {
                        stackItem = stackItem.next;
                    } else {
                        continue subtreeTests;
                    }
                }

                reachabilityTest.computeIfPresent(subtreeTest.getKey(), (k, v) -> true);
                break;
            }
        }
    }
}
