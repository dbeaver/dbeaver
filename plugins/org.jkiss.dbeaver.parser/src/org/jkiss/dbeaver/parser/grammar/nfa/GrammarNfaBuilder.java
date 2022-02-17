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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class GrammarNfaBuilder implements ExpressionVisitor<Object, GrammarNfaBuilder.NfaFragment> {

    private final GrammarNfa nfa;
    private GrammarRule rule;
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

    public GrammarNfaBuilder(GrammarNfa nfa) {
        this.nfa = nfa;
    }

    public List<GrammarNfaTransition> getTerminalTransitions() {
        return this.terminalTransitions;
    }

    private int nextExprId() {
        return this.exprId++;
    }

    public NfaFragment traverseRule(GrammarRule rule) {
        int exprId = this.nextExprId();
        this.rule = rule;
        NfaFragment fragment = rule.expression.apply(this, null);
        GrammarNfaState start = nfa.createState(rule);
        GrammarNfaState end = nfa.createState(rule);
        nfa.createTransition(start, fragment.from,
                GrammarNfaOperation.makeRuleOperation(exprId, ParseOperationKind.RULE_START, rule.name));
        nfa.createTransition(fragment.to, end,
                GrammarNfaOperation.makeRuleOperation(exprId, ParseOperationKind.RULE_END, rule.name));
        return new NfaFragment(start, end);
    }

    @Override
    public NfaFragment visitAlternative(AlternativeExpression alternatives, Object arg) {
        int exprId = this.nextExprId();
        GrammarNfaState from = nfa.createState(rule);
        GrammarNfaState to = nfa.createState(rule);
        for (RuleExpression alt : alternatives.children) {
            NfaFragment fragment = alt.apply(this, null);
            nfa.createTransition(from, fragment.from, GrammarNfaOperation.makeEmpty(exprId));
            nfa.createTransition(fragment.to, to, GrammarNfaOperation.makeEmpty(exprId));
        }
        return new NfaFragment(from, to);
    }

    @Override
    public NfaFragment visitCharacters(CharactersExpression charactersExpression, Object arg) {
        int exprId = this.nextExprId();
        GrammarNfaState from = nfa.createState(rule);
        GrammarNfaState to = nfa.createState(rule);
        // TODO escape string literal, see
        // https://docs.oracle.com/javase/tutorial/essential/regex/literals.html
        this.terminalTransitions.add(nfa.createTransition(from, to,
                GrammarNfaOperation.makeTerm(exprId, Pattern.quote(charactersExpression.pattern))));
        return new NfaFragment(from, to);
    }

    @Override
    public NfaFragment visitCheck(CheckExpression checkExpression, Object arg) {
        // TODO if it will be needed
        throw new RuntimeException();
    }

    @Override
    public NfaFragment visitCheckNot(CheckNotExpression checkNotExpression, Object arg) {
        // TODO if it will be needed
        throw new RuntimeException();
    }

    @Override
    public NfaFragment visitSequence(SequenceExpression sequence, Object arg) {
        int exprId = this.nextExprId();
        GrammarNfaState from = nfa.createState(rule);
        GrammarNfaState to = nfa.createState(rule);

        int stepsCount = sequence.children.size();
        if (stepsCount > 0) {
            GrammarNfaState prev = from;
            for (int i = 0; i < stepsCount; i++) {
                NfaFragment part = sequence.children.get(i).apply(this, null);
                if (i == 0) {
                    nfa.createTransition(prev, part.from, GrammarNfaOperation.makeSequenceOperation(exprId,
                            ParseOperationKind.SEQ_ENTER, 0, stepsCount, 0));
                } else {
                    nfa.createTransition(prev, part.from, GrammarNfaOperation.makeSequenceOperation(exprId,
                            ParseOperationKind.SEQ_STEP, 0, stepsCount, i));
                }
                prev = part.to;
            }
            nfa.createTransition(prev, to, GrammarNfaOperation.makeSequenceOperation(exprId,
                    ParseOperationKind.SEQ_EXIT, 0, stepsCount, stepsCount));

        } else {
            nfa.createTransition(from, to, GrammarNfaOperation.makeEmpty(exprId));
        }
        return new NfaFragment(from, to);
    }

    @Override
    public NfaFragment visitRuleCall(RuleCallExpression ruleCallExpression, Object arg) {
        int exprId = this.nextExprId();
        GrammarNfaState from = nfa.createState(rule);
        GrammarNfaState to = nfa.createState(rule);
        GrammarNfaState call = nfa.createState(rule);
        nfa.createTransition(from, call,
                GrammarNfaOperation.makeRuleOperation(exprId, ParseOperationKind.CALL, ruleCallExpression.ruleName));
        nfa.createTransition(call, to,
                GrammarNfaOperation.makeRuleOperation(exprId, ParseOperationKind.RESUME, ruleCallExpression.ruleName));
        return new NfaFragment(from, to);
    }

    @Override
    public NfaFragment visitNumber(NumberExpression numberExpression, Object arg) {
        int exprId = this.nextExprId();
        int minNum = numberExpression.min;
        int maxNum = numberExpression.max;

        if (minNum < 0 || maxNum < minNum)
            throw new IllegalArgumentException("Max loop number can't be 0");

        GrammarNfaState from = nfa.createState(rule);
        GrammarNfaState to = nfa.createState(rule);
        NfaFragment part = numberExpression.child.apply(this, null);

        if (maxNum == 1) {
            nfa.createTransition(from, part.from, GrammarNfaOperation.makeEmpty(exprId));
            nfa.createTransition(part.to, to, GrammarNfaOperation.makeEmpty(exprId));
        } else {
            nfa.createTransition(from, part.from, GrammarNfaOperation.makeSequenceOperation(exprId,
                    ParseOperationKind.LOOP_ENTER, minNum, maxNum, null));
            nfa.createTransition(part.to, part.from, GrammarNfaOperation.makeSequenceOperation(exprId,
                    ParseOperationKind.LOOP_INCREMENT, minNum, maxNum, null));
            nfa.createTransition(part.to, to, GrammarNfaOperation.makeSequenceOperation(exprId,
                    ParseOperationKind.LOOP_EXIT, minNum, maxNum, null));
        }
        if (minNum == 0) {
            nfa.createTransition(from, to, GrammarNfaOperation.makeEmpty(exprId));
        }

        return new NfaFragment(from, to);
    }

    @Override
    public NfaFragment visitRegex(RegexExpression regexExpression, Object arg) {
        int exprId = this.nextExprId();
        GrammarNfaState from = nfa.createState(rule);
        GrammarNfaState to = nfa.createState(rule);
        this.terminalTransitions
                .add(nfa.createTransition(from, to, GrammarNfaOperation.makeTerm(exprId, regexExpression.pattern)));
        return new NfaFragment(from, to);
    }

}
