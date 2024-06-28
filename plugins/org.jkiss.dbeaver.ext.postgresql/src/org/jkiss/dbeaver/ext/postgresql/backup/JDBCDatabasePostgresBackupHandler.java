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
package org.jkiss.dbeaver.ext.postgresql.backup;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.InternalDatabaseConfig;
import org.jkiss.dbeaver.model.sql.backup.JDBCDatabaseBackupHandler;
import org.jkiss.dbeaver.model.sql.backup.SQLBackupConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

public class JDBCDatabasePostgresBackupHandler implements JDBCDatabaseBackupHandler {

    private static final Log log = Log.getLog(JDBCDatabasePostgresBackupHandler.class);

    @Override
    public void doBackup(
        @NotNull Connection connection,
        int currentSchemaVersion,
        @NotNull InternalDatabaseConfig databaseConfig
    ) throws DBException {
        try {
            URI uri = new URI(databaseConfig.getUrl().replace("jdbc:", ""));
            Path workspace = DBWorkbench.getPlatform().getWorkspace().getAbsolutePath().resolve(SQLBackupConstants.BACKUP_FOLDER);
            Path backupFile = workspace.resolve(uri.getPath().replace("/", "") + "_"
                + SQLBackupConstants.BACKUP_FILE_NAME + databaseConfig.getSchema()
                + currentSchemaVersion + SQLBackupConstants.BACKUP_FILE_TYPE);
            if (Files.notExists(backupFile)) {
                Files.createDirectories(workspace);
                ProcessBuilder processBuilder = getBuilder(databaseConfig, uri, backupFile);
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();

                try (InputStream inputStream = process.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

                    StringBuilder processOutput = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        processOutput.append(line).append("\n");
                    }

                    int exitCode = process.waitFor();

                    if (exitCode == 0) {
                        log.info("Postgres backup successful");
                    } else {
                        Files.deleteIfExists(backupFile);
                        log.error("Postgres backup failed with output: " + processOutput.toString());
                        throw new DBException("Postgres backup failed");
                    }
                } catch (IOException e) {
                    log.error("Error reading process output", e);
                }
            }
        } catch (Exception e) {
            log.error("Create backup is failed: " + e.getMessage());
            throw new DBException("Create backup is failed: " + e.getMessage());
        }
    }

    private static ProcessBuilder getBuilder(@NotNull InternalDatabaseConfig databaseConfig, URI uri, Path backupFile) {
        String databaseName = uri.getPath();
        if (databaseName != null) {
            if (databaseName.startsWith("/")) {
                databaseName = databaseName.substring(1);
            }
            int questionMarkIndex = databaseName.indexOf("?");
            if (questionMarkIndex != -1) {
                databaseName = databaseName.substring(0, questionMarkIndex);
            }
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
            "pg_dump",
            databaseName,
            "--host", uri.getHost(),
            "--port", String.valueOf(uri.getPort()),
            "--blobs",
            "--verbose",
            "--file", backupFile.toAbsolutePath().toString()
        );

        if (CommonUtils.isNotEmpty(databaseConfig.getSchema())) {
            processBuilder.command().add("--schema");
            processBuilder.command().add(databaseConfig.getSchema());
        }

        String backupCommand = String.join(" ", processBuilder.command());
        log.info("Command started: " + backupCommand);

        processBuilder.command().add("--username");
        processBuilder.command().add(databaseConfig.getUser());

        if (CommonUtils.isNotEmpty(databaseConfig.getPassword())) {
            processBuilder.environment().put("PGPASSWORD", databaseConfig.getPassword());
        }
        return processBuilder;
    }
}
