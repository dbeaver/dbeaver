package org.jkiss.dbeaver.parser.grammar;

public class CharactersExpression extends TerminalExpression {

    public CharactersExpression(String characters) {
        super(characters);
    }

    @Override
    protected <T, R> R applyImpl(ExpressionVisitor<T, R> visitor, T arg) {
        return visitor.visitCharacters(this, arg);
    }

}
