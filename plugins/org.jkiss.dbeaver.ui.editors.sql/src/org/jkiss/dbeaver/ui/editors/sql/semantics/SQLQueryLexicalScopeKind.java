package org.jkiss.dbeaver.ui.editors.sql.semantics;

public enum SQLQueryLexicalScopeKind {
    // where only keywords expected
    KEYWORDS,
    // where table references expected
    ROWSETS,
    // where value expressions expected
    VALUES
}

