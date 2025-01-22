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
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
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
            createDatabaseConnection(databaseName, project, driver);
        } else {
            for (Path dbFile : fileList) {
                String databaseName = createDatabaseName(Collections.singletonList(dbFile));
                createDatabaseConnection(databaseName, project, driver);
            }
        }
    }

    private void createDatabaseConnection(@NotNull String databaseName, DBPProject project, DBPDriver driver) {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setDatabaseName(databaseName);
        DBPDataSourceContainer dsContainer = project.getDataSourceRegistry().createDataSource(driver, configuration);
        dsContainer.setName("File: " + CommonUtils.truncateString(databaseName, 32));
        dsContainer.setTemporary(true);

        try {
            project.getDataSourceRegistry().addDataSource(dsContainer);
        } catch (DBException e) {
            log.error(e);
            return;
        }

        try {
            UIUtils.runInProgressService(monitor -> {
                try {
                    if (dsContainer.connect(monitor, true, true)) {
                        DBPDataSource dataSource = dsContainer.getDataSource();
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

    protected abstract DriverReference getDriverReference();

    protected boolean isSingleDatabaseConnection() {
        return true;
    }

}
