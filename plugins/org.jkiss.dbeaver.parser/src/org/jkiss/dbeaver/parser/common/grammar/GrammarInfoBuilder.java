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

public class GrammarInfoBuilder {
    private String name;
    private String startRuleName;
    private String skipRuleName;
    private boolean useSkipRule;
    private boolean caseSensitiveTerms;
    private int ruleIdCounter = 0;
    private Map<String, GrammarRule> rules = new HashMap<>();

    public GrammarInfoBuilder(String name) {
        this.name = name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
    
    public void setStartRuleName(String name) {
        this.startRuleName = name;        
    }    

    public String getStartRuleName() {
        return this.startRuleName;
    }

    public void setSkipRuleName(String name) {
        this.skipRuleName = name;        
    }
    
    public String getSkipRuleName() {
        return this.skipRuleName;
    }
    
    public void setUseSkipRule(boolean value) {
        this.useSkipRule = value;
    }

    public boolean getUseSkipRule() {
        return this.useSkipRule;
    }

    public boolean getCaseSensitiveTerms() {
        return caseSensitiveTerms;
    }

    public void setCaseSensitiveTerms(boolean value) {
        this.caseSensitiveTerms = value;
    }

    public GrammarRule setRule(String name, RuleExpression expression) {
        GrammarRule rule = new GrammarRule(this.ruleIdCounter++, name, this.getUseSkipRule(), this.getCaseSensitiveTerms(), expression);
        this.rules.put(name, rule);
        return rule;
    }

    public GrammarRule findRule(String name) {
        return this.rules.get(name);
    }
    
    public GrammarInfo buildGrammarInfo() {
        return new GrammarInfo(this.name, this.getStartRuleName(), this.getSkipRuleName(), new HashMap<>(this.rules));
    }
}
