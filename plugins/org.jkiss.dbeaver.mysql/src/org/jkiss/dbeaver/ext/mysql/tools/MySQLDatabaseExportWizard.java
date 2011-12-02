/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.text.NumberFormat;
import java.util.List;

class MySQLDatabaseExportWizard extends MySQLAbstractToolWizard implements IExportWizard {

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

    private MySQLDatabaseExportWizardPageSettings mainPage;

    public MySQLDatabaseExportWizard(MySQLCatalog catalog) {
        super(catalog, "Export");
        this.method = DumpMethod.NORMAL;
        this.outputFile = new File(catalog.getName() + "-" + RuntimeUtils.getCurrentTimeStamp() + ".sql");
	}

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Database export");
        setNeedsProgressMonitor(true);
        mainPage = new MySQLDatabaseExportWizardPageSettings(this);
    }

    public void addPages() {
        super.addPages();
        addPage(mainPage);
        addPage(logPage);
    }

	@Override
	public void onSuccess() {
        UIUtils.showMessageBox(getShell(), "Database export", "Database '" + getCatalog().getName() + "' export completed", SWT.ICON_INFORMATION);
        Program.launch(outputFile.getAbsoluteFile().getParentFile().getAbsolutePath());
	}

    @Override
    protected void fillProcessParameters(List<String> cmd)
    {
        String dumpPath = new File(getServerHome().getHomePath(), "bin/mysqldump").getAbsolutePath();
        cmd.add(dumpPath);
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
    }

    @Override
    protected boolean isVerbose()
    {
        return true;
    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, ProcessBuilder processBuilder, Process process)
    {
        new DumpTransformerJob(monitor, process.getInputStream()).start();
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

}
