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

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;

import java.util.Collection;
import java.util.Map;

/**
 * DBPDriver
 */
public interface DBPDriver extends DBPObject
{
    DBPDataSourceProvider getDataSourceProvider()
        throws DBException;

    DBPClientManager getClientManager();

    String getId();

    String getName();

    String getFullName();

    String getDescription();

    String getNote();

    DBPImage getIcon();

    String getDriverClassName();

    String getDefaultPort();

    String getSampleURL();

    String getWebURL();

    boolean isClientRequired();

    boolean supportsDriverProperties();

    boolean isEmbedded();
    boolean isAnonymousAccess();

    boolean isCustomDriverLoader();

    DBXTreeNode getNavigatorRoot();

    Collection<DBPPropertyDescriptor> getConnectionPropertyDescriptors();

    Map<Object, Object> getDefaultConnectionProperties();

    Map<Object, Object> getConnectionProperties();

    Map<Object, Object> getDriverParameters();

    Object getDriverParameter(String name);

    Collection<? extends DBPDriverLocalPath> getPathList();

    boolean isSupportedByLocalSystem();

    Collection<String> getClientHomeIds();

    DBPClientHome getClientHome(String homeId);

    ClassLoader getClassLoader();

    Collection<? extends DBPDriverFile> getFiles();

    Object getDriverInstance(DBRRunnableContext runnableContext) throws DBException;

    void validateFilesPresence(DBRRunnableContext runnableContext);

    void loadDriver(DBRRunnableContext runnableContext) throws DBException;

}
