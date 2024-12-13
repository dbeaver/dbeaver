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
package org.jkiss.dbeaver.registry.rm;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.rm.RMController;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceConfigurationManagerBuffer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceFolder;
import org.jkiss.dbeaver.registry.DataSourceRegistry;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class DataSourceRegistryRM<T extends DataSourceDescriptor> extends DataSourceRegistry<T> {
    private static final Log log = Log.getLog(DataSourceRegistryRM.class);

    @NotNull
    private final RMController rmController;

    public DataSourceRegistryRM(
        @NotNull DBPProject project,
        @NotNull RMController rmController,
        @NotNull DBPPreferenceStore preferenceStore
    ) {
        super(project, new DataSourceConfigurationManagerRM(project, rmController), preferenceStore);
        this.rmController = rmController;

        // We shouldn't refresh config on update events
//        addDataSourceListener(event -> {
//            if (event.getAction() == DBPEvent.Action.OBJECT_UPDATE && event.getObject() instanceof DBPDataSourceContainer) {
//                refreshConfig();
//            }
//        });
    }

    @Override
    protected void persistDataSourceCreate(@NotNull DBPDataSourceContainer container) {
        if (getProject().isInMemory()) {
            return;
        }
        DataSourceConfigurationManagerBuffer buffer = new DataSourceConfigurationManagerBuffer();
        saveConfigurationToManager(new VoidProgressMonitor(), buffer, dsc -> dsc.equals(container));

        try {
            rmController.createProjectDataSources(
                getRemoteProjectId(), new String(buffer.getData(), StandardCharsets.UTF_8), List.of(container.getId()));
            lastError = null;
        } catch (DBException e) {
            lastError = e;
            log.error("Error persisting rm data source update", e);
        }
    }

    @Override
    protected void persistDataSourceUpdate(@NotNull DBPDataSourceContainer container) {
        if (getProject().isInMemory()) {
            return;
        }
        DataSourceConfigurationManagerBuffer buffer = new DataSourceConfigurationManagerBuffer();
        saveConfigurationToManager(new VoidProgressMonitor(), buffer, dsc -> dsc.equals(container));

        try {
            rmController.updateProjectDataSources(
                getRemoteProjectId(), new String(buffer.getData(), StandardCharsets.UTF_8), List.of(container.getId()));
            lastError = null;
        } catch (DBException e) {
            lastError = e;
            log.error("Error persisting rm data source update", e);
        }
    }

    @Override
    protected void persistDataSourceDelete(@NotNull DBPDataSourceContainer container) {
        if (getProject().isInMemory()) {
            return;
        }
        try {
            rmController.deleteProjectDataSources(getRemoteProjectId(), new String[]{container.getId()});
            lastError = null;
        } catch (DBException e) {
            lastError = e;
            log.error("Error persisting rm data source update", e);
        }
    }

    @Override
    protected void persistDataFolderDelete(@NotNull String folderPath, boolean dropContents) {
        if (getProject().isInMemory()) {
            return;
        }
        try {
            rmController.deleteProjectDataSourceFolders(getRemoteProjectId(), new String[]{folderPath}, dropContents);
            lastError = null;
        } catch (DBException e) {
            lastError = e;
            log.error("Error persisting rm data folder delete", e);
        }
    }

    @NotNull
    @Override
    public DataSourceFolder addFolder(@Nullable DBPDataSourceFolder parent, @NotNull String name) {
        if (getProject().isInMemory()) {
            return createFolder(parent, name);
        }
        try {
            rmController.createProjectDataSourceFolder(getRemoteProjectId(), parent == null ? name : parent.getFolderPath() + "/" + name);
            lastError = null;
        } catch (DBException e) {
            lastError = e;
            log.error("Error persisting rm data folder create", e);
            return null;
        }
        return createFolder(parent, name);
    }


    @Override
    public void moveFolder(@NotNull String oldPath, @NotNull String newPath) {
        if (getProject().isInMemory()) {
            super.moveFolder(oldPath, newPath);
            return;
        }
        try {
            rmController.moveProjectDataSourceFolder(getRemoteProjectId(), oldPath, newPath);
            lastError = null;
        } catch (DBException e) {
            lastError = e;
            log.error("Error persisting rm data folder move", e);
            return;
        }
        super.moveFolder(oldPath, newPath);
    }

    @Override
    protected void saveDataSources(DBRProgressMonitor monitor) {
        if (getProject().isInMemory()) {
            return;
        }

        DataSourceConfigurationManagerBuffer buffer = new DataSourceConfigurationManagerBuffer();
        saveConfigurationToManager(monitor, buffer, null);

        try {
            rmController.updateProjectDataSources(
                getRemoteProjectId(), new String(buffer.getData(), StandardCharsets.UTF_8), List.of());
            lastError = null;
        } catch (DBException e) {
            lastError = e;
            log.error("Error saving data source configuration", e);
        }
    }

    @NotNull
    private String getRemoteProjectId() {
        return getProject().getId();
    }
}
