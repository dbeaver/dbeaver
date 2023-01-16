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
package org.jkiss.dbeaver.parser.common;

import org.jkiss.dbeaver.parser.common.grammar.nfa.GrammarNfaOperation;

import java.util.ArrayList;

/**
 * Transition between parser finite state machine nodes
 */
class ParserFsmStep {
    private final ParserFsmNode from;
    private final ParserFsmNode to;
    private final String pattern;
    private final String tag;
    private final ArrayList<GrammarNfaOperation> operations;

    public ParserFsmStep(ParserFsmNode from, ParserFsmNode to, String pattern, String tag, ArrayList<GrammarNfaOperation> operations) {
        this.from = from;
        this.to = to;
        this.pattern = pattern;
        this.tag = tag;
        this.operations = operations;
    }

    public ParserFsmNode getFrom() {
        return from;
    }

    public ParserFsmNode getTo() {
        return to;
    }

    public String getPattern() {
        return pattern;
    }

    public String getTag() {
        return tag;
    }

    public ArrayList<GrammarNfaOperation> getOperations() {
        return operations;
    }
}
