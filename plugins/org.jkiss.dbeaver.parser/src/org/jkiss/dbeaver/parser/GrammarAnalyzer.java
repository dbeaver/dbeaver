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

import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfaBuilder.NfaFragment;
import org.jkiss.dbeaver.parser.grammar.nfa.*;

import java.util.*;

/* 
 * Class responsible for building terminal-guided automaton based on grammar graph
 */
class GrammarAnalyzer {

    private final List<GrammarNfaTransition> terminalTransitions;
    private final NfaFragment root;

    /*
     * Grammar graph traversal step
     */
    private static class Step implements Iterable<GrammarNfaTransition> {
        private final Step prev;
        private final boolean isUp;
        private final GrammarNfaTransition transition;

        private Step(Step prev, boolean isUp, GrammarNfaTransition transition) {
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
            ArrayList<String> steps = new ArrayList<>();
            for (Step x = this; x != null; x = x.prev) {
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
                    if (this.current == null) {
                        throw new NoSuchElementException();
                    } else {
                        GrammarNfaTransition result = this.current.transition;
                        this.current = this.current.prev;
                        return result;
                    }
                }

                @Override
                public boolean hasNext() {
                    return this.current != null;
                }
            };
        }

    }

    public GrammarAnalyzer(List<GrammarNfaTransition> terminalTransitions, NfaFragment root) {
        this.terminalTransitions = terminalTransitions;
        this.root = root;
    }

    /**
     * Build a parser finite state machine associating grammar graph operations with terminal-guided transitions
     * @return parser finite state machine
     */
    public ParserFsm buildTerminalsGraph() {
        int fsmsStatesCount = this.root.getFrom().getNext().size() + this.terminalTransitions.size();
        Map<GrammarNfaTransition, ParserFsmNode> states = new HashMap<>(fsmsStatesCount);
        List<ParserFsmNode> initialStates = new ArrayList<>();

        // map each by-text-part transition to finite state machine node
        for (GrammarNfaTransition startTransition : this.root.getFrom().getNext()) {
            ParserFsmNode initialState = new ParserFsmNode(states.size(), false);
            states.put(startTransition, initialState);
            initialStates.add(initialState);
        }
        ParserFsmNode endState = new ParserFsmNode(states.size(), true);
        for (GrammarNfaTransition transition : this.terminalTransitions) {
            //System.out.println("#" + states.size() + " for " + transition.getOperation());
            states.put(transition, new ParserFsmNode(states.size(), false));
        }

        // discover and register all text-start transitions
        for (GrammarNfaTransition startTransition : this.root.getFrom().getNext()) {
            discoverPathsFrom(states, startTransition);
        }
        // discover and register all transitions between terminals
        for (GrammarNfaTransition transition : this.terminalTransitions) {
            discoverPathsFrom(states, transition);
        }

        List<ParserFsmNode> parseFsmStates = new ArrayList<>(states.values());
        parseFsmStates.add(endState);
        return new ParserFsm(initialStates, parseFsmStates);
    }

    /**
     * Discover grammar graph operations associated with all paths from a given transition to other terminal or text-end transitions
     * @param states collection to register
     * @param transition af grammar graph
     */
    private void discoverPathsFrom(Map<GrammarNfaTransition, ParserFsmNode> states, GrammarNfaTransition transition) {
        //System.out.println("starting from " + transition);
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
            states.get(transition).connectTo(states.get(path.transition), path.transition.getOperation().getPattern(), ops);
            //System.out.println(transition.getOperation() + " --> " + path.transition.getOperation() + " { "
            //        + String.join(", ", ops.stream().map(GrammarNfaOperation::toString).collect(Collectors.toList())) + " } ");
        }
    }

    /**
     * Find all paths by DFS from a given transition to other terminal or text-end transitions
     * @param transition of grammar graph
     * @return list of path ending steps
     */
    private List<Step> findPaths(GrammarNfaTransition transition) {
        ArrayDeque<Step> stack = new ArrayDeque<>();
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
                    stack.push(currStep.exit(currStep.transition));

                    for (GrammarNfaTransition child : currStep.transition.getTo().getNext()) {
                        Step next = currStep.enter(child);
                        if (child.getTo() == root.getTo()) {
                            result.add(next);
                        }
                        if (child.getOperation().getKind().equals(ParseOperationKind.TERM)) {
                            result.add(next);
                        } else {
                            stack.push(next);
                        }
                    }
                }
            }
        }
        return result;
    }
}