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
package org.jkiss.dbeaver.parser.common.grammar.nfa;

import org.jkiss.dbeaver.parser.common.TermPatternInfo;
import org.jkiss.dbeaver.parser.common.grammar.GrammarRule;

public class GrammarNfaOperation {
    private final int exprId;
    private final ParseOperationKind kind;
    private final String tag;
    private final TermPatternInfo pattern;
    private final GrammarRule rule;
    private final Integer minIterations;
    private final Integer maxIterations;
    private final Integer exprPosition;

    private GrammarNfaOperation(int exprId, ParseOperationKind kind, String tag, TermPatternInfo pattern, GrammarRule ruleName,
            Integer minIterations, Integer maxIterations, Integer exprPosition
    ) {
        this.exprId = exprId;
        this.kind = kind;
        this.tag = tag;
        this.pattern = pattern;
        this.rule = ruleName;
        this.minIterations = minIterations;
        this.maxIterations = maxIterations;
        this.exprPosition = exprPosition;
        this.validate();
    }

    public int getExprId() {
        return exprId;
    }

    public ParseOperationKind getKind() {
        return kind;
    }

    public String getTag() {
        return tag;
    }

    public TermPatternInfo getPattern() {
        return pattern;
    }

    public GrammarRule getRule() {
        return rule;
    }

    public Integer getMinIterations() {
        return minIterations;
    }

    public Integer getMaxIterations() {
        return maxIterations;
    }

    public Integer getExprPosition() {
        return exprPosition;
    }

    /**
     * Checks correctness of operation itself
     */
    private void validate() {
        switch (this.kind) {
            case RULE_START:
            case RULE_END:
            case CALL:
            case RESUME:
                if (this.rule == null)
                    throw new IllegalArgumentException();
                break;
            case LOOP_ENTER:
            case LOOP_INCREMENT:
            case LOOP_EXIT:
            case SEQ_ENTER:
            case SEQ_STEP:
            case SEQ_EXIT:
                if (this.maxIterations == null || this.minIterations == null)
                    throw new IllegalArgumentException();
                if (this.kind == ParseOperationKind.SEQ_STEP && this.exprPosition == null)
                    throw new IllegalArgumentException();
                break;
            case TERM:
                if (this.pattern == null)
                    throw new IllegalArgumentException();
                break;
            case NONE:
                // do nothing
                break;
            default:
                throw new UnsupportedOperationException("Unexpected parse operation kind " + this.kind);
        }
    }

    @Override
    public String toString() {
        String result = this.kind.toString() + "#" + this.exprId + "[";
        switch (this.kind) {
            case RULE_START:
            case RULE_END:
            case CALL:
            case RESUME:
                result += this.rule.getName();
                break;
            case LOOP_ENTER:
            case LOOP_INCREMENT:
            case LOOP_EXIT:
            case SEQ_ENTER:
            case SEQ_STEP:
            case SEQ_EXIT:
                if (this.exprPosition != null) {
                    result += this.exprPosition + "/";
                }
                result += this.minIterations + ".." + this.maxIterations;
                break;
            case TERM:
                result += this.pattern.pattern;
                break;
            case NONE:
                // do nothing
                break;
            default:
                throw new UnsupportedOperationException("Unexpected parse opeation kind " + this.kind);
        }
        return result + "]";
    }

    public static GrammarNfaOperation makeRuleOperation(int exprId, ParseOperationKind opKind, String tag, GrammarRule rule) {
        return new GrammarNfaOperation(exprId, opKind, tag, null, rule, null, null, null);
    }

    public static GrammarNfaOperation makeSequenceOperation(int exprId, ParseOperationKind opKind, int min, int max,
            Integer exprPosition) {
        return new GrammarNfaOperation(exprId, opKind, null, null, null, min, max, exprPosition);
    }

    public static GrammarNfaOperation makeTerm(int exprId, String tag, TermPatternInfo pattern) {
        return new GrammarNfaOperation(exprId, ParseOperationKind.TERM, tag, pattern, null, null, null, null);
    }

    public static GrammarNfaOperation makeEmpty(int exprId) {
        return new GrammarNfaOperation(exprId, ParseOperationKind.NONE, null, null, null, null, null, null);
    }
}
