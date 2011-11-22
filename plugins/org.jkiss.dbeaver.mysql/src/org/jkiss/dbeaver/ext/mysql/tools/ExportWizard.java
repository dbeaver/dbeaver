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

class ExportWizard extends AbstractToolWizard implements IExportWizard, DBRRunnableWithProgress {

    static final Log log = LogFactory.getLog(ExportWizard.class);

    public enum DumpMethod {
        ONLINE,
        LOCK_ALL_TABLES,
        NORMAL
    }

    File outputFile;
    DumpMethod method;
    boolean noCreateStatements;
    boolean addDropStatements;
    boolean disableKeys;
    boolean noExtendedInserts;
    boolean dumpEvents;
    boolean comments;

    private ExportWizardPageSettings mainPage;
    private ExportWizardPageFinal logPage;

    private Process process;

    public ExportWizard(MySQLCatalog catalog) {
        super(catalog);
        this.method = DumpMethod.ONLINE;
        this.outputFile = new File(catalog.getName() + "-" + RuntimeUtils.getCurrentTimeStamp() + ".sql");
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(CoreMessages.dialog_project_export_wizard_window_title);
        setNeedsProgressMonitor(true);
        mainPage = new ExportWizardPageSettings(this);
        logPage = new ExportWizardPageFinal();
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
        return true;
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
        cmd.add("--host=" + getConnectionInfo().getHostName());
        if (!CommonUtils.isEmpty(getConnectionInfo().getHostPort())) {
            cmd.add("--port=" + getConnectionInfo().getHostPort());
        }
        cmd.add("-u");
        cmd.add(getConnectionInfo().getUserName());
        cmd.add("--password=" + getConnectionInfo().getUserPassword());
        cmd.add("-v");
        cmd.add("-q");
        cmd.add(getCatalog().getName());

        try {
            //ProcessBuilder builder = new ProcessBuilder(cmd);
            process = Runtime.getRuntime().exec(cmd.toArray(new String[cmd.size()]));//builder.start();
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
            logPage.appendLog("Database dump started at " + new Date() + ContentUtils.getDefaultLineSeparator());
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
                logPage.appendLog("Dump finished " + new Date() + ContentUtils.getDefaultLineSeparator());
            }
        }
    }

}
