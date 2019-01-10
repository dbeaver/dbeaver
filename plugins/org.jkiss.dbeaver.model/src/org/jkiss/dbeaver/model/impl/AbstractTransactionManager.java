/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

    @Override
    public boolean isEnabled() {
        return false;
    }
}
