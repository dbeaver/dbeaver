/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
