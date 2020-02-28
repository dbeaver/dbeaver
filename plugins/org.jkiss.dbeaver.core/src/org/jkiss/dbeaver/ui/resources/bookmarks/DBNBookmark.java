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
package org.jkiss.dbeaver.ui.resources.bookmarks;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * DBNBookmark
 */
public class DBNBookmark extends DBNResource
{
    private BookmarkStorage storage;

    DBNBookmark(DBNNode parentNode, IResource resource, DBPResourceHandler handler) throws DBException, CoreException
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

    public BookmarkStorage getStorage() {
        return storage;
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
    public String getNodeTargetName() {
        List<String> dsPath = storage.getDataSourcePath();
        return CommonUtils.isEmpty(dsPath) ? super.getNodeName() : dsPath.get(dsPath.size() - 1);
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

}
