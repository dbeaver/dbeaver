/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PostgreDatabaseBackupAllHandler
    extends PostgreNativeToolHandler<PostgreBackupAllSettings, DBSObject, PostgreDatabaseBackupAllInfo> {

    @Override
    protected boolean isExportWizard() {
        return true;
    }

    @Override
    public Collection<PostgreDatabaseBackupAllInfo> getRunInfo(PostgreBackupAllSettings settings) {
        return settings.getExportObjects();
    }

    @Override
    protected PostgreBackupAllSettings createTaskSettings(DBRRunnableContext context, DBTTask task) throws DBException {
        PostgreBackupAllSettings settings = new PostgreBackupAllSettings();
        settings.loadSettings(context, new TaskPreferenceStore(task));
        return settings;
    }

    @Override
    protected boolean validateTaskParameters(DBTTask task, PostgreBackupAllSettings settings, Log log) {
        if (PostgreSQLTasks.TASK_DATABASE_BACKUP_ALL.equals(task.getType().getId())) {
            for (PostgreDatabaseBackupAllInfo exportObject : settings.getExportObjects()) {
                final File dir = settings.getOutputFolder(exportObject);
                if (!dir.exists() && !dir.mkdirs()) {
                    log.error("Can't create directory '" + dir.getAbsolutePath() + "'");
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected boolean needsModelRefresh() {
        return false;
    }

    @Override
    public boolean isVerbose() {
        return true;
    }

    @Override
    protected boolean isLogInputStream() {
        return false;
    }

    @Override
    public void fillProcessParameters(
        PostgreBackupAllSettings settings,
        PostgreDatabaseBackupAllInfo arg,
        List<String> cmd
    ) throws IOException {

        super.fillProcessParameters(settings, arg, cmd);

        if (!CommonUtils.isEmpty(settings.getEncoding())) {
            cmd.add("--encoding=" + settings.getEncoding());
        }
        if (settings.isExportOnlyMetadata()) {
            cmd.add("--schema-only");
        }
        if (settings.isExportOnlyGlobals()) {
            cmd.add("--globals-only");
        }
        if (settings.isExportOnlyRoles()) {
            cmd.add("--roles-only");
        }
        if (settings.isExportOnlyTablespaces()) {
            cmd.add("--tablespaces-only");
        }
        if (settings.isNoPrivileges()) {
            cmd.add("--no-privileges");
        }
        if (settings.isNoOwner()) {
            cmd.add("--no-owner");
        }
        if (!settings.isAddRolesPasswords()) {
            // We do not want to dump users passwords by default
            cmd.add("--no-role-passwords");
        }

        cmd.add("--file");
        cmd.add(settings.getOutputFile(arg).getAbsolutePath());

        // Databases
        if (settings.getExportObjects().isEmpty()) {
            // If not specified, the postgres database will be used, and if that does not exist, template1 will be used.
        } else {
            List<PostgreDatabase> includedDatabases = arg.getDatabases();
            if (!CommonUtils.isEmpty(includedDatabases)) {
                final PostgreDataSource dataSource = arg.getDataSource();
                final List<PostgreDatabase> allDatabases = dataSource.getDatabases();
                if (allDatabases.size() != includedDatabases.size()) {
                    // pg_dumpall does not have parameter "include database", only "exclude database".
                    // So we need to create list of excluded databases
                    List<PostgreDatabase> allDatabasesCopy = new ArrayList<>(allDatabases);
                    allDatabasesCopy.removeAll(includedDatabases);
                    for (PostgreDatabase database : allDatabasesCopy) {
                        // Use explicit quotes in case of quoted identifiers (#5950)
                        cmd.add("--exclude-database=" + escapeCLIIdentifier(database.getName()));
                    }
                }
            }
        }
    }

    @Override
    protected List<String> getCommandLine(PostgreBackupAllSettings settings, PostgreDatabaseBackupAllInfo arg) throws IOException {
        List<String> cmd = new ArrayList<>();
        fillProcessParameters(settings, arg, cmd);
        return cmd;
    }
}
