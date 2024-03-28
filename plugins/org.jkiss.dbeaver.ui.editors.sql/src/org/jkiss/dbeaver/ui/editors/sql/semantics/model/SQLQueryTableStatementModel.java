package org.jkiss.dbeaver.ui.editors.sql.semantics.model;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;

public abstract class SQLQueryTableStatementModel extends SQLQueryModelContent {
    @Nullable
    private final SQLQueryRowsTableDataModel tableModel;
    private SQLQueryDataContext givenContext = null;
    private SQLQueryDataContext resultContext = null;
    
    public SQLQueryTableStatementModel(@NotNull STMTreeNode syntaxNode, @Nullable SQLQueryRowsTableDataModel tableModel) {
        super(syntaxNode.getRealInterval(), syntaxNode, tableModel);
        this.tableModel = tableModel;
    }

    @Nullable
    public SQLQueryRowsTableDataModel getTableModel() {
        return this.tableModel;
    }

    @Override
    final void applyContext(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        this.givenContext = context;
        if (this.tableModel != null) {
            this.resultContext = this.tableModel.propagateContext(context, statistics);
            this.propagateContextImpl(this.resultContext, statistics);
        }
    }
    
    @Override
    public SQLQueryDataContext getGivenDataContext() {
        return this.givenContext;
    }
    
    @Override
    public SQLQueryDataContext getResultDataContext() {
        return this.resultContext;
    }

    protected abstract void propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics);
}
