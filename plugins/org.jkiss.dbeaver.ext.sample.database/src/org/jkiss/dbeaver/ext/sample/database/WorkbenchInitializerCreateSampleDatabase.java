/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.sample.database;

import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;

public class WorkbenchInitializerCreateSampleDatabase implements IWorkbenchWindowInitializer {

    private static final String PROP_SAMPLE_DB_CANCELED = "sample.database.canceled";
    private static final String SAMPLE_DB1_ID = "dbeaver-sample-database-sqlite-1";
    private static final String SAMPLE_DB1_FOLDER = "sample-database-sqlite-1";

    private static final String SAMPLE_DB_FILE_NAME = "Chinook.db";
    private static final String SAMPLE_DB_SOURCE_PATH = "data/" + SAMPLE_DB_FILE_NAME;

    private static final Log log = Log.getLog(WorkbenchInitializerCreateSampleDatabase.class);

    @Override
    public void initializeWorkbenchWindow(IWorkbenchWindow window) {
        if (DBeaverCore.getGlobalPreferenceStore().getBoolean(PROP_SAMPLE_DB_CANCELED)) {
            // Create was canceled
            return;
        }
        if (DataSourceRegistry.getAllDataSources().size() > 1) {
            // Seems to be experienced user - no need in sampel db
            return;
        }
        IProject activeProject = DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
        if (activeProject == null) {
            // No active project
            return;
        }
        if (DataSourceRegistry.findDataSource(SAMPLE_DB1_ID) != null) {
            // Already exist
            return;
        }
        if (!UIUtils.confirmAction(window.getShell(),
            "Create Sample Database",
            "Do you want to create sample database?\nIt can be used as an example to explore basic " + GeneralUtils.getProductName() + " features."))
        {
            DBeaverCore.getGlobalPreferenceStore().setValue(PROP_SAMPLE_DB_CANCELED, true);
            return;
        }

        createSampleDatabase(activeProject);
    }

    private void createSampleDatabase(IProject project) {
        DataSourceRegistry dsRegistry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(project);
        DataSourceDescriptor dataSource = dsRegistry.getDataSource(SAMPLE_DB1_ID);
        if (dataSource != null) {
            return;
        }
        DataSourceProviderDescriptor genericDSProvider = DataSourceProviderRegistry.getInstance().getDataSourceProvider("generic");
        if (genericDSProvider == null) {
            log.error("Can't find generic data source provider");
            return;
        }
        DriverDescriptor sqliteDriver = genericDSProvider.getDriver("sqlite_jdbc");
        if (sqliteDriver == null) {
            log.error("Can't find SQLite driver is generic provider");
            return;
        }
        // Extract bundled database to workspace metadata
        File dbFolder = new File(GeneralUtils.getMetadataFolder(), SAMPLE_DB1_FOLDER);
        if (!dbFolder.exists()) {
            if (!dbFolder.mkdirs()) {
                log.error("Can't create target database folder " + dbFolder.getAbsolutePath());
                return;
            }
        }
        File dbFile = new File(dbFolder, SAMPLE_DB_FILE_NAME);
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(SAMPLE_DB_SOURCE_PATH)) {
            try (OutputStream os = new FileOutputStream(dbFile)) {
                IOUtils.copyStream(is, os);
            }
        } catch (IOException e) {
            log.error("Error extracting sample database to workspace", e);
            return;
        }

        DBPConnectionConfiguration connectionInfo = new DBPConnectionConfiguration();
        connectionInfo.setDatabaseName(dbFile.getAbsolutePath());
        connectionInfo.setConnectionType(DBPConnectionType.DEV);
        connectionInfo.setUrl(genericDSProvider.getInstance(sqliteDriver).getConnectionURL(sqliteDriver, connectionInfo));
        dataSource = new DataSourceDescriptor(dsRegistry, SAMPLE_DB1_ID, sqliteDriver, connectionInfo);
        dataSource.setSavePassword(true);
        dataSource.setShowSystemObjects(true);
        dataSource.setName("DBeaver Sample Database (SQLite)");
        dsRegistry.addDataSource(dataSource);
    }
}

