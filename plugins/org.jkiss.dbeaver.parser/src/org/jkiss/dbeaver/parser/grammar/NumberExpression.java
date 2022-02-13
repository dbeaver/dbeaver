package org.jkiss.dbeaver.parser.grammar;

public class NumberExpression extends UnaryExpression {

    public final int min;
    public final int max;

    protected NumberExpression(RuleExpression expr, int min, int max) {
        super(expr);
        this.min = min;
        this.max = max;

    }

    @Override
    protected <T, R> R applyImpl(ExpressionVisitor<T, R> visitor, T arg) {
        return visitor.visitNumber(this, arg);
    }
}
