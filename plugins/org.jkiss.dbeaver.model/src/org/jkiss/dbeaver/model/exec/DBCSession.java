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
package org.jkiss.dbeaver.model.exec;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Execution session
 */
public interface DBCSession extends DBPCloseableObject, DBDPreferences {

    /**
     * Session title
     * @return title
     */
    @NotNull
    String getTaskTitle();

    /**
     * Data source of this session
     * @return data source
     */
    @NotNull
    DBCExecutionContext getExecutionContext();

    /**
     * Data source of this session
     * @return data source
     */
    @NotNull
    DBPDataSource getDataSource();

    /**
     * Performs check that this context is really connected to remote database
     * @return connected state
     */
    boolean isConnected();

    /**
     * Context's progress monitor.
     * Each context has it's progress monitor which is passed at context creation time and never changes.
     * @return progress monitor
     */
    @NotNull
    DBRProgressMonitor getProgressMonitor();

    /**
     * Context's purpose
     * @return purpose
     */
    @NotNull
    DBCExecutionPurpose getPurpose();

    /**
     * Prepares statements
     */
    @NotNull
    DBCStatement prepareStatement(
        @NotNull DBCStatementType type,
        @NotNull String query,
        boolean scrollable,
        boolean updatable,
        boolean returnGeneratedKeys) throws DBCException;

    boolean isLoggingEnabled();
    /**
     * Enables/disables operations logging within this session
     * @param enable enable
     */
    void enableLogging(boolean enable);
}
