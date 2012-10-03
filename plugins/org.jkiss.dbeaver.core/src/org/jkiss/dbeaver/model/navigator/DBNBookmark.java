/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.impl.project.BookmarkStorage;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;

import java.io.InputStream;

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
        DataSourceRegistry dataSourceRegistry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(getResource().getProject());
        if (dataSourceRegistry != null) {
            DataSourceDescriptor dataSource = dataSourceRegistry.getDataSource(storage.getDataSourceId());
            if (dataSource != null) {
                dsInfo = " ('" + dataSource.getName() + "' - " + dataSource.getDriver().getName() + ")";
            }
        }
        return storage.getDescription() + dsInfo;
    }

    @Override
    public Image getNodeIcon()
    {
        return storage.getImage();
    }

    @Override
    public void rename(DBRProgressMonitor monitor, String newName) throws DBException
    {
        try {
            storage.setTitle(newName);
            InputStream data = storage.serialize();
            ((IFile)getResource()).setContents(data, true, false, monitor.getNestedMonitor());
        } catch (Exception e) {
            throw new DBException(e);
        }
    }
}
