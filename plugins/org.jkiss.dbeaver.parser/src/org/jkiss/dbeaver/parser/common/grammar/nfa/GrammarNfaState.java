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

import org.jkiss.dbeaver.parser.common.TermPatternInfo;
import org.jkiss.dbeaver.parser.common.grammar.GrammarRule;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * State in the grammar graph
 */
public class GrammarNfaState {
    private final int id;
    private final GrammarRule rule;
    private final ArrayList<GrammarNfaTransition> next;

    private HashMap<TermPatternInfo, ArrayList<GrammarNfaTransition>> nextByTerm = null;

    private TermGroup eofGroup = null;
    private LinkedHashMap<String, TermGroup> termGroupByName = null;
    private Pattern pattern = null;

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

    public void registerExpectedTerm(TermPatternInfo p, GrammarNfaTransition t) {
        if (nextByTerm == null) {
            nextByTerm = new HashMap<>();
        }

        ArrayList<GrammarNfaTransition> tt = nextByTerm.computeIfAbsent(p, k -> new ArrayList<>());
        if (!tt.contains(t)) {
            tt.add(t);
            tt.trimToSize();
        }
    }

    public Set<TermPatternInfo> getExpectedTerms() {
        return nextByTerm == null ? Collections.emptySet() : Collections.unmodifiableSet(nextByTerm.keySet());
    }

    public int getExpectedTermsCount() {
        return nextByTerm.size();
    }

    public boolean isExpectedTermsPopulated() {
        return nextByTerm != null;
    }

    public Map<TermPatternInfo, List<GrammarNfaTransition>> getAllNextByTerms() {
        return Collections.unmodifiableMap(this.nextByTerm);
    }

    public Iterable<GrammarNfaTransition> getNextByTerm(TermPatternInfo term) {
        if (nextByTerm != null) {
            ArrayList<GrammarNfaTransition> transitions = nextByTerm.get(term);
            if (transitions != null) {
                return transitions;
            }
        }
        return Collections.emptyList();
    }

    public void compact() {
        this.next.trimToSize();

        if (nextByTerm != null) {
            for (ArrayList<GrammarNfaTransition> tt : nextByTerm.values()) {
                tt.trimToSize();
            }
        }
    }

    public void prepare() {
        // if (nextByTerm == null) {
        //     System.out.println("WTF " + rule.getName());
        // }
        List<TermPatternInfo> parts = new ArrayList<>(nextByTerm.size());
        this.termGroupByName = new LinkedHashMap<>();
        for (Map.Entry<TermPatternInfo, ArrayList<GrammarNfaTransition>> step : nextByTerm.entrySet()) {
            String groupName = step.getKey().makeRegexGroupName();
            TermGroup termGroup = new TermGroup(groupName, step.getKey(), step.getValue());
            if (step.getKey().isEOF()) {
                this.eofGroup = termGroup;
            } else {
                parts.add(step.getKey());
                this.termGroupByName.put(groupName, termGroup);
            }
        }
        parts.sort(Comparator.comparingInt(a -> a.caps.priority));
        this.pattern = Pattern.compile("(" + parts.stream().map(TermPatternInfo::makeRegexGroup).collect(Collectors.joining("|")) + ")");
    }

    public DispatchResult dispatch(String text, int position) {
        if (pattern == null) {
            throw new IllegalStateException();
        }
        if (position >= text.length()) {
            if (eofGroup != null) {
                return new DispatchResult(eofGroup.term, position, eofGroup.transitions);
            }
        } else {
            Matcher matcher = this.pattern.matcher(text);
            if (matcher.find(position)) {
                for (TermGroup g : this.termGroupByName.values()) {
                    int end = matcher.end(g.groupName);
                    if (end > 0 && end - position > 0 && matcher.start(g.groupName) == position) {
                        return new DispatchResult(g.term, end, g.transitions);
                    }
                }
            }
        }
        return null;
    }

    private static class TermGroup {
        private final String groupName;
        private final TermPatternInfo term;
        private final ArrayList<GrammarNfaTransition> transitions;

        public TermGroup(String groupName, TermPatternInfo term, ArrayList<GrammarNfaTransition> transitions) {
            this.groupName = groupName;
            this.term = term;
            this.transitions = transitions;
            this.transitions.trimToSize();
        }
    }

    public static class DispatchResult {
        public final TermPatternInfo term;
        public final int end;
        public final List<GrammarNfaTransition> transitions;

        public DispatchResult(TermPatternInfo term, int end, ArrayList<GrammarNfaTransition> transitions) {
            this.term = term;
            this.end = end;
            this.transitions = Collections.unmodifiableList(transitions);
        }
    }
}