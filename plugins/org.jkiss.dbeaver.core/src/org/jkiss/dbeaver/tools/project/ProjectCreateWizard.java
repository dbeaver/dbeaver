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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverNature;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.preferences.PrefPageProjectSettings;
import org.jkiss.dbeaver.ui.preferences.WizardPrefPage;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;

public class ProjectCreateWizard extends BasicNewProjectResourceWizard implements INewWizard {

    private ProjectCreateData data = new ProjectCreateData();
    private WizardPrefPage projectSettingsPage;

    public ProjectCreateWizard() {
	}

	@Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection);
        setWindowTitle(CoreMessages.dialog_project_create_wizard_title);
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        super.addPages();
        final PrefPageProjectSettings projectSettingsPref = new PrefPageProjectSettings();
        projectSettingsPage = new WizardPrefPage(projectSettingsPref, "Resources", "Project resources");
        addPage(projectSettingsPage);
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        if (page instanceof WizardNewProjectCreationPage) {
            return projectSettingsPage;
        }
        return super.getNextPage(page);
    }

    @Override
	public boolean performFinish() {
        if (super.performFinish()) {
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
            } catch (InterruptedException ex) {
                return false;
            } catch (InvocationTargetException ex) {
                UIUtils.showErrorDialog(
                    getShell(),
                    CoreMessages.dialog_project_create_wizard_error_cannot_create,
                    CoreMessages.dialog_project_create_wizard_error_cannot_create_message,
                    ex.getTargetException());
                return false;
            }
            return true;
        } else {
            return false;
        }
	}

    private void createProject(DBRProgressMonitor monitor) throws DBException, CoreException
    {
        final IProgressMonitor nestedMonitor = RuntimeUtils.getNestedMonitor(monitor);
        final IProject project = getNewProject();

        final IProjectDescription description = project.getDescription();
        if (!CommonUtils.isEmpty(data.getDescription())) {
            description.setComment(data.getDescription());
        }
        description.setNatureIds(new String[] {DBeaverNature.NATURE_ID});
        project.setDescription(description, nestedMonitor);

        if (!project.isOpen()) {
            project.open(nestedMonitor);
        }
    }

}
