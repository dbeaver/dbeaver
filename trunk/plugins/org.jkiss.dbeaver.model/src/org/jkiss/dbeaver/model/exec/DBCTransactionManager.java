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

package org.jkiss.dbeaver.model.exec;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
* Transaction manager.
 * It can be implemented by execution context.
 */
public interface DBCTransactionManager
{
    DBPTransactionIsolation getTransactionIsolation() throws DBCException;

    void setTransactionIsolation(DBRProgressMonitor monitor, DBPTransactionIsolation transactionIsolation) throws DBCException;

    boolean isAutoCommit() throws DBCException;

    void setAutoCommit(DBRProgressMonitor monitor, boolean autoCommit) throws DBCException;

    boolean supportsSavepoints();

    DBCSavepoint setSavepoint(DBRProgressMonitor monitor, String name)
        throws DBCException;

    void releaseSavepoint(DBRProgressMonitor monitor, DBCSavepoint savepoint) throws DBCException;

    void commit(DBCSession session) throws DBCException;

    void rollback(DBCSession session, @Nullable DBCSavepoint savepoint) throws DBCException;

}
