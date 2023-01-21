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

import org.jkiss.dbeaver.parser.common.grammar.nfa.GrammarNfaOperation;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Node of the parser finite state machine representing logical position in the text between some terminals
 */
class ParserFsmNode {
    private final int id;
    private final ArrayList<ParserFsmStep> steps = new ArrayList<>();
    private final Map<String, TermGroup> stepsByTermGroup = new HashMap<>();
    private final ArrayList<ParserFsmStep> finalSteps = new ArrayList<>();
    private final boolean isEnd;
    private Pattern pattern;

    /**
     * A bunch of transitions associated with the same terminal
     */
    private static class TermGroup {
        private final String groupName;
        private final String pattern;
        private final ArrayList<ParserFsmStep> steps;

        public TermGroup(String groupName, String pattern, ArrayList<ParserFsmStep> steps) {
            this.groupName = groupName;
            this.pattern = pattern;
            this.steps = steps;
            this.steps.trimToSize();
        }
    }

    public ParserFsmNode(int id, boolean isEnd) {
        this.id = id;
        this.isEnd = isEnd;
    }

    public void compact() {
        this.steps.trimToSize();
        for (ParserFsmStep s : steps) {
            s.getOperations().trimToSize();
        }
        for (TermGroup g : stepsByTermGroup.values()) {
            g.steps.trimToSize();
        }
        finalSteps.trimToSize();
    }

    public boolean isEnd() {
        return this.isEnd;
    }

    public int getId() {
        return this.id;
    }

    public List<ParserFsmStep> getTransitions() {
        return Collections.unmodifiableList(this.steps);
    }

    /**
     * Register transition from the current node to the target node,
     * associate it to the given patterns and grammar operations
     */
    public void connectTo(ParserFsmNode target, String pattern, String tag, ArrayList<GrammarNfaOperation> operations) {
        this.steps.add(new ParserFsmStep(this, target, pattern, tag, operations));
    }

    /**
     * Prepare common recognizing pattern for all possible terminals associated with presented parsing steps
     */
    public void prepare() {
        Map<String, ArrayList<ParserFsmStep>> stepsByTerm = new HashMap<>();
        for (ParserFsmStep s : this.steps) {
            if (s.getPattern() != null) {
                stepsByTerm.computeIfAbsent(s.getPattern(), p -> new ArrayList<>()).add(s);
            } else {
                finalSteps.add(s);
            }
        }

        List<String> parts = new ArrayList<>(stepsByTerm.size());
        for (var step : stepsByTerm.entrySet()) {
            String groupName = "g" + this.stepsByTermGroup.size();
            parts.add("(?<" + groupName + ">(" + step.getKey() + "))");
            this.stepsByTermGroup.put(groupName, new TermGroup(groupName, step.getKey(), step.getValue()));
        }
        this.pattern = Pattern.compile("(" + String.join("|", parts) + ")");
    }

    /**
     * Fills given collection with parsing steps by the terminal matched at the given position in the text
     * @param text
     * @param position
     */
    public void dispatch(String text, int position, ArrayList<ParserDispatchResult> results) {
        Matcher matcher = this.pattern.matcher(text);
        if (matcher.find(position)) {
            for (TermGroup g : this.stepsByTermGroup.values()) {
                int end = matcher.end(g.groupName);
                if (end >= 0 && matcher.start(g.groupName) == position) {
                    for (ParserFsmStep step : g.steps) {
                        //if (s.getTo() != null) {
                        //    System.out.println("found " + g.pattern + " at " + position + "  " + s.getFrom().id + " --> " + s.getTo().id);
                        //}
                        results.add(new ParserDispatchResult(end, step));
                    }
                }
            }
        }

        for (ParserFsmStep s : finalSteps) {
            results.add(new ParserDispatchResult(position, s));
        }
    }

    @Override
    public String toString() {
        return "#" + this.id;
    }
}
