package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
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
        PostgreDatabaseRestoreSettings settings = new PostgreDatabaseRestoreSettings();
        settings.loadSettings(context, new TaskPreferenceStore(task));

        return settings;
    }

    @Override
    protected boolean validateTaskParameters(DBTTask task, PostgreDatabaseRestoreSettings settings, Log log) {
        if (task.getType().getId().equals(PostgreSQLTasks.TASK_DATABASE_BACKUP)) {
            final File dir = settings.getOutputFolder();
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    log.error("Can't create directory '" + dir.getAbsolutePath() + "'");
                    return false;
                }
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
    public void fillProcessParameters(PostgreDatabaseRestoreSettings settings, PostgreDatabaseRestoreInfo arg, List<String> cmd) throws IOException {
        super.fillProcessParameters(settings, arg, cmd);

        if (settings.isCleanFirst()) {
            cmd.add("--clean");
        }
        if (settings.isNoOwner()) {
            cmd.add("--no-owner");
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
        List<DBSObject> databaseObjects = settings.getDatabaseObjects();
        if (!CommonUtils.isEmpty(databaseObjects)) {
            cmd.add("--dbname=" + databaseObjects.get(0).getName());
        }
        if (settings.getFormat() == PostgreBackupRestoreSettings.ExportFormat.DIRECTORY) {
            cmd.add(settings.getInputFile());
        }

        return cmd;
    }

    @Override
    protected boolean isLogInputStream() {
        return true;
    }

    @Override
    protected boolean isMergeProcessStreams() {
        return true;
    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, DBTTask task, PostgreDatabaseRestoreSettings settings, PostgreDatabaseRestoreInfo arg, ProcessBuilder processBuilder, Process process, Log log) throws IOException {
        super.startProcessHandler(monitor, task, settings, arg, processBuilder, process, log);
        if (settings.getFormat() != PostgreBackupRestoreSettings.ExportFormat.DIRECTORY) {
            new BinaryFileTransformerJob(monitor, task, new File(settings.getInputFile()), process.getOutputStream(), log).start();
        }
    }

    @Override
    public void validateErrorCode(int exitCode) throws IOException {
    if (exitCode == 1) {
        DBWorkbench.getPlatformUI().showWarningMessageBox("Warning", "Database restore finished with warnings.\nPlease check the error log to see what is wrong.");
    } else {
        super.validateErrorCode(exitCode);
    }
}

}
