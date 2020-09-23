package org.jkiss.dbeaver.tasks.nativetool;

import org.eclipse.core.runtime.IStatus;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocationManager;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskExecutionListener;
import org.jkiss.dbeaver.model.task.DBTTaskHandler;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ProgressStreamReader;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.util.*;

public abstract class AbstractNativeToolHandler<SETTINGS extends AbstractNativeToolSettings<BASE_OBJECT>, BASE_OBJECT extends DBSObject, PROCESS_ARG> implements DBTTaskHandler {

    @Override
    public void executeTask(
        @NotNull DBRRunnableContext runnableContext,
        @NotNull DBTTask task,
        @NotNull Locale locale,
        @NotNull Log log,
        @NotNull PrintStream logStream,
        @NotNull DBTTaskExecutionListener listener) throws DBException {
        SETTINGS settings = createTaskSettings(runnableContext, task);
        settings.setLogWriter(logStream);
        if (!validateTaskParameters(task, settings, log)) {
            return;
        }
        try {
            runnableContext.run(true, true, monitor -> {
                monitor.beginTask(task.getType().getName(), 1);
                monitor.subTask(task.getType().getName());

                Log.setLogWriter(logStream);
                listener.taskStarted(task);
                Throwable error = null;
                try {
                    doExecute(monitor, task, settings, log);
                } catch (Exception e) {
                    error = e;
                } finally {
                    listener.taskFinished(settings, error);
                    Log.setLogWriter(null);
                }

                monitor.worked(1);
                monitor.done();
            });
        } catch (InvocationTargetException e) {
            throw new DBException("Error executing native tool", e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
        }
    }

    protected boolean isNativeClientHomeRequired() {
        return true;
    }

    protected boolean isMergeProcessStreams() {
        return false;
    }

    protected boolean needsModelRefresh() {
        return true;
    }

    private void validateClientHome(DBRProgressMonitor monitor, SETTINGS settings) throws DBCException {
        DBPDataSourceContainer dataSourceContainer = settings.getDataSourceContainer();
        if (isNativeClientHomeRequired()) {
            String clientHomeId = dataSourceContainer.getConnectionConfiguration().getClientHomeId();
            final DBPDriver driver = dataSourceContainer.getDriver();
            final List<DBPNativeClientLocation> clientLocations = driver.getNativeClientLocations();
            final DBPNativeClientLocationManager locationManager = driver.getNativeClientManager();
            if (locationManager != null) {
                clientLocations.addAll(locationManager.findLocalClientLocations());
            }
            if (clientHomeId == null) {
                if (!clientLocations.isEmpty()) {
                    settings.setClientHome(clientLocations.get(0));
                } else {
                    settings.setClientHome(null);
                }
                if (settings.getClientHome() == null) {
                    throw new DBCException("Client binaries location is not specified");
                }
            } else {
                DBPNativeClientLocation clientHome = DBUtils.findObject(clientLocations, clientHomeId);
                if (clientHome == null) {
                    clientHome = settings.findNativeClientHome(clientHomeId);
                }
                settings.setClientHome(clientHome);
            }
            if (settings.getClientHome() == null) {
                throw new DBCException("Native client home '" + clientHomeId + "' not found");
            }
        }

        DBPNativeClientLocation clientHome = settings.getClientHome();
        if (!isNativeClientHomeRequired() || clientHome == null) {
            return;
        }
        try {
            clientHome.validateFilesPresence(monitor);
        } catch (DBException e) {
            throw new DBCException("Error downloading client file(s)", e);
        } catch (InterruptedException e) {
            // ignore
            throw new DBCException("Client file download interrupted", e);
        }
    }

    public abstract Collection<PROCESS_ARG> getRunInfo(SETTINGS settings);

    public Collection<BASE_OBJECT> getUpdatedObjects(PROCESS_ARG settings) {
        return Collections.emptyList();
    }

    protected abstract SETTINGS createTaskSettings(DBRRunnableContext context, DBTTask task) throws DBException;

    protected boolean validateTaskParameters(DBTTask task, SETTINGS settings, Log log) {
        return true;
    }

    abstract protected java.util.List<String> getCommandLine(SETTINGS settings, PROCESS_ARG arg) throws IOException;

    public abstract void fillProcessParameters(SETTINGS settings, PROCESS_ARG arg, List<String> cmd) throws IOException;

    protected void setupProcessParameters(SETTINGS settings, PROCESS_ARG arg, ProcessBuilder process) {
    }

    protected boolean isLogInputStream() {
        return true;
    }

    protected void startProcessHandler(DBRProgressMonitor monitor, DBTTask task, SETTINGS settings, PROCESS_ARG arg, ProcessBuilder processBuilder, Process process, Log log) throws IOException {
        LogReaderJob logReaderJob = new LogReaderJob(
            task,
            settings,
            processBuilder,
            isLogInputStream() ? process.getInputStream() : process.getErrorStream());
        logReaderJob.start();
    }

    public boolean executeProcess(DBRProgressMonitor monitor, DBTTask task, SETTINGS settings, PROCESS_ARG arg, Log log) throws IOException, InterruptedException {
        monitor.beginTask(task.getType().getName(), 1);
        try {
            monitor.subTask("Start native tool");
            final List<String> commandLine = getCommandLine(settings, arg);
            final File execPath = new File(commandLine.get(0));

            ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
            processBuilder.directory(execPath.getParentFile());
            if (this.isMergeProcessStreams()) {
                processBuilder.redirectErrorStream(true);
            }
            setupProcessParameters(settings, arg, processBuilder);
            Process process = processBuilder.start();

            startProcessHandler(monitor, task, settings, arg, processBuilder, process, log);

            monitor.subTask("Executing");
            Thread.sleep(100);

            for (; ; ) {
                Thread.sleep(100);
                if (monitor.isCanceled()) {
                    process.destroy();
                }
                try {
                    final int exitCode = process.exitValue();
                    validateErrorCode(exitCode);
                } catch (IllegalThreadStateException e) {
                    // Still running
                    continue;
                }
                break;
            }
            //process.waitFor();
        } catch (IOException e) {
            log.error("IO error: " + e.getMessage());
            throw e;
        } finally {
            monitor.done();
        }

        return true;
    }

    public void validateErrorCode(int exitCode) throws IOException {
        if (exitCode != 0) {
            throw new IOException("Process failed (exit code = " + exitCode + "). See error log.");
        }
    }

    protected void notifyToolFinish(String toolName, long workTime) {
        // Notify agent
        if (workTime > DBWorkbench.getPlatformUI().getLongOperationTimeout() * 1000) {
            DBWorkbench.getPlatformUI().notifyAgent(toolName, IStatus.INFO);
        }
    }

    protected void onSuccess(DBTTask task, SETTINGS settings, long workTime) {

        StringBuilder message = new StringBuilder();
        message.append("Task [").append(task.getName()).append("] is completed (").append(workTime).append("ms)");
        List<String> objNames = new ArrayList<>();
        for (BASE_OBJECT obj : settings.getDatabaseObjects()) {
            objNames.add(obj.getName());
        }
        message.append("\nObject(s) processed: ").append(String.join(",", objNames));
        DBWorkbench.getPlatformUI().showMessageBox(task.getName(), message.toString(), false);

    }

    protected void onError(DBTTask task, SETTINGS settings, long workTime) {
//        DBWorkbench.getPlatformUI().showError(
//            taskTitle,
//            errorMessage == null ? "Internal error" : errorMessage,
//            SWT.ICON_ERROR);
    }

    protected boolean doExecute(DBRProgressMonitor monitor, DBTTask task, SETTINGS settings, Log log) throws DBException, InterruptedException {
        validateClientHome(monitor, settings);

        long startTime = System.currentTimeMillis();

        boolean isSuccess = true;
        try {
            for (PROCESS_ARG arg : getRunInfo(settings)) {
                if (monitor.isCanceled()) break;
                if (!executeProcess(monitor, task, settings, arg, log)) {
                    isSuccess = false;
                }
            }
            DBPDataSourceContainer dataSourceContainer = settings.getDataSourceContainer();
            boolean refreshObjects = isSuccess && !monitor.isCanceled();
            if (refreshObjects && needsModelRefresh()) {
                // Refresh navigator node (script execution can change everything inside)
                for (BASE_OBJECT object : settings.getDatabaseObjects()) {
                    final DBNDatabaseNode node = dataSourceContainer.getPlatform().getNavigatorModel().findNode(object);
                    if (node != null) {
                        node.refreshNode(monitor, this);
                    }
                }
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            log.error(e);
            throw new DBException("Error executing process", e);
        }
        if (monitor.isCanceled()) {
            throw new InterruptedException();
        }

        long workTime = System.currentTimeMillis() - startTime;
        notifyToolFinish(task.getType().getName() + " - " + task.getName() + " has finished", workTime);
        if (isSuccess) {
            onSuccess(task, settings, workTime);
        } else {
            onError(task, settings, workTime);
        }

        return isSuccess;
    }

    public static abstract class DumpJob extends Thread {
        protected DBRProgressMonitor monitor;
        protected InputStream input;
        protected File outFile;
        protected Log log;

        protected DumpJob(String name, DBRProgressMonitor monitor, InputStream stream, File outFile, Log log) {
            super(name);
            this.monitor = monitor;
            this.input = stream;
            this.outFile = outFile;
            this.log = log;
        }

        @Override
        public final void run() {
            try {
                runDump();
            } catch (IOException e) {
                log.error(e);
            }
        }

        protected abstract void runDump()
            throws IOException;
    }

    public static class DumpCopierJob extends DumpJob {
        public DumpCopierJob(DBRProgressMonitor monitor, String name, InputStream stream, File outFile, Log log) {
            super(name, monitor, stream, outFile, log);
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

    public static class TextFileTransformerJob extends Thread {
        private final DBRProgressMonitor monitor;
        private final DBTTask task;
        private OutputStream output;
        private File inputFile;
        private String inputCharset;
        private String outputCharset;
        private Log log;

        public TextFileTransformerJob(DBRProgressMonitor monitor, DBTTask task, File inputFile, OutputStream stream, String inputCharset, String outputCharset, Log log) {
            super(task.getName());
            this.monitor = monitor;
            this.task = task;
            this.output = stream;
            this.inputFile = inputFile;
            this.inputCharset = inputCharset;
            this.outputCharset = outputCharset;
            this.log = log;
        }

        @Override
        public void run() {
            try {
                try (InputStream scriptStream = new ProgressStreamReader(
                    monitor,
                    task.getName(),
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
                log.error(e);
            } finally {
                monitor.done();
            }
        }
    }

    public static class BinaryFileTransformerJob extends Thread {
        private final DBRProgressMonitor monitor;
        private final DBTTask task;
        private OutputStream output;
        private File inputFile;
        private Log log;

        public BinaryFileTransformerJob(DBRProgressMonitor monitor, DBTTask task, File inputFile, OutputStream stream, Log log) {
            super(task.getName());
            this.monitor = monitor;
            this.task = task;
            this.output = stream;
            this.inputFile = inputFile;
            this.log = log;
        }

        @Override
        public void run() {
            try (InputStream scriptStream = new ProgressStreamReader(
                monitor,
                task.getName(),
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
                log.error(e);
            } finally {
                try {
                    output.close();
                } catch (IOException e) {
                    log.error(e);
                }
                monitor.done();
            }
        }
    }

    private class LogReaderJob extends Thread {
        private DBTTask task;
        private SETTINGS settings;
        private PrintStream logWriter;
        private ProcessBuilder processBuilder;
        private InputStream input;

        protected LogReaderJob(DBTTask task, SETTINGS settings, ProcessBuilder processBuilder, InputStream stream) {
            super("Log reader for " + task.getName());
            this.task = task;
            this.settings = settings;
            this.logWriter = settings.getLogWriter();
            this.processBuilder = processBuilder;
            this.input = stream;
        }

        @Override
        public void run() {
            String lf = GeneralUtils.getDefaultLineSeparator();
            List<String> command = processBuilder.command();

            // Dump command line
            StringBuilder cmdString = new StringBuilder();
            for (String cmd : command) {
                if (NativeToolUtils.isSecureString(settings, cmd)) {
                    cmd = "******";
                }
                if (cmdString.length() > 0) cmdString.append(' ');
                cmdString.append(cmd);
            }
            cmdString.append(lf);

            try {
                logWriter.print(cmdString.toString());

                logWriter.print("Task '" + task.getName() + "' started at " + new Date() + lf);
                logWriter.flush();

                InputStream in = input;
                try (Reader reader = new InputStreamReader(in, GeneralUtils.getDefaultConsoleEncoding())) {
                    StringBuilder buf = new StringBuilder();
                    for (; ; ) {
                        int b = reader.read();
                        if (b == -1) {
                            break;
                        }
                        buf.append((char) b);
                        if (b == '\n') {
                            logWriter.println(buf.toString());
                            logWriter.flush();
                            buf.setLength(0);
                        }
                        //int avail = input.available();
                    }
                }

            } catch (IOException e) {
                // just skip
                logWriter.println(e.getMessage() + lf);
            } finally {
                logWriter.print("Task '" + task.getName() + "' finished at " + new Date() + lf);
                logWriter.flush();
            }
        }
    }

    private class NullReaderJob extends Thread {
        private InputStream input;

        protected NullReaderJob(DBTTask task, InputStream stream) {
            super("Task " + task.getName() + " log reader");
            this.input = stream;
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[1000];
                for (; ; ) {
                    int count = input.read(buffer);
                    if (count <= 0) {
                        break;
                    }
                }
            } catch (IOException e) {
                // just skip
            }
        }
    }

    protected String getInputCharset() {
        return GeneralUtils.UTF8_ENCODING;
    }

    protected String getOutputCharset() {
        return GeneralUtils.UTF8_ENCODING;
    }

}
