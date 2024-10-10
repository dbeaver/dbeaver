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
package org.jkiss.dbeaver.registry.storage;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.connection.InternalDatabaseConfig;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

public class H2Migrator {

    private static final Log log = Log.getLog(H2Migrator.class);

    public static final String V1_DRIVER_NAME = "h2_embedded";
    public static final String V2_DRIVER_NAME = "h2_embedded_v2";

    // language=H2
    private static final String EXPORT_SCRIPT = "SCRIPT TO ?";
    // language=H2
    private static final String EXPORT_SCRIPT_USING_PASSWORD = "SCRIPT TO ? COMPRESSION DEFLATE CIPHER AES PASSWORD ? CHARSET 'UTF-8'";
    // no 'language=H2' — due to an annoying error about unresolvable statement 'FROM_1X'
    private static final String IMPORT_SCRIPT = "RUNSCRIPT FROM ? FROM_1X";
    // no 'language=H2' — due to an annoying error about unresolvable statement 'FROM_1X'
    private static final String IMPORT_SCRIPT_USING_PASSWORD =
        "RUNSCRIPT FROM ? COMPRESSION DEFLATE CIPHER AES PASSWORD ? CHARSET 'UTF-8' FROM_1X";

    @NotNull
    private final DBRProgressMonitor monitor;
    @NotNull
    private final DataSourceProviderRegistry dataSourceProviderRegistry;
    @NotNull
    private final InternalDatabaseConfig databaseConfiguration;
    @NotNull
    private final String resolvedDbUrl;
    @NotNull
    private final Properties dbProperties;

    public H2Migrator(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DataSourceProviderRegistry dataSourceProviderRegistry,
        @NotNull InternalDatabaseConfig databaseConfiguration,
        @NotNull String resolvedDbUrl,
        @NotNull Properties dbProperties
    ) {
        this.monitor = monitor;
        this.dataSourceProviderRegistry = dataSourceProviderRegistry;
        this.databaseConfiguration = databaseConfiguration;
        this.resolvedDbUrl = resolvedDbUrl;
        this.dbProperties = dbProperties;
    }

    /**
     * <p>
     * Migrates the H2 database from version 1 to version 2.
     * </p>
     * <p>
     * IMPORTANT: if the database is already version 2, but the driver in the configuration is version 1,
     * or the link points to a v1 file, this method updates the database configuration.
     * </p>
     */
    public void migrateDatabaseIfNeeded(@NotNull String dbNameV1, @NotNull String dbNameV2) {
        if (!resolvedDbUrl.endsWith(dbNameV1) ||
            !V1_DRIVER_NAME.equals(databaseConfiguration.getDriver()) ||
            resolvedDbUrl.startsWith("jdbc:h2:mem:")
        ) {
            log.trace("No migration needed");
            return;
        }

        var workspacePaths = new WorkspacePaths(resolvedDbUrl, dbNameV1, dbNameV2);

        // the changed config is not written to disk immediately, so it is possible that the database is migrated,
        // but the config on disk remains old
        if (workspacePaths.v2Paths.dbDataFile.toFile().exists() &&
            (resolvedDbUrl.endsWith(dbNameV1) || V1_DRIVER_NAME.equals(databaseConfiguration.getDriver()))
        ) {
            updateConfig(workspacePaths);
            return;
        }

        var oldUrl = databaseConfiguration.getUrl();
        var oldDriver = databaseConfiguration.getDriver();
        try {
            migrateDatabase(workspacePaths);
            log.debug("H2 v1->v2 migration was successful");
        } catch (Exception e) {
            log.error("Migration H2 v1->v2 failed", e);
            rollback(workspacePaths, oldUrl, oldDriver);
        }
    }

    private void migrateDatabase(@NotNull WorkspacePaths workspacePaths) throws DBException, SQLException, IOException {
        try {
            monitor.beginTask("H2 database v1 -> v2 migration started", 3);

            final var v1Driver = getDriver(V1_DRIVER_NAME);
            final var v2Driver = getDriver(V2_DRIVER_NAME);

            final var exportFilePath = workspacePaths.exportFilePath.toString();

            monitor.subTask("Exporting v1 database");
            if (dbProperties.getProperty(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD) == null) {
                executeScript(v1Driver, resolvedDbUrl, EXPORT_SCRIPT, exportFilePath);
            } else {
                executeScript(v1Driver, resolvedDbUrl, EXPORT_SCRIPT_USING_PASSWORD, exportFilePath);
            }
            monitor.worked(1);
            monitor.subTask("Creating v1 database backup '" + workspacePaths.v1DataBackupPath + "'");
            Files.move(workspacePaths.v1Paths.dbDataFile, workspacePaths.v1DataBackupPath, StandardCopyOption.REPLACE_EXISTING);
            if (workspacePaths.v1Paths.dbTraceFile.toFile().exists()) {
                Files.move(workspacePaths.v1Paths.dbTraceFile, workspacePaths.v1TraceBackupPath, StandardCopyOption.REPLACE_EXISTING);
            }

            updateConfig(workspacePaths);
            monitor.worked(1);
            monitor.subTask("Importing data to new v2 database");
            var updatedResolvedDbUrl = GeneralUtils.replaceVariables(databaseConfiguration.getUrl(), SystemVariablesResolver.INSTANCE);
            if (dbProperties.getProperty(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD) == null) {
                executeScript(v2Driver, updatedResolvedDbUrl, IMPORT_SCRIPT, exportFilePath);
            } else {
                executeScript(v2Driver, updatedResolvedDbUrl, IMPORT_SCRIPT_USING_PASSWORD, exportFilePath);
            }

            removeExportFile(workspacePaths);
            log.debug("Export file removed '" + workspacePaths.exportFilePath + "'");
            monitor.worked(1);
        } finally {
            monitor.done();
        }
    }

    @NotNull
    private Driver getDriver(@NotNull String driverName) throws DBException {
        var driverDescriptor = dataSourceProviderRegistry.findDriver(driverName);
        if (driverDescriptor == null) {
            throw new DBException("Driver '" + driverName + "' couldn't be resolved");
        }
        return driverDescriptor.getDriverInstance(monitor);
    }

    private void executeScript(
        @NotNull Driver driver,
        @NotNull String dbUrl,
        @NotNull String script,
        @NotNull String filePath
    ) throws SQLException {
        try (var connection = driver.connect(dbUrl, dbProperties);
             var statement = connection.prepareStatement(script)
        ) {
            statement.setString(1, filePath);
            var password = dbProperties.getProperty(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD);
            if (password != null) {
                statement.setString(2, password);
            }
            statement.execute();
        }
    }

    private void updateConfig(@NotNull WorkspacePaths workspacePaths) {
        if (!V2_DRIVER_NAME.equals(databaseConfiguration.getDriver())) {
            log.debug("Using database driver '" + V2_DRIVER_NAME + "' instead of '" + V1_DRIVER_NAME + "' from config");
            databaseConfiguration.setDriver(V2_DRIVER_NAME);
        }

        var updatedDbUrl = CommonUtils.replaceLast(
            databaseConfiguration.getUrl(),
            workspacePaths.v1Paths.dbName,
            workspacePaths.v2Paths.dbName
        );
        if (!updatedDbUrl.equals(databaseConfiguration.getUrl())) {
            log.debug("Using database file '" + workspacePaths.v2Paths.dbDataFile + "' instead of '"
                + workspacePaths.v1Paths.dbDataFile + "' from config");
            databaseConfiguration.setUrl(updatedDbUrl);
        }
    }

    private void removeExportFile(@NotNull WorkspacePaths workspacePaths) {
        var exportFile = workspacePaths.exportFilePath.toFile();
        if (exportFile.exists()) {
            if (!exportFile.delete()) {
                log.error("Unable to remove H2 v1 export script file");
            }
        }
    }

    private void rollback(@NotNull WorkspacePaths workspacePaths, @NotNull String oldUrl, @NotNull String oldDriver) {
        removeExportFile(workspacePaths);
        try {
            Files.move(workspacePaths.v1DataBackupPath, workspacePaths.v1Paths.dbDataFile, StandardCopyOption.REPLACE_EXISTING);
            if (workspacePaths.v1TraceBackupPath.toFile().exists()) {
                Files.move(workspacePaths.v1TraceBackupPath, workspacePaths.v1Paths.dbTraceFile, StandardCopyOption.REPLACE_EXISTING);
            }
            log.debug("v1 files restored");
        } catch (IOException e) {
            log.error("Unable to restore old database file '" + workspacePaths.v1Paths.dbDataFile + "'", e);
        }
        databaseConfiguration.setUrl(oldUrl);
        databaseConfiguration.setDriver(oldDriver);
    }

    public static boolean isH2Database(InternalDatabaseConfig databaseConfiguration) {
        return databaseConfiguration.getUrl().startsWith("jdbc:h2");
    }

    private static class WorkspacePaths {

        private static final String V1_DATA_BACKUP_FILE_NAME = "h2db_v1_backup";
        private static final String V1_TRACE_BACKUP_FILE_NAME = "h2db_trace_v1_backup";

        private static final String EXPORT_SCRIPT_FILE_NAME = "H2v1ExportScript";

        @NotNull
        private final H2FilesPaths v1Paths;
        @NotNull
        private final H2FilesPaths v2Paths;

        @NotNull
        private final Path v1DataBackupPath;
        @NotNull
        private final Path v1TraceBackupPath;
        @NotNull
        private final Path exportFilePath;

        private WorkspacePaths(@NotNull String resolvedDbUrl, @NotNull String dbNameV1, @NotNull String dbNameV2) {
            var dbFolderPath = getFolderPath(resolvedDbUrl);

            v1Paths = new H2FilesPaths(dbFolderPath, dbNameV1);
            v2Paths = new H2FilesPaths(dbFolderPath, dbNameV2);


            v1DataBackupPath = dbFolderPath.resolve(createBackupFileName(v1Paths.dbDataFile));
            v1TraceBackupPath = dbFolderPath.resolve(createBackupFileName(v1Paths.dbTraceFile));

            exportFilePath = dbFolderPath.resolve(EXPORT_SCRIPT_FILE_NAME);
        }

        private String createBackupFileName(Path file) {
            String backupFileName = file.getFileName().toString() + ".backup";
            if (!backupFileName.startsWith(".")) {
                backupFileName = "." + backupFileName;
            }
            return backupFileName;
        }

        @NotNull
        private static Path getFolderPath(@NotNull String resolvedDbUrl) {
            var filePath = Paths.get(resolvedDbUrl.substring("jdbc:h2:".length()));
            return filePath.getParent();
        }
    }

    private static class H2FilesPaths {
        @NotNull
        private final String dbName;
        @NotNull
        private final Path dbDataFile;
        @NotNull
        private final Path dbTraceFile;

        private H2FilesPaths(@NotNull Path folderPath, @NotNull String dbName) {
            this.dbName = dbName;

            dbDataFile = folderPath.resolve(dbName + ".mv.db");
            dbTraceFile = folderPath.resolve(dbName + ".trace.db");
        }
    }
}
