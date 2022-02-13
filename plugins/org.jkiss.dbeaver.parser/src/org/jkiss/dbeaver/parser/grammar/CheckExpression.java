package org.jkiss.dbeaver.parser.grammar;

public class CheckExpression extends UnaryExpression {

    public CheckExpression(RuleExpression rule) {
        super(rule);
    }

    @Override
    protected <T, R> R applyImpl(ExpressionVisitor<T, R> visitor, T arg) {
        return visitor.visitCheck(this, arg);
    }

}
