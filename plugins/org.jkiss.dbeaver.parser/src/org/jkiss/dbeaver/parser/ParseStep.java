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
package org.jkiss.dbeaver.parser;

import java.util.List;

import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfaOperation;

public class ParseStep {
    private final ParseState from;
    private final ParseState to;
    private final String pattern;
    private final List<GrammarNfaOperation> operations;

    public ParseStep(ParseState from, ParseState to, String pattern, List<GrammarNfaOperation> operations) {
        this.operations = operations;
        this.from = from;
        this.to = to;
        this.pattern = pattern;
    }

    public ParseState getFrom() {
        return from;
    }

    public ParseState getTo() {
        return to;
    }

    public String getPattern() {
        return pattern;
    }

    public List<GrammarNfaOperation> getOperations() {
        return operations;
    }

    
    
}
