/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.connection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadata;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * DBPDriver
 */
public interface DBPDriver extends DBPNamedObject
{
    /**
     * Driver contributor
     */
    @NotNull
    DBPDataSourceProvider getDataSourceProvider();

    @NotNull
    DBPDataSourceProviderDescriptor getProviderDescriptor();

    @NotNull
    String getId();

    @NotNull
    String getProviderId();

    @Deprecated
    @Nullable
    String getCategory();

    @NotNull
    List<String> getCategories();

    @NotNull
    String getFullName();

    @Nullable
    String getDescription();

    @NotNull
    DBPImage getIcon();

    @NotNull
    DBPImage getPlainIcon();

    @NotNull
    DBPImage getIconBig();

    @Nullable
    String getDriverClassName();

    @Nullable
    String getDefaultPort();

    @Nullable
    String getDefaultDatabase();

    @Nullable
    String getDefaultServer();

    @Nullable
    String getDefaultUser();

    @Nullable
    String getSampleURL();

    @Nullable
    String getWebURL();

    @Nullable
    String getPropertiesWebURL();

    @NotNull
    SQLDialectMetadata getScriptDialect();

    boolean isClientRequired();

    boolean supportsDriverProperties();

    boolean isEmbedded();
    boolean isAnonymousAccess();
    boolean isAllowsEmptyPassword();
    boolean isLicenseRequired();
    boolean isCustomDriverLoader();
    boolean isUseURL();
    // Can be created
    boolean isInstantiable();
    // Driver shipped along with JDK/DBeaver, doesn't need any additional libraries. Basically it is ODBC driver.
    boolean isInternalDriver();
    // Custom driver: created by user
    boolean isCustom();
    // Temporary driver: used for automatically created drivers when connection  configuration is broken
    boolean isTemporary();

    boolean isDisabled();
    DBPDriver getReplacedBy();

    int getPromotedScore();

    @Nullable
    DBXTreeNode getNavigatorRoot();

    @NotNull
    Collection<DBPPropertyDescriptor> getConnectionPropertyDescriptors();

    @NotNull
    Map<String, Object> getDefaultConnectionProperties();

    @NotNull
    Map<String, Object> getConnectionProperties();

    @NotNull
    Map<String, Object> getDriverParameters();

    @Nullable
    Object getDriverParameter(String name);

    boolean isSupportedByLocalSystem();

    String getLicense();

    /**
     * Client manager or null
     */
    @Nullable
    DBPNativeClientLocationManager getNativeClientManager();

    @NotNull
    List<DBPNativeClientLocation> getNativeClientLocations();

    @Nullable
    ClassLoader getClassLoader();

    @NotNull
    List<? extends DBPDriverLibrary> getDriverLibraries();

    List<? extends DBPDriverFileSource> getDriverFileSources();

    @NotNull
    <T> T getDriverInstance(@NotNull DBRProgressMonitor monitor) throws DBException;

    void loadDriver(DBRProgressMonitor monitor) throws DBException;

}
