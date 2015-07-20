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
package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.virtual.DBVModel;

import java.util.Collection;

/**
 * DBSDataSourceContainer
 */
public interface DBSDataSourceContainer extends DBSObject, DBDPreferences, DBPNamedObject2
{
    /**
     * Container unique ID
     * @return id
     */
    @NotNull
    String getId();

    /**
     * Associated driver
     * @return driver descriptor reference
     */
    @NotNull
    DBPDriver getDriver();

    @NotNull
    DBPApplication getApplication();

    /**
     * Connection configuration.
     * @return connection details
     */
    @NotNull
    DBPConnectionConfiguration getConnectionConfiguration();

    /**
     * Actual connection configuration. Contains actual parameters used to connect to this datasource.
     * Differs from getConnectionConfiguration() in case if tunnel or proxy was used.
     * @return actual connection configuration.
     */
    @NotNull
    DBPConnectionConfiguration getActualConnectionConfiguration();

    boolean isShowSystemObjects();

    boolean isConnectionReadOnly();

    boolean isDefaultAutoCommit();

    void setDefaultAutoCommit(boolean autoCommit, DBCExecutionContext updateContext, boolean updateConnection)
        throws DBException;

    @Nullable
    DBPTransactionIsolation getActiveTransactionsIsolation();

    @Nullable
    Integer getDefaultTransactionsIsolation();

    void setDefaultTransactionsIsolation(DBPTransactionIsolation isolationLevel)
        throws DBException;

    /**
     * Search for object filter which corresponds specified object type and parent object.
     * Search filter which match any super class or interface implemented by specified type.
     * @param type object type
     * @param parentObject parent object (in DBS objects hierarchy)
     * @return object filter or null if not filter was set for specified type
     */
    @Nullable
    DBSObjectFilter getObjectFilter(Class<?> type, @Nullable DBSObject parentObject, boolean firstMatch);

    void setObjectFilter(Class<?> type, DBSObject parentObject, DBSObjectFilter filter);

    DBVModel getVirtualModel();

    DBPClientHome getClientHome();

    boolean isConnected();

    /**
     * Initiate connection job.
     */
    void initConnection(DBRProgressMonitor monitor, DBRProgressListener onFinish);

    /**
     * Connects to datasource.
     * This is async method and returns immediately.
     * Connection will be opened in separate job, so no progress monitor is required.
     * @param monitor progress monitor
     * @throws DBException on error
     */
    boolean connect(DBRProgressMonitor monitor) throws DBException;

    /**
     * Disconnects from datasource.
     * This is async method and returns immediately.
     * Connection will be closed in separate job, so no progress monitor is required.
     * @param monitor progress monitor
     * @throws DBException on error
     * @return true on disconnect, false if disconnect action was canceled
     */
    boolean disconnect(DBRProgressMonitor monitor) throws DBException;

    /**
     * Reconnects datasource.
     * @param monitor progress monitor
     * @return true on reconnect, false if reconnect action was canceled
     * @throws org.jkiss.dbeaver.DBException on any DB error
     */
    boolean reconnect(DBRProgressMonitor monitor) throws DBException;

    String getFolderPath();

    void setFolderPath(String folderPath);

    Collection<DBPDataSourceUser> getUsers();

    void acquire(DBPDataSourceUser user);

    void release(DBPDataSourceUser user);

    void fireEvent(DBPEvent event);

    /**
     * Preference store associated with this datasource
     * @return preference store
     */
    @NotNull
    DBPPreferenceStore getPreferenceStore();

    @NotNull
    DBPDataSourceRegistry getRegistry();

    void persistConfiguration();

}
