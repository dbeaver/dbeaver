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
package org.jkiss.dbeaver.parser.grammar;

import java.util.*;

public class GrammarInfo {
    public final String name;
    public final String startRule;
    public final Map<String, GrammarRule> rules;

    public GrammarInfo(String name, String startRule, Map<String, GrammarRule> rules) {
        this.name = name;
        this.startRule = startRule;
        this.rules = Collections.unmodifiableMap(rules);
    }

    public static GrammarInfo ofRules(String name, GrammarRule... rules) {
        Map<String, GrammarRule> rulesByName = new HashMap<>();
        for (GrammarRule rule : rules) {
            if (rulesByName.containsKey(rule.name)) {
                throw new RuntimeException("Ambiguity at '" + rule.name + "' was met");
            } else {
                rulesByName.put(rule.name, rule);
            }
        }
        return new GrammarInfo(name, rules.length > 0 ? rules[0].name : null, rulesByName);
    }
}
