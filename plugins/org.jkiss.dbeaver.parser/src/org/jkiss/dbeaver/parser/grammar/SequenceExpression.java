package org.jkiss.dbeaver.parser.grammar;

import java.util.List;

public class SequenceExpression extends GroupExpression {

    protected SequenceExpression(List<RuleExpression> exprs) {
        super(exprs);
    }

    @Override
    protected <T, R> R applyImpl(ExpressionVisitor<T, R> visitor, T arg) {
        return visitor.visitSequence(this, arg);
    }
}
