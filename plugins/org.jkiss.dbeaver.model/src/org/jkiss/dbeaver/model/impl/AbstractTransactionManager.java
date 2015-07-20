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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSavepoint;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
* AbstractTransactionManager
*/
public class AbstractTransactionManager implements DBCTransactionManager {

    @Override
    public DBPTransactionIsolation getTransactionIsolation()
        throws DBCException
    {
        return null;
    }

    @Override
    public void setTransactionIsolation(@NotNull DBRProgressMonitor monitor, @NotNull DBPTransactionIsolation transactionIsolation)
        throws DBCException
    {
        throw new DBCException("Transaction isolation change not supported");
    }

    @Override
    public boolean isAutoCommit()
        throws DBCException
    {
        return true;
    }

    @Override
    public void setAutoCommit(@NotNull DBRProgressMonitor monitor, boolean autoCommit)
        throws DBCException
    {
        if (!autoCommit) {
            throw new DBCException("Transactional mode not supported");
        }
    }

    @Override
    public boolean supportsSavepoints()
    {
        return false;
    }

    @Override
    public DBCSavepoint setSavepoint(@NotNull DBRProgressMonitor monitor, String name)
        throws DBCException
    {
        throw new DBCException("Savepoint not supported");
    }

    @Override
    public void releaseSavepoint(@NotNull DBRProgressMonitor monitor, @NotNull DBCSavepoint savepoint)
        throws DBCException
    {
        throw new DBCException("Savepoint not supported");
    }

    @Override
    public void commit(@NotNull DBCSession session)
        throws DBCException
    {
        // do nothing
    }

    @Override
    public void rollback(@NotNull DBCSession session, DBCSavepoint savepoint)
        throws DBCException
    {
        throw new DBCException("Transactions not supported");
    }
}
