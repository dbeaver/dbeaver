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

package org.jkiss.dbeaver.tools.transfer.stream.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverSubstitutionDescriptor;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;
import org.jkiss.dbeaver.model.impl.SimpleExclusiveLock;
import org.jkiss.dbeaver.model.impl.data.DefaultValueHandler;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.model.net.DBWNetworkHandler;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.secret.DBSSecretValue;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadata;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.virtual.DBVModel;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.IVariableResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Data container transfer producer
 */
class StreamDataSourceContainer implements DBPDataSourceContainer {

    private static final Log log = Log.getLog(StreamDataSourceContainer.class);

    private Path inputFile;
    private String name;
    private final DBPExclusiveResource exclusiveLock = new SimpleExclusiveLock();
    private final DBVModel virtualModel;

    StreamDataSourceContainer(String name) {
        this.name = name;
        this.virtualModel = new DBVModel(this);
    }

    @NotNull
    @Override
    public String getId() {
        return inputFile == null ? name : inputFile.getFileName().toString();
    }

    @NotNull
    @Override
    public DBPDriver getDriver() {
        throw new IllegalStateException("Not supported");
    }

    @NotNull
    @Override
    public DBPDataSourceConfigurationStorage getConfigurationStorage() {
        throw new IllegalStateException("Stream datasource doesn't have config storage");
    }

    @NotNull
    @Override
    public DBPDataSourceOrigin getOrigin() {
        throw new IllegalStateException("Stream datasource doesn't have origin");
    }

    @NotNull
    @Override
    public DBPConnectionConfiguration getConnectionConfiguration() {
        return new DBPConnectionConfiguration();
    }

    @NotNull
    @Override
    public DBPConnectionConfiguration getActualConnectionConfiguration() {
        return new DBPConnectionConfiguration();
    }

    @NotNull
    @Override
    public DBNBrowseSettings getNavigatorSettings() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean isProvided() {
        return true;
    }

    @Override
    public boolean isManageable() {
        return false;
    }

    @Override
    public boolean isAccessCheckRequired() {
        return isManageable();
    }

    @Override
    public boolean isExternallyProvided() {
        return false;
    }

    @Override
    public boolean isTemplate() {
        return false;
    }

    @Override
    public boolean isTemporary() {
        return true;
    }

    @Override
    public boolean isConnectionReadOnly() {
        return true;
    }

    @Override
    public boolean isSavePassword() {
        return false;
    }

    @Override
    public void setSavePassword(boolean savePassword) {

    }

    @Override
    public boolean isCredentialsSaved() {
        return false;
    }

    @Override
    public void setDescription(String description) {

    }

    @Override
    public boolean isDefaultAutoCommit() {
        return false;
    }

    @Override
    public void setDefaultAutoCommit(boolean autoCommit) {

    }

    @Nullable
    @Override
    public DBPTransactionIsolation getActiveTransactionsIsolation() {
        return null;
    }

    @Nullable
    @Override
    public Integer getDefaultTransactionsIsolation() {
        return null;
    }

    @Override
    public void setDefaultTransactionsIsolation(DBPTransactionIsolation isolationLevel) {

    }

    @Override
    public boolean isExtraMetadataReadEnabled() {
        return false;
    }

    @Nullable
    @Override
    public DBSObjectFilter getObjectFilter(Class<?> type, @Nullable DBSObject parentObject, boolean firstMatch) {
        return null;
    }

    @Override
    public void setObjectFilter(Class<?> type, DBSObject parentObject, DBSObjectFilter filter) {

    }

    @Nullable
    @Override
    public String getClientApplicationName() {
        return null;
    }

    @Override
    public void setClientApplicationName(@NotNull String applicationName) {
        // noop
    }

    @Override
    public DBVModel getVirtualModel() {
        return virtualModel;
    }

    @Override
    public DBPNativeClientLocation getClientHome() {
        return null;
    }

    @NotNull
    @Override
    public DBWNetworkHandler[] getActiveNetworkHandlers() {
        return new DBWNetworkHandler[0];
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isConnecting() {
        return false;
    }

    @Nullable
    @Override
    public String getConnectionError() {
        return null;
    }

    @Override
    public boolean connect(DBRProgressMonitor monitor, boolean initialize, boolean reflect) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    @Override
    public boolean disconnect(DBRProgressMonitor monitor) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    @Override
    public boolean reconnect(DBRProgressMonitor monitor) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return null;
    }

    @Nullable
    @Override
    public DBPDataSource getDataSource() {
        return null;
    }

    @Nullable
    @Override
    public DBPDataSourceFolder getFolder() {
        return null;
    }

    @Override
    public void setFolder(@Nullable DBPDataSourceFolder folder) {

    }

    @Override
    public Collection<DBPDataSourceTask> getTasks() {
        return null;
    }

    @Override
    public void acquire(DBPDataSourceTask user) {

    }

    @Override
    public void release(DBPDataSourceTask user) {

    }

    @Override
    public void fireEvent(DBPEvent event) {

    }

    @Nullable
    @Override
    public String getExtension(@NotNull String name) {
        return null;
    }

    @Override
    public void setExtension(@NotNull String name, @Nullable String value) {

    }

    @NotNull
    @Override
    public DBPPreferenceStore getPreferenceStore() {
        return DBWorkbench.getPlatform().getPreferenceStore();
    }

    @NotNull
    @Override
    public DBPDataSourceRegistry getRegistry() {
        return null;
    }

    @NotNull
    @Override
    public DBPProject getProject() {
        return null;
    }

    @Override
    public boolean persistConfiguration() {
        return true;
    }

    @Override
    public Date getConnectTime() {
        if (inputFile != null) {
            try {
                return new Date(Files.getLastModifiedTime(inputFile).toMillis());
            } catch (IOException e) {
                log.debug(e);
            }
        }
        return new Date();
    }

    @NotNull
    @Override
    public SQLDialectMetadata getScriptDialect() {
        return DBWorkbench.getPlatform().getSQLDialectRegistry().getDialect(BasicSQLDialect.ID);
    }

    @Override
    public void resetPassword() {

    }

    @Override
    public void resetAllSecrets() {

    }

    @Override
    public IVariableResolver getVariablesResolver(boolean actualConfig) {
        return null;
    }

    @Override
    public DBPDataSourceContainer createCopy(DBPDataSourceRegistry forRegistry) {
        return null;
    }

    @Override
    public DBPExclusiveResource getExclusiveLock() {
        return exclusiveLock;
    }

    @Override
    public boolean hasModifyPermission(DBPDataSourcePermission permission) {
        return false;
    }

    @Override
    public List<DBPDataSourcePermission> getModifyPermission() {
        return null;
    }

    @Override
    public void setModifyPermissions(@Nullable Collection<DBPDataSourcePermission> permissions) {

    }

    @Override
    public void setName(String newName) {

    }

    @NotNull
    @Override
    public String getName() {
        return inputFile == null ? name : inputFile.getFileName().toString();
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBDDataFormatterProfile getDataFormatterProfile() {
        return DBWorkbench.getPlatform().getDataFormatterRegistry().getGlobalProfile();
    }

    @Override
    public boolean isUseNativeDateTimeFormat() {
        return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(ModelPreferences.RESULT_NATIVE_DATETIME_FORMAT);
    }

    @Override
    public boolean isUseNativeNumericFormat() {
        return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(ModelPreferences.RESULT_NATIVE_NUMERIC_FORMAT);
    }

    @Override
    public boolean isUseScientificNumericFormat() {
        return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(ModelPreferences.RESULT_SCIENTIFIC_NUMERIC_FORMAT);
    }

    @NotNull
    @Override
    public DBDValueHandler getDefaultValueHandler() {
        return DefaultValueHandler.INSTANCE;
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    public boolean isSharedCredentials() {
        return false;
    }

    @Override
    public List<DBSSecretValue> listSharedCredentials() throws DBException {
        return List.of();
    }

    @Override
    public void setSharedCredentials(boolean sharedCredentials) {

    }

    @Override
    public boolean isSharedCredentialsSelected() {
        return false;
    }

    @Override
    public void setSelectedSharedCredentials(@NotNull DBSSecretValue secretValue) {

    }

    @Override
    public boolean isForceUseSingleConnection() {
        return false;
    }

    @Override
    public void setForceUseSingleConnection(boolean value) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public void persistSecrets(DBSSecretController secretController) throws DBException {

    }

    @Override
    public void resolveSecrets(DBSSecretController secretController) throws DBException {

    }

    @Nullable
    @Override
    public String getRequiredExternalAuth() {
        return null;
    }

    @Nullable
    @Override
    public DBPDriverSubstitutionDescriptor getDriverSubstitution() {
        return null;
    }

    @Override
    public void setDriverSubstitution(@Nullable DBPDriverSubstitutionDescriptor driverSubstitution) {
        // do nothing
    }

    @Override
    public Map<String, String> getTags() {
        return null;
    }

    @Override
    public String getTagValue(String tagName) {
        return null;
    }

    @Override
    public void setTagValue(String tagName, String tagValue) {

    }

    @Override
    public void dispose() {
        virtualModel.dispose();
    }
}
