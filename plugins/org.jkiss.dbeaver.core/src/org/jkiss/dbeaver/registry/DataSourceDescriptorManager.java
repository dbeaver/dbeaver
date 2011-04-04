/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.impl.jdbc.edit.JDBCObjectManager;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceDisconnectHandler;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.NewConnectionWizard;

import java.util.Map;

/**
 * DataSourceDescriptorManager
 */
public class DataSourceDescriptorManager extends JDBCObjectManager<DataSourceDescriptor> implements DBEObjectMaker<DataSourceDescriptor> {

    public long getMakerOptions()
    {
        return 0;
    }

    public DataSourceDescriptor createNewObject(IWorkbenchWindow workbenchWindow, DBECommandContext commander, Object parent, Object copyFrom)
    {
        if (copyFrom != null) {
            DataSourceDescriptor dsTpl = (DataSourceDescriptor)copyFrom;
            DataSourceRegistry registry = parent instanceof DataSourceRegistry ? (DataSourceRegistry)parent : dsTpl.getRegistry();
            DataSourceDescriptor dataSource = new DataSourceDescriptor(
                registry,
                DataSourceDescriptor.generateNewId(dsTpl.getDriver()),
                dsTpl.getDriver(),
                new DBPConnectionInfo(dsTpl.getConnectionInfo()));
            dataSource.setSchemaFilter(dsTpl.getSchemaFilter());
            dataSource.setCatalogFilter(dsTpl.getCatalogFilter());
            dataSource.setDescription(dsTpl.getDescription());
            dataSource.setSavePassword(dsTpl.isSavePassword());
            dataSource.setShowSystemObjects(dsTpl.isShowSystemObjects());
            // Generate new name
            String origName = dsTpl.getName();
            String newName = origName;
            for (int i = 0; ; i++) {
                if (registry.findDataSourceByName(newName) == null) {
                    break;
                }
                newName = origName + " " + (i + 1);
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
        return null;
    }

    public void deleteObject(DBECommandContext commander, final DataSourceDescriptor object, Map<String, Object> options)
    {
        Runnable remover = new Runnable() {
            public void run()
            {
                object.getRegistry().removeDataSource(object);
            }
        };
        if (object.isConnected()) {
            DataSourceDisconnectHandler.execute(object, remover);
        } else {
            remover.run();
        }
    }


}