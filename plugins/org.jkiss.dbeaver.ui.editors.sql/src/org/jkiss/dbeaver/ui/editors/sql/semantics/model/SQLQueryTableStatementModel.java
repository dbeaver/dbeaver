package org.jkiss.dbeaver.ui.editors.sql.semantics.model;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;

public abstract class SQLQueryTableStatementModel extends SQLQueryModelContent {
    @Nullable
    private final SQLQueryRowsTableDataModel tableModel;
    
    public SQLQueryTableStatementModel(@NotNull Interval region, @Nullable SQLQueryRowsTableDataModel tableModel) {
        super(region);
        this.tableModel = tableModel;
    }

    @Nullable
    public SQLQueryRowsTableDataModel getTableModel() {
        return this.tableModel;
    }

    @Override
    void applyContext(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        if (this.tableModel != null) {
            SQLQueryDataContext tableContext = this.tableModel.propagateContext(context, statistics);
            this.propagateContextImpl(tableContext, statistics);
        }
    }

    protected abstract void propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics);
}
