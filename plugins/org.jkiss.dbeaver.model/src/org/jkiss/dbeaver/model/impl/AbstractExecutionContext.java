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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * Abstract execution context.
 * All regular DBCExecutionContext implementations should extend this class.
 * It provides bootstrap init functions and QM notifications.
 */
public abstract class AbstractExecutionContext<DATASOURCE extends DBPDataSource> implements DBCExecutionContext
{
    private static final Log log = Log.getLog(AbstractExecutionContext.class);

    private static long idSequence = 0;

    @NotNull
    protected final DATASOURCE dataSource;
    protected final String purpose;
    protected final long id;

    public AbstractExecutionContext(@NotNull DATASOURCE dataSource, String purpose) {
        this.dataSource = dataSource;
        this.purpose = purpose;
        this.id = generateContextId();

        log.debug("Execution context opened (" + dataSource.getName() + "; " + purpose + "; " + this.id +  ")");
    }

    public static synchronized long generateContextId() {
        return idSequence++;
    }

    @Override
    public long getContextId() {
        return this.id;
    }

    @NotNull
    @Override
    public String getContextName() {
        return purpose;
    }

    @NotNull
    @Override
    public DATASOURCE getDataSource() {
        return dataSource;
    }

    @Nullable
    @Override
    public DBCExecutionContextDefaults getContextDefaults() {
        return null;
    }

    @NotNull
    protected DBPConnectionBootstrap getBootstrapSettings() {
        return getDataSource().getContainer().getActualConnectionConfiguration().getBootstrap();
    }

    /**
     * Context boot procedure.
     * Executes bootstrap queries and other init functions.
     * This function must be called by all implementations.
     */
    protected boolean initContextBootstrap(@NotNull DBRProgressMonitor monitor, boolean autoCommit) throws DBCException
    {
        // Notify QM
        QMUtils.getDefaultHandler().handleContextOpen(this, !autoCommit);

        // Execute bootstrap queries
        DBPConnectionBootstrap bootstrap = getBootstrapSettings();
        List<String> initQueries = bootstrap.getInitQueries();
        if (!CommonUtils.isEmpty(initQueries)) {
            monitor.subTask("Run bootstrap queries");
            try (DBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Run bootstrap queries")) {
                for (String query : initQueries) {
                    // Replace variables
                    query = GeneralUtils.replaceVariables(query, getDataSource().getContainer().getVariablesResolver(true));
                    try {
                        try (DBCStatement dbStat = session.prepareStatement(DBCStatementType.SCRIPT, query, false, false, false)) {
                            dbStat.executeStatement();
                        }
                    } catch (Exception e) {
                        String message = "Error executing bootstrap query: " + query;
                        if (bootstrap.isIgnoreErrors()) {
                            log.warn(message);
                        } else {
                            throw new DBCException(message, e, this);
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    protected void closeContext()
    {
        QMUtils.getDefaultHandler().handleContextClose(this);

        log.debug("Execution context closed (" + dataSource.getName() + ", " + this.id +  ")");
    }

    @Override
    public String toString() {
        return dataSource.getName() + " - " + purpose;
    }

}
