package org.jkiss.dbeaver.parser.grammar.nfa;

import java.util.ArrayList;
import java.util.List;

import org.jkiss.dbeaver.parser.grammar.GrammarRule;

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