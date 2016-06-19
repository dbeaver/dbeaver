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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.qm.QMExecutionHandler;
import org.jkiss.dbeaver.model.runtime.features.DBRFeature;

import java.util.Map;

/**
 * Default execution handler.
 * Handle methods are no-ops.
 */
public abstract class DefaultExecutionHandler implements QMExecutionHandler {

    @Override
    public void handleContextOpen(@NotNull DBCExecutionContext context, boolean transactional)
    {

    }

    @Override
    public void handleContextClose(@NotNull DBCExecutionContext context)
    {

    }

    @Override
    public void handleSessionOpen(@NotNull DBCSession session)
    {

    }

    @Override
    public void handleSessionClose(@NotNull DBCSession session)
    {

    }

    @Override
    public void handleTransactionAutocommit(@NotNull DBCExecutionContext context, boolean autoCommit)
    {

    }

    @Override
    public void handleTransactionIsolation(@NotNull DBCExecutionContext context, @NotNull DBPTransactionIsolation level)
    {

    }

    @Override
    public void handleTransactionCommit(@NotNull DBCExecutionContext context)
    {

    }

    @Override
    public void handleTransactionSavepoint(@NotNull DBCSavepoint savepoint)
    {

    }

    @Override
    public void handleTransactionRollback(@NotNull DBCExecutionContext context, DBCSavepoint savepoint)
    {

    }

    @Override
    public void handleStatementOpen(@NotNull DBCStatement statement)
    {

    }

    @Override
    public void handleStatementExecuteBegin(@NotNull DBCStatement statement)
    {

    }

    @Override
    public void handleStatementExecuteEnd(@NotNull DBCStatement statement, long rows, Throwable error)
    {
        
    }

    @Override
    public void handleStatementBind(@NotNull DBCStatement statement, Object column, Object value)
    {

    }

    @Override
    public void handleStatementClose(@NotNull DBCStatement statement, long rows)
    {

    }

    @Override
    public void handleResultSetOpen(@NotNull DBCResultSet resultSet)
    {

    }

    @Override
    public void handleResultSetClose(@NotNull DBCResultSet resultSet, long rowCount)
    {

    }

    @Override
    public void handleScriptBegin(@NotNull DBCSession session)
    {

    }

    @Override
    public void handleScriptEnd(@NotNull DBCSession session)
    {

    }

    @Override
    public void handleFeatureUsage(@NotNull DBRFeature feature, @Nullable Map<String, Object> parameters) {

    }
}
