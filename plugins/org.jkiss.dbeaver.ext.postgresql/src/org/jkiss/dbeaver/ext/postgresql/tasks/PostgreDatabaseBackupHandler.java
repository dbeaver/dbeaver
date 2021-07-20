package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PostgreDatabaseBackupHandler extends PostgreNativeToolHandler<PostgreDatabaseBackupSettings, DBSObject, PostgreDatabaseBackupInfo> {
    @Override
    public Collection<PostgreDatabaseBackupInfo> getRunInfo(PostgreDatabaseBackupSettings settings) {
        return settings.getExportObjects();
    }

    @Override
    protected PostgreDatabaseBackupSettings createTaskSettings(DBRRunnableContext context, DBTTask task) throws DBException {
        PostgreDatabaseBackupSettings settings = new PostgreDatabaseBackupSettings();
        settings.loadSettings(context, new TaskPreferenceStore(task));

        return settings;
    }

    @Override
    protected boolean validateTaskParameters(DBTTask task, PostgreDatabaseBackupSettings settings, Log log) {
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
    public void fillProcessParameters(PostgreDatabaseBackupSettings settings, PostgreDatabaseBackupInfo arg, List<String> cmd) throws IOException {
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

        if (!USE_STREAM_MONITOR || settings.getFormat() == PostgreBackupRestoreSettings.ExportFormat.DIRECTORY) {
            cmd.add("--file");
            cmd.add(settings.getOutputFile(arg).getAbsolutePath());
        }

        // Objects
        if (settings.getExportObjects().isEmpty()) {
            // no dump
        } else if (!CommonUtils.isEmpty(arg.getTables())) {
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

    private static String escapeCLIIdentifier(String name) {
        if (RuntimeUtils.isWindows()) {
            // On Windows it is simple
            return "\"" + name.replace("\"", "\\\"") + "\"";
        } else {
            // On Unixes it is more tricky (https://unix.stackexchange.com/questions/30903/how-to-escape-quotes-in-shell)
            //return "\"" + name.replace("\"", "\"\\\"\"") + "\"";
            return name;
            //return "\"" + name.replace("\"", "\\\"") + "\"";
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
    protected void startProcessHandler(DBRProgressMonitor monitor, DBTTask task, PostgreDatabaseBackupSettings settings, PostgreDatabaseBackupInfo arg, ProcessBuilder processBuilder, Process process, Log log) throws IOException {
        super.startProcessHandler(monitor, task, settings, arg, processBuilder, process, log);
        if (USE_STREAM_MONITOR && settings.getFormat() != PostgreBackupRestoreSettings.ExportFormat.DIRECTORY) {
            File outFile = settings.getOutputFile(arg);
            DumpCopierJob job = new DumpCopierJob(monitor, "Export database", process.getInputStream(), outFile, log);
            job.start();
        }
    }
}
