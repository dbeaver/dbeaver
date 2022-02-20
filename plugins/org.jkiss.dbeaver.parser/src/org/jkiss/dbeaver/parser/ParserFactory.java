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

import org.jkiss.dbeaver.parser.grammar.*;
import org.jkiss.dbeaver.parser.grammar.nfa.*;
import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfaBuilder.NfaFragment;

/**
 * Factory of parsers caching parser finite state machine
 */
public class ParserFactory {
    
    private final GrammarInfo grammar;
    private final ParserFsm parserFsm;

    private ParserFactory(GrammarInfo grammar, ParserFsm parserFsm) {
        this.grammar = grammar;
        this.parserFsm = parserFsm;
    }
    
    public Parser createParser() {
        return new Parser(grammar, parserFsm);
    }
    
    public static ParserFactory getFactory(GrammarInfo grammar) {
        GrammarNfaBuilder builder = new GrammarNfaBuilder(grammar);
        
        NfaFragment root = builder.traverseGrammar();
        List<GrammarNfaTransition> terminalTransitions = builder.getTerminalTransitions();

        GrammarAnalyzer analyzer = new GrammarAnalyzer(terminalTransitions, root);
        ParserFsm parserFsm = analyzer.buildTerminalsGraph();
        parserFsm.prepare();

        return new ParserFactory(grammar, parserFsm);
    }
}
