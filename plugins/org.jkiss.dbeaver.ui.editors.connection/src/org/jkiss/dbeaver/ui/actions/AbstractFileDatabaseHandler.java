/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.actions;

import org.jkiss.api.DriverReference;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.fs.DBFUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.file.IFileTypeHandler;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Database file handler
 */
public abstract class AbstractFileDatabaseHandler implements IFileTypeHandler {

    @Override
    public void openFiles(
        @NotNull List<Path> fileList,
        @NotNull Map<String, String> parameters,
        @Nullable DBPDataSourceContainer providedDataSource
    ) throws DBException {
        DBPProject project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        if (project == null) {
            throw new DBException("No active project - cannot open file");
        }
        DBPDriver driver = DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver(getDriverReference());
        if (driver == null) {
            throw new DBException("Driver '" + getDriverReference() + "' not found");
        }

        if (isSingleDatabaseConnection()) {
            String databaseName = createDatabaseName(fileList);
            String connectionName = createConnectionName(fileList);
            createDatabaseConnection(connectionName, databaseName, project, driver);
        } else {
            for (Path dbFile : fileList) {
                String databaseName = createDatabaseName(Collections.singletonList(dbFile));
                String connectionName = createConnectionName(Collections.singletonList(dbFile));
                createDatabaseConnection(connectionName, databaseName, project, driver);
            }
        }
    }

    private void createDatabaseConnection(
        @NotNull String connectionName,
        @NotNull String databaseName,
        @NotNull DBPProject project,
        @NotNull DBPDriver driver
    ) throws DBException {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setDatabaseName(databaseName);

        DBPDataSourceContainer dsContainer = DBFUtils.createTemporaryDataSourceContainer(connectionName, project, driver, configuration);
        if (dsContainer == null) {
            return;
        }

        DBPDataSourceContainer finalDsContainer = dsContainer;
        UIUtils.runWithMonitor(monitor -> {
            if (finalDsContainer.isConnected() || finalDsContainer.connect(monitor, true, true)) {
                DBPDataSource dataSource = finalDsContainer.getDataSource();
                DBNDatabaseNode openNode = DBNUtils.getDefaultDatabaseNodeToOpen(monitor, dataSource);

                if (openNode == null) {
                    throw new DBException("Cannot determine target node for " + dsContainer.getName());
                } else {
                    UIUtils.syncExec(() -> NavigatorHandlerObjectOpen.openEntityEditor(
                        openNode,
                        null,
                        null,
                        null,
                        UIUtils.getActiveWorkbenchWindow(),
                        true,
                        false));
                }
            }
            return null;
        });
    }


    protected abstract String getDatabaseTerm();

    protected abstract String createDatabaseName(@NotNull List<Path> fileList);

    protected abstract String createConnectionName(List<Path> fileList);

    protected abstract DriverReference getDriverReference();

    protected boolean isSingleDatabaseConnection() {
        return true;
    }

}
