package org.jkiss.dbeaver.ext.mysql.tasks;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class MySQLScriptExecuteHandler extends MySQLNativeToolHandler<MySQLScriptExecuteSettings, MySQLCatalog, MySQLCatalog> {

    @Override
    public Collection<MySQLCatalog> getRunInfo(MySQLScriptExecuteSettings settings) {
        return settings.getDatabaseObjects();
    }

    @Override
    protected MySQLScriptExecuteSettings createTaskSettings(DBRRunnableContext context, DBTTask task) throws DBException {
        MySQLScriptExecuteSettings settings = new MySQLScriptExecuteSettings();
        boolean isImport = task.getType().getId().equals(MySQLTasks.TASK_DATABASE_RESTORE);
        settings.setImport(isImport);
        settings.loadSettings(context, new TaskPreferenceStore(task));

        return settings;
    }

    @Override
    protected boolean validateTaskParameters(DBTTask task, MySQLScriptExecuteSettings settings, Log log) {
        return true;
    }

    @Override
    protected List<String> getCommandLine(MySQLScriptExecuteSettings settings, MySQLCatalog arg) throws IOException {
        List<String> cmd = getMySQLToolCommandLine(this, settings, arg);
        if (settings.isVerbose()) {
            cmd.add("-v");
        }

        cmd.add(arg.getName());
        return cmd;
    }

    @Override
    public void fillProcessParameters(MySQLScriptExecuteSettings settings, MySQLCatalog arg, List<String> cmd) throws IOException {
        String dumpPath = RuntimeUtils.getNativeClientBinary(settings.getClientHome(), MySQLConstants.BIN_FOLDER, "mysql").getAbsolutePath(); //$NON-NLS-1$
        cmd.add(dumpPath);
        if (settings.getLogLevel() == MySQLScriptExecuteSettings.LogLevel.Debug) {
            cmd.add("--debug-info"); //$NON-NLS-1$
        }
        if (settings.isNoBeep()) {
            cmd.add("--no-beep"); //$NON-NLS-1$
        }
        settings.addExtraCommandArgs(cmd);
    }

    @Override
    protected boolean isMergeProcessStreams() {
        return true;
    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, DBTTask task, MySQLScriptExecuteSettings settings, MySQLCatalog arg, ProcessBuilder processBuilder, Process process, Log log) throws IOException {
        File inputFile = new File(settings.getInputFile());
        if (!inputFile.exists()) {
            throw new IOException("File '" + inputFile.getAbsolutePath() + "' doesn't exist");
        }
        if (settings.isImport()) {
            super.startProcessHandler(monitor, task, settings, arg, processBuilder, process, log);
            new BinaryFileTransformerJob(
                monitor,
                task,
                inputFile,
                process.getOutputStream(), log).start();
        } else {
            super.startProcessHandler(monitor, task, settings, arg, processBuilder, process, log);
            new TextFileTransformerJob(monitor, task, inputFile, process.getOutputStream(), getInputCharset(), getOutputCharset(), log).start();
        }
    }


}
