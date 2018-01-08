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

package org.jkiss.dbeaver.model.connection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;
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

    /**
     * Client manager or null
     */
    @Nullable
    DBPClientManager getClientManager();

    @NotNull
    String getId();

    @NotNull
    String getProviderId();

    @NotNull
    String getFullName();

    @Nullable
    String getDescription();

    @NotNull
    DBPImage getIcon();

    @Nullable
    String getDriverClassName();

    @Nullable
    String getDefaultPort();

    @Nullable
    String getSampleURL();

    @Nullable
    String getWebURL();

    boolean isClientRequired();

    boolean supportsDriverProperties();

    boolean isEmbedded();
    boolean isAnonymousAccess();
    boolean isCustomDriverLoader();
    boolean isInternalDriver();

    @Nullable
    DBXTreeNode getNavigatorRoot();

    @NotNull
    Collection<DBPPropertyDescriptor> getConnectionPropertyDescriptors();

    @NotNull
    Map<Object, Object> getDefaultConnectionProperties();

    @NotNull
    Map<Object, Object> getConnectionProperties();

    @NotNull
    Map<Object, Object> getDriverParameters();

    @Nullable
    Object getDriverParameter(String name);

    boolean isSupportedByLocalSystem();

    @NotNull
    Collection<String> getClientHomeIds();

    @Nullable
    DBPClientHome getClientHome(String homeId);

    @Nullable
    ClassLoader getClassLoader();

    @NotNull
    Collection<? extends DBPDriverLibrary> getDriverLibraries();

    @NotNull
    Object getDriverInstance(@NotNull DBRProgressMonitor monitor) throws DBException;

    void loadDriver(DBRProgressMonitor monitor) throws DBException;

}
