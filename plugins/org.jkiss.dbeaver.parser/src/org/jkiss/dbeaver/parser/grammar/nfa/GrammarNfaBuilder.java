/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.parser.grammar.nfa;

import org.jkiss.dbeaver.parser.grammar.*;

import java.util.*;

/**
 * Builder of the complete grammar graph in form of non-deterministic finite automaton
 * describing all possible subsequences of the text and its logical structure
 */
public class GrammarNfaBuilder {

    private final GrammarInfo grammar;
    private final GrammarNfa nfa;
    private int exprId = 0;

    private final List<GrammarNfaTransition> terminalTransitions = new ArrayList<>();

    public static class NfaFragment {
        private final GrammarNfaState from;
        private final GrammarNfaState to;

        public NfaFragment(GrammarNfaState from, GrammarNfaState to) {
            this.from = from;
            this.to = to;
        }

        public GrammarNfaState getFrom() {
            return from;
        }

        public GrammarNfaState getTo() {
            return to;
        }
    }

    public GrammarNfaBuilder(GrammarInfo grammar) {
        this.grammar = grammar;
        this.nfa = new GrammarNfa();
    }

    public List<GrammarNfaTransition> getTerminalTransitions() {
        return this.terminalTransitions;
    }

    private int nextExprId() {
        return this.exprId++;
    }

    /**
     * Walk through the grammar and build the complete grammar graph in form of non-deterministic finite automaton
     * @return start and final states of the complete grammar graph
     */
    public NfaFragment traverseGrammar() {
        Collection<GrammarRule> grammarRules = grammar.getRules();
        HashMap<GrammarRule, GrammarNfaBuilder.NfaFragment> ruleFragments = new HashMap<>(grammarRules.size());
        RuleVisitor ruleVisitor = new RuleVisitor();
        for (GrammarRule rule : grammarRules) {
            ruleFragments.put(rule, ruleVisitor.traverseRule(rule));
        }

        for (GrammarNfaTransition n : nfa.getTransitions().toArray(new GrammarNfaTransition[0])) {
            if(n.getOperation().getKind() == ParseOperationKind.CALL) {
                nfa.removeTransition(n);
                nfa.createTransition(n.getFrom(), ruleFragments.get(n.getOperation().getRule()).getFrom(), GrammarNfaOperation.makeEmpty(n.getOperation().getExprId()));
            }
            if(n.getOperation().getKind() == ParseOperationKind.RESUME) {
                nfa.removeTransition(n);
                nfa.createTransition(ruleFragments.get(n.getOperation().getRule()).getTo(), n.getTo(), GrammarNfaOperation.makeEmpty(n.getOperation().getExprId()));
            }
        }

        return ruleFragments.get(grammar.findRule(grammar.getStartRuleName()));
    }

    private class RuleVisitor implements ExpressionVisitor<GrammarRule, GrammarNfaBuilder.NfaFragment> {
        private NfaFragment traverseRule(GrammarRule rule) {
            int exprId = nextExprId();
            NfaFragment fragment = rule.getExpression().apply(this, rule);
            GrammarNfaState start = nfa.createState(rule);
            GrammarNfaState end = nfa.createState(rule);
            nfa.createTransition(start, fragment.from, GrammarNfaOperation.makeRuleOperation(exprId, ParseOperationKind.RULE_START, rule));
            nfa.createTransition(fragment.to, end, GrammarNfaOperation.makeRuleOperation(exprId, ParseOperationKind.RULE_END, rule));
            return new NfaFragment(start, end);
        }

        private NfaFragment visitSkipRuleIfNeeded(GrammarRule rule) {
            if (rule.isUseSkipRule()) {
                GrammarRule skipRule = grammar.findRule(grammar.getSkipRuleName());
                return this.traverseRule(skipRule);
            } else {
                GrammarNfaState state = nfa.createState(rule);
                return new NfaFragment(state, state);
            }
        }

        @Override
        public NfaFragment visitAlternative(AlternativeExpression alternatives, GrammarRule rule) {
            int exprId = nextExprId();
            GrammarNfaState from = nfa.createState(rule);
            GrammarNfaState to = nfa.createState(rule);
            for (RuleExpression alt : alternatives.children) {
                NfaFragment fragment = alt.apply(this, rule);
                nfa.createTransition(from, fragment.from, GrammarNfaOperation.makeEmpty(exprId));
                nfa.createTransition(fragment.to, to, GrammarNfaOperation.makeEmpty(exprId));
            }
            return new NfaFragment(from, to);
        }

        @Override
        public NfaFragment visitCharacters(CharactersExpression charactersExpression, GrammarRule rule) {
            int exprId = nextExprId();
            NfaFragment head = this.visitSkipRuleIfNeeded(rule);
            GrammarNfaState from = head.to;
            GrammarNfaState to = nfa.createState(rule);
            
            String rawChars = charactersExpression.pattern;
            
            // optional checks to separate consequent words from each other
            String lookbehind = Character.isLetterOrDigit(rawChars.charAt(0)) ? "\\b" : "";
            String lookahead = Character.isLetterOrDigit(rawChars.charAt(rawChars.length() - 1)) ? "\\b" : "";
            String pattern = lookbehind + RegexExpression.escapeSpecialChars(rawChars) + lookahead;
            
            terminalTransitions.add(nfa.createTransition(
                from, to, GrammarNfaOperation.makeTerm(exprId, pattern)
            ));
            return new NfaFragment(head.from, to);
        }

        @Override
        public NfaFragment visitCheck(CheckExpression checkExpression, GrammarRule rule) {
            // TODO if it will be needed
            throw new RuntimeException();
        }

        @Override
        public NfaFragment visitCheckNot(CheckNotExpression checkNotExpression, GrammarRule rule) {
            // TODO if it will be needed
            throw new RuntimeException();
        }

        @Override
        public NfaFragment visitSequence(SequenceExpression sequence, GrammarRule rule) {
            int exprId = nextExprId();
            GrammarNfaState from = nfa.createState(rule);
            GrammarNfaState to = nfa.createState(rule);

            int stepsCount = sequence.children.size();
            if (stepsCount > 0) {
                GrammarNfaState prev = from;
                for (int i = 0; i < stepsCount; i++) {
                    NfaFragment part = sequence.children.get(i).apply(this, rule);
                    if (i == 0) {
                        nfa.createTransition(prev, part.from, GrammarNfaOperation.makeSequenceOperation(
                            exprId, ParseOperationKind.SEQ_ENTER, 0, stepsCount, 0
                        ));
                    } else {
                        nfa.createTransition(prev, part.from, GrammarNfaOperation.makeSequenceOperation(
                            exprId, ParseOperationKind.SEQ_STEP, 0, stepsCount, i
                        ));
                    }
                    prev = part.to;
                }
                nfa.createTransition(prev, to, GrammarNfaOperation.makeSequenceOperation(
                    exprId, ParseOperationKind.SEQ_EXIT, 0, stepsCount, stepsCount
                ));

            } else {
                nfa.createTransition(from, to, GrammarNfaOperation.makeEmpty(exprId));
            }
            return new NfaFragment(from, to);
        }

        @Override
        public NfaFragment visitRuleCall(RuleCallExpression ruleCallExpression, GrammarRule rule) {
            int exprId = nextExprId();
            GrammarNfaState from = nfa.createState(rule);
            GrammarNfaState to = nfa.createState(rule);
            GrammarNfaState call = nfa.createState(rule);
            GrammarRule targetRule = grammar.findRule(ruleCallExpression.ruleName);
            nfa.createTransition(from, call, GrammarNfaOperation.makeRuleOperation(exprId, ParseOperationKind.CALL, targetRule));
            nfa.createTransition(call, to, GrammarNfaOperation.makeRuleOperation(exprId, ParseOperationKind.RESUME, targetRule));
            return new NfaFragment(from, to);
        }

        @Override
        public NfaFragment visitNumber(NumberExpression numberExpression, GrammarRule rule) {
            int exprId = nextExprId();
            int minNum = numberExpression.min;
            int maxNum = numberExpression.max;

            if (minNum < 0 || maxNum < minNum)
                throw new IllegalArgumentException("Max loop number can't be 0");

            GrammarNfaState from = nfa.createState(rule);
            GrammarNfaState to = nfa.createState(rule);
            NfaFragment part = numberExpression.child.apply(this, rule);

            if (maxNum == 1) {
                nfa.createTransition(from, part.from, GrammarNfaOperation.makeEmpty(exprId));
                nfa.createTransition(part.to, to, GrammarNfaOperation.makeEmpty(exprId));
            } else {
                nfa.createTransition(from, part.from, GrammarNfaOperation.makeSequenceOperation(
                    exprId, ParseOperationKind.LOOP_ENTER, minNum, maxNum, null
                ));
                nfa.createTransition(part.to, part.from, GrammarNfaOperation.makeSequenceOperation(
                    exprId, ParseOperationKind.LOOP_INCREMENT, minNum, maxNum, null
                ));
                nfa.createTransition(part.to, to, GrammarNfaOperation.makeSequenceOperation(
                    exprId, ParseOperationKind.LOOP_EXIT, minNum, maxNum, null
                ));
            }
            if (minNum == 0) {
                nfa.createTransition(from, to, GrammarNfaOperation.makeEmpty(exprId));
            }

            return new NfaFragment(from, to);
        }

        @Override
        public NfaFragment visitRegex(RegexExpression regexExpression, GrammarRule rule) {
            int exprId = nextExprId();
            NfaFragment head = this.visitSkipRuleIfNeeded(rule);
            GrammarNfaState from = head.to;
            GrammarNfaState to = nfa.createState(rule);
            terminalTransitions.add(nfa.createTransition(from, to, GrammarNfaOperation.makeTerm(exprId, regexExpression.pattern)));
            return new NfaFragment(head.from, to);
        }
    }
}
