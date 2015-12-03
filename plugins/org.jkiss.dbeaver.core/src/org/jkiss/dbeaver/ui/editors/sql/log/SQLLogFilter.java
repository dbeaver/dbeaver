/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.sql.log;

import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.qm.QMEventFilter;
import org.jkiss.dbeaver.model.qm.QMMetaEvent;
import org.jkiss.dbeaver.model.qm.meta.*;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

/**
 * SQL log filter
 */
class SQLLogFilter implements QMEventFilter {

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
        } else {
            if (object instanceof QMMStatementExecuteInfo) {
                return belongsToEditor(((QMMStatementExecuteInfo) object).getStatement().getSession());
            } else if (object instanceof QMMStatementInfo) {
                return belongsToEditor(((QMMStatementInfo) object).getSession());
            } else if (object instanceof QMMTransactionInfo) {
                return belongsToEditor(((QMMTransactionInfo)object).getSession());
            } else if (object instanceof QMMTransactionSavepointInfo) {
                return belongsToEditor(((QMMTransactionSavepointInfo)object).getTransaction().getSession());
            }
        }
        return false;
    }

    private boolean belongsToEditor(QMMSessionInfo session) {
        DBCExecutionContext executionContext = session.getReference();
        return executionContext != null && executionContext == editor.getExecutionContext();
    }

}
