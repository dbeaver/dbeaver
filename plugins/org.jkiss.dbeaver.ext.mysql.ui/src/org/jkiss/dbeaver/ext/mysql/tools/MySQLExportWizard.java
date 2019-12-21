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
package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.mysql.MySQLServerHome;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.ext.mysql.tasks.MySQLDatabaseExportInfo;
import org.jkiss.dbeaver.ext.mysql.tasks.MySQLExportSettings;
import org.jkiss.dbeaver.ext.mysql.tasks.MySQLTasks;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.tasks.ui.nativetool.AbstractImportExportWizard;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MySQLExportWizard extends AbstractImportExportWizard<MySQLExportSettings, MySQLDatabaseExportInfo> implements IExportWizard {

    private MySQLExportWizardPageObjects objectsPage;
    private MySQLExportWizardPageSettings settingsPage;

    public MySQLExportWizard(Collection<DBSObject> objects) {
        super(objects, MySQLUIMessages.tools_db_export_wizard_task_name);
    }

    public MySQLExportWizard(DBTTask task) {
        super(task);
    }

    @Override
    protected MySQLExportSettings createSettings() {
        return new MySQLExportSettings();
    }

    @Override
    public String getTaskTypeId() {
        return MySQLTasks.TASK_DATABASE_BACKUP;
    }

    @Override
    public void saveTaskState(DBRRunnableContext runnableContext, Map<String, Object> state) {
        objectsPage.saveState();
        settingsPage.saveState();
        getSettings().saveSettings(runnableContext, new TaskPreferenceStore(state));
    }

    @Override
    public boolean isRunTaskOnFinish() {
        return getCurrentTask() != null;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection);
        objectsPage = new MySQLExportWizardPageObjects(this);
        settingsPage = new MySQLExportWizardPageSettings(this);
    }

    @Override
    public void addPages() {
        addTaskConfigPages();
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
            MySQLUIMessages.tools_db_export_wizard_title,
            CommonUtils.truncateString(NLS.bind(MySQLUIMessages.tools_db_export_wizard_message_export_completed, getObjectsName()), 255),
            SWT.ICON_INFORMATION);
        UIUtils.launchProgram(getSettings().getOutputFolder().getAbsolutePath());
	}

    @Override
    public void fillProcessParameters(List<String> cmd, MySQLDatabaseExportInfo arg) throws IOException
    {
        File dumpBinary = RuntimeUtils.getNativeClientBinary(getClientHome(), MySQLConstants.BIN_FOLDER, "mysqldump"); //$NON-NLS-1$
        String dumpPath = dumpBinary.getAbsolutePath();
        cmd.add(dumpPath);
        MySQLExportSettings settings = getSettings();
        switch (settings.getMethod()) {
            case LOCK_ALL_TABLES:
                cmd.add("--lock-all-tables"); //$NON-NLS-1$
                break;
            case ONLINE:
                cmd.add("--single-transaction"); //$NON-NLS-1$
                break;
        }

        if (settings.isNoCreateStatements()) {
            cmd.add("--no-create-info"); //$NON-NLS-1$
        } else {
            if (CommonUtils.isEmpty(arg.getTables())) {
                cmd.add("--routines"); //$NON-NLS-1$
            }
        }
        if (settings.isAddDropStatements()) {
        	cmd.add("--add-drop-table"); //$NON-NLS-1$
        } else {
            cmd.add("--skip-add-drop-table"); //$NON-NLS-1$
        }
        if (settings.isDisableKeys()) cmd.add("--disable-keys"); //$NON-NLS-1$
        if (settings.isExtendedInserts()) {
            cmd.add("--extended-insert"); //$NON-NLS-1$
        } else {
            cmd.add("--skip-extended-insert"); //$NON-NLS-1$
        }
        if (settings.isBinariesInHex()) {
            cmd.add("--hex-blob"); //$NON-NLS-1$
        }
        if (settings.isNoData()) {
            cmd.add("--no-data"); //$NON-NLS-1$
        }
        if (settings.isDumpEvents()) cmd.add("--events"); //$NON-NLS-1$
        if (settings.isComments()) cmd.add("--comments"); //$NON-NLS-1$

        settings.addExtraCommandArgs(cmd);
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
        settingsPage.saveState();

        return super.performFinish();
    }

    @Override
    public MySQLServerHome findNativeClientHome(String clientHomeId)
    {
        return MySQLDataSourceProvider.getServerHome(clientHomeId);
    }

    @Override
    public List<MySQLDatabaseExportInfo> getRunInfo() {
        return getSettings().getExportObjects();
    }

    @Override
    protected List<String> getCommandLine(MySQLDatabaseExportInfo arg) throws IOException
    {
        List<String> cmd = MySQLToolScript.getMySQLToolCommandLine(this, arg);
        if (!CommonUtils.isEmpty(arg.getTables())) {
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

        String outFileName = GeneralUtils.replaceVariables(getSettings().getOutputFilePattern(), name -> {
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
                case VARIABLE_DATE:
                    return RuntimeUtils.getCurrentDate();
                default:
                    System.getProperty(name);
            }
            return null;
        });

        File outFile = new File(getSettings().getOutputFolder(), outFileName);
        boolean isFiltering = getSettings().isRemoveDefiner();
        Thread job = isFiltering ?
            new DumpFilterJob(monitor, process.getInputStream(), outFile) :
            new DumpCopierJob(monitor, MySQLUIMessages.tools_db_export_wizard_monitor_export_db, process.getInputStream(), outFile);
        job.start();
    }


    class DumpFilterJob extends DumpJob {
        private Pattern DEFINER_PATTER = Pattern.compile("DEFINER\\s*=\\s*`[^*]*`@`[0-9a-z\\-_\\.%]*`", Pattern.CASE_INSENSITIVE);

        DumpFilterJob(DBRProgressMonitor monitor, InputStream stream, File outFile)
        {
            super(MySQLUIMessages.tools_db_export_wizard_job_dump_log_reader, monitor, stream, outFile);
        }

        @Override
        public void runDump() throws IOException {
            monitor.beginTask(MySQLUIMessages.tools_db_export_wizard_monitor_export_db, 100);
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
