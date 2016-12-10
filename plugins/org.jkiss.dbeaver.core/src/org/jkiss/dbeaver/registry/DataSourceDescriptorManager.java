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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.ui.dialogs.connection.CreateConnectionDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.NewConnectionWizard;

import java.util.Map;

/**
 * DataSourceDescriptorManager
 */
public class DataSourceDescriptorManager extends AbstractObjectManager<DataSourceDescriptor> implements DBEObjectMaker<DataSourceDescriptor, DBPObject> {

    @Override
    public long getMakerOptions()
    {
        return 0;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, DataSourceDescriptor> getObjectsCache(DataSourceDescriptor object)
    {
        return null;
    }

    @Override
    public boolean canCreateObject(DBPObject parent)
    {
        return true;
    }

    @Override
    public boolean canDeleteObject(DataSourceDescriptor object)
    {
        return true;
    }

    @Override
    public DataSourceDescriptor createNewObject(DBRProgressMonitor monitor, DBECommandContext commandContext, DBPObject parent, Object copyFrom)
    {
        if (copyFrom != null) {
            DataSourceDescriptor dsTpl = (DataSourceDescriptor)copyFrom;
            DBPDataSourceRegistry registry;
            DBPDataSourceFolder folder = null;
            if (parent instanceof DataSourceRegistry) {
                registry = (DBPDataSourceRegistry) parent;
            } else if (parent instanceof DBPDataSourceFolder) {
                folder = (DBPDataSourceFolder)parent;
                registry = folder.getDataSourceRegistry();
            } else {
                registry = dsTpl.getRegistry();
            }
            DataSourceDescriptor dataSource = new DataSourceDescriptor(
                registry,
                DataSourceDescriptor.generateNewId(dsTpl.getDriver()),
                dsTpl.getDriver(),
                new DBPConnectionConfiguration(dsTpl.getConnectionConfiguration()));
            dataSource.copyFrom(dsTpl);
            if (folder != null) {
                dataSource.setFolder(folder);
            }
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
            DBeaverUI.asyncExec(new Runnable() {
                @Override
                public void run() {
                    CreateConnectionDialog dialog = new CreateConnectionDialog(
                        DBeaverUI.getActiveWorkbenchWindow(),
                        new NewConnectionWizard());
                    dialog.open();
                }
            });
        }
        return null;
    }

    @Override
    public void deleteObject(DBECommandContext commandContext, final DataSourceDescriptor object, Map<String, Object> options)
    {
        Runnable remover = new Runnable() {
            @Override
            public void run()
            {
                object.getRegistry().removeDataSource(object);
            }
        };
        if (object.isConnected()) {
            DataSourceHandler.disconnectDataSource(object, remover);
        } else {
            remover.run();
        }
    }


}