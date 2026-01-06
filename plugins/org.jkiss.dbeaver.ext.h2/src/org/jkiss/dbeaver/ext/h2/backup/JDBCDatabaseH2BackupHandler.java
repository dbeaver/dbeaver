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
package org.jkiss.dbeaver.ext.h2.backup;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.InternalDatabaseConfig;
import org.jkiss.dbeaver.model.sql.backup.JDBCDatabaseBackupHandler;
import org.jkiss.dbeaver.model.sql.backup.SQLBackupConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

public class JDBCDatabaseH2BackupHandler implements JDBCDatabaseBackupHandler {
    private static final Log log = Log.getLog(JDBCDatabaseH2BackupHandler.class);

    @Override
    public void doBackup(
        @NotNull Connection connection,
        int currentSchemaVersion,
        @NotNull InternalDatabaseConfig databaseConfig
    ) throws DBException, IOException {
        Path workspace = DBWorkbench.getPlatform().getWorkspace().getAbsolutePath().resolve(SQLBackupConstants.BACKUP_FOLDER);
        Path backupFile = workspace.resolve(SQLBackupConstants.BACKUP_FILE_NAME + currentSchemaVersion
            + SQLBackupConstants.BACKUP_FILE_TYPE);
        try (Statement statement = connection.createStatement()) {
            if (Files.notExists(backupFile)) {
                Files.createDirectories(workspace);

                String backupCommand = "BACKUP TO '" + backupFile + "'";
                statement.execute(backupCommand);

                log.info("Reserve backup created to path: " + workspace + "backup");
            }
        } catch (Exception e) {
            Files.deleteIfExists(backupFile);
            log.error("Create backup is failed: " + e.getMessage());
            throw new DBException("Backup is failed: " + e.getMessage());
        }
    }
}
