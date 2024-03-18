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

import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.fs.DBFUtils;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PostgreDatabaseRestoreHandler extends PostgreNativeToolHandler<PostgreDatabaseRestoreSettings, DBSObject, PostgreDatabaseRestoreInfo> {

    @Override
    public Collection<PostgreDatabaseRestoreInfo> getRunInfo(PostgreDatabaseRestoreSettings settings) {
        return Collections.singletonList(settings.getRestoreInfo());
    }

    @Override
    protected PostgreDatabaseRestoreSettings createTaskSettings(DBRRunnableContext context, DBTTask task) throws DBException {
        PostgreDatabaseRestoreSettings settings = new PostgreDatabaseRestoreSettings(task.getProject());
        settings.loadSettings(context, new TaskPreferenceStore(task));

        return settings;
    }

    @Override
    protected boolean validateTaskParameters(DBTTask task, PostgreDatabaseRestoreSettings settings, Log log) {
        if (task.getType().getId().equals(PostgreSQLTasks.TASK_DATABASE_BACKUP)) {
            final File dir = new File(settings.getOutputFilePattern());
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    log.error("Can't create directory '" + dir.getAbsolutePath() + "'");
                    return false;
                }
            }
        } else if (task.getType().getId().equals(PostgreSQLTasks.TASK_DATABASE_RESTORE)) {
            DBPDataSource dataSource = settings.getDataSourceContainer().getDataSource();
            if (dataSource != null && DBUtils.isReadOnly(settings.getDataSourceContainer().getDataSource())) {
                log.error(NLS.bind(ModelMessages.tasks_restore_readonly_message, dataSource.getName()));
                return false; 
            }
        }
        return true;
    }

    @Override
    protected boolean needsModelRefresh() {
        return true;
    }

    @Override
    public boolean isVerbose() {
        return true;
    }

    @Override
    public void fillProcessParameters(
        PostgreDatabaseRestoreSettings settings,
        PostgreDatabaseRestoreInfo arg,
        List<String> cmd
    ) throws IOException {
        super.fillProcessParameters(settings, arg, cmd);

        if (settings.isCleanFirst()) {
            cmd.add("--clean");
        }
        if (settings.isNoOwner()) {
            cmd.add("--no-owner");
        }
        if (settings.isCreateDatabase()) {
            cmd.add("--create");
        }
    }

    @Override
    protected boolean isExportWizard() {
        return false;
    }

    @Override
    protected List<String> getCommandLine(PostgreDatabaseRestoreSettings settings, PostgreDatabaseRestoreInfo arg) throws IOException {
        List<String> cmd = new ArrayList<>();
        fillProcessParameters(settings, arg, cmd);

        if (settings.getFormat() != PostgreBackupRestoreSettings.ExportFormat.PLAIN) {
            cmd.add("--format=" + settings.getFormat().getId());
        }
        cmd.add("--dbname=" + settings.getRestoreInfo().getDatabase()); // database name here can be used without quotes
        if (!isUseStreamTransfer(settings.getInputFile()) ||
            settings.getFormat() == PostgreBackupRestoreSettings.ExportFormat.DIRECTORY
        ) {
            cmd.add(settings.getInputFile());
        }

        return cmd;
    }

    @Override
    protected boolean isLogInputStream() {
        return false;
    }

    @Override
    protected boolean isMergeProcessStreams() {
        return false;
    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, DBTTask task, PostgreDatabaseRestoreSettings settings, PostgreDatabaseRestoreInfo arg, ProcessBuilder processBuilder, Process process, Log log) throws IOException, DBException {
        final Path inputFile = DBFUtils.resolvePathFromString(monitor, task.getProject(), settings.getInputFile());
        if (!Files.exists(inputFile)) {
            throw new IOException("File '" + inputFile + "' doesn't exist");
        }
        super.startProcessHandler(monitor, task, settings, arg, processBuilder, process, log);
        if (isUseStreamTransfer(inputFile.toString()) && settings.getFormat() != PostgreBackupRestoreSettings.ExportFormat.DIRECTORY) {
            new BinaryFileTransformerJob(monitor, task, inputFile, process.getOutputStream(), log).start();
        }
    }

    @Override
    public void validateErrorCode(int exitCode) throws IOException {
    if (exitCode == 1) {
        DBWorkbench.getPlatformUI().showWarningNotification("Warning", "Database restore finished with warnings.\nPlease check the error log to see what is wrong.");
    } else {
        super.validateErrorCode(exitCode);
    }
}

}
