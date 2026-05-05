/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.editors.sql.log;

import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.qm.QMEvent;
import org.jkiss.dbeaver.model.qm.QMEventFilter;
import org.jkiss.dbeaver.model.qm.meta.*;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

import java.util.Objects;

/**
 * SQL log filter
 */
public class SQLLogFilter implements QMEventFilter {

    private SQLEditor editor;

    public SQLLogFilter(SQLEditor editor)
    {
        this.editor = editor;
    }

    @Override
    public boolean accept(QMEvent event) {
        // Accept only following events:
        // - statement execution (if statement belongs to specific execution context)
        // - transaction/savepoint changes (if txn belongs to current datasource)
        // - session changes (if session belongs to active datasource)
        QMMObject object = event.getObject();
        if (object instanceof QMMConnectionInfo) {
            return editor.getDataSourceContainer() != null && Objects.equals(((QMMConnectionInfo) object).getContainerId(), editor.getDataSourceContainer().getId());
        } else {
            if (object instanceof QMMStatementExecuteInfo) {
                return belongsToExecutionContext(((QMMStatementExecuteInfo) object).getStatement().getConnection());
            } else if (object instanceof QMMStatementInfo) {
                return belongsToExecutionContext(((QMMStatementInfo) object).getConnection());
            } else if (object instanceof QMMTransactionInfo) {
                return belongsToExecutionContext(((QMMTransactionInfo) object).getConnection());
            } else if (object instanceof QMMTransactionSavepointInfo) {
                return belongsToExecutionContext(((QMMTransactionSavepointInfo) object).getTransaction().getConnection());
            }
        }
        return false;
    }
    
    public SQLEditor getEditor() {
        return editor;
    }

    private boolean belongsToExecutionContext(QMMConnectionInfo session) {
        String containerId = session.getContainerId();
        String contextName = session.getContextName();
        DBCExecutionContext executionContext = editor.getExecutionContext();
        return executionContext != null &&
                Objects.equals(executionContext.getDataSource().getContainer().getId(), containerId) &&
                Objects.equals(executionContext.getContextName(), contextName);
    }

}
