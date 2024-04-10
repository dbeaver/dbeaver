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
import org.jkiss.dbeaver.model.sql.backup.BackupConstant;
import org.jkiss.dbeaver.model.sql.backup.BackupDatabase;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

public class PostgresDatabaseBackup implements BackupDatabase {

    private static final Log log = Log.getLog(PostgresDatabaseBackup.class);

    @Override
    public void doBackup(
            @NotNull Connection connection,
            int currentSchemaVersion,
            @NotNull InternalDatabaseConfig databaseConfig
    ) throws DBException {
        try {
            Path workspace = DBWorkbench.getPlatform().getWorkspace().getAbsolutePath().resolve(BackupConstant.BACKUP_FOLDER);
            Path backupFile = workspace.resolve(BackupConstant.BACKUP_FILE_NAME + databaseConfig.getSchema()
                    + currentSchemaVersion + BackupConstant.BACKUP_FILE_TYPE);
            URI uri = new URI(databaseConfig.getUrl().replace("jdbc:", ""));
            if (Files.notExists(backupFile)) {
                Files.createDirectories(workspace);
                String backupCommand = String.format("pg_dump -h %s -p %d -U %s -F c -b -v -f %s --schema=%s %s",
                        uri.getHost(),
                        uri.getPort(),
                        databaseConfig.getUser(),
                        backupFile.toAbsolutePath(),
                        databaseConfig.getSchema(),
                        uri.getPath().replace("/", ""));
                Process process = Runtime.getRuntime().exec(backupCommand);
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    log.info("Postgres backup successful");
                } else {
                    log.error("Postgres backup failed");
                    throw new DBException("Postgres backup failed");
                }
            }
        } catch (Exception e) {
            log.error("Create backup is failed: " + e.getMessage());
            throw new DBException("Create backup is failed: " + e.getMessage());
        }
    }
}
