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
package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

/**
 * DBNDataSource
 */
public class DBNDataSource extends DBNDatabaseNode implements IAdaptable
{
    private final DBPDataSourceContainer dataSource;
    private DBXTreeNode treeRoot;

    public DBNDataSource(@NotNull DBNNode parentNode, @NotNull DBPDataSourceContainer dataSource)
    {
        super(parentNode);
        this.dataSource = dataSource;
        this.treeRoot = dataSource.getDriver().getNavigatorRoot();
        registerNode();
    }

    @Override
    public DBNNode getParentNode()
    {
        String folderPath = dataSource.getFolderPath();
        if (!CommonUtils.isEmpty(folderPath)) {
            DBNLocalFolder localFolder = ((DBNProjectDatabases) super.getParentNode()).getLocalFolder(folderPath);
            if (localFolder != null) {
                return localFolder;
            }
        }
        return super.getParentNode();
    }

    @Override
    protected void dispose(boolean reflect)
    {
        unregisterNode(reflect);

        super.dispose(reflect);
    }

    @Override
    public DBPDataSourceContainer getObject()
    {
        return dataSource;
    }

    @Override
    public Object getValueObject()
    {
        return dataSource.getDataSource();
    }

    @Override
    public String getNodeName()
    {
        return dataSource.getName();
    }

    @Override
    public String getNodeDescription()
    {
        return dataSource.getDescription();
    }

    @Override
    public String getNodeFullName()
    {
        return getNodeName();
    }

    @Override
    public boolean isManagable()
    {
        return true;
    }

    @Override
    public DBXTreeNode getMeta()
    {
        return treeRoot;
    }

    @Override
    protected void reloadObject(DBRProgressMonitor monitor, DBSObject object) {

    }

    @Override
    public boolean initializeNode(@Nullable DBRProgressMonitor monitor, DBRProgressListener onFinish)
    {
        if (!dataSource.isConnected()) {
            dataSource.initConnection(monitor, onFinish);
        } else {
            if (onFinish != null) {
                onFinish.onTaskFinished(Status.OK_STATUS);
            }
        }
        return dataSource.isConnected();
    }

    @Override
    public DBPImage getNodeIcon() {
        DBPImage image = super.getNodeIcon();
        boolean hasNetworkHandlers = hasNetworkHandlers();
        if (dataSource.isConnectionReadOnly() || hasNetworkHandlers) {
            if (image instanceof DBIconComposite) {
                ((DBIconComposite) image).setTopRight(hasNetworkHandlers ? DBIcon.OVER_EXTERNAL : null);
                ((DBIconComposite) image).setBottomLeft(dataSource.isConnectionReadOnly() ? DBIcon.OVER_LOCK : null);
            } else {
                image = new DBIconComposite(
                    image,
                    false,
                    null,
                    hasNetworkHandlers ? DBIcon.OVER_EXTERNAL : null,
                    dataSource.isConnectionReadOnly() ? DBIcon.OVER_LOCK : null,
                    null);
            }
        }
        return image;
    }

    public boolean hasNetworkHandlers() {
        for (DBWHandlerConfiguration handler : dataSource.getConnectionConfiguration().getDeclaredHandlers()) {
            if (handler.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getAdapter(Class adapter) {
        if (adapter == DBNDataSource.class) {
            return this;
        } else if (DBPDataSourceContainer.class.isAssignableFrom(adapter)) {
            return dataSource;
        }
        return null;
    }

    @Override
    @NotNull
    public DBPDataSourceContainer getDataSourceContainer()
    {
        return dataSource;
    }

    @Override
    public boolean supportsRename()
    {
        return true;
    }

    @Override
    public void rename(DBRProgressMonitor monitor, String newName)
    {
        dataSource.setName(newName);
        dataSource.persistConfiguration();
        dataSource.fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, dataSource, null));
    }

    @Override
    protected void afterChildRead()
    {
        // Notify datasource listeners about state change.
        // We make this action here because we can't update state in
        // initializeNode if this action caused by readChildren.
        // Because readChildren executes in separate job - this job reused by
        // datasource connect job and it do not updates UI after connect because
        // we need to read datasource children immediately.
        // It breaks loading process. So we refresh datasource state manually
        // right after children nodes read
        dataSource.fireEvent(
            new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, dataSource, true));
    }

    @Override
    public boolean supportsDrop(DBNNode otherNode)
    {
        return otherNode == null || otherNode instanceof DBNDataSource;
    }

    @Override
    public void dropNodes(Collection<DBNNode> nodes) throws DBException
    {
        String folderPath = dataSource.getFolderPath();
        for (DBNNode node : nodes) {
            if (node instanceof DBNDataSource) {
                ((DBNDataSource) node).setFolderPath(folderPath);
            }
        }
        DBNModel.updateConfigAndRefreshDatabases(this);
    }

    public void setFolderPath(String folder)
    {
        dataSource.setFolderPath(folder);
    }

    public DBNNode refreshNode(DBRProgressMonitor monitor, Object source) throws DBException
    {
        DBNNode node = super.refreshNode(monitor, source);
        if (node == this) {
            // Refresh succeeded. Let's fire event
            dataSource.fireEvent(
                new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, dataSource));
        }
        return node;
    }

    @Override
    public String toString() {
        return dataSource.toString();
    }
}
