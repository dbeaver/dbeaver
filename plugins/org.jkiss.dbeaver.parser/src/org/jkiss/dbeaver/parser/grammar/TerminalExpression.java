package org.jkiss.dbeaver.parser.grammar;

public abstract class TerminalExpression extends RuleExpression {

    public final String pattern;

    public TerminalExpression(String pattern) {
        this.pattern = pattern;
    }

}
