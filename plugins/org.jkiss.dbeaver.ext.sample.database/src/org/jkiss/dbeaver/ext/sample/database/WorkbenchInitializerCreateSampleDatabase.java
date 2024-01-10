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
package org.jkiss.dbeaver.ext.sample.database;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
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
        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PROP_SAMPLE_DB_CANCELED)) {
            // Create was canceled
            return;
        }
        if (DataSourceRegistry.getAllDataSources().size() > 1) {
            // Seems to be experienced user - no need in sampel db
            return;
        }
        DBPProject activeProject = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        if (activeProject == null || !activeProject.isRegistryLoaded()) {
            // No active project
            return;
        }
        DBPDataSourceRegistry registry = activeProject.getDataSourceRegistry();
        if (isSampleDatabaseExists(registry)) {
            // Already exist
            return;
        }
        if (!showCreateSampleDatabasePrompt(window.getShell())) {
            DBWorkbench.getPlatform().getPreferenceStore().setValue(PROP_SAMPLE_DB_CANCELED, true);
            return;
        }
        createSampleDatabase(registry);
    }

    static boolean isSampleDatabaseExists(DBPDataSourceRegistry registry) {
        return registry.getDataSource(SAMPLE_DB1_ID) != null;
    }

    static boolean showCreateSampleDatabasePrompt(Shell shell) {
        return UIUtils.confirmAction(
                shell,
                SampleDatabaseMessages.dialog_create_title,
                NLS.bind(SampleDatabaseMessages.dialog_create_description, GeneralUtils.getProductName())
        );
    }

    static void createSampleDatabase(DBPDataSourceRegistry dsRegistry) {
        DataSourceDescriptor dataSource = (DataSourceDescriptor)dsRegistry.getDataSource(SAMPLE_DB1_ID);
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
        File dbFolder = GeneralUtils.getMetadataFolder().resolve(SAMPLE_DB1_FOLDER).toFile();
        if (!dbFolder.exists()) {
            if (!dbFolder.mkdirs()) {
                log.error("Can't create target database folder " + dbFolder.getAbsolutePath());
                return;
            }
        }
        File dbFile = new File(dbFolder, SAMPLE_DB_FILE_NAME);
        try (InputStream is = WorkbenchInitializerCreateSampleDatabase.class.getClassLoader().getResourceAsStream(SAMPLE_DB_SOURCE_PATH)) {
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
        connectionInfo.setUrl(sqliteDriver.getConnectionURL(connectionInfo));
        dataSource = new DataSourceDescriptor(dsRegistry, SAMPLE_DB1_ID, sqliteDriver, connectionInfo);
        dataSource.setSavePassword(true);
        dataSource.getNavigatorSettings().setShowSystemObjects(true);
        dataSource.setName("DBeaver Sample Database (SQLite)");
        try {
            dsRegistry.addDataSource(dataSource);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Connection create error", null, e);
        }
    }
}

