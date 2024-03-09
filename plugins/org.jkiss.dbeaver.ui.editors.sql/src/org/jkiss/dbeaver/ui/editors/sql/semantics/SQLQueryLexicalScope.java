package org.jkiss.dbeaver.ui.editors.sql.semantics;

import java.util.Comparator;
import java.util.List;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.ui.editors.sql.semantics.completion.SQLQueryCompletionScope;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;

public class SQLQueryLexicalScope {    
    private final STMTreeNode start, end;
    private final boolean includesStart, includesEnd;
    private final Interval region;
    private final SQLQueryLexicalScopeKind kind;
    
    private SQLQueryDataContext context = null;
    private List<SQLQueryLexicalScopeItem> items = null;

    public SQLQueryLexicalScope(STMTreeNode start, STMTreeNode end, boolean includesStart, boolean includesEnd, SQLQueryLexicalScopeKind kind) {
        this.start = start;
        this.end = end;
        this.includesStart = includesStart;
        this.includesEnd = includesEnd;
        
        Interval a = start.getRealInterval(), b = end.getRealInterval();
        this.region = Interval.of(includesStart ? a.a : (a.b + 1), includesEnd ? b.b : (b.a - 1));
        this.kind = kind;
    }
    
    public Interval getInterval() {
        return this.region;
    }
    
    public SQLQueryDataContext getContext() {
        return this.context;
    }
    
    public void setContext(SQLQueryDataContext context) {
        this.context = context;
    }
    
    public void registerItem(SQLQueryLexicalScopeItem item) {
        this.items = AbstractSyntaxNode.orderedInsert(this.items, x -> x.getSyntaxNode().getRealInterval().a, item, Comparator.comparingInt(x -> x));
    }
    
    public SQLQueryCompletionScope prepareCompletionScope() {
        return switch (this.kind) {
            case KEYWORDS -> SQLQueryCompletionScope.forKeywords(this);
            case ROWSETS -> SQLQueryCompletionScope.forTableReferences(this);
            case VALUES -> SQLQueryCompletionScope.forValueExpressions(this);
            default -> throw new UnsupportedOperationException("Unexpected lexical scope kind " + this.kind);
        };
    }
}