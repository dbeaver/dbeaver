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
package org.jkiss.dbeaver.parser.common.grammar;

import java.util.*;

/**
 * Parsing rule set describing logical structure of some text
 */
public class GrammarInfo {
    private final String name;
    private final String startRuleName;
    private final String skipRuleName;
    private final Map<String, GrammarRule> rules;

    public GrammarInfo(String name, String startRule, String skipRule, Map<String, GrammarRule> rules) {
        this.name = name;
        this.startRuleName = startRule;
        this.skipRuleName = skipRule;
        this.rules = Collections.unmodifiableMap(rules);
    }
    
    public String getName() {
        return this.name;
    }

    public String getStartRuleName() {
        return this.startRuleName;
    }

    public String getSkipRuleName() {
        return this.skipRuleName;
    }

    public GrammarRule getRule(String name) {
        GrammarRule rule = this.rules.get(name);
        if (rule == null) {
            throw new IllegalArgumentException("Rule '" + name + "' not defined in grammar '" + this.getName() + "'.");
        }
        return rule;
    }

    public GrammarRule findRule(String name) {
        return this.rules.get(name);
    }
    
    public Collection<GrammarRule> getRules() {
        return Collections.unmodifiableCollection(this.rules.values());
    }
    
    public static GrammarInfo ofRules(String name, GrammarRule... rules) {
        Map<String, GrammarRule> rulesByName = new HashMap<>();
        for (GrammarRule rule : rules) {
            if (rulesByName.containsKey(rule.getName())) {
                throw new RuntimeException("Ambiguity at '" + rule.getName() + "' was met");
            } else {
                rulesByName.put(rule.getName(), rule);
            }
        }
        return new GrammarInfo(name, rules.length > 0 ? rules[0].getName() : null, null, rulesByName);
    }
}
