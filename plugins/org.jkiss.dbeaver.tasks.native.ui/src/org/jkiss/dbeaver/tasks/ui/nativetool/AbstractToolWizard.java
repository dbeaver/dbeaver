/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.tasks.ui.nativetool;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskHandler;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ProgressStreamReader;
import org.jkiss.dbeaver.tasks.nativetool.AbstractNativeToolSettings;
import org.jkiss.dbeaver.tasks.ui.nativetool.internal.TaskNativeUIMessages;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskWizardExecutor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Abstract wizard
 */
public abstract class AbstractToolWizard<SETTINGS extends AbstractNativeToolSettings<BASE_OBJECT>, BASE_OBJECT extends DBSObject, PROCESS_ARG>
    extends TaskConfigurationWizard {

    private static final Log log = Log.getLog(AbstractToolWizard.class);

    private final DBPPreferenceStore preferenceStore;
    private final SETTINGS settings;

    protected String taskTitle;
    protected final ToolWizardPageLog logPage;
    private boolean finished;
    protected boolean transferFinished;
    private boolean refreshObjects;
    private boolean isSuccess;
    private String errorMessage;

    protected AbstractToolWizard(@NotNull Collection<BASE_OBJECT> databaseObjects, @NotNull String taskTitle) {
        this.preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        this.settings = createSettings();
        this.settings.getDatabaseObjects().addAll(databaseObjects);
        this.taskTitle = taskTitle;
        this.logPage = new ToolWizardPageLog(taskTitle);
    }

    public AbstractToolWizard(@NotNull DBTTask task) {
        super(task);
        this.preferenceStore = new TaskPreferenceStore(task);
        this.settings = createSettings();
        this.taskTitle = task.getType().getName();
        this.logPage = new ToolWizardPageLog(taskTitle);
    }

    protected abstract SETTINGS createSettings();

    public SETTINGS getSettings() {
        return settings;
    }

    @NotNull
    protected DBPPreferenceStore getPreferenceStore() {
        return preferenceStore;
    }

    public DBPProject getProject() {
        if (settings.getDataSourceContainer() != null) {
            return settings.getDataSourceContainer().getProject();
        }
        return super.getProject();
    }

    @Override
    protected String getDefaultWindowTitle() {
        return taskTitle;
    }

    @Override
    public boolean canFinish() {
        if (!super.canFinish()) {
            return false;
        }
        if (isSingleTimeWizard()) {
            return !finished;
        }
        // [#2917] Finish button is always enabled (!finished && super.canFinish())
        return true;
    }

    /**
     * @return true if this wizard can be executed only once
     */
    protected boolean isSingleTimeWizard() {
        return false;
    }

    protected boolean needsModelRefresh() {
        return true;
    }

    public List<BASE_OBJECT> getDatabaseObjects() {
        return settings.getDatabaseObjects();
    }

    public DBPConnectionConfiguration getConnectionInfo() {
        DBPDataSourceContainer ds = settings.getDataSourceContainer();
        if (ds != null) {
            return ds.getConnectionConfiguration();
        }
        return null;
    }

    public DBPNativeClientLocation getClientHome() {
        return settings.getClientHome();
    }

    public String getToolUserName() {
        return settings.getToolUserName();
    }

    public void setToolUserName(String toolUserName) {
        this.settings.setToolUserName(toolUserName);
    }

    public String getToolUserPassword() {
        return settings.getToolUserPassword();
    }

    public void setToolUserPassword(String toolUserPassword) {
        this.settings.setToolUserPassword(toolUserPassword);
    }

    public String getExtraCommandArgs() {
        return settings.getExtraCommandArgs();
    }

    public DBPDataSourceContainer getDataSourceContainer() {
        return settings.getDataSourceContainer();
    }

    public DBPNativeClientLocation findNativeClientHome(String clientHomeId) {
        return null;
    }

    public abstract Collection<PROCESS_ARG> getRunInfo();

    public Collection<BASE_OBJECT> getUpdatedObjects(PROCESS_ARG settings) {
        return Collections.emptyList();
    }

    @Override
    public void createPageControls(Composite pageContainer) {
        settings.loadSettings(getRunnableContext(), getPreferenceStore());

        super.createPageControls(pageContainer);

        updateErrorMessage();
    }

    public void updateErrorMessage() {
        WizardPage currentPage = (WizardPage) getStartingPage();

        if (isNativeClientHomeRequired()) {
            String clientHomeId = getDataSourceContainer().getConnectionConfiguration().getClientHomeId();
            List<DBPNativeClientLocation> nativeClientLocations = getDataSourceContainer().getDriver().getNativeClientLocations();
            if (clientHomeId == null) {
                if (nativeClientLocations != null && !nativeClientLocations.isEmpty()) {
                    settings.setClientHome(nativeClientLocations.get(0));
                } else {
                    settings.setClientHome(null);
                }
                if (settings.getClientHome() == null) {
                    currentPage.setErrorMessage(TaskNativeUIMessages.tools_wizard_message_no_client_home);
                    getContainer().updateMessage();
                    return;
                }
            } else {
                DBPNativeClientLocation clientHome = DBUtils.findObject(nativeClientLocations, clientHomeId);
                if (clientHome == null) {
                    clientHome = findNativeClientHome(clientHomeId);
                }
                settings.setClientHome(clientHome);
            }
            if (settings.getClientHome() == null) {
                currentPage.setErrorMessage(NLS.bind(TaskNativeUIMessages.tools_wizard_message_client_home_not_found, clientHomeId));
            } else {
                currentPage.setErrorMessage(null);
            }
            getContainer().updateMessage();
        }
    }

    private boolean validateClientFiles() {
        if (!isNativeClientHomeRequired() || settings.getClientHome() == null) {
            return true;
        }
        try {
            UIUtils.run(getContainer(), true, true, monitor -> {
                try {
                    settings.getClientHome().validateFilesPresence(monitor);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Download native client file(s)", "Error downloading client file(s)", e.getTargetException());
            ((WizardPage) getContainer().getCurrentPage()).setErrorMessage("Error downloading native client file(s)");
            getContainer().updateMessage();
            return false;
        } catch (InterruptedException e) {
            // ignore
            return false;
        }
        return true;
    }

    @Override
    public boolean performFinish() {
        // Save settings
        settings.saveSettings(getRunnableContext(), getPreferenceStore());

        if (!validateClientFiles()) {
            return false;
        }

        if (isRunTaskOnFinish()) {
            return super.performFinish();
        }

        if (getContainer().getCurrentPage() != logPage) {
            getContainer().showPage(logPage);
        }

        if (getCurrentTask() == null) {
            try {
                DBTTaskHandler handler = null;
                try {
                    handler = getTaskType().createHandler();
                } catch (Throwable e) {
                    // No handler
                }
                if (handler != null) {
                    // Execute directly - without task serialize/deserialize
                    // We need it because some data producers cannot be serialized properly (e.g. ResultSetDatacontainer - see #7342)
                    DBTTask temporaryTask = getProject().getTaskManager().createTemporaryTask(getTaskType(), getTaskType().getName());
                    saveConfigurationToTask(temporaryTask);
                    TaskWizardExecutor executor = new TaskWizardExecutor(getRunnableContext(), temporaryTask, log, logPage.getLogWriter());
                    executor.executeTask();
                    return false;
                }
            } catch (Exception e) {
                DBWorkbench.getPlatformUI().showError(e.getMessage(), "Error running task", e);
                return false;
            }
        }

        long startTime = System.currentTimeMillis();
        try {
            UIUtils.run(getContainer(), true, true, new ToolRunner());
        } catch (InterruptedException ex) {
            UIUtils.showMessageBox(getShell(), taskTitle, NLS.bind(TaskNativeUIMessages.tools_wizard_error_task_canceled, taskTitle, getObjectsName()), SWT.ICON_ERROR);
            return false;
        } catch (InvocationTargetException ex) {
            DBWorkbench.getPlatformUI().showError(
                NLS.bind(TaskNativeUIMessages.tools_wizard_error_task_error_title, taskTitle),
                TaskNativeUIMessages.tools_wizard_error_task_error_message + taskTitle,
                ex.getTargetException());
            return false;
        } finally {
            getContainer().updateButtons();
        }
        long workTime = System.currentTimeMillis() - startTime;
        notifyToolFinish(taskTitle + " finished", workTime);
        if (isSuccess) {
            onSuccess(workTime);
        } else {
            onError();
        }
        return false;
    }

    protected void notifyToolFinish(String toolName, long workTime) {
        // Make a sound
        Display.getCurrent().beep();
        // Notify agent
        if (workTime > DBWorkbench.getPlatformUI().getLongOperationTimeout() * 1000) {
            DBWorkbench.getPlatformUI().notifyAgent(toolName, IStatus.INFO);
        }
    }

    public String getObjectsName() {
        StringBuilder str = new StringBuilder();
        for (BASE_OBJECT object : settings.getDatabaseObjects()) {
            if (str.length() > 0) str.append(",");
            str.append(object.getName());
        }
        return str.toString();
    }

    public boolean executeProcess(DBRProgressMonitor monitor, PROCESS_ARG arg)
        throws IOException, CoreException, InterruptedException {
        monitor.beginTask(getWindowTitle(), 1);
        try {
            final List<String> commandLine = getCommandLine(arg);
            final File execPath = new File(commandLine.get(0));

            ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
            processBuilder.directory(execPath.getParentFile());
            if (this.isMergeProcessStreams()) {
                processBuilder.redirectErrorStream(true);
            }
            setupProcessParameters(processBuilder);
            Process process = processBuilder.start();

            startProcessHandler(monitor, arg, processBuilder, process);

            Thread.sleep(100);

            for (; ; ) {
                Thread.sleep(100);
                if (monitor.isCanceled()) {
                    process.destroy();
                }
                try {
                    final int exitCode = process.exitValue();
                    if (exitCode != 0) {
                        errorMessage = NLS.bind(TaskNativeUIMessages.tools_wizard_log_process_exit_code, exitCode);
                        logPage.appendLog(errorMessage + "\n", true);
                        return false;
                    }
                } catch (IllegalThreadStateException e) {
                    // Still running
                    continue;
                }
                break;
            }
            //process.waitFor();
        } catch (IOException e) {
            monitor.done();
            log.error(e);
            logPage.appendLog(NLS.bind(TaskNativeUIMessages.tools_wizard_log_io_error, e.getMessage()) + "\n", true);
            return false;
        }

        return true;
    }

    protected boolean isNativeClientHomeRequired() {
        return true;
    }

    protected boolean isMergeProcessStreams() {
        return false;
    }

    public boolean isVerbose() {
        return false;
    }

    protected void onSuccess(long workTime) {

    }

    protected void onError() {
        UIUtils.showMessageBox(
            getShell(),
            taskTitle,
            errorMessage == null ? "Internal error" : errorMessage,
            SWT.ICON_ERROR);
    }

    abstract protected java.util.List<String> getCommandLine(PROCESS_ARG arg) throws IOException;

    public abstract void fillProcessParameters(List<String> cmd, PROCESS_ARG arg) throws IOException;

    protected void setupProcessParameters(ProcessBuilder process) {
    }

    protected abstract void startProcessHandler(DBRProgressMonitor monitor, PROCESS_ARG arg, ProcessBuilder processBuilder, Process process);

    public boolean isSecureString(String string) {
        String password = getToolUserPassword();
        return !CommonUtils.isEmpty(password) && string.contains(password);
    }

    public abstract class DumpJob extends Thread {
        protected DBRProgressMonitor monitor;
        protected InputStream input;
        protected File outFile;

        protected DumpJob(String name, DBRProgressMonitor monitor, InputStream stream, File outFile) {
            super(name);
            this.monitor = monitor;
            this.input = stream;
            this.outFile = outFile;
        }

        @Override
        public final void run() {
            try {
                runDump();
            } catch (IOException e) {
                logPage.appendLog(e.getMessage());
            }
        }

        protected abstract void runDump()
            throws IOException;
    }

    public class DumpCopierJob extends DumpJob {
        public DumpCopierJob(DBRProgressMonitor monitor, String name, InputStream stream, File outFile) {
            super(name, monitor, stream, outFile);
        }

        @Override
        public void runDump() throws IOException {
            monitor.beginTask(getName(), 100);
            long totalBytesDumped = 0;
            long prevStatusUpdateTime = 0;
            byte[] buffer = new byte[10000];
            try {
                NumberFormat numberFormat = NumberFormat.getInstance();

                try (OutputStream output = new FileOutputStream(outFile)) {
                    for (; ; ) {
                        int count = input.read(buffer);
                        if (count <= 0) {
                            break;
                        }
                        totalBytesDumped += count;
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - prevStatusUpdateTime > 300) {
                            monitor.subTask(numberFormat.format(totalBytesDumped) + " bytes");
                            prevStatusUpdateTime = currentTime;
                        }
                        output.write(buffer, 0, count);
                    }
                    output.flush();
                }
            } finally {
                monitor.done();
            }
        }
    }

    public class TextFileTransformerJob extends Thread {
        private DBRProgressMonitor monitor;
        private OutputStream output;
        private File inputFile;
        private String inputCharset;
        private String outputCharset;

        public TextFileTransformerJob(DBRProgressMonitor monitor, File inputFile, OutputStream stream, String inputCharset, String outputCharset) {
            super(taskTitle);
            this.monitor = monitor;
            this.output = stream;
            this.inputFile = inputFile;
            this.inputCharset = inputCharset;
            this.outputCharset = outputCharset;
        }

        @Override
        public void run() {
            try {
                try (InputStream scriptStream = new ProgressStreamReader(
                    monitor,
                    taskTitle,
                    new FileInputStream(inputFile),
                    inputFile.length())) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(scriptStream, inputCharset));
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, outputCharset));
                    while (!monitor.isCanceled()) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        writer.println(line);
                        writer.flush();
                    }
                    output.flush();
                } finally {
                    IOUtils.close(output);
                }
            } catch (IOException e) {
                log.debug(e);
                logPage.appendLog(e.getMessage());
            } finally {
                monitor.done();
                transferFinished = true;
            }
        }
    }

    public class BinaryFileTransformerJob extends Thread {
        private DBRProgressMonitor monitor;
        private OutputStream output;
        private File inputFile;

        public BinaryFileTransformerJob(DBRProgressMonitor monitor, File inputFile, OutputStream stream) {
            super(taskTitle);
            this.monitor = monitor;
            this.output = stream;
            this.inputFile = inputFile;
        }

        @Override
        public void run() {
            try (InputStream scriptStream = new ProgressStreamReader(
                monitor,
                taskTitle,
                new FileInputStream(inputFile),
                inputFile.length())) {
                byte[] buffer = new byte[100000];
                while (!monitor.isCanceled()) {
                    int readSize = scriptStream.read(buffer);
                    if (readSize < 0) {
                        break;
                    }
                    output.write(buffer, 0, readSize);
                    output.flush();
                }
                output.flush();
            } catch (IOException e) {
                log.debug(e);
                logPage.appendLog(e.getMessage() + "\n");
            } finally {
                try {
                    output.close();
                } catch (IOException e) {
                    log.error(e);
                }
                monitor.done();
                transferFinished = true;
            }
        }
    }

    private class ToolRunner implements DBRRunnableWithProgress {
        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            try {
                isSuccess = true;
                for (PROCESS_ARG arg : getRunInfo()) {
                    if (monitor.isCanceled()) break;
                    if (!executeProcess(monitor, arg)) {
                        isSuccess = false;
                    }
                }
                refreshObjects = isSuccess && !monitor.isCanceled();
                if (refreshObjects && needsModelRefresh()) {
                    // Refresh navigator node (script execution can change everything inside)
                    for (BASE_OBJECT object : settings.getDatabaseObjects()) {
                        final DBNDatabaseNode node = settings.getDataSourceContainer().getPlatform().getNavigatorModel().findNode(object);
                        if (node != null) {
                            node.refreshNode(monitor, AbstractToolWizard.this);
                        }
                    }
                }
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            } finally {
                finished = true;
            }
            if (monitor.isCanceled()) {
                throw new InterruptedException();
            }
        }
    }

}
