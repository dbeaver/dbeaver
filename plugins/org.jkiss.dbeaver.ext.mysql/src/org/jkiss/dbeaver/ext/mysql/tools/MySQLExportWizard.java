/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.MySQLServerHome;
import org.jkiss.dbeaver.ext.mysql.MySQLUtils;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractToolWizard;

import java.io.*;
import java.text.NumberFormat;
import java.util.List;

class MySQLExportWizard extends AbstractToolWizard<MySQLCatalog> implements IExportWizard {


    public enum DumpMethod {
        ONLINE,
        LOCK_ALL_TABLES,
        NORMAL
    }

    private File outputFile;
    DumpMethod method;
    boolean noCreateStatements;
    boolean addDropStatements = true;
    boolean disableKeys = true;
    boolean extendedInserts = true;
    boolean dumpEvents;
    boolean comments;
    public boolean removeDefiner;

    private MySQLExportWizardPageObjects objectsPage;
    private MySQLExportWizardPageSettings settingsPage;

    public MySQLExportWizard(MySQLCatalog catalog) {
        super(catalog, MySQLMessages.tools_db_export_wizard_task_name);
        this.method = DumpMethod.NORMAL;
        this.outputFile = new File(DialogUtils.getCurDialogFolder(), catalog.getName() + "-" + RuntimeUtils.getCurrentTimeStamp() + ".sql"); //$NON-NLS-1$ //$NON-NLS-2$
	}

    public File getOutputFile()
    {
        return outputFile;
    }

    public void setOutputFile(File outputFile)
    {
        DialogUtils.setCurDialogFolder(outputFile.getParentFile().getAbsolutePath());
        this.outputFile = outputFile;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(MySQLMessages.tools_db_export_wizard_title);
        setNeedsProgressMonitor(true);
        objectsPage = new MySQLExportWizardPageObjects(this);
        settingsPage = new MySQLExportWizardPageSettings(this);
    }

    @Override
    public void addPages() {
        super.addPages();
        addPage(objectsPage);
        addPage(settingsPage);
        addPage(logPage);
    }

	@Override
	public void onSuccess() {
        UIUtils.showMessageBox(
                getShell(),
                MySQLMessages.tools_db_export_wizard_title,
                NLS.bind(MySQLMessages.tools_db_export_wizard_message_export_completed, getDatabaseObject().getName()),
                SWT.ICON_INFORMATION);
        UIUtils.launchProgram(outputFile.getAbsoluteFile().getParentFile().getAbsolutePath());
	}

    @Override
    public void fillProcessParameters(List<String> cmd) throws IOException
    {
        File dumpBinary = MySQLUtils.getHomeBinary(getClientHome(), "mysqldump"); //$NON-NLS-1$
        String dumpPath = dumpBinary.getAbsolutePath();
        cmd.add(dumpPath);
        switch (method) {
            case LOCK_ALL_TABLES:
                cmd.add("--lock-all-tables"); //$NON-NLS-1$
                break;
            case ONLINE:
                cmd.add("--single-transaction"); //$NON-NLS-1$
                break;
        }

        if (noCreateStatements) {
            cmd.add("--no-create-info"); //$NON-NLS-1$
        } else {
            cmd.add("--routines"); //$NON-NLS-1$
        }
        if (addDropStatements) cmd.add("--add-drop-table"); //$NON-NLS-1$
        if (disableKeys) cmd.add("--disable-keys"); //$NON-NLS-1$
        if (extendedInserts) cmd.add("--extended-insert"); //$NON-NLS-1$
        if (dumpEvents) cmd.add("--events"); //$NON-NLS-1$
        if (comments) cmd.add("--comments"); //$NON-NLS-1$
    }


    @Override
    public MySQLServerHome findServerHome(String clientHomeId)
    {
        return MySQLDataSourceProvider.getServerHome(clientHomeId);
    }

    @Override
    protected List<String> getCommandLine() throws IOException
    {
        return MySQLToolScript.getMySQLToolCommandLine(this);
    }

    @Override
    public boolean isVerbose()
    {
        return true;
    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, ProcessBuilder processBuilder, Process process)
    {
        logPage.startLogReader(
            processBuilder,
            process.getErrorStream());
        new DumpCopierJob(monitor, process.getInputStream()).start();
    }

    class DumpCopierJob extends Thread {
        private DBRProgressMonitor monitor;
        private InputStream input;

        protected DumpCopierJob(DBRProgressMonitor monitor, InputStream stream)
        {
            super(MySQLMessages.tools_db_export_wizard_job_dump_log_reader);
            this.monitor = monitor;
            this.input = stream;
        }

        @Override
        public void run()
        {
            monitor.beginTask(MySQLMessages.tools_db_export_wizard_monitor_export_db, 100);
            long totalBytesDumped = 0;
            long prevStatusUpdateTime = 0;
            byte[] buffer = new byte[10000];
            try {
                NumberFormat numberFormat = NumberFormat.getInstance();

                try (OutputStream output = new FileOutputStream(outputFile)){
                    for (;;) {
                        int count = input.read(buffer);
                        if (count <= 0) {
                            break;
                        }
                        totalBytesDumped += count;
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - prevStatusUpdateTime > 300) {
                            monitor.subTask(NLS.bind(MySQLMessages.tools_db_export_wizard_monitor_bytes, numberFormat.format(totalBytesDumped)));
                            prevStatusUpdateTime = currentTime;
                        }
                        output.write(buffer, 0, count);
                    }
                    output.flush();
                }
            } catch (IOException e) {
                logPage.appendLog(e.getMessage());
            }
            finally {
                monitor.done();
            }
        }
    }

    class DumpFilterJob extends Thread {
        private DBRProgressMonitor monitor;
        private InputStream input;

        protected DumpFilterJob(DBRProgressMonitor monitor, InputStream stream)
        {
            super(MySQLMessages.tools_db_export_wizard_job_dump_log_reader);
            this.monitor = monitor;
            this.input = stream;
        }

        @Override
        public void run()
        {
            monitor.beginTask(MySQLMessages.tools_db_export_wizard_monitor_export_db, 100);
            long prevStatusUpdateTime = 0;
            try {
                NumberFormat numberFormat = NumberFormat.getInstance();

                LineNumberReader reader = new LineNumberReader(new InputStreamReader(input, ContentUtils.DEFAULT_CHARSET));
                try (OutputStream output = new FileOutputStream(outputFile)) {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
                    for (;;) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - prevStatusUpdateTime > 300) {
                            monitor.subTask("Saved " + numberFormat.format(reader.getLineNumber()) + " lines");
                            prevStatusUpdateTime = currentTime;
                        }
                        line = filterLine(line);
                        writer.write(line);
                        writer.newLine();
                    }
                    output.flush();
                }
            } catch (IOException e) {
                logPage.appendLog(e.getMessage());
            }
            finally {
                monitor.done();
            }
        }

        @NotNull
        private String filterLine(@NotNull String line) {
            return line;
        }
    }

}
