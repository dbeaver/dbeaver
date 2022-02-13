package org.jkiss.dbeaver.parser.grammar;

public class GrammarRule {
    public final String name;
    public final RuleExpression expression;

    public GrammarRule(String name, RuleExpression expression) {
        this.name = name;
        this.expression = expression;
    }

    @Override
    public String toString() {
        return this.name + ": " + this.expression + ";";
    }

}