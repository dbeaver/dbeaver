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

package org.jkiss.dbeaver.model.qm;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.*;

/**
 * Query manager execution handler.
 * Handler methods are invoked right at time of DBC operation, so they should work as fast as possible.
 * Implementers should not invoke any DBC execution function in passed objects - otherwise execution handling may enter infinite recursion.
 */
public interface QMExecutionHandler {

    String getHandlerName();

    void handleContextOpen(DBCExecutionContext context, boolean transactional);

    void handleContextClose(DBCExecutionContext context);

    void handleSessionOpen(DBCSession session);

    void handleSessionClose(DBCSession session);

    void handleTransactionAutocommit(DBCExecutionContext context, boolean autoCommit);

    void handleTransactionIsolation(DBCExecutionContext context, DBPTransactionIsolation level);

    void handleTransactionCommit(DBCExecutionContext context);

    void handleTransactionSavepoint(DBCSavepoint savepoint);

    void handleTransactionRollback(DBCExecutionContext context, @Nullable DBCSavepoint savepoint);

    void handleStatementOpen(DBCStatement statement);

    void handleStatementExecuteBegin(DBCStatement statement);

    void handleStatementExecuteEnd(DBCStatement statement, long rows, Throwable error);

    void handleStatementBind(DBCStatement statement, Object column, @Nullable Object value);

    void handleStatementClose(DBCStatement statement, long rows);

    void handleResultSetOpen(DBCResultSet resultSet);

    void handleResultSetClose(DBCResultSet resultSet, long rowCount);

    void handleScriptBegin(DBCSession session);
    
    void handleScriptEnd(DBCSession session);

}
