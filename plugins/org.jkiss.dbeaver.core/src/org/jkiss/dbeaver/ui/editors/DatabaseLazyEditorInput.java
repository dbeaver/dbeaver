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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.jobs.DisconnectJob;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.dialogs.ConnectionLostDialog;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Collections;

/**
 * Lazy input. Use by entity editors which are created during DBeaver startup (from memo by factory).
 */
public class DatabaseLazyEditorInput implements IDatabaseEditorInput
{
    private final DBPDataSourceContainer dataSource;
    private final IProject project;
    private final String nodePath;
    private final String nodeName;
    private final String activePageId;
    private final String activeFolderId;

    public DatabaseLazyEditorInput(DBPDataSourceContainer dataSource, IProject project, String nodePath, String nodeName, String activePageId, String activeFolderId) {
        this.dataSource = dataSource;
        this.project = project;
        this.nodePath = nodePath;
        if (nodeName == null) {
            int divPos = nodePath.lastIndexOf('/');
            nodeName = divPos == -1 ? nodePath : nodePath.substring(divPos + 1);
        }
        this.nodeName = nodeName;
        this.activePageId = activePageId;
        this.activeFolderId = activeFolderId;
    }

    @Override
    public boolean exists()
    {
        return false;
    }

    @Override
    public ImageDescriptor getImageDescriptor()
    {
        return DBeaverIcons.getImageDescriptor(dataSource.getDriver().getIcon());
    }

    @Override
    public String getName()
    {
        return nodeName;
    }

    @Override
    public IPersistableElement getPersistable()
    {
        return null;
    }

    @Override
    public String getToolTipText()
    {
        return nodeName;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter)
    {
        return null;
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return null;
    }

    @Override
    public DBNDatabaseNode getNavigatorNode()
    {
        return null;
    }

    @Override
    public DBSObject getDatabaseObject()
    {
        return dataSource;
    }

    @Override
    public String getDefaultPageId()
    {
        return activePageId;
    }

    @Override
    public String getDefaultFolderId()
    {
        return activeFolderId;
    }

    @Nullable
    @Override
    public DBECommandContext getCommandContext()
    {
        return null;
    }

    @Override
    public Collection<String> getAttributeNames() {
        return Collections.emptyList();
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public Object setAttribute(String name, Object value) {
        return null;
    }

    @Override
    public DBPPropertySource getPropertySource()
    {
        return new PropertySourceCustom();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof DatabaseLazyEditorInput) {
            DatabaseLazyEditorInput li = (DatabaseLazyEditorInput) obj;
            return dataSource == li.dataSource &&
                project == li.project &&
                CommonUtils.equalObjects(nodePath, li.nodePath) &&
                CommonUtils.equalObjects(nodeName, li.nodeName) &&
                CommonUtils.equalObjects(activePageId, li.activePageId) &&
                CommonUtils.equalObjects(activeFolderId, li.activeFolderId);
        }
        return false;
    }

    public IDatabaseEditorInput initializeRealInput(final DBRProgressMonitor monitor) throws DBException
    {
        final DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();

        while (!dataSource.isConnected()) {
            boolean connected;
            try {
                connected = dataSource.connect(monitor, true, true);
            } catch (final DBException e) {
                // Connection error
                final Integer result = new UITask<Integer>() {
                    @Override
                    protected Integer runTask() {
                        ConnectionLostDialog clDialog = new ConnectionLostDialog(DBeaverUI.getActiveWorkbenchShell(), dataSource, e, "Close");
                        return clDialog.open();
                    }
                }.execute();
                if (result == IDialogConstants.STOP_ID) {
                    // Close editor
                    return null;
                } else if (result == IDialogConstants.RETRY_ID) {
                    continue;
                } else {
                    return new ErrorEditorInput(GeneralUtils.makeExceptionStatus(e), navigatorModel.getNodeByObject(dataSource));
                }
            }
            if (!connected) {
                throw new DBException("Connection to '" + dataSource.getName() + "' canceled");
            }
            break;
        }
        try {
            DBNDataSource dsNode = (DBNDataSource) navigatorModel.getNodeByObject(monitor, dataSource, true);
            if (dsNode == null) {
                throw new DBException("Datasource '" + dataSource.getName() + "' navigator node not found");
            }

            dsNode.initializeNode(monitor, null);

            final DBNNode node = navigatorModel.getNodeByPath(
                monitor, project, nodePath);
            if (node == null) {
                throw new DBException("Navigator node '" + nodePath + "' not found");
            }
            if (node instanceof DBNDatabaseNode) {
                EntityEditorInput realInput = new EntityEditorInput((DBNDatabaseNode) node);
                realInput.setDefaultFolderId(activeFolderId);
                realInput.setDefaultPageId(activePageId);
                return realInput;
            } else {
                throw new DBException("Database node has bad type: " + node.getClass().getName());
            }
        } catch (DBException e) {
            return new ErrorEditorInput(GeneralUtils.makeExceptionStatus(e), navigatorModel.getNodeByObject(dataSource));
        }
    }

}
