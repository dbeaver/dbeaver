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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.ui.dialogs.connection.CreateConnectionDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.NewConnectionWizard;

import java.util.Map;

/**
 * DataSourceDescriptorManager
 */
public class DataSourceDescriptorManager extends AbstractObjectManager<DataSourceDescriptor> implements DBEObjectMaker<DataSourceDescriptor, DataSourceRegistry> {

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
    public boolean canCreateObject(DataSourceRegistry parent)
    {
        return true;
    }

    @Override
    public boolean canDeleteObject(DataSourceDescriptor object)
    {
        return true;
    }

    @Override
    public DataSourceDescriptor createNewObject(DBECommandContext commandContext, DataSourceRegistry parent, Object copyFrom)
    {
        if (copyFrom != null) {
            DataSourceDescriptor dsTpl = (DataSourceDescriptor)copyFrom;
            DBPDataSourceRegistry registry = parent != null ? parent : dsTpl.getRegistry();
            DataSourceDescriptor dataSource = new DataSourceDescriptor(
                registry,
                DataSourceDescriptor.generateNewId(dsTpl.getDriver()),
                dsTpl.getDriver(),
                new DBPConnectionConfiguration(dsTpl.getConnectionConfiguration()));
            dataSource.copyFrom(dsTpl);
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
            if (parent != null) {
                registry = parent;
            } else {
                registry = DBeaverCore.getInstance().getProjectRegistry().getActiveDataSourceRegistry();
            }
            CreateConnectionDialog dialog = new CreateConnectionDialog(
                DBeaverUI.getActiveWorkbenchWindow(),
                new NewConnectionWizard(registry));
            dialog.open();
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