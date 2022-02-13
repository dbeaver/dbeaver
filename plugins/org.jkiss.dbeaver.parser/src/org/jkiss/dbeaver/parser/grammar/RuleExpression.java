package org.jkiss.dbeaver.parser.grammar;

public abstract class RuleExpression {

    public final <T, R> R apply(ExpressionVisitor<T, R> visitor, T arg) {
        return this.applyImpl(visitor, arg);
    }

    protected abstract <T, R> R applyImpl(ExpressionVisitor<T, R> visitor, T arg);

    @Override
    public String toString() {
        return ExpressionPrinter.format(this);
    }
}
