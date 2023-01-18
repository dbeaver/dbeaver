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

import org.jkiss.dbeaver.parser.common.grammar.GrammarRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Non-deterministic finite automaton representing a complete graph of the grammar
 */
public class GrammarNfa {
    private final List<GrammarNfaState> states = new ArrayList<>();
    private final List<GrammarNfaTransition> transitions = new ArrayList<>();

    public GrammarNfaState createState(GrammarRule rule) {
        GrammarNfaState state = new GrammarNfaState(this.states.size(), rule);
        this.states.add(state);
        return state;
    }

    public GrammarNfaTransition createTransition(GrammarNfaState from, GrammarNfaState to, GrammarNfaOperation operation) {
        GrammarNfaTransition transition = new GrammarNfaTransition(from, to, operation);
        this.transitions.add(transition);
        from.getNext().add(transition);
        return transition;
    }

    public List<GrammarNfaState> getStates() {
        return this.states;
    }

    public List<GrammarNfaTransition> getTransitions() {
        return this.transitions;
    }

    public void removeTransition(GrammarNfaTransition n) {
        this.transitions.remove(n);
        n.getFrom().remove(n);
    }

    public void compact() {
        for (GrammarNfaState s : this.states) {
            s.compact();
        }
    }
}
