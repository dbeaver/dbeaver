package org.jkiss.dbeaver.parser.grammar.nfa;

public class GrammarNfaTransition {
    private final GrammarNfaState from;
    private final GrammarNfaState to;
    private final GrammarNfaOperation operation;

    public GrammarNfaTransition(GrammarNfaState from, GrammarNfaState to, GrammarNfaOperation operation) {
        this.from = from;
        this.to = to;
        this.operation = operation;
    }

    @Override
    public String toString() {
        return this.from.getId() + "->" + this.to.getId() + " " + this.operation.toString();
    }

    public GrammarNfaState getFrom() {
        return from;
    }

    public GrammarNfaState getTo() {
        return to;
    }

    public GrammarNfaOperation getOperation() {
        return operation;
    }

}
