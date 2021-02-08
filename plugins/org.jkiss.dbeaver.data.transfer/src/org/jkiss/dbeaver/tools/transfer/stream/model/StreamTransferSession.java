/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.tools.transfer.stream.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Stream session
 */
public class StreamTransferSession extends AbstractSession {

    private final StreamExecutionContext executionContext;

    StreamTransferSession(DBRProgressMonitor monitor, StreamExecutionContext executionContext, DBCExecutionPurpose purpose, String taskTitle) {
        super(monitor, purpose, taskTitle);
        this.executionContext = executionContext;
    }

    @NotNull
    @Override
    public DBCExecutionContext getExecutionContext() {
        return executionContext;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return executionContext.getDataSource();
    }

    @NotNull
    @Override
    public DBCStatement prepareStatement(@NotNull DBCStatementType type, @NotNull String query, boolean scrollable, boolean updatable, boolean returnGeneratedKeys) throws DBCException {
        throw new DBCFeatureNotSupportedException();
    }

    @Override
    public void cancelBlock(@NotNull DBRProgressMonitor monitor, Thread blockThread) throws DBException {
        // do nothing
    }
}
