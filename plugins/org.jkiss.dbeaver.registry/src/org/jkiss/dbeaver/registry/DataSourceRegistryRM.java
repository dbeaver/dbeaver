/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.rm.RMController;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class DataSourceRegistryRM extends DataSourceRegistry {
    private static final Log log = Log.getLog(DataSourceRegistryRM.class);

    private final RMController client;

    public DataSourceRegistryRM(DBPProject project, @NotNull RMController client) {
        super(project, new DataSourceConfigurationManagerRM(project, client));
        this.client = client;

        // We shouldn't refresh config on update events
//        addDataSourceListener(event -> {
//            if (event.getAction() == DBPEvent.Action.OBJECT_UPDATE && event.getObject() instanceof DBPDataSourceContainer) {
//                refreshConfig();
//            }
//        });
    }

    @Override
    protected void persistDataSourceCreate(@NotNull DBPDataSourceContainer container) {
        DataSourceConfigurationManagerBuffer buffer = new DataSourceConfigurationManagerBuffer();
        saveConfigurationToManager(new VoidProgressMonitor(), buffer, dsc -> dsc.equals(container));

        try {
            client.createProjectDataSources(
                getRemoteProjectId(), new String(buffer.getData(), StandardCharsets.UTF_8), List.of(container.getId()));
            lastError = null;
        } catch (DBException e) {
            lastError = e;
            log.error("Error persisting remote data source update", e);
        }
    }

    @Override
    protected void persistDataSourceUpdate(@NotNull DBPDataSourceContainer container) {
        DataSourceConfigurationManagerBuffer buffer = new DataSourceConfigurationManagerBuffer();
        saveConfigurationToManager(new VoidProgressMonitor(), buffer, dsc -> dsc.equals(container));

        try {
            client.updateProjectDataSources(
                getRemoteProjectId(), new String(buffer.getData(), StandardCharsets.UTF_8), List.of(container.getId()));
            lastError = null;
        } catch (DBException e) {
            lastError = e;
            log.error("Error persisting remote data source update", e);
        }
    }

    @Override
    protected void persistDataSourceDelete(@NotNull DBPDataSourceContainer container) {
        try {
            client.deleteProjectDataSources(getRemoteProjectId(), new String[]{container.getId()});
            lastError = null;
        } catch (DBException e) {
            lastError = e;
            log.error("Error persisting remote data source update", e);
        }
    }

    @Override
    protected void persistDataFolderDelete(@NotNull String folderPath, boolean dropContents) {
        try {
            client.deleteProjectDataSourceFolders(getRemoteProjectId(), new String[]{folderPath}, dropContents);
            lastError = null;
        } catch (DBException e) {
            lastError = e;
            log.error("Error persisting remote data folder delete", e);
        }
    }

    @Override
    public DataSourceFolder addFolder(DBPDataSourceFolder parent, String name) {
        try {
            client.createProjectDataSourceFolder(getRemoteProjectId(), parent == null ? name : parent.getFolderPath() + "/" + name);
            lastError = null;
        } catch (DBException e) {
            lastError = e;
            log.error("Error persisting remote data folder move", e);
            return null;
        }
        return super.addFolder(parent, name);
    }


    @Override
    public void moveFolder(@NotNull String oldPath, @NotNull String newPath) {
        try {
            client.moveProjectDataSourceFolder(getRemoteProjectId(), oldPath, newPath);
            lastError = null;
        } catch (DBException e) {
            lastError = e;
            log.error("Error persisting remote data folder move", e);
            return;
        }
        super.moveFolder(oldPath, newPath);
    }

    @Override
    protected void saveDataSources(DBRProgressMonitor monitor) {
        DataSourceConfigurationManagerBuffer buffer = new DataSourceConfigurationManagerBuffer();
        saveConfigurationToManager(monitor, buffer, null);

        try {
            client.updateProjectDataSources(
                getRemoteProjectId(), new String(buffer.getData(), StandardCharsets.UTF_8), List.of());
            lastError = null;
        } catch (DBException e) {
            lastError = e;
            log.error("Error saving data source configuration", e);
        }
    }

    @NotNull
    private String getRemoteProjectId() {
        if (getProject().getEclipseProject() == null) {
            return getProject().getId();
        } else {
            return getProject().getEclipseProject().getLocation().lastSegment();
        }
    }
}
