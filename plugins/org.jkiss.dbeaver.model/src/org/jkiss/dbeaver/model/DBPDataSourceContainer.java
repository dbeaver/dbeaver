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
package org.jkiss.dbeaver.model;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPClientHome;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.net.DBWNetworkHandler;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.virtual.DBVModel;

import java.util.Collection;
import java.util.Date;

/**
 * DBPDataSourceContainer
 */
public interface DBPDataSourceContainer extends DBSObject, DBDPreferences, DBPNamedObject2
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
    DBPPlatform getPlatform();

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

    boolean isProvided();

    boolean isTemporary();

    boolean isShowSystemObjects();

    boolean isShowUtilityObjects();

    boolean isConnectionReadOnly();

    boolean isDefaultAutoCommit();

    void setDefaultAutoCommit(boolean autoCommit, @Nullable  DBCExecutionContext updateContext, boolean updateConnection, @Nullable  Runnable onFinish)
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

    DBWNetworkHandler[] getActiveNetworkHandlers();

    /**
     * Checks this data source is connected.
     * Do not check whether underlying connection is alive or not.
     */
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
     * @param initialize initialize datasource after connect (call DBPDataSource.initialize)
     * @param reflect notify UI about connection state change
     * @throws DBException on error
     */
    boolean connect(DBRProgressMonitor monitor, boolean initialize, boolean reflect) throws DBException;

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

    @Nullable
    DBPDataSource getDataSource();

    @Nullable
    DBPDataSourceFolder getFolder();

    void setFolder(@Nullable DBPDataSourceFolder folder);

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

    @NotNull
    ISecurePreferences getSecurePreferences();

    Date getConnectTime();
}
