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

import java.util.HashMap;
import java.util.List;

import org.jkiss.dbeaver.parser.grammar.GrammarInfo;
import org.jkiss.dbeaver.parser.grammar.GrammarRule;
import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfa;
import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfaBuilder;
import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfaOperation;
import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfaTransition;
import org.jkiss.dbeaver.parser.grammar.nfa.ParseOperationKind;

public class ParserFactory {
    
    private final ParseFsm parseFsm;

    private ParserFactory(ParseFsm parseFsm) {
        this.parseFsm = parseFsm;
    }
    
    public Parser createParser() {
        return new Parser(parseFsm); 
    }
    
    public static ParserFactory getFactory(GrammarInfo grammar) {
        GrammarNfa nfa = new GrammarNfa();
        GrammarNfaBuilder builder = new GrammarNfaBuilder(nfa);
        HashMap<String, GrammarNfaBuilder.NfaFragment> ruleFragments = new HashMap<>();
        for(GrammarRule rule : grammar.rules.values()) {
            ruleFragments.put(rule.name, builder.traverseRule(rule));
        }

        for (GrammarNfaTransition n : nfa.getTransitions().toArray(new GrammarNfaTransition[0])) {
            if(n.getOperation().getKind() == ParseOperationKind.CALL) {
                nfa.removeTransition(n);
                nfa.createTransition(n.getFrom(), ruleFragments.get(n.getOperation().getRuleName()).getFrom(), GrammarNfaOperation.makeEmpty(n.getOperation().getExprId()));
            }
            if(n.getOperation().getKind() == ParseOperationKind.RESUME) {
                nfa.removeTransition(n);
                nfa.createTransition(ruleFragments.get(n.getOperation().getRuleName()).getTo(), n.getTo(), GrammarNfaOperation.makeEmpty(n.getOperation().getExprId()));
            }
        }

        List<GrammarNfaTransition> terminalTransitions = builder.getTerminalTransitions();
        GrammarAnalyzer analyzer = new GrammarAnalyzer(terminalTransitions, ruleFragments.get(grammar.startRule));
        ParseFsm parseFsm = analyzer.buildTerminalsGraph();
        parseFsm.prepare();

        return new ParserFactory(parseFsm);
    }
}
