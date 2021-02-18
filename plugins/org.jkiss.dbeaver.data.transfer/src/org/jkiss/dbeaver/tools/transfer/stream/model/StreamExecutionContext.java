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
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.impl.AbstractExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSInstance;

/**
 * Data container transfer producer
 */
public class StreamExecutionContext extends AbstractExecutionContext<StreamDataSource> {

    StreamExecutionContext(@NotNull StreamDataSource dataSource, String purpose) {
        super(dataSource, purpose);
    }

    @Override
    public DBSInstance getOwnerInstance() {
        return dataSource;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @NotNull
    @Override
    public StreamTransferSession openSession(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionPurpose purpose, @NotNull String task) {
        return new StreamTransferSession(monitor, this, purpose, task);
    }

    @Override
    public void checkContextAlive(DBRProgressMonitor monitor) throws DBException {

    }

    @NotNull
    @Override
    public InvalidateResult invalidateContext(@NotNull DBRProgressMonitor monitor, boolean closeOnFailure) throws DBException {
        return InvalidateResult.ALIVE;
    }

    @Override
    public void close() {

    }
}
