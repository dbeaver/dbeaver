package org.jkiss.dbeaver.parser.grammar;

public class RuleCallExpression extends RuleExpression {

    public final String ruleName;

    public RuleCallExpression(String ruleName) {
        this.ruleName = ruleName;
    }

    @Override
    protected <T, R> R applyImpl(ExpressionVisitor<T, R> visitor, T arg) {
        return visitor.visitRuleCall(this, arg);
    }

}
