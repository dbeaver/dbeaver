// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator.advanced.regex;

import java.util.List;
import dk.brics.automaton.Transition;
import dk.brics.automaton.State;
import dk.brics.automaton.RegExp;
import java.util.Random;
import dk.brics.automaton.Automaton;

public class Xeger
{
    private final Automaton automaton;
    private final Random random;
    
    public Xeger(final String regex, final Random random) {
        assert regex != null;
        assert random != null;
        this.automaton = new RegExp(regex).toAutomaton();
        this.random = random;
    }
    
    public Xeger(final String regex) {
        this(regex, new Random());
    }
    
    public String generate() {
        final StringBuilder builder = new StringBuilder();
        this.generate(builder, this.automaton.getInitialState());
        return builder.toString();
    }
    
    private void generate(final StringBuilder builder, final State state) {
        final List<Transition> transitions = (List<Transition>)state.getSortedTransitions(true);
        if (transitions.size() == 0) {
            assert state.isAccept();
        }
        else {
            final int nroptions = state.isAccept() ? transitions.size() : (transitions.size() - 1);
            final int option = XegerUtils.getRandomInt(0, nroptions, this.random);
            if (state.isAccept() && option == 0) {
                return;
            }
            final Transition transition = transitions.get(option - (state.isAccept() ? 1 : 0));
            this.appendChoice(builder, transition);
            this.generate(builder, transition.getDest());
        }
    }
    
    private void appendChoice(final StringBuilder builder, final Transition transition) {
        final char c = (char)XegerUtils.getRandomInt(transition.getMin(), transition.getMax(), this.random);
        builder.append(c);
    }
}
