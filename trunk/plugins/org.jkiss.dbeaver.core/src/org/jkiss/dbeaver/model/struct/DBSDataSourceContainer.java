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
package org.jkiss.dbeaver.model.struct;

import org.eclipse.jface.preference.IPreferenceStore;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.virtual.DBVModel;

import java.util.Collection;

/**
 * DBSDataSourceContainer
 */
public interface DBSDataSourceContainer extends DBSObject, DBDPreferences, IDataSourceProvider
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

    /**
     * Connection info
     * @return connection details
     */
    @NotNull
    DBPConnectionInfo getConnectionInfo();

    /**
     * Actual connection info. Contains actual parameters used to connect to this datasource.
     * Differs from getConnectionInfo() in case if tunnel or proxy was used.
     * @return actual connection info.
     */
    DBPConnectionInfo getActualConnectionInfo();

    boolean isShowSystemObjects();

    boolean isConnectionReadOnly();

    boolean isDefaultAutoCommit();

    void setDefaultAutoCommit(boolean autoCommit, boolean updateConnection);

    boolean isConnectionAutoCommit();

    @Nullable
    DBPTransactionIsolation getDefaultTransactionsIsolation();

    void setDefaultTransactionsIsolation(DBPTransactionIsolation isolationLevel);

    /**
     * Search for object filter which corresponds specified object type and parent object.
     * Search filter which match any super class or interface implemented by specified type.
     * @param type object type
     * @param parentObject parent object (in DBS objects hierarchy)
     * @return object filter or null if not filter was set for specified type
     */
    @Nullable
    DBSObjectFilter getObjectFilter(Class<?> type, @Nullable DBSObject parentObject);

    DBVModel getVirtualModel();

    DBPClientHome getClientHome();

    boolean isConnected();

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

    Collection<DBPDataSourceUser> getUsers();

    void acquire(DBPDataSourceUser user);

    void release(DBPDataSourceUser user);

    void fireEvent(DBPEvent event);

    /**
     * Preference store associated with this datasource
     * @return preference store
     */
    IPreferenceStore getPreferenceStore();

    @NotNull
    DBPDataSourceRegistry getRegistry();

    void persistConfiguration();

}
