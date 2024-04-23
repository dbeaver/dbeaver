package org.jkiss.dbeaver.model.sql.semantics.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

/**
 * Describes a statements operating with the table (INSERT, DELETE, ...)
 */
public abstract class SQLQueryTableStatementModel extends SQLQueryModelContent {
    @Nullable
    private final SQLQueryRowsTableDataModel tableModel;
    @Nullable
    private SQLQueryDataContext givenContext = null;
    @Nullable
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

    @Nullable
    @Override
    public SQLQueryDataContext getGivenDataContext() {
        return this.givenContext;
    }

    @Nullable
    @Override
    public SQLQueryDataContext getResultDataContext() {
        return this.resultContext;
    }

    protected abstract void propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics);
}
