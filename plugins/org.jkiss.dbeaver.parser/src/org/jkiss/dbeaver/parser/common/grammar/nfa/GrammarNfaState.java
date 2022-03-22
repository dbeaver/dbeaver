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
package org.jkiss.dbeaver.parser.common.grammar.nfa;

import java.util.ArrayList;
import java.util.List;

import org.jkiss.dbeaver.parser.common.grammar.GrammarRule;

/**
 * State in the grammar graph
 */
public class GrammarNfaState {
    private final int id;
    private final GrammarRule rule;
    private final List<GrammarNfaTransition> next;

    public GrammarNfaState(int id, GrammarRule rule) {
        this.id = id;
        this.rule = rule;
        this.next = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "GrammarNfaState#" + this.id;
    }

    public int getId() {
        return id;
    }

    public GrammarRule getRule() {
        return rule;
    }

    public List<GrammarNfaTransition> getNext() {
        return next;
    }

    public void remove(GrammarNfaTransition t) {
        this.next.remove(t);
    }
}