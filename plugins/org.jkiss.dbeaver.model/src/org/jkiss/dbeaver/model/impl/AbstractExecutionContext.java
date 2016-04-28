/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
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

    @NotNull
    protected final DATASOURCE dataSource;
    protected final String purpose;

    public AbstractExecutionContext(@NotNull DATASOURCE dataSource, String purpose) {
        this.dataSource = dataSource;
        this.purpose = purpose;
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


    /**
     * Context boot procedure.
     * Executes bootstrap queries and other init functions.
     * This function must be called by all implementations.
     */
    protected void initContextBootstrap(@NotNull DBRProgressMonitor monitor, boolean autoCommit) throws DBCException
    {
        // Notify QM
        QMUtils.getDefaultHandler().handleContextOpen(this, !autoCommit);

        // Execute bootstrap queries
        DBPConnectionBootstrap bootstrap = dataSource.getContainer().getConnectionConfiguration().getBootstrap();
        List<String> initQueries = bootstrap.getInitQueries();
        if (!CommonUtils.isEmpty(initQueries)) {
            try (DBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Run bootstrap queries")) {
                for (String query : initQueries) {
                    try {
                        try (DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query, false, false, false))
                        {
                            dbStat.executeStatement();
                        }
                    } catch (Exception e) {
                        String message = "Error executing bootstrap query: " + query;
                        if (bootstrap.isIgnoreErrors()) {
                            log.warn(message);
                        } else {
                            throw new DBCException(message, e, dataSource);
                        }
                    }
                }
            }
        }
    }

    protected void closeContext()
    {
        QMUtils.getDefaultHandler().handleContextClose(this);
    }

    @Override
    public String toString() {
        String dsName = dataSource instanceof DBPNamedObject ? dataSource.getName() : dataSource.toString();
        return dsName + " - " + purpose;
    }

}
