package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PostgreScriptExecuteHandler extends PostgreNativeToolHandler<PostgreScriptExecuteSettings, DBSObject, PostgreDatabase> {

    @Override
    public Collection<PostgreDatabase> getRunInfo(PostgreScriptExecuteSettings settings) {
        return Collections.singletonList(settings.getDatabase());
    }

    @Override
    protected PostgreScriptExecuteSettings createTaskSettings(DBRRunnableContext context, DBTTask task) throws DBException {
        PostgreScriptExecuteSettings settings = new PostgreScriptExecuteSettings();
        settings.loadSettings(context, new TaskPreferenceStore(task));

        return settings;
    }

    @Override
    protected boolean needsModelRefresh() {
        return true;
    }

    @Override
    public boolean isVerbose() {
        return false;
    }

    @Override
    public void fillProcessParameters(PostgreScriptExecuteSettings settings, PostgreDatabase arg, List<String> cmd) throws IOException {
        super.fillProcessParameters(settings, arg, cmd);

        if (arg.getDataSource().isServerVersionAtLeast(9, 5)) {
            cmd.add("--echo-errors"); //$NON-NLS-1$
        }
    }

    @Override
    protected boolean isExportWizard() {
        return false;
    }

    @Override
    protected List<String> getCommandLine(PostgreScriptExecuteSettings settings, PostgreDatabase arg) throws IOException {
        List<String> cmd = new ArrayList<>();
        fillProcessParameters(settings, arg, cmd);

        cmd.add(arg.getName());

        return cmd;
    }

    @Override
    protected boolean isLogInputStream() {
        return true;
    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, DBTTask task, PostgreScriptExecuteSettings settings, PostgreDatabase arg, ProcessBuilder processBuilder, Process process, Log log) throws IOException {
        final File inputFile = new File(settings.getInputFile());
        if (!inputFile.exists()) {
            throw new IOException("File '" + inputFile.getAbsolutePath() + "' doesn't exist");
        }
        super.startProcessHandler(monitor, task, settings, arg, processBuilder, process, log);
        new BinaryFileTransformerJob(monitor, task, inputFile, process.getOutputStream(), log).start();
    }

}
