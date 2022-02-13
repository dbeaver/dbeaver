package org.jkiss.dbeaver.parser.grammar;

public interface ExpressionVisitor<T, R> {

    R visitAlternative(AlternativeExpression alternativesExpr, T arg);

    R visitCharacters(CharactersExpression characters, T arg);

    R visitCheck(CheckExpression checkExpression, T arg);

    R visitCheckNot(CheckNotExpression checkNotExpression, T arg);

    R visitSequence(SequenceExpression sequenceExpression, T arg);

    R visitRuleCall(RuleCallExpression ruleCallExpression, T arg);

    R visitNumber(NumberExpression numberExpression, T arg);

    R visitRegex(RegexExpression regexExpression, T arg);

}
