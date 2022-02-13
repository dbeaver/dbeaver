package org.jkiss.dbeaver.parser.grammar;

public class RegexExpression extends TerminalExpression {

    public RegexExpression(String pattern) {
        super(pattern);
    }

    @Override
    protected <T, R> R applyImpl(ExpressionVisitor<T, R> visitor, T arg) {
        return visitor.visitRegex(this, arg);
    }

}
