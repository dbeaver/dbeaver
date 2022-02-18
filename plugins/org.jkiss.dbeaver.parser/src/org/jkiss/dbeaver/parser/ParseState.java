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

import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfaOperation;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseState {

    private final int id;
    private final List<ParseStep> steps = new ArrayList<>();
    private final Map<String, TermGroup> stepsByTermGroup = new HashMap<>();
    private final List<ParseStep> finalSteps = new ArrayList<>();
    private final boolean isEnd;
    private Pattern pattern;

    private static class TermGroup {
        private final String groupName;
        private final String pattern;
        private final List<ParseStep> steps;

        public TermGroup(String groupName, String pattern, List<ParseStep> steps) {
            this.groupName = groupName;
            this.pattern = pattern;
            this.steps = steps;
        }      
        
    }

    public ParseState(int id, boolean isEnd) {
        this.id = id;
        this.isEnd = isEnd;
    }

    public boolean isEnd() {
        return this.isEnd;
    }

    public void connectTo(ParseState taret, String pattern, List<GrammarNfaOperation> operations) {
        this.steps.add(new ParseStep(this, taret, pattern, operations));
    }

    public int getId() {
        return this.id;
    }

    public Iterable<ParseStep> getTransitions() {
        return this.steps;
    }

    public void prepare() {
        Map<String, List<ParseStep>> stepsByTerm = new HashMap<>();
        for (ParseStep s : this.steps) {
            if (s.getPattern() != null) {
                stepsByTerm.computeIfAbsent(s.getPattern(), p -> new ArrayList<>()).add(s);
            } else {
                finalSteps.add(s);
            }
        }

        List<String> parts = new ArrayList<>(stepsByTerm.size());
        for (Entry<String, List<ParseStep>> ss : stepsByTerm.entrySet()) {
            String groupName = "g" + this.stepsByTermGroup.size();
            parts.add("(?<" + groupName + ">(" + ss.getKey() + "))");
            this.stepsByTermGroup.put(groupName, new TermGroup(groupName, ss.getKey(), ss.getValue()));
        }
        this.pattern = Pattern.compile("(" + String.join("|", parts) + ")");
    }

    public Iterable<ParseDispatchResult> dispatch(String text, int position) {
        ArrayList<ParseDispatchResult> results = new ArrayList<>();
        Matcher matcher = this.pattern.matcher(text);
        if (matcher.find(position)) {
            for (TermGroup g : this.stepsByTermGroup.values()) {
                int end = matcher.end(g.groupName);
                if (end >= 0 && matcher.start(g.groupName) == position) {
                    for (ParseStep s : g.steps) {
                        if (s.getTo() != null) {
                            System.out.println(
                                    "found " + g.pattern + " at " + position + "  " + s.getFrom().id + " --> " + s.getTo().id);
                        }
                        results.add(new ParseDispatchResult(end, s));
                    }
                }
            }
        }

        for (ParseStep s : finalSteps) {
            results.add(new ParseDispatchResult(position, s));
        }

        return results;
    }

}
