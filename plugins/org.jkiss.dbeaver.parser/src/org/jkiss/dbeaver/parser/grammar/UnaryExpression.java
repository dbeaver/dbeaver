package org.jkiss.dbeaver.parser.grammar;

public abstract class UnaryExpression extends RuleExpression {

    public final RuleExpression child;

    public UnaryExpression(RuleExpression expr) {
        this.child = expr;
    }

}
