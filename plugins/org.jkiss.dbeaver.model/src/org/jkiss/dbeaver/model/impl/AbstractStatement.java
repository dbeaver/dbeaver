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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPAutoCloser;
import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.qm.QMUtils;

/**
 * Manageable result set
 */
public abstract class AbstractStatement<SESSION extends DBCSession> implements DBCStatement, DBPAutoCloser {

    private static final Log log = Log.getLog(AbstractStatement.class);

    @NotNull
    protected final SESSION connection;
    @Nullable
    private DBCExecutionSource statementSource;
    @Nullable
    private DBPCloseableObject executeFinalizer;

    public AbstractStatement(@NotNull SESSION session) {
        this.connection = session;
    }

    @Override
    @NotNull
    public SESSION getSession() {
        return connection;
    }

    @Override
    @Nullable
    public DBCExecutionSource getStatementSource() {
        return statementSource;
    }

    @Override
    public void setStatementSource(@Nullable DBCExecutionSource source) {
        this.statementSource = source;
    }

    protected boolean isQMLoggingEnabled() {
        return true;
    }

    @Override
    public void close() throws DBException {
        if (isQMLoggingEnabled()) {
            // Handle close
            long updateRowCount = 0;
            try {
                updateRowCount = getUpdateRowCount();
            } catch (DBCException e) {
                log.debug(e);
            }
            QMUtils.getDefaultHandler().handleStatementClose(this, updateRowCount);
        }

        runCloseDependants();
    }

    // Close dependants will be called AFTER the statement is close
    @Override
    public void autoCloseDependant(@NotNull DBPCloseableObject dependent) {
        if (this.executeFinalizer != null) {
            log.error("Internal error: double set of close finalizer " + dependent);
        }
        this.executeFinalizer = dependent;
    }

    // Forcibly run close dependants
    // May be needed if statement cannot be closed for some reason
    public void runCloseDependants() throws DBException {
        if (this.executeFinalizer != null) {
            this.executeFinalizer.close();
            this.executeFinalizer = null;
        }
    }

}
