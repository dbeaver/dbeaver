/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Execution context
 */
public class WMISession extends AbstractSession {

    private final WMIDataSource dataSource;

    public WMISession(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle, WMIDataSource dataSource)
    {
        super(monitor, purpose, taskTitle);
        this.dataSource = dataSource;
    }

    @NotNull
    @Override
    public DBCExecutionContext getExecutionContext() {
        return dataSource;
    }

    @NotNull
    @Override
    public WMIDataSource getDataSource()
    {
        return dataSource;
    }

    @NotNull
    @Override
    public DBCStatement prepareStatement(@NotNull DBCStatementType type, @NotNull String query, boolean scrollable, boolean updatable, boolean returnGeneratedKeys) throws DBCException
    {
        return new WMIStatement(this, type, query);
    }

    @Override
    public void cancelBlock(@NotNull DBRProgressMonitor monitor, @Nullable Thread blockThread) throws DBException
    {
        // Cancel WMI async call
    }

}
