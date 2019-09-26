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
package org.jkiss.dbeaver.ui.task;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.ui.dialogs.BaseWizard;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.Map;

public abstract class TaskConfigurationWizard extends BaseWizard implements IWorkbenchWizard {

    private DBTTask currentTask;
    private IStructuredSelection currentSelection;

    protected TaskConfigurationWizard() {
    }

    protected TaskConfigurationWizard(@Nullable DBTTask task) {
        this.currentTask = task;
    }

    protected abstract String getDefaultWindowTitle();

    public boolean isTaskEditor() {
        return currentTask != null;
    }

    public abstract String getTaskTypeId();

    public abstract void saveTaskState(Map<String, Object> state);

    public IStructuredSelection getCurrentSelection() {
        return currentSelection;
    }

    public DBTTask getCurrentTask() {
        return currentTask;
    }

    public void setCurrentTask(DBTTask currentTask) {
        this.currentTask = currentTask;
        updateWizardTitle();
        getContainer().updateButtons();
    }

    public DBPProject getProject() {
        return currentTask != null ? currentTask.getProject() : NavigatorUtils.getSelectedProject();
    }

    protected void updateWizardTitle() {
        String wizTitle = getDefaultWindowTitle();
        if (isTaskEditor()) {
            wizTitle += " - [" + currentTask.getName() + "]";
        }
        setWindowTitle(wizTitle);
    }

    @Override
    public TaskConfigurationWizardDialog getContainer() {
        return (TaskConfigurationWizardDialog) super.getContainer();
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        updateWizardTitle();
        setNeedsProgressMonitor(true);
        this.currentSelection = currentSelection;
    }

    @Override
    public void addPages() {
        super.addPages();
        // If we are in task edit mode then add special first page.
        // Do not add it if this is an ew task wizard (because this page is added separately)
        if (getCurrentTask() != null && getCurrentTask().getProject().getTaskManager().getTaskConfiguration(getCurrentTask().getId()) != null) {
            // Task editor. Add first page
            addPage(new TaskConfigurationWizardPageTask(getCurrentTask()));
        }
    }

    @Override
    public boolean canFinish() {
        if (currentTask != null) {
            return true;
        }
        for (IWizardPage page : getPages()) {
            if (isPageValid(page) && !page.isPageComplete()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean performFinish() {
        if (currentTask != null) {
            saveTask();
        }
        return true;
    }

    protected boolean isPageValid(IWizardPage page) {
        return true;
    }

    public void saveTask() {
        TaskConfigurationWizardPageTask taskPage = getContainer().getTaskPage();
        if (taskPage != null) {
            taskPage.saveSettings();
        }
        saveTaskState(getCurrentTask().getProperties());
    }

}