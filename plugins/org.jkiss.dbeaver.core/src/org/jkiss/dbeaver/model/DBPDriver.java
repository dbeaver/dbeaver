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

package org.jkiss.dbeaver.model;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;

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

    Image getIcon();

    String getDriverClassName();

    Object getDriverInstance(IRunnableContext runnableContext) throws DBException;

    String getDefaultPort();

    String getSampleURL();

    String getWebURL();

    boolean isClientRequired();

    boolean supportsDriverProperties();

    boolean isAnonymousAccess();

    boolean isCustomDriverLoader();

    Collection<IPropertyDescriptor> getConnectionPropertyDescriptors();

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

    void validateFilesPresence(IRunnableContext runnableContext);

    void loadDriver(IRunnableContext runnableContext) throws DBException;

}
