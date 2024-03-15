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
package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.fs.DBFUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PostgreDatabaseBackupHandler extends PostgreNativeToolHandler<PostgreDatabaseBackupSettings, DBSObject, PostgreDatabaseBackupInfo> {

    private static final Log log = Log.getLog(PostgreDatabaseBackupHandler.class);

    @Override
    public Collection<PostgreDatabaseBackupInfo> getRunInfo(PostgreDatabaseBackupSettings settings) {
        return settings.getExportObjects();
    }

    @Override
    protected PostgreDatabaseBackupSettings createTaskSettings(DBRRunnableContext context, DBTTask task) throws DBException {
        PostgreDatabaseBackupSettings settings = new PostgreDatabaseBackupSettings(task.getProject());
        settings.loadSettings(context, new TaskPreferenceStore(task));

        return settings;
    }

    @Override
    protected boolean validateTaskParameters(DBTTask task, PostgreDatabaseBackupSettings settings, Log log) {
        if (task.getType().getId().equals(PostgreSQLTasks.TASK_DATABASE_BACKUP)) {
            for (PostgreDatabaseBackupInfo exportObject : settings.getExportObjects()) {
                final String dir = settings.getOutputFolder(exportObject);
                try {
                    Path outputFolderPath = DBFUtils.resolvePathFromString(new VoidProgressMonitor(), task.getProject(), dir);
                    if (!Files.exists(outputFolderPath)) {
                        Files.createDirectories(outputFolderPath);
                    }
                }
                catch (Exception e) {
                    log.error("Can't create directory '" + dir + "'", e);
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
    protected boolean isExportWizard() {
        return true;
    }

    @Override
    public void fillProcessParameters(
        PostgreDatabaseBackupSettings settings,
        PostgreDatabaseBackupInfo arg,
        List<String> cmd
    ) throws IOException {
        super.fillProcessParameters(settings, arg, cmd);

        cmd.add("--format=" + settings.getFormat().getId());
        if (!CommonUtils.isEmpty(settings.getCompression())) {
            cmd.add("--compress=" + settings.getCompression());
        }
        if (!CommonUtils.isEmpty(settings.getEncoding())) {
            cmd.add("--encoding=" + settings.getEncoding());
        }
        if (settings.isUseInserts()) {
            cmd.add("--inserts");
        }
        if (settings.isNoPrivileges()) {
            cmd.add("--no-privileges");
        }
        if (settings.isNoOwner()) {
            cmd.add("--no-owner");
        }
        if (settings.isDropObjects()) {
            cmd.add("--clean");
        }
        if (settings.isCreateDatabase()) {
            cmd.add("--create");
        }

        if (!isUseStreamTransfer(settings.getOutputFile(arg)) ||
            settings.getFormat() == PostgreBackupRestoreSettings.ExportFormat.DIRECTORY
        ) {
            cmd.add("--file");
            cmd.add(settings.getOutputFile(arg));
        }

        // Objects
        if (settings.getExportObjects().isEmpty()) {
            log.debug("Can't find specific schemas/tables for the backup");
            return;
        }
        if (!CommonUtils.isEmpty(arg.getTables())) {
            for (PostgreTableBase table : arg.getTables()) {
                cmd.add("-t");
                // Use explicit quotes in case of quoted identifiers (#5950)
                cmd.add(escapeCLIIdentifier(table.getFullyQualifiedName(DBPEvaluationContext.DDL)));
            }
        } else if (!CommonUtils.isEmpty(arg.getSchemas())) {
            for (PostgreSchema schema : arg.getSchemas()) {
                cmd.add("-n");
                // Use explicit quotes in case of quoted identifiers (#5950)
                cmd.add(escapeCLIIdentifier(DBUtils.getQuotedIdentifier(schema)));
            }
        }
    }

    @Override
    protected List<String> getCommandLine(PostgreDatabaseBackupSettings settings, PostgreDatabaseBackupInfo arg) throws IOException {
        List<String> cmd = new ArrayList<>();
        fillProcessParameters(settings, arg, cmd);
        cmd.add(arg.getDatabase().getName());

        return cmd;
    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, DBTTask task, PostgreDatabaseBackupSettings settings, PostgreDatabaseBackupInfo arg, ProcessBuilder processBuilder, Process process, Log log) throws IOException, DBException {
        super.startProcessHandler(monitor, task, settings, arg, processBuilder, process, log);
        String outFileName = settings.getOutputFile(arg);
        if (isUseStreamTransfer(outFileName) && settings.getFormat() != PostgreBackupRestoreSettings.ExportFormat.DIRECTORY) {
            Path outFile = DBFUtils.resolvePathFromString(monitor, task.getProject(), outFileName);
            DumpCopierJob job = new DumpCopierJob(monitor, "Export database", process.getInputStream(), outFile, log);
            job.start();
        }
    }
}
