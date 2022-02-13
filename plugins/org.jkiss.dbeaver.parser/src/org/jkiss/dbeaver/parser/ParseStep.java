package org.jkiss.dbeaver.parser;

import java.util.List;

import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfaOperation;

public class ParseStep {
    private final ParseState from;
    private final ParseState to;
    private final String pattern;
    private final List<GrammarNfaOperation> operations;

    public ParseStep(ParseState from, ParseState to, String pattern, List<GrammarNfaOperation> operations) {
        this.operations = operations;
        this.from = from;
        this.to = to;
        this.pattern = pattern;
    }

    public ParseState getFrom() {
        return from;
    }

    public ParseState getTo() {
        return to;
    }

    public String getPattern() {
        return pattern;
    }

    public List<GrammarNfaOperation> getOperations() {
        return operations;
    }

    
    
}
