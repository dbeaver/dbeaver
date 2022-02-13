package org.jkiss.dbeaver.parser.grammar;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
