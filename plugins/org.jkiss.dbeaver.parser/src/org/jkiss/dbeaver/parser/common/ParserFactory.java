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

import org.jkiss.dbeaver.parser.common.grammar.GrammarInfo;
import org.jkiss.dbeaver.parser.common.grammar.nfa.GrammarNfaBuilder;
import org.jkiss.dbeaver.parser.common.grammar.nfa.GrammarNfaBuilder.NfaFragment;
import org.jkiss.dbeaver.parser.common.grammar.nfa.GrammarNfaTransition;

import java.util.List;

/**
 * Factory of parsers caching parser finite state machine
 */
public class ParserFactory {
    
    private final GrammarInfo grammar;
    private final NfaFragment nfa;
    private final List<String> errors;

    private ParserFactory(GrammarInfo grammar, NfaFragment nfa, List<String> errors) {
        this.grammar = grammar;
        this.nfa = nfa;
        this.errors = errors;
    }
    
    public static ParserFactory getFactory(GrammarInfo grammar) {
        GrammarNfaBuilder builder = new GrammarNfaBuilder(grammar);
        NfaFragment root = builder.traverseGrammar();
        List<GrammarNfaTransition> terminalTransitions = builder.getTerminalTransitions();

        if (builder.getErrors().size() > 0) {
            return new ParserFactory(grammar, null, builder.getErrors());
        }

        GrammarAnalyzer analyzer = new GrammarAnalyzer(terminalTransitions, root);
        analyzer.discoverByTermRelations();

        if (analyzer.getErrors().size() > 0) {
            return new ParserFactory(grammar, null, analyzer.getErrors());
        }

        return new ParserFactory(grammar, root, List.of());
    }

    public List<String> getErrors() {
        return this.errors;
    }

    public Parser createParser() {
        return new Parser(grammar, nfa);
    }
}
