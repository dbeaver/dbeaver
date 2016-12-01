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
package org.jkiss.dbeaver.ext.postgresql.tools;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class PostgreBackupWizard extends PostgreBackupRestoreWizard<PostgreDatabaseBackupInfo> implements IExportWizard {

    String compression;
    String encoding;
    boolean showViews;
    public List<PostgreDatabaseBackupInfo> objects = new ArrayList<>();

    private PostgreBackupWizardPageObjects objectsPage;
    private PostgreBackupWizardPageSettings settingsPage;

    public PostgreBackupWizard(Collection<DBSObject> objects) {
        super(objects, "Database backup");

        final DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        this.outputFilePattern = store.getString("Postgre.export.outputFilePattern");
        if (CommonUtils.isEmpty(this.outputFilePattern)) {
            this.outputFilePattern = "dump-${database}-${timestamp}.backup";
        }
        showViews = CommonUtils.getBoolean(store.getString("Postgre.export.showViews"), false);
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
	public void onSuccess() {
        UIUtils.showMessageBox(
            getShell(),
            "Database export",
            "Export '" + getObjectsName() + "'",
            SWT.ICON_INFORMATION);
        UIUtils.launchProgram(outputFolder.getAbsolutePath());
	}

    @Override
    public void fillProcessParameters(List<String> cmd, PostgreDatabaseBackupInfo arg) throws IOException
    {
        super.fillProcessParameters(cmd, arg);

        cmd.add("--format=" + format.getId());
        if (!CommonUtils.isEmpty(compression)) {
            cmd.add("--compress=" + compression);
        }
        if (!CommonUtils.isEmpty(encoding)) {
            cmd.add("--encoding=" + encoding);
        }

        // Objects
        if (objects.isEmpty()) {
            // no dump
        } else if (!CommonUtils.isEmpty(arg.getTables())) {
            for (PostgreTableBase table : arg.getTables()) {
                cmd.add("-t");
                cmd.add(table.getFullyQualifiedName(DBPEvaluationContext.DDL));
            }
        } else if (!CommonUtils.isEmpty(arg.getSchemas())) {
            for (PostgreSchema schema : arg.getSchemas()) {
                cmd.add("-n");
                cmd.add(schema.getName());
            }
        }
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

        final DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        store.setValue("Postgre.export.outputFilePattern", this.outputFilePattern);
        store.setValue("Postgre.export.showViews", showViews);

        return super.performFinish();
    }

    @Override
    public Collection<PostgreDatabaseBackupInfo> getRunInfo() {
        return objects;
    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, final PostgreDatabaseBackupInfo arg, ProcessBuilder processBuilder, Process process)
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
                    case VARIABLE_TIMESTAMP:
                        return RuntimeUtils.getCurrentTimeStamp();
                    default:
                        System.getProperty(name);
                }
                return null;
            }
        });

        File outFile = new File(outputFolder, outFileName);
        Thread job = new DumpCopierJob(monitor, "Export database", process.getInputStream(), outFile);
        job.start();
    }


}
