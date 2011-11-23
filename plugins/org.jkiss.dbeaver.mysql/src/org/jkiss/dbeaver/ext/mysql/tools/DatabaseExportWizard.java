/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.tools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class DatabaseExportWizard extends AbstractToolWizard implements IExportWizard, DBRRunnableWithProgress {

    static final Log log = LogFactory.getLog(DatabaseExportWizard.class);
    private ProcessBuilder processBuilder;

    public enum DumpMethod {
        ONLINE,
        LOCK_ALL_TABLES,
        NORMAL
    }

    File outputFile;
    DumpMethod method;
    boolean noCreateStatements;
    boolean addDropStatements = true;
    boolean disableKeys = true;
    boolean extendedInserts = true;
    boolean dumpEvents;
    boolean comments;

    private DatabaseExportWizardPageSettings mainPage;
    private DatabaseExportWizardPageFinal logPage;

    private Process process;
    private boolean finished;

    public DatabaseExportWizard(MySQLCatalog catalog) {
        super(catalog);
        this.method = DumpMethod.NORMAL;
        this.outputFile = new File(catalog.getName() + "-" + RuntimeUtils.getCurrentTimeStamp() + ".sql");
	}

    @Override
    public boolean canFinish()
    {
        return !finished && super.canFinish();
    }

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(CoreMessages.dialog_project_export_wizard_window_title);
        setNeedsProgressMonitor(true);
        mainPage = new DatabaseExportWizardPageSettings(this);
        logPage = new DatabaseExportWizardPageFinal();
    }

    public void addPages() {
        super.addPages();
        addPage(mainPage);
        addPage(logPage);
    }

	@Override
	public boolean performFinish() {
        if (getContainer().getCurrentPage() != logPage) {
            getContainer().showPage(logPage);
        }
        try {
            RuntimeUtils.run(getContainer(), true, true, this);
        }
        catch (InterruptedException ex) {
            UIUtils.showMessageBox(getShell(), "Database export", "Database '" + getCatalog().getName() + "' export canceled", SWT.ICON_ERROR);
            return false;
        }
        catch (InvocationTargetException ex) {
            UIUtils.showErrorDialog(
                getShell(),
                "Export error",
                "Cannot perform database export",
                ex.getTargetException());
            return false;
        }
        UIUtils.showMessageBox(getShell(), "Database export", "Database '" + getCatalog().getName() + "' export completed", SWT.ICON_INFORMATION);
        Program.launch(outputFile.getAbsoluteFile().getParentFile().getAbsolutePath());
        getContainer().updateButtons();
        return false;
	}

    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
    {
        try {
            exportProjects(monitor);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        }
        if (monitor.isCanceled()) {
            throw new InterruptedException();
        }
    }

    public boolean exportProjects(DBRProgressMonitor monitor)
        throws IOException, CoreException, InterruptedException
    {
        String dumpPath = new File(getServerHome().getHomePath(), "bin/mysqldump").getAbsolutePath();
        java.util.List<String> cmd = new ArrayList<String>();
        cmd.add(dumpPath);
        cmd.add("-v");
        cmd.add("-q");
        switch (method) {
            case LOCK_ALL_TABLES:
                cmd.add("--lock-all-tables");
                break;
            case ONLINE:
                cmd.add("--single-transaction");
                break;
        }

        if (noCreateStatements) cmd.add("--no-create-info");
        if (addDropStatements) cmd.add("--add-drop-table");
        if (disableKeys) cmd.add("--disable-keys");
        if (extendedInserts) cmd.add("--extended-insert");
        if (dumpEvents) cmd.add("--events");
        if (comments) cmd.add("--comments");

        cmd.add("--host=" + getConnectionInfo().getHostName());
        if (!CommonUtils.isEmpty(getConnectionInfo().getHostPort())) {
            cmd.add("--port=" + getConnectionInfo().getHostPort());
        }
        cmd.add("-u");
        cmd.add(getConnectionInfo().getUserName());
        cmd.add("--password=" + getConnectionInfo().getUserPassword());

        cmd.add(getCatalog().getName());

        try {
            processBuilder = new ProcessBuilder(cmd);
            process = processBuilder.start();
            new DumpTransformerJob(monitor, process.getInputStream()).start();
            new LogReaderJob(process.getErrorStream()).start();

            for (;;) {
                Thread.sleep(100);
                if (monitor.isCanceled()) {
                    process.destroy();
                }
                try {
                    process.exitValue();
                } catch (Exception e) {
                    // Still running
                    continue;
                }
                break;
            }
            //process.waitFor();
        } catch (IOException e) {
            log.error(e);
            logPage.setErrorMessage(e.getMessage());
            return false;
        }

        finished = true;
        return true;
    }


    class DumpTransformerJob extends Thread {
        private DBRProgressMonitor monitor;
        private InputStream input;

        protected DumpTransformerJob(DBRProgressMonitor monitor, InputStream stream)
        {
            super("Dump log reader");
            this.monitor = monitor;
            this.input = stream;
        }

        public void run()
        {
            monitor.beginTask("Export database", 100);
            long totalBytesDumped = 0;
            long prevStatusUpdateTime = 0;
            byte[] buffer = new byte[10000];
            try {
                NumberFormat numberFormat = NumberFormat.getInstance();
                OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile), 10000);
                try {
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
                    //logPage.setMessage("Done (" + String.valueOf(totalBytesDumped) + " bytes)");
                    output.flush();
                } finally {
                    IOUtils.close(output);
                }
            } catch (IOException e) {
                logPage.appendLog(e.getMessage());
            }
            finally {
                monitor.done();
            }
        }
    }

    class LogReaderJob extends Thread {
        private InputStream input;
        //private BufferedReader in;
        protected LogReaderJob(InputStream stream)
        {
            super("Dump log reader");
            //in = new BufferedReader(new InputStreamReader(stream), 100);
            this.input = stream;
        }

        public void run()
        {
            String lf = ContentUtils.getDefaultLineSeparator();
            List<String> command = processBuilder.command();
            StringBuilder cmdString = new StringBuilder();
            for (String cmd : command) {
                if (cmd.startsWith("--password")) continue;
                if (cmdString.length() > 0) cmdString.append(' ');
                cmdString.append(cmd);
            }
            cmdString.append(lf);
            logPage.appendLog(cmdString.toString());
            logPage.appendLog("Database dump started at " + new Date() + lf);

            try {
/*
                String line;
                while ((line = in.readLine()) != null) {
                    progressDialog.appendLog(line);
                }
*/

                StringBuilder buf = new StringBuilder();
                for (;;) {
                    int b = input.read();
                    if (b == -1) {
                        break;
                    }
                    buf.append((char)b);
                    int avail = input.available();
                    if (b == 0x0A) {
                        logPage.appendLog(buf.toString());
                        buf.setLength(0);
                    }
                }

            } catch (IOException e) {
                // just skip
            } finally {
                logPage.appendLog("Dump finished " + new Date() + lf);
            }
        }
    }

}
