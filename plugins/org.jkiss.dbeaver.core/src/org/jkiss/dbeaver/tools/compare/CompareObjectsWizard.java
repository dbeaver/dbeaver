/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.compare;

import org.jkiss.dbeaver.Log;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class CompareObjectsWizard extends Wizard implements IExportWizard {

    private static final Log log = Log.getLog(CompareObjectsWizard.class);

    private static final String RS_COMPARE_WIZARD_DIALOG_SETTINGS = "CompareWizard";//$NON-NLS-1$

    private CompareObjectsSettings settings;

    public CompareObjectsWizard(List<DBNDatabaseNode> nodes)
    {
        this.settings = new CompareObjectsSettings(nodes);
        IDialogSettings section = UIUtils.getDialogSettings(RS_COMPARE_WIZARD_DIALOG_SETTINGS);
        setDialogSettings(section);

        settings.loadFrom(section);
    }

    @Override
    public void dispose()
    {
        super.dispose();
    }

    public CompareObjectsSettings getSettings()
    {
        return settings;
    }

    @Override
    public void addPages()
    {
        super.addPages();
        addPage(new CompareObjectsPageSettings());
        addPage(new CompareObjectsPageOutput());
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection)
    {
        setWindowTitle("Compare objects");
        setNeedsProgressMonitor(true);
    }

    private void showError(String error)
    {
        ((WizardPage)getContainer().getCurrentPage()).setErrorMessage(error);
    }

    @Override
    public boolean performFinish()
    {
        // Save settings
        getSettings().saveTo(getDialogSettings());
        showError(null);

        // Compare
        final CompareObjectsExecutor executor = new CompareObjectsExecutor(settings);
        try {
            DBeaverUI.run(getContainer(), true, true, new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        CompareReport report = generateReport(monitor, executor);

                        renderReport(monitor, report);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
            UIUtils.showMessageBox(getShell(), "Objects compare", "Objects compare finished", SWT.ICON_INFORMATION);
        } catch (InvocationTargetException e) {
            if (executor.getInitializeError() != null) {
                showError(executor.getInitializeError().getMessage());
            } else {
                log.error(e.getTargetException());
                showError(e.getTargetException().getMessage());
            }
            return false;
        } catch (InterruptedException e) {
            showError("Compare interrupted");
            return false;
        } finally {
            executor.dispose();
        }

        // Done
        return true;
    }

    private CompareReport generateReport(DBRProgressMonitor monitor, CompareObjectsExecutor executor) throws DBException, InterruptedException
    {
        monitor.beginTask("Compare objects", 1000);
        CompareReport report = executor.compareObjects(monitor, getSettings().getNodes());
        monitor.done();
        return report;
    }

    private void renderReport(DBRProgressMonitor monitor, CompareReport report)
    {
        try {
            File reportFile;
            switch (settings.getOutputType()) {
                case BROWSER:
                    reportFile = File.createTempFile("compare-report", ".html");
                    break;
                default:
                {
                    StringBuilder fileName = new StringBuilder("compare");//"compare-report.html";
                    for (DBNDatabaseNode node : report.getNodes()) {
                        fileName.append("-").append(CommonUtils.escapeIdentifier(node.getName()));
                    }
                    fileName.append("-report.html");
                    reportFile = new File(settings.getOutputFolder(), fileName.toString());
                    break;
                }
            }

            reportFile.deleteOnExit();
            OutputStream outputStream = new FileOutputStream(reportFile);
            try {
                monitor.beginTask("Render report", report.getReportLines().size());
                CompareReportRenderer reportRenderer = new CompareReportRenderer();
                reportRenderer.renderReport(monitor, report, getSettings(), outputStream);
                monitor.done();
            } finally {
                ContentUtils.close(outputStream);
            }
            UIUtils.launchProgram(reportFile.getAbsolutePath());
        } catch (IOException e) {
            showError(e.getMessage());
            log.error(e);
        }
    }

}