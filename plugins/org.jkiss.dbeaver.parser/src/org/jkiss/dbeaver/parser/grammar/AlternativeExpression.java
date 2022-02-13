package org.jkiss.dbeaver.parser.grammar;

import java.util.List;

public class AlternativeExpression extends GroupExpression {

    protected AlternativeExpression(List<RuleExpression> alts) {
        super(alts);
    }

    @Override
    protected <T, R> R applyImpl(ExpressionVisitor<T, R> visitor, T arg) {
        return visitor.visitAlternative(this, arg);
    }
}
