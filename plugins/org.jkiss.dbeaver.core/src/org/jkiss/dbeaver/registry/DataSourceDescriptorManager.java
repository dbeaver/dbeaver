/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.edit.DBOCreator;
import org.jkiss.dbeaver.model.impl.edit.DBOManagerImpl;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceDisconnectHandler;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.NewConnectionWizard;

import java.util.Map;

/**
 * DataSourceDescriptorManager
 */
public class DataSourceDescriptorManager extends DBOManagerImpl<DataSourceDescriptor> implements DBOCreator<DataSourceDescriptor> {

    public CreateResult createNewObject(IWorkbenchWindow workbenchWindow, Object parent, DataSourceDescriptor copyFrom)
    {
        if (copyFrom != null) {
            DataSourceRegistry registry = parent instanceof DataSourceRegistry ? (DataSourceRegistry)parent : copyFrom.getRegistry();
            DataSourceDescriptor dataSource = new DataSourceDescriptor(
                registry,
                DataSourceDescriptor.generateNewId(copyFrom.getDriver()),
                copyFrom.getDriver(),
                new DBPConnectionInfo(copyFrom.getConnectionInfo()));
            dataSource.setSchemaFilter(copyFrom.getSchemaFilter());
            dataSource.setCatalogFilter(copyFrom.getCatalogFilter());
            dataSource.setDescription(copyFrom.getDescription());
            dataSource.setSavePassword(copyFrom.isSavePassword());
            dataSource.setShowSystemObjects(copyFrom.isShowSystemObjects());
            // Generate new name
            String origName = copyFrom.getName();
            String newName;
            for (int i = 0; ; i++) {
                newName = origName + " " + (i + 1);
                if (registry.findDataSourceByName(newName) == null) {
                    break;
                }
            }
            dataSource.setName(newName);
            registry.addDataSource(dataSource);
        } else {
            DataSourceRegistry registry;
            if (parent instanceof DataSourceRegistry) {
                registry = (DataSourceRegistry)parent;
            } else {
                registry = DBeaverCore.getInstance().getProjectRegistry().getActiveDataSourceRegistry();
            }
            ConnectionDialog dialog = new ConnectionDialog(workbenchWindow,
                new NewConnectionWizard(registry));
            dialog.open();
        }
        return CreateResult.CANCEL;
    }

    public void deleteObject(Map<String, Object> options)
    {
        if (getObject().isConnected()) {
            DataSourceDisconnectHandler.execute(getObject());
        }
        getObject().getRegistry().removeDataSource(getObject());
    }

}