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

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.MySQLServerHome;
import org.jkiss.dbeaver.ext.mysql.MySQLUtils;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractToolWizard;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MySQLExportWizard extends AbstractToolWizard<DBSObject, MySQLDatabaseExportInfo> implements IExportWizard {

    public static final String VARIABLE_HOST = "host";
    public static final String VARIABLE_DATABASE = "database";
    public static final String VARIABLE_TABLE = "table";
    public static final String VARIABLE_TIMESTAMP = "timestamp";

    public enum DumpMethod {
        ONLINE,
        LOCK_ALL_TABLES,
        NORMAL
    }

    private File outputFolder;

    String outputFilePattern;
    DumpMethod method;
    boolean noCreateStatements;
    boolean addDropStatements = true;
    boolean disableKeys = true;
    boolean extendedInserts = true;
    boolean dumpEvents;
    boolean comments;
    boolean removeDefiner;
    boolean binariesInHex;
    boolean showViews;
    public List<MySQLDatabaseExportInfo> objects = new ArrayList<>();

    private MySQLExportWizardPageObjects objectsPage;
    private MySQLExportWizardPageSettings settingsPage;

    public MySQLExportWizard(Collection<DBSObject> objects) {
        super(objects, MySQLMessages.tools_db_export_wizard_task_name);
        this.method = DumpMethod.NORMAL;
        this.outputFolder = new File(DialogUtils.getCurDialogFolder()); //$NON-NLS-1$ //$NON-NLS-2$

        final DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        this.outputFilePattern = store.getString("MySQL.export.outputFilePattern");
        if (CommonUtils.isEmpty(this.outputFilePattern)) {
            this.outputFilePattern = "dump-${database}-${timestamp}.sql";
        }
        noCreateStatements = CommonUtils.getBoolean(store.getString("MySQL.export.noCreateStatements"), false);
        addDropStatements = CommonUtils.getBoolean(store.getString("MySQL.export.addDropStatements"), true);
        disableKeys = CommonUtils.getBoolean(store.getString("MySQL.export.disableKeys"), true);
        extendedInserts = CommonUtils.getBoolean(store.getString("MySQL.export.extendedInserts"), true);
        dumpEvents = CommonUtils.getBoolean(store.getString("MySQL.export.dumpEvents"), false);
        comments = CommonUtils.getBoolean(store.getString("MySQL.export.comments"), false);
        removeDefiner = CommonUtils.getBoolean(store.getString("MySQL.export.removeDefiner"), false);
        binariesInHex = CommonUtils.getBoolean(store.getString("MySQL.export.binariesInHex"), false);
        showViews = CommonUtils.getBoolean(store.getString("MySQL.export.showViews"), false);
    }

    public File getOutputFolder()
    {
        return outputFolder;
    }

    public void setOutputFolder(File outputFolder)
    {
        if (outputFolder != null) {
            DialogUtils.setCurDialogFolder(outputFolder.getAbsolutePath());
        }
        this.outputFolder = outputFolder;
    }

    public String getOutputFilePattern() {
        return outputFilePattern;
    }

    public void setOutputFilePattern(String outputFilePattern) {
        this.outputFilePattern = outputFilePattern;
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
    public IWizardPage getNextPage(IWizardPage page) {
        if (page == settingsPage) {
            return null;
        }
        return super.getNextPage(page);
    }

    @Override
    public IWizardPage getPreviousPage(IWizardPage page) {
        if (page == logPage) {
            return settingsPage;
        }
        return super.getPreviousPage(page);
    }

    @Override
	public void onSuccess() {
        UIUtils.showMessageBox(
            getShell(),
            MySQLMessages.tools_db_export_wizard_title,
            NLS.bind(MySQLMessages.tools_db_export_wizard_message_export_completed, getObjectsName()),
            SWT.ICON_INFORMATION);
        UIUtils.launchProgram(outputFolder.getAbsolutePath());
	}

    @Override
    public void fillProcessParameters(List<String> cmd, MySQLDatabaseExportInfo arg) throws IOException
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
            if (CommonUtils.isEmpty(arg.getTables())) {
                cmd.add("--routines"); //$NON-NLS-1$
            }
        }
        if (addDropStatements) cmd.add("--add-drop-table"); //$NON-NLS-1$
        if (disableKeys) cmd.add("--disable-keys"); //$NON-NLS-1$
        if (extendedInserts) {
            cmd.add("--extended-insert"); //$NON-NLS-1$
        } else {
            cmd.add("--skip-extended-insert"); //$NON-NLS-1$
        }
        if (binariesInHex) {
            cmd.add("--hex-blob"); //$NON-NLS-1$
        }
        if (dumpEvents) cmd.add("--events"); //$NON-NLS-1$
        if (comments) cmd.add("--comments"); //$NON-NLS-1$
    }

    @Override
    public boolean performFinish() {
        final File dir = outputFolder;
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                logPage.setMessage("Can't create directory '" + dir.getAbsolutePath() + "'", IMessageProvider.ERROR);
                getContainer().updateMessage();
                return false;
            }
        }
        objectsPage.saveState();

        final DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        store.setValue("MySQL.export.outputFilePattern", this.outputFilePattern);
        store.setValue("MySQL.export.noCreateStatements", noCreateStatements);
        store.setValue("MySQL.export.addDropStatements", addDropStatements);
        store.setValue("MySQL.export.disableKeys", disableKeys);
        store.setValue("MySQL.export.extendedInserts", extendedInserts);
        store.setValue("MySQL.export.dumpEvents", dumpEvents);
        store.setValue("MySQL.export.comments", comments);
        store.setValue("MySQL.export.removeDefiner", removeDefiner);
        store.setValue("MySQL.export.binariesInHex", binariesInHex);
        store.setValue("MySQL.export.showViews", showViews);

        return super.performFinish();
    }

    @Override
    public MySQLServerHome findServerHome(String clientHomeId)
    {
        return MySQLDataSourceProvider.getServerHome(clientHomeId);
    }

    @Override
    public Collection<MySQLDatabaseExportInfo> getRunInfo() {
        return objects;
    }

    @Override
    protected List<String> getCommandLine(MySQLDatabaseExportInfo arg) throws IOException
    {
        List<String> cmd = MySQLToolScript.getMySQLToolCommandLine(this, arg);
        if (objects.isEmpty()) {
            // no dump
        } else if (!CommonUtils.isEmpty(arg.getTables())) {
            cmd.add(arg.getDatabase().getName());
            for (MySQLTableBase table : arg.getTables()) {
                cmd.add(table.getName());
            }
        } else {
            cmd.add(arg.getDatabase().getName());
        }

        return cmd;
    }

    @Override
    public boolean isVerbose()
    {
        return true;
    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, final MySQLDatabaseExportInfo arg, ProcessBuilder processBuilder, Process process)
    {
        logPage.startLogReader(
            processBuilder,
            process.getErrorStream());

        String outFileName = GeneralUtils.replaceVariables(outputFilePattern, new GeneralUtils.IVariableResolver() {
            @Override
            public String get(String name) {
                switch (name) {
                    case VARIABLE_DATABASE:
                        return arg.getDatabase().getName();
                    case VARIABLE_HOST:
                        return arg.getDatabase().getDataSource().getContainer().getConnectionConfiguration().getHostName();
                    case VARIABLE_TABLE:
                        final Iterator<MySQLTableBase> iterator = arg.getTables() == null ? null : arg.getTables().iterator();
                        if (iterator != null && iterator.hasNext()) {
                            return iterator.next().getName();
                        } else {
                            return "null";
                        }
                    case VARIABLE_TIMESTAMP:
                        return RuntimeUtils.getCurrentTimeStamp();
                    default:
                        System.getProperty(name);
                }
                return null;
            }
        });

        File outFile = new File(outputFolder, outFileName);
        boolean isFiltering = removeDefiner;
        Thread job = isFiltering ?
            new DumpFilterJob(monitor, process.getInputStream(), outFile) :
            new DumpCopierJob(monitor, process.getInputStream(), outFile);
        job.start();
    }

    abstract class DumpJob extends Thread {
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
    }

    class DumpCopierJob extends DumpJob {
        protected DumpCopierJob(DBRProgressMonitor monitor, InputStream stream, File outFile)
        {
            super(MySQLMessages.tools_db_export_wizard_job_dump_log_reader, monitor, stream, outFile);
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

                try (OutputStream output = new FileOutputStream(outFile)){
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

    private static Pattern DEFINER_PATTER = Pattern.compile("DEFINER\\s*=\\s*`[^*]*`@`[0-9a-z\\-_\\.%]*`", Pattern.CASE_INSENSITIVE);

    class DumpFilterJob extends DumpJob {
        protected DumpFilterJob(DBRProgressMonitor monitor, InputStream stream, File outFile)
        {
            super(MySQLMessages.tools_db_export_wizard_job_dump_log_reader, monitor, stream, outFile);
        }

        @Override
        public void run()
        {
            monitor.beginTask(MySQLMessages.tools_db_export_wizard_monitor_export_db, 100);
            long prevStatusUpdateTime = 0;
            try {
                NumberFormat numberFormat = NumberFormat.getInstance();

                LineNumberReader reader = new LineNumberReader(new InputStreamReader(input, ContentUtils.DEFAULT_CHARSET));
                try (OutputStream output = new FileOutputStream(outFile)) {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, ContentUtils.DEFAULT_CHARSET));
                    for (;;) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        if (removeDefiner) {
                            final Matcher matcher = DEFINER_PATTER.matcher(line);
                            if (matcher.find()) {
                                line = matcher.replaceFirst("");
                            }
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
                    writer.flush();
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
