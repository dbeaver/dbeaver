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
package org.jkiss.dbeaver.model.sql.semantics;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.atn.RangeTransition;
import org.antlr.v4.runtime.atn.RuleStartState;
import org.antlr.v4.runtime.atn.RuleStopState;
import org.antlr.v4.runtime.atn.RuleTransition;
import org.antlr.v4.runtime.atn.SetTransition;
import org.antlr.v4.runtime.atn.Transition;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Trees;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser.SqlQueriesContext;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SyntaxParserTest;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.stm.STMTreeRuleNode;
import org.jkiss.dbeaver.model.stm.STMTreeTermNode;
import org.jkiss.dbeaver.utils.ListNode;
import org.jkiss.utils.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public abstract class SQLQueryCompletionScope {
    
    public static enum SQLCompletionItemKind {
        RESERVED,
        
    }
    
    public static class SQLCompletionItem {
        public final SQLCompletionItemKind kind;
        public final String text;
        
        private SQLCompletionItem(SQLCompletionItemKind kind, String text) {
            this.kind = kind;
            this.text = text;
        }
        
        public static SQLCompletionItem forReservedWord(String text) {
            return new SQLCompletionItem(SQLCompletionItemKind.RESERVED, text);
        }        
    }

    public static final SQLQueryCompletionScope EMPTY = new SQLQueryCompletionScope() {
        @Override
        protected List<SQLCompletionItem> resolveImpl() {
            return Collections.emptyList();
        }
    };
    
    public static final SQLQueryCompletionScope OFFQUERY = new SQLQueryCompletionScope() {
        
        @Override
        protected List<SQLCompletionItem> resolveImpl() {
            return prepareReservedWordsAtRoot();
        }
    };
    
    private List<SQLCompletionItem> items = null;
    
    private SQLQueryCompletionScope() {
    }
    
    public List<SQLCompletionItem> resolve() {
        return this.items != null ? this.items : (this.items = this.resolveImpl());        
    }
    
    public static void main(String[] args) {
        System.out.println(String.join(", ", OFFQUERY.resolve().stream().map(x -> x.text).collect(Collectors.toList())));
        
        var input = CharStreams.fromString("select * from tab where x < 2");
        var ll = new SQLStandardLexer(input, Map.of("'", "'"));
        var tokens = new CommonTokenStream(ll);
        tokens.fill();
        var pp = new SQLStandardParser(tokens);
        SqlQueriesContext tree = pp.sqlQueries();
        
        var tabNode = ((STMTreeTermNode)SyntaxParserTest.findNode(tree, t -> "tab".equals(Trees.getNodeText(t, pp)))).getRealInterval();
        System.out.println(String.join(", ", prepareReservedWordsAtSubtreePosition(tree, tabNode.a - 1).stream().map(x -> x.text).collect(Collectors.toList())));

        var fromNode = ((STMTreeTermNode)SyntaxParserTest.findNode(tree, t -> "from".equals(Trees.getNodeText(t, pp)))).getRealInterval();
        System.out.println(String.join(", ", prepareReservedWordsAtSubtreePosition(tree, fromNode.a - 1).stream().map(x -> x.text).collect(Collectors.toList())));
    }

    protected abstract List<SQLCompletionItem> resolveImpl();

    private static Pair<STMTreeNode, Boolean> findChildBeforeOrAtPosition(STMTreeNode node, int position) {
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
    
    public static SQLQueryCompletionScope forKeywordsAt(SQLQueryNodeModel sqlQueryNodeModel, int position) {
        return new SQLQueryCompletionScope() {
            @Override
            protected List<SQLCompletionItem> resolveImpl() {
                return prepareReservedWordsAtSubtreePosition(sqlQueryNodeModel.getTreeNode(), position);
            }            
        };
    }
    
    private static final List<SQLCompletionItem> prepareReservedWordsAtSubtreePosition(STMTreeNode subroot, int position) {
        ATN atn = SQLStandardParser._ATN;
        
        Interval range = subroot.getRealInterval();
        if (range.a <= position && range.b >= position) {
            Pair<STMTreeNode, Boolean> p = Pair.of(subroot, true);
            while (p.getSecond() && !(p.getFirst() instanceof STMTreeTermNode term)) {
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
                    throw new IllegalStateException("TODO/WTF");
                }
                // TODO watch for state context rule  
            }
   
            return prepareReservedWordsAtState(subroot, initialState);
        } else {
            return Collections.emptyList();
        }                
    }
    
    //class SQLTableReferenceCompletionScope extends SQLCompletionScope {
    //    
    //}
    //
    //class SQLValueExpressionCompletionScope extends SQLCompletionScope {
    //    
    //} 
    
//    private class SQLScriptCompletionScope extends SQLQueryCompletionScope {
//    
//        private final List<SQLCompletionItem> predictedWords;
//    }
    
    
    // TODO consider state's context tree handling to exclude unwanted predictions

    private final static Set<String> knownReservedWords = new HashSet<>(BasicSQLDialect.INSTANCE.getReservedWords());
    
    private static final Set<Integer> knownReservedWordsExcludeRules = Set.of(
            SQLStandardParser.RULE_nonReserved, 
            SQLStandardParser.RULE_anyUnexpected, 
            SQLStandardParser.RULE_aggregateExprParam, 
            SQLStandardParser.RULE_anyWord, 
            SQLStandardParser.RULE_tableHintKeywords
    );

    private static final List<SQLCompletionItem> prepareReservedWordsAtState(STMTreeNode node, ATNState initialState) {
        ATN atn = SQLStandardParser._ATN;
        ListNode<Integer> stack = ListNode.of(null);
        {
            var path = new LinkedList<RuleNode>();
            for (STMTreeNode n = node instanceof TerminalNode ? node.getStmParent() : node; 
                 n != null && n instanceof RuleNode rn;
                 n = n.getStmParent()) {
                path.addFirst(rn);
            }
            for (RuleNode rn: path) {
                stack = ListNode.push(stack, rn.getRuleContext().getRuleIndex());
            }
        }
        
        int atnStateIndex = node.getAtnState();
        if (atnStateIndex < 0) { 
            return Collections.emptyList();  // TODO error node met, consider using previous valid node 
        } else {
            return prepareReservedWordsImpl(atn, stack, initialState);
        }
    }
    
    private static final List<SQLCompletionItem> prepareReservedWordsAtRoot() {
        ATN atn = SQLStandardParser._ATN;
        ListNode<Integer> emptyStack = ListNode.of(null);
        ATNState initialState = atn.states.get(atn.ruleToStartState[SQLStandardParser.RULE_sqlQueries].stateNumber);
        return prepareReservedWordsImpl(atn, emptyStack, initialState);
    }
    
    private static final List<SQLCompletionItem> prepareReservedWordsImpl(ATN atn, ListNode<Integer> stack, ATNState initialState) {
        Set<String> predictedWords = new HashSet<>();
        
        Collection<Transition> tt = collectFollowingTerms(atn, stack, initialState, knownReservedWordsExcludeRules);

        IntervalSet transitionTokens = getTransitionTokens(tt);
        
        for (Interval interval: transitionTokens.getIntervals()) {
            int a = interval.a;
            int b = interval.b;
            for (int v = a; v <= b; v++) {
                String word = SQLStandardParser.VOCABULARY.getDisplayName(v);
                if (word != null && knownReservedWords.contains(word)) {
                    predictedWords.add(word);
                }
            }
        }
        
        List<SQLCompletionItem> result = predictedWords.stream()
                                                       .sorted()
                                                       .map(s -> SQLCompletionItem.forReservedWord(s))
                                                       .collect(Collectors.toUnmodifiableList());
        return result;
    }

    private static IntervalSet getTransitionTokens(Collection<Transition> transitions) {
        IntervalSet tokens = new IntervalSet();
        for (Transition transition: transitions) {
            switch (transition.getSerializationType()) {
                case Transition.ATOM: 
                {
                    tokens.add(((AtomTransition)transition).label);
                    break;
                }
                case Transition.RANGE:
                {
                    RangeTransition t = (RangeTransition)transition;
                    Interval trange = Interval.of(t.from, t.to);
                    tokens.add(t.from, t.to);
                    break;
                }
                case Transition.SET:
                    tokens.addAll(((SetTransition)transition).set);
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
    
    private static Collection<Transition> collectFollowingTerms(ATN atn, ListNode<Integer> stateStack,  ATNState state, Set<Integer> exceptRules) {
        HashSet<Transition> visited = new HashSet<>();
        HashSet<Transition> results = new HashSet<>();
        LinkedList<Pair<Transition, ListNode<Integer>>> q = new LinkedList<>();
        for (Transition t: state.getTransitions()) {
            q.addLast(Pair.of(t, stateStack));
        }
        while (q.size() > 0) {
            Pair<Transition, ListNode<Integer>> pair = q.removeLast();
            Transition transition = pair.getFirst();
            ListNode<Integer> stack = pair.getSecond();

            transition:
            switch (transition.getSerializationType()) {
                case Transition.ATOM:
                case Transition.RANGE:
                case Transition.SET:
                case Transition.NOT_SET: 
                case Transition.WILDCARD:
                    results.add(transition);
                    break;
                case Transition.RULE:
                {
                    RuleTransition rt = (RuleTransition)transition;
                    if (exceptRules.contains(rt.ruleIndex)) {
                        break transition;
                    }
                    // fallthrough
                }
                case Transition.EPSILON:
                case Transition.PREDICATE:
                case Transition.ACTION:
                case Transition.PRECEDENCE:
                {
                    switch (transition.target.getStateType()) {
                    case ATNState.RULE_STOP: 
                        if (stack != null && stack.data != null && ((RuleStopState)transition.target).ruleIndex == stack.data.intValue()) {
                            stack = stack.next; // pop
                        } else {
                            break transition;
                        }
                        break;
                    case ATNState.RULE_START:
                        if (stack == null || stack.data == null || findSubrules(atn, stack.data).contains(((RuleStartState)transition.target).ruleIndex)) {
                            stack = ListNode.push(stack, ((RuleStartState)transition.target).ruleIndex);
                        } else {
                            break transition;
                        }
                        break;
                    }
                    for (Transition t: transition.target.getTransitions()) {
                        if (visited.add(t)) {
                            q.addLast(Pair.of(t, stack));
                        }
                    }                    
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Unrecognized ATN transition type.");
            }
        }
        return results;
    }

    private static final Map<Integer, Set<Integer>> subrulesByRule = new HashMap<>();
    
    private static Set<Integer> findSubrules(ATN atn, int ruleIndex) {
        return subrulesByRule.computeIfAbsent(ruleIndex, x -> {
            HashSet<Integer> result = new HashSet<>();
            for (var s: atn.states) {
                if (s.ruleIndex == ruleIndex) {
                    for (var t: s.getTransitions()) {
                        if (t.target instanceof RuleStartState rs && rs.ruleIndex != ruleIndex) {
                            result.add(rs.ruleIndex);
                        }
                    }
                }
            }
            return result;
        });
    }
}