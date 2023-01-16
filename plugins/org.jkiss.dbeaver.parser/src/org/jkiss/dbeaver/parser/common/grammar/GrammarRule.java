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

public class GrammarRule {
    private final int id;
    private final String name;
    private final boolean useSkipRule;
    private final boolean caseSensitiveTerms;
    private final RuleExpression expression;

    public GrammarRule(int id, String name, boolean useSkipRule, boolean caseSensitiveTerms, RuleExpression expression) {
        this.id = id;
        this.name = name;
        this.useSkipRule = useSkipRule;
        this.expression = expression;
        this.caseSensitiveTerms = caseSensitiveTerms;
    }

    @Override
    public String toString() {
        return this.name + ": " + this.expression + ";";
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isUseSkipRule() {
        return useSkipRule;
    }

    public boolean isCaseSensitiveTerms() {
        return caseSensitiveTerms;
    }

    public RuleExpression getExpression() {
        return expression;
    }

}