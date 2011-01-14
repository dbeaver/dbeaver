/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.model.edit.DBOCreator;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
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
            DataSourceDescriptor dataSource = new DataSourceDescriptor(
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
            DataSourceRegistry registry = copyFrom.getRegistry();
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
            ConnectionDialog dialog = new ConnectionDialog(workbenchWindow, new NewConnectionWizard(workbenchWindow));
            dialog.open();
        }
        return CreateResult.CANCEL;
    }

    public void deleteObject(Map<String, Object> options)
    {
        if (getObject().isConnected()) {
            DataSourceDisconnectHandler.execute(getObject());
        }
        DataSourceRegistry.getDefault().removeDataSource(getObject());
    }

}