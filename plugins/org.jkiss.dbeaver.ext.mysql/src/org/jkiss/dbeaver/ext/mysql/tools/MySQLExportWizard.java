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
package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.MySQLServerHome;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractImportExportWizard;
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

class MySQLExportWizard extends AbstractImportExportWizard<MySQLDatabaseExportInfo> implements IExportWizard {

    public enum DumpMethod {
        ONLINE,
        LOCK_ALL_TABLES,
        NORMAL
    }

    DumpMethod method;
    boolean noCreateStatements;
    boolean addDropStatements = true;
    boolean disableKeys = true;
    boolean extendedInserts = true;
    boolean dumpEvents;
    boolean comments;
    boolean removeDefiner;
    boolean binariesInHex;
    boolean noData;
    boolean showViews;
    private String extraCommandArgs;

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
        noData = CommonUtils.getBoolean(store.getString("MySQL.export.noData"), false);
        showViews = CommonUtils.getBoolean(store.getString("MySQL.export.showViews"), false);
        extraCommandArgs = store.getString("MySQL.export.extraArgs");
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection);
        objectsPage = new MySQLExportWizardPageObjects(this);
        settingsPage = new MySQLExportWizardPageSettings(this);
    }

    @Override
    public void addPages() {
        addPage(objectsPage);
        addPage(settingsPage);
        super.addPages();
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
	public void onSuccess(long workTime) {
        UIUtils.showMessageBox(
            getShell(),
            MySQLMessages.tools_db_export_wizard_title,
            CommonUtils.truncateString(NLS.bind(MySQLMessages.tools_db_export_wizard_message_export_completed, getObjectsName()), 255),
            SWT.ICON_INFORMATION);
        UIUtils.launchProgram(outputFolder.getAbsolutePath());
	}

    public String getExtraCommandArgs() {
        return extraCommandArgs;
    }

    public void setExtraCommandArgs(String extraCommandArgs) {
        this.extraCommandArgs = extraCommandArgs;
    }

    @Override
    public void fillProcessParameters(List<String> cmd, MySQLDatabaseExportInfo arg) throws IOException
    {
        File dumpBinary = RuntimeUtils.getHomeBinary(getClientHome(), MySQLConstants.BIN_FOLDER, "mysqldump"); //$NON-NLS-1$
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
        if (noData) {
            cmd.add("--no-data"); //$NON-NLS-1$
        }
        if (dumpEvents) cmd.add("--events"); //$NON-NLS-1$
        if (comments) cmd.add("--comments"); //$NON-NLS-1$
	    
        if (!CommonUtils.isEmptyTrimmed(extraCommandArgs)) {
            cmd.add(extraCommandArgs);
        }
    }

    @Override
    protected void setupProcessParameters(ProcessBuilder process) {
        if (!CommonUtils.isEmpty(getToolUserPassword())) {
            process.environment().put(MySQLConstants.ENV_VARIABLE_MYSQL_PWD, getToolUserPassword());
        }
    }

    @Override
    public boolean performFinish() {
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
        store.setValue("MySQL.export.noData", noData);
        store.setValue("MySQL.export.showViews", showViews);
        store.setValue("MySQL.export.extraArgs", extraCommandArgs);

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
        super.startProcessHandler(monitor, arg, processBuilder, process);

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
            new DumpCopierJob(monitor, MySQLMessages.tools_db_export_wizard_monitor_export_db, process.getInputStream(), outFile);
        job.start();
    }

    private static Pattern DEFINER_PATTER = Pattern.compile("DEFINER\\s*=\\s*`[^*]*`@`[0-9a-z\\-_\\.%]*`", Pattern.CASE_INSENSITIVE);

    class DumpFilterJob extends DumpJob {
        protected DumpFilterJob(DBRProgressMonitor monitor, InputStream stream, File outFile)
        {
            super(MySQLMessages.tools_db_export_wizard_job_dump_log_reader, monitor, stream, outFile);
        }

        @Override
        public void runDump() throws IOException {
            monitor.beginTask(MySQLMessages.tools_db_export_wizard_monitor_export_db, 100);
            long prevStatusUpdateTime = 0;
            try {
                NumberFormat numberFormat = NumberFormat.getInstance();

                LineNumberReader reader = new LineNumberReader(new InputStreamReader(input, GeneralUtils.DEFAULT_ENCODING));
                try (OutputStream output = new FileOutputStream(outFile)) {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, GeneralUtils.DEFAULT_ENCODING));
                    for (;;) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        final Matcher matcher = DEFINER_PATTER.matcher(line);
                        if (matcher.find()) {
                            line = matcher.replaceFirst("");
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
