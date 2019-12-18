/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.tools;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreBackupSettings;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

class PostgreBackupWizard extends PostgreBackupRestoreWizard<PostgreBackupSettings, PostgreDatabaseBackupInfo> implements IExportWizard {

    public List<PostgreDatabaseBackupInfo> objects = new ArrayList<>();

    private PostgreBackupWizardPageObjects objectsPage;
    private PostgreBackupWizardPageSettings settingsPage;

    public PostgreBackupWizard(Collection<DBSObject> objects) {
        super(objects, PostgreMessages.wizard_backup_title);
    }

    @Override
    public String getTaskTypeId() {
        return "postgresDatabaseBackup";
    }

    @Override
    public void saveTaskState(DBRRunnableContext runnableContext, Map<String, Object> state) {
        // TODO: implement
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection);
        objectsPage = new PostgreBackupWizardPageObjects(this);
        settingsPage = new PostgreBackupWizardPageSettings(this);
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
            PostgreMessages.wizard_backup_msgbox_success_title,
            NLS.bind(PostgreMessages.wizard_backup_msgbox_success_description, CommonUtils.truncateString(getObjectsName(), 255)),
            SWT.ICON_INFORMATION);
        UIUtils.launchProgram(getSettings().getOutputFolder().getAbsolutePath());
	}

    @Override
    public void fillProcessParameters(List<String> cmd, PostgreDatabaseBackupInfo arg) throws IOException
    {
        super.fillProcessParameters(cmd, arg);

        cmd.add("--format=" + format.getId());
        if (!CommonUtils.isEmpty(getSettings().getCompression())) {
            cmd.add("--compress=" + getSettings().getCompression());
        }
        if (!CommonUtils.isEmpty(getSettings().getEncoding())) {
            cmd.add("--encoding=" + getSettings().getEncoding());
        }
        if (getSettings().isUseInserts()) {
            cmd.add("--inserts");
        }
        if (getSettings().isNoPrivileges()) {
            cmd.add("--no-privileges");
        }
        if (getSettings().isNoOwner()) {
            cmd.add("--no-owner");
        }

        // Objects
        if (objects.isEmpty()) {
            // no dump
        } else if (!CommonUtils.isEmpty(arg.getTables())) {
            for (PostgreTableBase table : arg.getTables()) {
                cmd.add("-t");
                // Use explicit quotes in case of quoted identifiers (#5950)
                cmd.add(escapeCLIIdentifier(table.getSchema().getName() + "." + table.getName()));
            }
        }
        if (!CommonUtils.isEmpty(arg.getSchemas())) {
            for (PostgreSchema schema : arg.getSchemas()) {
                cmd.add("-n");
                // Use explicit quotes in case of quoted identifiers (#5950)
                cmd.add(escapeCLIIdentifier(schema.getName()));
            }
        }
    }

    private static String escapeCLIIdentifier(String name) {
        return "\"" + name.replace("\"", "\\\"") + "\"";
    }

    @Override
    protected List<String> getCommandLine(PostgreDatabaseBackupInfo arg) throws IOException {
        List<String> cmd = PostgreToolScript.getPostgreToolCommandLine(this, arg);
        cmd.add(arg.getDatabase().getName());

        return cmd;
    }

    @Override
    public boolean performFinish() {
        objectsPage.saveState();

        return super.performFinish();
    }

    @Override
    protected PostgreBackupSettings createSettings() {
        return new PostgreBackupSettings();
    }

    @Override
    public Collection<PostgreDatabaseBackupInfo> getRunInfo() {
        return objects;
    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, final PostgreDatabaseBackupInfo arg, ProcessBuilder processBuilder, Process process)
    {
        super.startProcessHandler(monitor, arg, processBuilder, process);

        String outFileName = GeneralUtils.replaceVariables(getSettings().getOutputFilePattern(), name -> {
            switch (name) {
                case VARIABLE_DATABASE:
                    return arg.getDatabase().getName();
                case VARIABLE_HOST:
                    return arg.getDatabase().getDataSource().getContainer().getConnectionConfiguration().getHostName();
                case VARIABLE_TABLE:
                    final Iterator<PostgreTableBase> iterator = arg.getTables() == null ? null : arg.getTables().iterator();
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
        Thread job = new DumpCopierJob(monitor, "Export database", process.getInputStream(), outFile);
        job.start();
    }


}
