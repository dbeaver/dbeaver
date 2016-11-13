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
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class PostgreRestoreWizard extends PostgreBackupRestoreWizard<PostgreDatabaseRestoreInfo> implements IExportWizard {

    private PostgreRestoreWizardPageSettings settingsPage;
    private PostgreDatabaseRestoreInfo restoreInfo;

    public String inputFile;

    public PostgreRestoreWizard(PostgreDatabase database) {
        super(Collections.<DBSObject>singletonList(database), "Database restore");
        restoreInfo = new PostgreDatabaseRestoreInfo(database);
    }

    @Override
    public boolean isExportWizard() {
        return false;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection);
        settingsPage = new PostgreRestoreWizardPageSettings(this);
    }

    @Override
    public void addPages() {
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
            "Database restore",
            "Restore '" + getObjectsName() + "'",
            SWT.ICON_INFORMATION);
	}

    @Override
    public void fillProcessParameters(List<String> cmd, PostgreDatabaseRestoreInfo arg) throws IOException
    {
        super.fillProcessParameters(cmd, arg);
    }

    @Override
    protected List<String> getCommandLine(PostgreDatabaseRestoreInfo arg) throws IOException {
        List<String> cmd = super.getCommandLine(arg);
        cmd.add("--format=" + format.getId());
        cmd.add("--dbname=" + arg.getDatabase().getName());

        return cmd;
    }

    @Override
    public Collection<PostgreDatabaseRestoreInfo> getRunInfo() {
        return Collections.singleton(restoreInfo);
    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, final PostgreDatabaseRestoreInfo arg, ProcessBuilder processBuilder, Process process)
    {
        super.startProcessHandler(monitor, arg, processBuilder, process);
        new BinaryFileTransformerJob(monitor, new File(inputFile), process.getOutputStream()).start();
    }

}
