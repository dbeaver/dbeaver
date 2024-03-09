package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.jkiss.dbeaver.model.stm.STMTreeNode;

public abstract class SQLQueryLexicalScopeItem {
    protected final STMTreeNode syntaxNode;

    public SQLQueryLexicalScopeItem(STMTreeNode syntaxNode) {
        super();
        this.syntaxNode = syntaxNode;
    }
    
    public STMTreeNode getSyntaxNode() {
        return this.syntaxNode;
    }
    
    public abstract STMTreeNode[] getSyntaxComponents();
}
