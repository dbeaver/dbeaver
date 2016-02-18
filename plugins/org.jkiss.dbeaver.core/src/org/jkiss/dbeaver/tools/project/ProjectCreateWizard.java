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
package org.jkiss.dbeaver.tools.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

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
            DBeaverUI.run(getContainer(), true, true, new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
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
        project.create(RuntimeUtils.getNestedMonitor(monitor));

        project.open(RuntimeUtils.getNestedMonitor(monitor));

        if (!CommonUtils.isEmpty(data.getDescription())) {
            final IProjectDescription description = workspace.newProjectDescription(project.getName());
            description.setComment(data.getDescription());
            project.setDescription(description, RuntimeUtils.getNestedMonitor(monitor));
        }
    }

}
