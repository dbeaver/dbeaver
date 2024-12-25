/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverSubstitutionDescriptor;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.model.net.DBWNetworkHandler;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.secret.DBPSecretHolder;
import org.jkiss.dbeaver.model.secret.DBSSecretValue;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadata;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.virtual.DBVModel;
import org.jkiss.dbeaver.runtime.IVariableResolver;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * DBPDataSourceContainer
 */
public interface DBPDataSourceContainer extends
    DBSObject, DBDFormatSettings, DBPNamedObject2, DBPDataSourcePermissionOwner, DBPSecretHolder
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
    DBPDataSourceConfigurationStorage getConfigurationStorage();

    @NotNull
    DBPDataSourceOrigin getOrigin();

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

    @NotNull
    DBNBrowseSettings getNavigatorSettings();

    boolean isProvided();

    /*
     * Returns true if datasource can be managed (edited or deleted).
     * Datasource is manageable if it belongs to its owner registry.
     */
    boolean isManageable();

    boolean isAccessCheckRequired();

    /**
     * @return true if datasource is provided by some dynamic DS provider. E.g. cloud configuration.
     */
    boolean isExternallyProvided();

    boolean isTemplate();

    boolean isTemporary();

    // We do not implement DBPHiddenObject because it is not really hidden.
    // This flag means that datasource shouldn't be included in the primary connection list.
    // Also hidden connections are excluded from persistence
    boolean isHidden();

    boolean isSharedCredentials();

    /**
     * find all available shared credentials for the current user
     */
    List<DBSSecretValue> listSharedCredentials() throws DBException;
    void setSharedCredentials(boolean sharedCredentials);
    boolean isSharedCredentialsSelected();
    void setSelectedSharedCredentials(@NotNull DBSSecretValue secretValue);

    boolean isConnectionReadOnly();

    /**
     * Flag saying that password value was saved in configuration.
     * It is a legacy flag, to determine that credentials are really saved use isCredentialsSaved.
     */
    boolean isSavePassword();

    void setSavePassword(boolean savePassword);

    /**
     * Determines that credentials for this datasource are saved
     */
    boolean isCredentialsSaved() throws DBException;

    void setDescription(String description);

    boolean isDefaultAutoCommit();

    void setDefaultAutoCommit(boolean autoCommit);

    @Nullable
    DBPTransactionIsolation getActiveTransactionsIsolation();

    @Nullable
    Integer getDefaultTransactionsIsolation();

    void setDefaultTransactionsIsolation(DBPTransactionIsolation isolationLevel);

    boolean isExtraMetadataReadEnabled();

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

    @Nullable
    String getClientApplicationName();

    void setClientApplicationName(@NotNull String applicationName);

    DBVModel getVirtualModel();

    DBPNativeClientLocation getClientHome();

    @NotNull
    DBWNetworkHandler[] getActiveNetworkHandlers();

    /**
     * Checks this data source is connected.
     * Do not check whether underlying connection is alive or not.
     */
    boolean isConnected();

    /**
     * Checks that this data source is in the connecting process
     */
    boolean isConnecting();

    /**
     * Returns last connection instantiation error if any
     */
    @Nullable
    String getConnectionError();

    /**
     * Connects to datasource.
     * This is sync method and returns after actual connection establishment.
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

    Collection<DBPDataSourceTask> getTasks();

    void acquire(DBPDataSourceTask user);

    void release(DBPDataSourceTask user);

    void fireEvent(DBPEvent event);

    /**
     * Preference store associated with this datasource
     * @return preference store
     */
    @NotNull
    DBPPreferenceStore getPreferenceStore();

    @NotNull
    DBPDataSourceRegistry getRegistry();

    @NotNull
    DBPProject getProject();

    /**
     * @return false on any error. Actual error can be read in registry.
     */
    boolean persistConfiguration();

    Date getConnectTime();

    @NotNull
    SQLDialectMetadata getScriptDialect();

    /**
     * reset all secured properties
     */
    void resetPassword();

    /**
     * Marks all secrets (credentials) as unresolved
     */
    void resetAllSecrets();

    /**
     * Make variable resolver for datasource properties.
     *
     * @param actualConfig if true then actual connection config will be used (e.g. with preprocessed host/port values).
     */
    IVariableResolver getVariablesResolver(boolean actualConfig);

    DBPDataSourceContainer createCopy(DBPDataSourceRegistry forRegistry);

    DBPExclusiveResource getExclusiveLock();
    
    boolean isForceUseSingleConnection();
    
    void setForceUseSingleConnection(boolean value);

    /**
     * Returns the type of required external authorization.
     * Null - if additional authorization is not required
     */
    @Nullable
    String getRequiredExternalAuth();

    @Nullable
    DBPDriverSubstitutionDescriptor getDriverSubstitution();

    void setDriverSubstitution(@Nullable DBPDriverSubstitutionDescriptor driverSubstitution);

    /**
     * Datasource tags. Tags can be used in various 3rd party integrations.
     */
    Map<String, String> getTags();

    String getTagValue(String tagName);

    void setTagValue(String tagName, String tagValue);

    /**
     * Extension settings. Any custom attributes assigned by product plugins for internal configuration purposes
     */
    @Nullable
    String getExtension(@NotNull String name);

    void setExtension(@NotNull String name, @Nullable String value);

    void dispose();

}
