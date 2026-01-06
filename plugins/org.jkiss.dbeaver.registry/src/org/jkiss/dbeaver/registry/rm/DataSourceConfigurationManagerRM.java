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
import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.rm.RMController;
import org.jkiss.dbeaver.registry.DataSourceConfigurationManager;
import org.jkiss.dbeaver.registry.DataSourceRegistry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Configuration files manager on FS
 */
public class DataSourceConfigurationManagerRM implements DataSourceConfigurationManager {

    private static final Log log = Log.getLog(DataSourceConfigurationManagerRM.class);

    @NotNull
    private final DBPProject project;
    private final RMController rmController;

    public DataSourceConfigurationManagerRM(@NotNull DBPProject project, @NotNull RMController client) {
        this.project = project;
        this.rmController = client;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public List<DBPDataSourceConfigurationStorage> getConfigurationStorages() {
        return Collections.singletonList(new DataSourceRMStorage(project));
    }

    @Override
    public InputStream readConfiguration(@NotNull String name, @Nullable Collection<String> dataSourceIds) throws DBException, IOException {
        if (name.startsWith(DataSourceRegistry.CREDENTIALS_CONFIG_FILE_PREFIX)) {
            // Credentials storage is not supported
            return null;
        }
        if (!name.equals(project.getName())) {
            throw new DBException("Wrong storage name: " + name);
        }
        String projectId = project.getId();
        try {
            String dsContent = rmController.getProjectsDataSources(
                projectId,
                dataSourceIds == null ? null : dataSourceIds.toArray(new String[0]));
            if (dsContent == null) {
                return null;
            }
            return new ByteArrayInputStream(dsContent.getBytes(StandardCharsets.UTF_8));
        } catch (DBException e) {
            throw new DBException("Could not load project datasources:\n" + e.getMessage(), e);
        }
    }

    @Override
    public void writeConfiguration(@NotNull String name, @Nullable byte[] data) throws DBException {
        throw new DBException("This method should not be called");
    }
}
