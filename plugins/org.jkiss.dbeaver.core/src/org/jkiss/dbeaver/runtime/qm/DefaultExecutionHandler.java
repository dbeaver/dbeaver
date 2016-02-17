/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.runtime.qm;

import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.qm.QMExecutionHandler;

/**
 * Default execution handler.
 * Handle methods are no-ops.
 */
public abstract class DefaultExecutionHandler implements QMExecutionHandler {

    @Override
    public void handleContextOpen(DBCExecutionContext context, boolean transactional)
    {

    }

    @Override
    public void handleContextClose(DBCExecutionContext context)
    {

    }

    @Override
    public void handleSessionOpen(DBCSession session)
    {

    }

    @Override
    public void handleSessionClose(DBCSession session)
    {

    }

    @Override
    public void handleTransactionAutocommit(DBCExecutionContext context, boolean autoCommit)
    {

    }

    @Override
    public void handleTransactionIsolation(DBCExecutionContext context, DBPTransactionIsolation level)
    {

    }

    @Override
    public void handleTransactionCommit(DBCExecutionContext context)
    {

    }

    @Override
    public void handleTransactionSavepoint(DBCSavepoint savepoint)
    {

    }

    @Override
    public void handleTransactionRollback(DBCExecutionContext context, DBCSavepoint savepoint)
    {

    }

    @Override
    public void handleStatementOpen(DBCStatement statement)
    {

    }

    @Override
    public void handleStatementExecuteBegin(DBCStatement statement)
    {

    }

    @Override
    public void handleStatementExecuteEnd(DBCStatement statement, long rows, Throwable error)
    {
        
    }

    @Override
    public void handleStatementBind(DBCStatement statement, Object column, Object value)
    {

    }

    @Override
    public void handleStatementClose(DBCStatement statement, long rows)
    {

    }

    @Override
    public void handleResultSetOpen(DBCResultSet resultSet)
    {

    }

    @Override
    public void handleResultSetClose(DBCResultSet resultSet, long rowCount)
    {

    }

    @Override
    public void handleScriptBegin(DBCSession session)
    {

    }

    @Override
    public void handleScriptEnd(DBCSession session)
    {

    }
}
