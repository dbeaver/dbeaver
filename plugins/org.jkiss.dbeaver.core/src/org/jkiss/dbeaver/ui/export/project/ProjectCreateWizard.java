/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.project;

import org.eclipse.osgi.util.NLS;
import org.jkiss.utils.CommonUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;

public class ProjectCreateWizard extends Wizard implements INewWizard {

    private ProjectCreateData data = new ProjectCreateData();

    public ProjectCreateWizard() {
	}

	@Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(CoreMessages.dialog_project_create_wizard_title);
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        super.addPages();
        addPage(new ProjectCreateWizardPageSettings(data));
    }

	@Override
	public boolean performFinish() {
        try {
            RuntimeUtils.run(getContainer(), true, true, new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        createProject(monitor);
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        }
        catch (InterruptedException ex) {
            return false;
        }
        catch (InvocationTargetException ex) {
            UIUtils.showErrorDialog(
                getShell(),
                CoreMessages.dialog_project_create_wizard_error_cannot_create,
                CoreMessages.dialog_project_create_wizard_error_cannot_create_message,
                ex.getTargetException());
            return false;
        }
        return true;
	}

    private void createProject(DBRProgressMonitor monitor) throws DBException, CoreException
    {
        IWorkspace workspace = DBeaverCore.getInstance().getWorkspace();
        IProject project = workspace.getRoot().getProject(data.getName());
        if (project.exists()) {
            throw new DBException(NLS.bind(CoreMessages.dialog_project_create_wizard_error_already_exists, data.getName()));
        }
        project.create(monitor.getNestedMonitor());

        project.open(monitor.getNestedMonitor());

        if (!CommonUtils.isEmpty(data.getDescription())) {
            final IProjectDescription description = workspace.newProjectDescription(project.getName());
            description.setComment(data.getDescription());
            project.setDescription(description, monitor.getNestedMonitor());
        }

        DBeaverCore.getInstance().getProjectRegistry().addProject(project);
    }

}
