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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.qm.QMUtils;

/**
 * Abstract execution context.
 * All regular DBCExecutionContext implementations should extend this class.
 * It provides bootstrap init functions and QM notifications.
 */
public abstract class AbstractExecutionContext<DATASOURCE extends DBPDataSource> implements DBCExecutionContext
{
    @NotNull
    protected final DATASOURCE dataSource;
    protected final String purpose;

    public AbstractExecutionContext(DATASOURCE dataSource, String purpose) {
        this.dataSource = dataSource;
        this.purpose = purpose;
    }

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
    protected void initContextBootstrap(boolean autoCommit) throws DBCException
    {
        QMUtils.getDefaultHandler().handleContextOpen(this, !autoCommit);
    }

    protected void closeContext()
    {
        QMUtils.getDefaultHandler().handleContextClose(this);
    }

    @Override
    public String toString() {
        String dsName = dataSource instanceof DBPNamedObject ? ((DBPNamedObject) dataSource).getName() : dataSource.toString();
        return dsName + " - " + purpose;
    }

}
