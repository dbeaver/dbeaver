/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPAutoCloser;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;

import java.io.Closeable;

/**
 * Manageable result set
 */
public abstract class AbstractStatement<SESSION extends DBCSession> implements DBCStatement, DBPAutoCloser {

    private static final Log log = Log.getLog(AbstractStatement.class);

    protected final SESSION connection;
    private DBCExecutionSource statementSource;
    private Closeable closeFinalizer;

    public AbstractStatement(SESSION session) {
        this.connection = session;
    }

    @Override
    public SESSION getSession() {
        return connection;
    }

    @Nullable
    @Override
    public DBCExecutionSource getStatementSource() {
        return statementSource;
    }

    @Override
    public void setStatementSource(@Nullable DBCExecutionSource source) {
        this.statementSource = source;
    }

    @Override
    public void close() {
        if (this.closeFinalizer != null) {
            try {
                this.closeFinalizer.close();
            } catch (Throwable e) {
                log.error("Error during statement '" + this + "' close", e);
            }
        }
    }

    @Override
    public void autoCloseDependant(@NotNull Closeable dependent) {
        this.closeFinalizer = dependent;
    }

}
