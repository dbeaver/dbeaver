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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.file.IFileTypeHandler;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Database file handler
 */
public abstract class AbstractFileDatabaseHandler implements IFileTypeHandler {

    private static final Log log = Log.getLog(AbstractFileDatabaseHandler.class);
    private static final String FILE_DATABASES_FOLDER = "File databases";

    @Override
    public void openFiles(
        @NotNull List<Path> fileList,
        @NotNull Map<String, String> parameters,
        @Nullable DBPDataSourceContainer providedDataSource
    ) {
        DBPProject project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        if (project == null) {
            log.error("No active project - cannot open file");
            return;
        }
        DBPDriver driver = DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver(getDriverReference());
        if (driver == null) {
            log.error("Driver '" + getDriverReference() + "' not found");
            return;
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

    private void createDatabaseConnection(String connectionName, @NotNull String databaseName, DBPProject project, DBPDriver driver) {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setDatabaseName(databaseName);

        DBPDataSourceRegistry registry = project.getDataSourceRegistry();
        String connectionId = "file_database_" + CommonUtils.escapeIdentifier(databaseName);
        DBPDataSourceContainer dsContainer = registry.getDataSource(connectionId);
        if (dsContainer == null) {
            dsContainer = registry.createDataSource(connectionId, driver, configuration);
            int conNameSuffix = 1;
            connectionName = "File - " + CommonUtils.truncateString(connectionName, 64);
            String finalConnectionName = connectionName;
            while (registry.findDataSourceByName(finalConnectionName) != null) {
                conNameSuffix++;
                finalConnectionName = connectionName + " " + conNameSuffix;
            }
            dsContainer.setName(finalConnectionName);
            dsContainer.setTemporary(true);
            DBPDataSourceFolder folder = registry.getFolder(FILE_DATABASES_FOLDER);
            DBNModel navigatorModel = project.getNavigatorModel();
            if (navigatorModel != null) {
                DBNProject projectNode = navigatorModel.getRoot().getProjectNode(project);
                if (projectNode != null) {
                    projectNode.getDatabases().getFolderNode(folder);
                }
            }
            dsContainer.setFolder(folder);

            try {
                registry.addDataSource(dsContainer);
            } catch (DBException e) {
                log.error(e);
                return;
            }
        }

        try {
            DBPDataSourceContainer finalDsContainer = dsContainer;
            UIUtils.runInProgressService(monitor -> {
                try {
                    if (finalDsContainer.isConnected() || finalDsContainer.connect(monitor, true, true)) {
                        DBPDataSource dataSource = finalDsContainer.getDataSource();
                        List<DBSEntity> entities = new ArrayList<>();
                        if (dataSource instanceof DBSObjectContainer container) {
                            getConnectionEntities(monitor, container, entities);
                        }

                        DBSObject objectToOpen;
                        if (entities.size() == 1) {
                            objectToOpen = entities.get(0);
                        } else {
                            if (entities.size() > 1) {
                                objectToOpen = entities.get(0).getParentObject();
                            } else {
                                objectToOpen = dataSource;
                            }
                        }
                        DBNDatabaseNode openNode = DBNUtils.getNodeByObject(monitor, objectToOpen, true);

                        if (openNode == null) {
                            DBWorkbench.getPlatformUI().showError("No objects", "Cannot determine target node");
                        } else {
                            UIUtils.syncExec(() -> {
                                NavigatorHandlerObjectOpen.openEntityEditor(
                                    openNode,
                                    null,
                                    null,
                                    null,
                                    UIUtils.getActiveWorkbenchWindow(),
                                    true,
                                    false);
                            });
                        }
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(
                "Connecting to " + getDatabaseTerm() + " datasource",
                "Error connecting to " + getDatabaseTerm() + " datasource",
                e.getTargetException());
        } catch (InterruptedException ignore) {

        }
    }

    private void getConnectionEntities(
        DBRProgressMonitor monitor,
        DBSObjectContainer container,
        List<DBSEntity> entities
    ) throws DBException {
        for (DBSObject child : container.getChildren(monitor)) {
            if (child instanceof DBSEntity entity) {
                entities.add(entity);
            } else if (child instanceof DBSObjectContainer oc) {
                getConnectionEntities(monitor, oc, entities);
            }
        }
    }

    private static void openNodeEditor(DBNNode node) {
        UIUtils.asyncExec(() -> {
            NavigatorUtils.openNavigatorNode(node, UIUtils.getActiveWorkbenchWindow());
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
