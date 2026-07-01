/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.config.sample;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;

public final class SampleDatabaseUtil {
    private static final Log log = Log.getLog(SampleDatabaseUtil.class);

    private static final String SAMPLE_DB1_ID = "dbeaver-sample-database-sqlite-1";
    private static final String SAMPLE_DB1_FOLDER = "sample-database-sqlite-1";
    private static final String SAMPLE_DB_FILE_NAME = "Chinook.db";
    private static final String SAMPLE_DB_SOURCE_PATH = "data/" + SAMPLE_DB_FILE_NAME;

    private SampleDatabaseUtil() {
    }

    public static boolean isSampleDatabaseExists(@NotNull DBPDataSourceRegistry registry) {
        return registry.getDataSource(SAMPLE_DB1_ID) != null;
    }

    public static void createSampleDatabase(@NotNull DBPDataSourceRegistry registry) {
        DBPDataSourceContainer dataSource = registry.getDataSource(SAMPLE_DB1_ID);
        if (dataSource != null) {
            return;
        }
        DBPDataSourceProviderDescriptor genericDSProvider = DataSourceProviderRegistry.getInstance().getDataSourceProvider("generic");
        if (genericDSProvider == null) {
            log.error("Can't find generic data source provider");
            return;
        }
        DBPDriver sqliteDriver = genericDSProvider.getDriver("sqlite_jdbc");
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
        try (InputStream is = SampleDatabaseUtil.class.getClassLoader().getResourceAsStream(SAMPLE_DB_SOURCE_PATH)) {
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
        dataSource = registry.createDataSource(SAMPLE_DB1_ID, sqliteDriver, connectionInfo);
        dataSource.setSavePassword(true);
        dataSource.getNavigatorSettings().setShowSystemObjects(true);
        dataSource.setName("DBeaver Sample Database (SQLite)");
        try {
            registry.addDataSource(dataSource);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Connection create error", null, e);
        }
    }
}
