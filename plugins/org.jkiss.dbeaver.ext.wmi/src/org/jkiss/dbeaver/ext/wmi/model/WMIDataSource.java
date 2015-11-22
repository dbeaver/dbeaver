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
package org.jkiss.dbeaver.ext.wmi.model;

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.wmi.service.WMIService;

import java.util.Collection;
import java.util.Collections;

/**
 * WMIDataSource
 */
public class WMIDataSource implements DBPDataSource, DBCExecutionContext, SQLDataSource, IAdaptable//, DBSObjectContainer, DBSObjectSelector
{
    private DBPDataSourceContainer container;
    private WMINamespace rootNamespace;
    private SQLDialect dialect;

    public WMIDataSource(DBPDataSourceContainer container)
        throws DBException
    {
        this.container = container;
        this.dialect = new WMIDialect();
    }

    @NotNull
    @Override
    public DBPDataSourceContainer getContainer()
    {
        return container;
    }

    @NotNull
    @Override
    public DBPDataSourceInfo getInfo()
    {
        return new WMIDataSourceInfo();
    }

    @NotNull
    @Override
    public DBCExecutionContext getDefaultContext(boolean meta) {
        return this;
    }

    @NotNull
    @Override
    public Collection<DBCExecutionContext> getAllContexts() {
        return Collections.<DBCExecutionContext>singleton(this);
    }

    @NotNull
    @Override
    public String getContextName() {
        return "WMI Data Source";
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return container;
    }

    @NotNull
    @Override
    public WMIDataSource getDataSource() {
        return this;
    }

    @Override
    public boolean isConnected()
    {
        return true;
    }

    @NotNull
    @Override
    public DBCSession openSession(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionPurpose purpose, @NotNull String task)
    {
        return new WMISession(monitor, purpose, task, this);
    }

    @NotNull
    @Override
    public DBCExecutionContext openIsolatedContext(@NotNull DBRProgressMonitor monitor, @NotNull String purpose) throws DBException
    {
        return this;
    }

    @NotNull
    @Override
    public InvalidateResult invalidateContext(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        throw new DBException("Connection invalidate not supported");
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        final DBPConnectionConfiguration connectionInfo = container.getActualConnectionConfiguration();
        try {
            WMIService service = WMIService.connect(
                connectionInfo.getServerName(),
                connectionInfo.getHostName(),
                connectionInfo.getUserName(),
                connectionInfo.getUserPassword(),
                null,
                connectionInfo.getDatabaseName());
            this.rootNamespace = new WMINamespace(null, this, connectionInfo.getDatabaseName(), service);
        } catch (UnsatisfiedLinkError e) {
            throw new DBException("Can't link with WMI native library", e);
        } catch (Throwable e) {
            throw new DBException("Can't connect to WMI service", e);
        }
    }

    @Override
    public void close()
    {
        if (rootNamespace != null) {
            rootNamespace.close();
            if (rootNamespace.service != null) {
                rootNamespace.service.close();
            }
            rootNamespace = null;
        }
    }

    @Association
    public Collection<WMINamespace> getNamespaces()
    {
        return Collections.singletonList(rootNamespace);
    }

    public WMIService getService()
    {
        return rootNamespace.service;
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == DBSObjectContainer.class) {
            return rootNamespace;
        }
        return null;
    }

    @Override
    public SQLDialect getSQLDialect() {
        return dialect;
    }

    @NotNull
    @Override
    public String getName() {
        return container.getName();
    }

    @Override
    public boolean isPersisted() {
        return true;
    }
}
