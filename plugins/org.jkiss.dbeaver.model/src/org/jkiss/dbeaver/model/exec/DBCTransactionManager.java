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

package org.jkiss.dbeaver.model.exec;

import org.jkiss.code.NotNull;
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

    void setTransactionIsolation(@NotNull DBRProgressMonitor monitor, @NotNull DBPTransactionIsolation transactionIsolation) throws DBCException;

    boolean isAutoCommit() throws DBCException;

    void setAutoCommit(@NotNull DBRProgressMonitor monitor, boolean autoCommit) throws DBCException;

    boolean supportsSavepoints();

    DBCSavepoint setSavepoint(@NotNull DBRProgressMonitor monitor, String name)
        throws DBCException;

    void releaseSavepoint(@NotNull DBRProgressMonitor monitor, @NotNull DBCSavepoint savepoint) throws DBCException;

    void commit(@NotNull DBCSession session) throws DBCException;

    void rollback(@NotNull DBCSession session, @Nullable DBCSavepoint savepoint) throws DBCException;

    boolean isEnabled();
}
