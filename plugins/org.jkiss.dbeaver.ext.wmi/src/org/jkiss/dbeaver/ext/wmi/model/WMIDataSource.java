/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.impl.AbstractExecutionContext;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.qm.QMUtils;
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
    private final DBPDataSourceContainer container;
    private WMINamespace rootNamespace;
    private final SQLDialect dialect;
    private final long id;

    public WMIDataSource(DBPDataSourceContainer container)
        throws DBException
    {
        this.container = container;
        this.dialect = new WMIDialect();
        this.id = AbstractExecutionContext.generateContextId();

        QMUtils.getDefaultHandler().handleContextOpen(this, false);
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
    public DBCExecutionContext[] getAllContexts() {
        return new DBCExecutionContext[] { this };
    }

    @Override
    public long getContextId() {
        return this.id;
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

    @Override
    public void checkContextAlive(DBRProgressMonitor monitor) throws DBException {
        // do nothing
    }

    @NotNull
    @Override
    public DBCExecutionContext openIsolatedContext(@NotNull DBRProgressMonitor monitor, @NotNull String purpose) throws DBException
    {
        return this;
    }

    @NotNull
    @Override
    public InvalidateResult invalidateContext(@NotNull DBRProgressMonitor monitor, boolean closeOnFailure) throws DBException
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
    public void close() {
        if (rootNamespace != null) {
            rootNamespace.close();
            if (rootNamespace.service != null) {
                rootNamespace.service.close();
            }
            rootNamespace = null;
        }

        QMUtils.getDefaultHandler().handleContextClose(this);
    }

    @Override
    public void shutdown(DBRProgressMonitor monitor)
    {
        this.close();
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
    public <T> T getAdapter(Class<T> adapter)
    {
        if (adapter == DBSObjectContainer.class) {
            return adapter.cast(rootNamespace);
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
