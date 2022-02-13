package org.jkiss.dbeaver.parser.grammar.nfa;

public enum ParseOperationKind {
    RULE_START,
    RULE_END,
    CALL,
    RESUME,
    LOOP_ENTER,
    LOOP_INCREMENT,
    LOOP_EXIT,
    SEQ_ENTER,
    SEQ_STEP,
    SEQ_EXIT,
    TERM,
    NONE
}
