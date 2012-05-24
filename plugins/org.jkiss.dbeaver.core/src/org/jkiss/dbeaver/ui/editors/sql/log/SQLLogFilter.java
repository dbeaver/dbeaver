/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.log;

import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.runtime.qm.QMMetaEvent;
import org.jkiss.dbeaver.runtime.qm.meta.*;
import org.jkiss.dbeaver.ui.controls.querylog.IQueryLogFilter;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

/**
 * SQL log filter
 */
class SQLLogFilter implements IQueryLogFilter {

    private SQLEditor editor;

    public SQLLogFilter(SQLEditor editor)
    {
        this.editor = editor;
    }

    @Override
    public boolean accept(QMMetaEvent event)
    {
        // Accept only following events:
        // - statement execution (if statement belongs to specific editor)
        // - transaction/savepoint changes (if txn belongs to current datasource)
        // - session changes (if session belongs to active datasource)
        QMMObject object = event.getObject();
        if (object instanceof QMMSessionInfo) {
            return ((QMMSessionInfo)object).getContainer() == editor.getDataSourceContainer();
        } else if (object instanceof QMMStatementExecuteInfo) {
            DBCStatement statement = ((QMMStatementExecuteInfo) object).getStatement().getReference();
            return statement != null && statement.getUserData() == editor.getResultsView().getDataReceiver();
        } else if (object instanceof QMMStatementInfo) {
            DBCStatement statement = ((QMMStatementInfo) object).getReference();
            return statement != null && statement.getUserData() == editor.getResultsView().getDataReceiver();
        } else if (object instanceof QMMStatementScripInfo) {
            return false;
        } else if (object instanceof QMMTransactionInfo) {
            return ((QMMTransactionInfo)object).getSession().getReference() == editor.getDataSource();
        } else if (object instanceof QMMTransactionSavepointInfo) {
            return ((QMMTransactionSavepointInfo)object).getTransaction().getSession().getReference() == editor.getDataSource();
        }
        return false;
    }

}
