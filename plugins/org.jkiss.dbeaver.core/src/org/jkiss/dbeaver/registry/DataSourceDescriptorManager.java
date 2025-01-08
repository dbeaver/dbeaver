/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.ui.ConnectionFeatures;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.ui.dialogs.connection.NewConnectionDialog;

import java.util.Map;

/**
 * DataSourceDescriptorManager
 */
public class DataSourceDescriptorManager extends AbstractObjectManager<DataSourceDescriptor> implements DBEObjectMaker<DataSourceDescriptor, DBPObject> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return 0;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, DataSourceDescriptor> getObjectsCache(DataSourceDescriptor object) {
        return null;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        if (container instanceof DBPProject) {
            return ((DBPProject) container).hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT);
        }
        return true;
    }

    @Override
    public boolean canDeleteObject(@NotNull DataSourceDescriptor object) {
        return object.getProject().hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT);
    }

    @Override
    public DataSourceDescriptor createNewObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext commandContext, @NotNull Object container, Object copyFrom, @NotNull Map<String, Object> options) throws DBException {
        if (copyFrom != null) {
            DataSourceDescriptor dsTpl = (DataSourceDescriptor) copyFrom;
            DBPDataSourceRegistry registry;
            DBPDataSourceFolder folder = null;
            if (container instanceof DataSourceRegistry) {
                registry = (DBPDataSourceRegistry) container;
            } else if (container instanceof DBPProject) {
                registry = ((DBPProject) container).getDataSourceRegistry();
            } else if (container instanceof DBPDataSourceFolder) {
                folder = (DBPDataSourceFolder) container;
                registry = folder.getDataSourceRegistry();
            } else {
                registry = dsTpl.getRegistry();
            }
            DataSourceDescriptor dataSource = registry.createDataSource(
                DataSourceDescriptor.generateNewId(dsTpl.getDriver()),
                dsTpl.getDriver(),
                new DBPConnectionConfiguration(dsTpl.getConnectionConfiguration())
            );
            dataSource.copyFrom(dsTpl);
            if (folder != null) {
                dataSource.setFolder(folder);
            } else if (dsTpl.getRegistry() == registry) {
                // Copy folder only if we copy in the same project
                dataSource.setFolder(dsTpl.getFolder());
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
            UIUtils.asyncExec(() -> NewConnectionDialog.openNewConnectionDialog(UIUtils.getActiveWorkbenchWindow()));
        }
        return null;
    }

    @Override
    public void deleteObject(@NotNull DBECommandContext commandContext, @NotNull final DataSourceDescriptor object, @NotNull Map<String, Object> options) {
        Runnable remover = () -> object.getRegistry().removeDataSource(object);
        if (object.isConnected()) {
            DataSourceHandler.disconnectDataSource(object, remover);
        } else {
            remover.run();
        }
        ConnectionFeatures.CONNECTION_DELETE.use(Map.of("driver", object.getDriver().getPreconfiguredId()));
    }


}