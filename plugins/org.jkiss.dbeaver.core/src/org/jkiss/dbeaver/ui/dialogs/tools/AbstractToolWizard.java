/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.tools;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPClientHome;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Abstract wizard
 */
public abstract class AbstractToolWizard<BASE_OBJECT extends DBSObject, PROCESS_ARG>
        extends Wizard implements DBRRunnableWithProgress {

    private static final Log log = Log.getLog(AbstractToolWizard.class);

    private final String PROP_NAME_EXTRA_ARGS = "tools.wizard." + getClass().getSimpleName() + ".extraArgs";

    private final List<BASE_OBJECT> databaseObjects;
    private DBPClientHome clientHome;
    private DBPDataSourceContainer dataSourceContainer;
    private DBPConnectionConfiguration connectionInfo;
    private String toolUserName;
    private String toolUserPassword;
    private String extraCommandArgs;

    protected String task;
    protected final DatabaseWizardPageLog logPage;
    private boolean finished;

    protected AbstractToolWizard(Collection<BASE_OBJECT> databaseObjects, String task)
    {
        this.databaseObjects = new ArrayList<>(databaseObjects);
        this.task = task;
        this.logPage = new DatabaseWizardPageLog(task);

        if (databaseObjects.isEmpty()) {
            throw new IllegalArgumentException("Empty object list");
        }
        for (BASE_OBJECT object : databaseObjects) {
            if (dataSourceContainer != null && dataSourceContainer != object.getDataSource().getContainer()) {
                throw new IllegalArgumentException("Objects from different data sources");
            }
            dataSourceContainer = object.getDataSource().getContainer();
            connectionInfo = dataSourceContainer.getActualConnectionConfiguration();
        }

        final DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        extraCommandArgs = store.getString(PROP_NAME_EXTRA_ARGS);
    }

    @Override
    public boolean canFinish()
    {
        return !finished && super.canFinish();
    }

    public List<BASE_OBJECT> getDatabaseObjects()
    {
        return databaseObjects;
    }

    public DBPConnectionConfiguration getConnectionInfo()
    {
        return connectionInfo;
    }

    public DBPClientHome getClientHome()
    {
        return clientHome;
    }

    public String getToolUserName()
    {
        return toolUserName;
    }

    public void setToolUserName(String toolUserName)
    {
        this.toolUserName = toolUserName;
    }

    public String getToolUserPassword()
    {
        return toolUserPassword;
    }

    public void setToolUserPassword(String toolUserPassword)
    {
        this.toolUserPassword = toolUserPassword;
    }

    public String getExtraCommandArgs() {
        return extraCommandArgs;
    }

    public void setExtraCommandArgs(String extraCommandArgs) {
        this.extraCommandArgs = extraCommandArgs;
    }

    protected void addExtraCommandArgs(List<String> cmd) {
        if (!CommonUtils.isEmptyTrimmed(extraCommandArgs)) {
            Collections.addAll(cmd, extraCommandArgs.split(" "));
        }
    }

    public abstract DBPClientHome findServerHome(String clientHomeId);

    public abstract Collection<PROCESS_ARG> getRunInfo();

    @Override
    public void createPageControls(Composite pageContainer)
    {
        super.createPageControls(pageContainer);

        WizardPage currentPage = (WizardPage) getStartingPage();

        String clientHomeId = connectionInfo.getClientHomeId();
        if (clientHomeId == null) {
            currentPage.setErrorMessage(CoreMessages.tools_wizard_message_no_client_home);
            getContainer().updateMessage();
            return;
        }
        clientHome = findServerHome(clientHomeId);//MySQLDataSourceProvider.getServerHome(clientHomeId);
        if (clientHome == null) {
            currentPage.setErrorMessage(NLS.bind(CoreMessages.tools_wizard_message_client_home_not_found, clientHomeId));
            getContainer().updateMessage();
        }
    }

    @Override
    public boolean performFinish() {
        // Save settings
        final DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        store.setValue(PROP_NAME_EXTRA_ARGS, extraCommandArgs);

        if (getContainer().getCurrentPage() != logPage) {
            getContainer().showPage(logPage);
        }
        long startTime = System.currentTimeMillis();
        try {
            DBeaverUI.run(getContainer(), true, true, this);
        }
        catch (InterruptedException ex) {
            UIUtils.showMessageBox(getShell(), task, NLS.bind(CoreMessages.tools_wizard_error_task_canceled, task, getObjectsName()), SWT.ICON_ERROR);
            return false;
        }
        catch (InvocationTargetException ex) {
            DBUserInterface.getInstance().showError(
                    NLS.bind(CoreMessages.tools_wizard_error_task_error_title, task),
                CoreMessages.tools_wizard_error_task_error_message + task,
                ex.getTargetException());
            return false;
        }
        finally {
            getContainer().updateButtons();

        }
        long workTime = System.currentTimeMillis() - startTime;
        notifyToolFinish(task + " finished", workTime);
        onSuccess(workTime);
        return false;
    }

    protected void notifyToolFinish(String toolName, long workTime) {
        // Make a sound
        Display.getCurrent().beep();
        // Notify agent
        if (workTime > DBeaverCore.getGlobalPreferenceStore().getLong(DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT) * 1000) {
            DBeaverUI.notifyAgent(toolName, IStatus.INFO);
        }
    }

    public String getObjectsName() {
        StringBuilder str = new StringBuilder();
        for (BASE_OBJECT object : databaseObjects) {
            if (str.length() > 0) str.append(",");
            str.append(object.getName());
        }
        return str.toString();
    }

    @Override
    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
    {
        try {
            for (PROCESS_ARG arg : getRunInfo()) {
                if (monitor.isCanceled()) break;
                executeProcess(monitor, arg);
            }
            if (!monitor.isCanceled()) {
                // Refresh navigator node (script execution can change everything inside)
                for (BASE_OBJECT object : databaseObjects) {
                    final DBNDatabaseNode node = dataSourceContainer.getPlatform().getNavigatorModel().findNode(object);
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

    public boolean executeProcess(DBRProgressMonitor monitor, PROCESS_ARG arg)
        throws IOException, CoreException, InterruptedException
    {
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

            for (;;) {
                Thread.sleep(100);
                if (monitor.isCanceled()) {
                    process.destroy();
                }
                try {
                    final int exitCode = process.exitValue();
                    if (exitCode != 0) {
                        logPage.appendLog(NLS.bind(CoreMessages.tools_wizard_log_process_exit_code, exitCode) + "\n", true);
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
            logPage.appendLog(NLS.bind(CoreMessages.tools_wizard_log_io_error, e.getMessage()) + "\n", true);
            return false;
        }

        return true;
    }

    protected boolean isMergeProcessStreams()
    {
        return false;
    }

    public boolean isVerbose()
    {
        return false;
    }

    protected void onSuccess(long workTime)
    {

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

        protected DumpJob(String name, DBRProgressMonitor monitor, InputStream stream, File outFile)
        {
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
        public DumpCopierJob(DBRProgressMonitor monitor, String name, InputStream stream, File outFile)
        {
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

                try (OutputStream output = new FileOutputStream(outFile)){
                    for (;;) {
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
            }
            finally {
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

        public TextFileTransformerJob(DBRProgressMonitor monitor, File inputFile, OutputStream stream, String inputCharset, String outputCharset)
        {
            super(task);
            this.monitor = monitor;
            this.output = stream;
            this.inputFile = inputFile;
            this.inputCharset = inputCharset;
            this.outputCharset = outputCharset;
        }

        @Override
        public void run()
        {
            try {
                try (InputStream scriptStream = new ProgressStreamReader(
                    monitor,
                    new FileInputStream(inputFile),
                    inputFile.length()))
                {
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
            }
            finally {
                monitor.done();
            }
        }
    }

    public class BinaryFileTransformerJob extends Thread {
        private DBRProgressMonitor monitor;
        private OutputStream output;
        private File inputFile;

        public BinaryFileTransformerJob(DBRProgressMonitor monitor, File inputFile, OutputStream stream)
        {
            super(task);
            this.monitor = monitor;
            this.output = stream;
            this.inputFile = inputFile;
        }

        @Override
        public void run()
        {
            try (InputStream scriptStream = new ProgressStreamReader(
                monitor,
                new FileInputStream(inputFile),
                inputFile.length()))
            {
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
            }
            finally {
                monitor.done();
            }
        }
    }

    private class ProgressStreamReader extends InputStream {

        static final int BUFFER_SIZE = 10000;

        private final DBRProgressMonitor monitor;
        private final InputStream original;
        private final long streamLength;
        private long totalRead;

        private ProgressStreamReader(DBRProgressMonitor monitor, InputStream original, long streamLength)
        {
            this.monitor = monitor;
            this.original = original;
            this.streamLength = streamLength;
            this.totalRead = 0;

            monitor.beginTask(task, (int)streamLength);
        }

        @Override
        public int read() throws IOException
        {
            int res = original.read();
            showProgress(res);
            return res;
        }

        @Override
        public int read(byte[] b) throws IOException
        {
            int res = original.read(b);
            showProgress(res);
            return res;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            int res = original.read(b, off, len);
            showProgress(res);
            return res;
        }

        @Override
        public long skip(long n) throws IOException
        {
            long res = original.skip(n);
            showProgress(res);
            return res;
        }

        @Override
        public int available() throws IOException
        {
            return original.available();
        }

        @Override
        public void close() throws IOException
        {
            monitor.done();
            original.close();
        }

        private void showProgress(long length)
        {
            totalRead += length;
            monitor.worked((int)length);
        }
    }

}
