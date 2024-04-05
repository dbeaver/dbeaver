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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.backup.BackupDatabase;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

public class PostgresDatabaseBackup implements BackupDatabase {

    private static final Log log = Log.getLog(PostgresDatabaseBackup.class);

    @Override
    public void doBackup(Connection connection, int currentSchemaVersion) {
        try {
            Path workspace = DBWorkbench.getPlatform().getWorkspace().getAbsolutePath().resolve("backup");
            Path backupFile = workspace.resolve("backupVersion" + currentSchemaVersion + ".zip");

            if (Files.notExists(backupFile)) {
                Files.createDirectories(workspace);
                Statement statement = connection.createStatement();

                String backupCommand = "pg_dump -h localhost -f " + backupFile + " postgres";
                ProcessBuilder processBuilder = new ProcessBuilder(backupCommand.split(" "));
                Process process = processBuilder.start();
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    log.info("Postgres backup successful");
                } else {
                    log.error("Postgres backup failed");
                }

                statement.close();
            }
        } catch (Exception e) {
            log.error("Create backup is failed: " + e.getMessage());
        }
    }
}
