package org.jkiss.dbeaver.parser.grammar;

public class CheckNotExpression extends UnaryExpression {

    public CheckNotExpression(RuleExpression expr) {
        super(expr);
    }

    @Override
    protected <T, R> R applyImpl(ExpressionVisitor<T, R> visitor, T arg) {
        return visitor.visitCheckNot(this, arg);
    }

}
