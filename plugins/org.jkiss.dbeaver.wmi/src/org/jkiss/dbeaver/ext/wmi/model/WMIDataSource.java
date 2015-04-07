/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.wmi.model;

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.wmi.service.WMIService;

import java.util.Collection;
import java.util.Collections;

/**
 * WMIDataSource
 */
public class WMIDataSource implements DBPDataSource, SQLDataSource, IAdaptable//, DBSObjectContainer, DBSObjectSelector
{
    private DBSDataSourceContainer container;
    private WMINamespace rootNamespace;
    private SQLDialect dialect;

    public WMIDataSource(DBSDataSourceContainer container)
        throws DBException
    {
        this.container = container;
        this.dialect = new WMIDialect();
    }

    @NotNull
    @Override
    public DBSDataSourceContainer getContainer()
    {
        return container;
    }

    @NotNull
    @Override
    public DBPDataSourceInfo getInfo()
    {
        return new WMIDataSourceInfo();
    }

    @Override
    public String getContextName() {
        return "WMI Data Source";
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

    @Override
    public DBCSession openSession(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String task)
    {
        return new WMISession(monitor, purpose, task, this);
    }

    @NotNull
    @Override
    public DBCExecutionContext openIsolatedContext(@NotNull DBRProgressMonitor monitor, @NotNull String purpose) throws DBException
    {
        return this;
    }

    @Override
    public InvalidateResult invalidateContext(DBRProgressMonitor monitor) throws DBException
    {
        throw new DBException("Connection invalidate not supported");
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        final DBPConnectionInfo connectionInfo = container.getActualConnectionInfo();
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
}
