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
package org.jkiss.dbeaver.ui.resources.bookmarks;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

/**
 * DBNBookmark
 */
public class DBNBookmark extends DBNResource
{
    private BookmarkStorage storage;

    public DBNBookmark(DBNNode parentNode, IResource resource, DBPResourceHandler handler) throws DBException, CoreException
    {
        super(parentNode, resource, handler);
        storage = new BookmarkStorage((IFile)resource, true);
    }

    @Override
    protected void dispose(boolean reflect)
    {
        storage.dispose();
        super.dispose(reflect);
    }

    @Override
    public String getNodeName()
    {
        return storage.getTitle();
    }

    @Override
    public String getNodeDescription()
    {
        String dsInfo = "";
        Collection<DBPDataSourceContainer> dataSources = getAssociatedDataSources();
        if (!CommonUtils.isEmpty(dataSources)) {
            DBPDataSourceContainer dataSource = dataSources.iterator().next();
            dsInfo = " ('" + dataSource.getName() + "' - " + dataSource.getDriver().getName() + ")";
        }
        return storage.getDescription() + dsInfo;
    }

    @Override
    public DBPImage getNodeIcon()
    {
        return storage.getImage();
    }

    @Override
    public void rename(DBRProgressMonitor monitor, String newName) throws DBException
    {
        IFile file = (IFile) getResource();
        if (file != null) {
            try {
                storage.setTitle(newName);
                InputStream data = storage.serialize();
                file.setContents(data, true, false, RuntimeUtils.getNestedMonitor(monitor));
            } catch (Exception e) {
                throw new DBException("Can't rename bookmark", e);
            }
        }
    }

    @Override
    public Collection<DBPDataSourceContainer> getAssociatedDataSources()
    {
        IResource resource = getResource();
        if (resource != null) {
            DataSourceRegistry dataSourceRegistry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(resource.getProject());
            if (dataSourceRegistry != null) {
                DBPDataSourceContainer dataSource = dataSourceRegistry.getDataSource(storage.getDataSourceId());
                if (dataSource != null) {
                    return Collections.singleton(dataSource);
                }
            }
        }
        return null;
    }

}
