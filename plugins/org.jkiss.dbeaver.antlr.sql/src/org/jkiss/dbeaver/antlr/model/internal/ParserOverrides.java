package org.jkiss.dbeaver.antlr.model.internal;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

public abstract class ParserOverrides extends Parser {

    public ParserOverrides(TokenStream input) {
        super(input);
    }

    @Override
    public ErrorNode createErrorNode(ParserRuleContext parent, Token t) {
        return new TreeTermErrorNode(t);
    }
    
    @Override
    public TerminalNode createTerminalNode(ParserRuleContext parent, Token t) {
        return new TreeTermNode(t);
    }
}
