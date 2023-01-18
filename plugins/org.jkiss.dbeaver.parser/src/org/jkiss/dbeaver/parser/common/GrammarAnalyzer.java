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

import org.jkiss.dbeaver.parser.common.grammar.nfa.GrammarNfaBuilder.NfaFragment;
import org.jkiss.dbeaver.parser.common.grammar.nfa.GrammarNfaState;
import org.jkiss.dbeaver.parser.common.grammar.nfa.GrammarNfaTransition;
import org.jkiss.dbeaver.parser.common.grammar.nfa.ParseOperationKind;

import java.util.*;

/*
 * Class responsible for building terminal-guided automaton based on grammar graph
 */
class GrammarAnalyzer {

    private final List<GrammarNfaTransition> terminalTransitions;
    private final NfaFragment root;
    private final Map<Integer, String> recursionErrorsByTargetId = new HashMap<>();

    /*
     * Grammar graph traversal step
     */
    private static class Step {
        private final boolean isUp;
        private final GrammarNfaState state;
        private final GrammarNfaTransition transition;

        private Step(boolean isUp, GrammarNfaState state, GrammarNfaTransition transition) {
            this.isUp = isUp;
            this.state = state;
            this.transition = transition;
        }

        public static Step initial(GrammarNfaState state) {
            return new Step(false, state, null);
        }

        public Step enter(GrammarNfaTransition transition) {
            return new Step(false, transition.getTo(), transition);
        }

        public Step exit() {
            return new Step(true, state, transition);
        }

        @Override
        public String toString() {
            return transition == null ? state.toString() : transition.toString();
        }
    }

    public GrammarAnalyzer(List<GrammarNfaTransition> terminalTransitions, NfaFragment root) {
        this.terminalTransitions = terminalTransitions;
        this.root = root;
    }

    public List<String> getErrors() {
        return List.copyOf(this.recursionErrorsByTargetId.values());
    }

    /**
     * Discover and register all text-start transitions
     */
    public void discoverByTermRelations() {
        Queue<GrammarNfaState> queue = new LinkedList<>();
        Set<GrammarNfaState> queued = new HashSet<>();

        queue.add(this.root.getFrom());

        while (!queue.isEmpty()) {
            GrammarNfaState state = queue.remove();
            this.discoverPaths(state, queue, queued);
        }
    }

    /**
     * Find all paths by DFS from a given transition to other terminal or text-end transitions
     */
    private void discoverPaths(GrammarNfaState start, Queue<GrammarNfaState> queue, Set<GrammarNfaState> queued) {
        ArrayDeque<Step> stack = new ArrayDeque<>();
        stack.push(Step.initial(start));
        BitSet active = new BitSet();

        int wtf = 0;
        while (stack.size() > 0) {
            Step currStep = stack.pop();
            if (currStep.isUp) {
                for (GrammarNfaTransition transition : currStep.state.getNext()) { // propagate expected terms back
                    if (transition.getOperation().getKind() != ParseOperationKind.TERM) {
                        for (TermPatternInfo term : transition.getTo().getExpectedTerms()) {
                            currStep.state.registerExpectedTerm(term, transition);
                        }
                    }
                }
                active.clear(currStep.state.getId());
            } else {
                if (active.get(currStep.state.getId())) {
                    int targetId = currStep.state.getId();
                    if (!recursionErrorsByTargetId.containsKey(targetId)) {
                        recursionErrorsByTargetId.put(targetId, "Recursion detected at " + currStep);
                    }
                    stack.push(currStep.exit());
                } else {
                    active.set(currStep.state.getId());
                    stack.push(currStep.exit());

                    if (!currStep.state.isExpectedTermsPopulated()) {
                        for (GrammarNfaTransition transition : currStep.state.getNext()) {
                            if (recursionErrorsByTargetId.containsKey(transition.getTo().getId())) {
                                continue;
                            }

                            if (transition.getTo() == root.getTo()) {
                                currStep.state.registerExpectedTerm(TermPatternInfo.EOF, transition);
                            }
                            if (transition.getOperation().getKind().equals(ParseOperationKind.TERM)) {
                                currStep.state.registerExpectedTerm(transition.getOperation().getPattern(), transition);
                                if (queued.add(transition.getTo())) {
                                    queue.add(transition.getTo());
                                }
                            } else {
                                stack.push(currStep.enter(transition));
                            }
                        }
                    }
                }
            }
        }
        start.prepare();
    }
}