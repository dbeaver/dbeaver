package org.jkiss.dbeaver.parser;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfaBuilder.NfaFragment;
import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfaOperation;
import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfaState;
import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfaTransition;
import org.jkiss.dbeaver.parser.grammar.nfa.ParseOperationKind;

public class GrammarAnalyzer {

    private static class Step implements Iterable<GrammarNfaTransition> {
        private final Step prev;
        private final boolean isUp;
        private final GrammarNfaTransition transition;

        public Step(Step prev, boolean isUp, GrammarNfaTransition transition) {
            this.prev = prev;
            this.isUp = isUp;
            this.transition = transition;
        }

        public static Step initial(GrammarNfaTransition transition) {
            return new Step(null, false, transition);
        }

        public Step enter(GrammarNfaTransition transition) {
            return new Step(this, false, transition);
        }

        public Step exit(GrammarNfaTransition transition) {
            return new Step(this, true, transition);
        }

        @Override
        public String toString() {
            var steps = new ArrayList<String>();
            for (var x = this; x != null; x = x.prev) {
                steps.add(x.transition.toString());
            }
            return "Path of " + String.join(", ", steps);
        }

        @Override
        public Iterator<GrammarNfaTransition> iterator() {
            return new Iterator<GrammarNfaTransition>() {
                private Step current = Step.this;

                @Override
                public GrammarNfaTransition next() {
                    GrammarNfaTransition result = this.current.transition;
                    this.current = this.current.prev;
                    return result;
                }

                @Override
                public boolean hasNext() {
                    return this.current != null;
                }
            };
        }

    }

    final List<GrammarNfaTransition> terminalTransitions;
    final NfaFragment root;
    final Map<GrammarNfaState, Set<GrammarNfaState>> paths = new HashMap<>();

    public GrammarAnalyzer(List<GrammarNfaTransition> terminalTransitions, NfaFragment root) {
        this.terminalTransitions = terminalTransitions;
        this.root = root;
    }

    public ParseFsm buildTerminalsGraph() {
        Map<GrammarNfaTransition, ParseState> states = new HashMap<>();
        List<ParseState> initialStates = new ArrayList<>();
        for (GrammarNfaTransition startTransition : this.root.getFrom().getNext()) {
            ParseState initialState = new ParseState(states.size(), false);
            states.put(startTransition, initialState);
            initialStates.add(initialState);
        }
        ParseState endState = new ParseState(states.size(), true);
        for (GrammarNfaTransition transition : this.terminalTransitions) {
            System.out.println("#" + states.size() + " for " + transition.getOperation());
            states.put(transition, new ParseState(states.size(), false));
        }
        for (GrammarNfaTransition startTransition : this.root.getFrom().getNext()) {
            findPathsFrom(states, startTransition);
        }
        for (GrammarNfaTransition transition : this.terminalTransitions) {
            findPathsFrom(states, transition);
        }

//		System.out.println(graph.size() + " paths found ");
//		for (var term: states.) {
//			System.out.println(term. src.getOperation() + " --> " + term.transition.getOperation() + " { " + String.join(", ", ops) + " }");
//		}

        List<ParseState> parseFsmStates = new ArrayList<>(states.values());
        parseFsmStates.add(endState);
        return new ParseFsm(initialStates, parseFsmStates);
    }

    private void findPathsFrom(Map<GrammarNfaTransition, ParseState> states, GrammarNfaTransition transition) {
        System.out.println("starting from " + transition);
        List<Step> paths = this.findPaths(transition);
        for (Step path : paths) {
            List<GrammarNfaOperation> ops = new ArrayList<>();
            for (GrammarNfaTransition step : path) {
                GrammarNfaOperation op = step.getOperation();
                if (op.getKind() != ParseOperationKind.TERM && op.getKind() != ParseOperationKind.NONE) {
                    ops.add(op);
                }
            }
            Collections.reverse(ops);
            states.get(transition).connectTo(states.get(path.transition), path.transition.getOperation().getPattern(),
                    ops);
            System.out.println(transition.getOperation() + " --> " + path.transition.getOperation() + " { "
                    + String.join(", ", ops.stream().map(o -> o.toString()).collect(Collectors.toList())) + " } ");
        }
    }

    private List<Step> findPaths(GrammarNfaTransition transition) {
        Stack<Step> stack = new Stack<>();
        stack.push(Step.initial(transition));
        List<Step> result = new ArrayList<>();
        BitSet active = new BitSet();

        while (stack.size() > 0) {
            Step currStep = stack.pop();
            if (currStep.isUp) {
                active.clear(currStep.transition.getTo().getId());
            } else {
                if (active.get(currStep.transition.getTo().getId())) {
                    throw new RuntimeException("recursion hit at " + currStep);
                } else {
                    active.set(currStep.transition.getTo().getId());
                    stack.add(currStep.exit(currStep.transition));

                    for (GrammarNfaTransition child : currStep.transition.getTo().getNext()) {
                        Step next = currStep.enter(child);
                        if (child.getTo() == root.getTo()) {
                            result.add(next);
                        }
                        if (child.getOperation().getKind().equals(ParseOperationKind.TERM)) {
                            result.add(next);
                        } else {
                            stack.add(next);
                        }
                    }
                }
            }
        }
        return result;
    }
}