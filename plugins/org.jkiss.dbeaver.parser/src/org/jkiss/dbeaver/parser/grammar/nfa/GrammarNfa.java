package org.jkiss.dbeaver.parser.grammar.nfa;

import java.util.ArrayList;
import java.util.List;

import org.jkiss.dbeaver.parser.grammar.GrammarRule;

public class GrammarNfa {
    private final List<GrammarNfaState> states = new ArrayList<GrammarNfaState>();
    private final List<GrammarNfaTransition> transitions = new ArrayList<GrammarNfaTransition>();

    public GrammarNfaState createState(GrammarRule rule) {
        GrammarNfaState state = new GrammarNfaState(this.states.size(), rule);
        this.states.add(state);
        return state;
    }

    public GrammarNfaTransition createTransition(GrammarNfaState from, GrammarNfaState to,
            GrammarNfaOperation operation) {
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
}
