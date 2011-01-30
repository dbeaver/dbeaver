/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class ProjectExportWizard extends Wizard implements IExportWizard {

    private ProjectExportWizardPage mainPage;

    public ProjectExportWizard() {
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Project Export Wizard"); //NON-NLS-1
        setNeedsProgressMonitor(true);
        mainPage = new ProjectExportWizardPage("Export project"); //NON-NLS-1
    }

    public void addPages() {
        super.addPages();
        addPage(mainPage);
    }

	@Override
	public boolean performFinish() {
        List<IProject> projects = mainPage.getProjectsToExport();
        File outputFolder = mainPage.getOutputFolder();

        return exportProjects(projects, outputFolder);
	}

    public boolean exportProjects(final List<IProject> projects, final File outputFolder)
    {
        DBRRunnableWithProgress op = new DBRRunnableWithProgress()
        {
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                if (!outputFolder.exists()) {
                    if (!outputFolder.mkdirs()) {
                        throw new InvocationTargetException(new IOException("Cannot create directory '" + outputFolder.getAbsolutePath() + "'"));
                    }
                }

                monitor.beginTask("Collect meta info", projects.size());
                for (IProject project : projects) {

                    monitor.worked(1);
                }
                monitor.done();

                for (IProject project : projects) {
                    int resourceCount = 5;
                    monitor.beginTask("Export project '" + project.getName() + "'", resourceCount);
                    try {
                        exportProject(monitor, project);
                    } finally {
                        monitor.done();
                    }
                }
            }
        };

        try {
            RuntimeUtils.run(getContainer(), true, true, op);
        }
        catch (InterruptedException ex) {
            return false;
        }
        catch (InvocationTargetException ex) {
            UIUtils.showErrorDialog(
                getShell(),
                "Export error",
                "Cannot export projects",
                ex.getTargetException());
            return false;
        }
        return true;
    }

    private void exportProject(DBRProgressMonitor monitor, IProject project) throws InterruptedException
    {
        Thread.sleep(5000);
    }

}
