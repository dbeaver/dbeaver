package org.jkiss.dbeaver.parser.common;


public enum TermPatternCaps {
    FIXED(0),
    VARIABLE(10),
    VAR_NULLABLE(20);

    public final int priority;

    TermPatternCaps(int priority) {
        this.priority = priority;
    }
}
