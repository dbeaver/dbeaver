/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.tasks.ui.wizard;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.internal.TaskUIMessages;
import org.jkiss.dbeaver.tasks.ui.registry.TaskUIRegistry;
import org.jkiss.dbeaver.tasks.ui.view.DatabaseTasksView;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseWizard;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class TaskConfigurationWizard<SETTINGS extends DBTTaskSettings> extends BaseWizard implements IWorkbenchWizard {

    private static final Log log = Log.getLog(TaskConfigurationWizard.class);

    private DBTTask currentTask;
    private IStructuredSelection currentSelection;
    private Button saveAsTaskButton;

    private Map<String, Object> variables;
    private DBTTaskContext taskContext;

    protected TaskConfigurationWizard() {
    }

    protected TaskConfigurationWizard(@Nullable DBTTask task) {
        this.currentTask = task;
    }

    protected abstract SETTINGS getSettings();

    protected abstract String getDefaultWindowTitle();

    public boolean isTaskEditor() {
        return currentTask != null;
    }

    public boolean isNewTaskEditor() {
        return currentTask != null && getProject().getTaskManager().getTaskById(currentTask.getId()) == null;
    }

    public abstract String getTaskTypeId();

    public abstract void saveTaskState(DBRRunnableContext runnableContext, DBTTask task, Map<String, Object> state);

    public boolean isRunTaskOnFinish() {
        return getCurrentTask() != null && !getCurrentTask().isTemporary() && !getContainer().isSelectorMode();
    }

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

    public DBTTaskType getTaskType() {
        return TaskRegistry.getInstance().getTaskType(getTaskTypeId());
    }

    protected void updateWizardTitle() {
        String wizTitle = getDefaultWindowTitle();
        if (isTaskEditor()) {
            TaskConfigurationWizardPageTask taskPage = getContainer() == null ? null : getContainer().getTaskPage();
            wizTitle += " - [" + (taskPage == null ? currentTask.getName() : taskPage.getTaskName()) + "]";
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
        getContainer().addPageChangedListener(event -> updateWizardTitle());
    }

    @Override
    public void addPages() {
        super.addPages();
        addTaskConfigPages();
    }

    protected boolean isTaskConfigPage(IWizardPage page) {
        return page instanceof TaskConfigurationWizardPageTask || page instanceof TaskConfigurationWizardPageSettings;
    }

    protected void addTaskConfigPages() {
        // If we are in task edit mode then add special first page.
        // Do not add it if this is an ew task wizard (because this page is added separately)
        if (isCurrentTaskSaved()) {
            // Task editor. Add first page
            addPage(new TaskConfigurationWizardPageTask(getCurrentTask()));
            addPage(new TaskConfigurationWizardPageSettings(getCurrentTask()));
        }
    }

    public boolean isCurrentTaskSaved() {
        return getCurrentTask() != null && getCurrentTask().getProject().getTaskManager().getTaskById(getCurrentTask().getId()) != null;
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        IWizardPage nextPage = super.getNextPage(page);
        if (nextPage instanceof TaskConfigurationWizardPageSettings &&
            page instanceof TaskConfigurationWizardPageTask &&
            !TaskUIRegistry.getInstance().supportsConfiguratorPage(getContainer().getTaskPage().getSelectedTaskType()))
        {
            // Skip settings page (not supported by task type)
            return getNextPage(nextPage);
        }
        return nextPage;
    }

    @Override
    public IWizardPage getPreviousPage(IWizardPage page) {
        IWizardPage prevPage = super.getPreviousPage(page);
        if (prevPage instanceof TaskConfigurationWizardPageSettings &&
            !TaskUIRegistry.getInstance().supportsConfiguratorPage(getContainer().getTaskPage().getSelectedTaskType()))
        {
            // Skip settings page (not supported by task type)
            return getPreviousPage(prevPage);
        }
        return prevPage;
    }

    @Override
    public boolean canFinish() {
        for (IWizardPage page : getPages()) {
            if (isPageValid(page) && !page.isPageComplete()) {
                return false;
            }
        }
        TaskConfigurationWizardPageTask taskPage = getContainer().getTaskPage();
        if (taskPage != null && !taskPage.isPageComplete()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean performFinish() {
        if (currentTask != null && !currentTask.isTemporary()) {
            saveTask();
        }

        if (isRunTaskOnFinish()) {
            if (!runTask()) {
                return false;
            }
        }

        return true;
    }

    protected boolean runTask() {
        try {
            DBTTask task = getCurrentTask();
            if (task == null) {
                task = getProject().getTaskManager().createTemporaryTask(getTaskType(), getWindowTitle());
                saveConfigurationToTask(task);
            }
            // Run task thru task manager
            // Pass executor to visualize task progress in UI
            TaskWizardExecutor executor = new TaskWizardExecutor(getRunnableContext(), task, log, System.out);
            if (getCurrentTask() == null) {
                // Execute directly in wizard
                executor.executeTask();
            } else {
                task.getProject().getTaskManager().runTask(task, executor, Collections.emptyMap());
            }
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Task run error", e.getMessage(), e);
            return false;
        }
        return true;
    }

    protected boolean isPageValid(IWizardPage page) {
        return true;
    }

    private void saveTask() {
        DBTTask currentTask = getCurrentTask();
        if (currentTask == null || currentTask.isTemporary()) {
            // Create new task
            DBTTaskType taskType = getTaskType();
            if (taskType == null) {
                DBWorkbench.getPlatformUI().showError("No task type", "Can't find task type " + getTaskTypeId());
                return;
            }
            EditTaskConfigurationDialog dialog = new EditTaskConfigurationDialog(getContainer().getShell(), getProject(), taskType);
            if (dialog.open() == IDialogConstants.OK_ID) {
                setCurrentTask(currentTask = dialog.getTask());
            } else {
                return;
            }
        } else {
            TaskConfigurationWizardPageTask taskPage = getContainer().getTaskPage();
            if (taskPage != null) {
                taskPage.saveSettings();
            }
        }
        DBTTask theTask = currentTask;
        saveConfigurationToTask(theTask);
    }

    protected void saveConfigurationToTask(DBTTask theTask) {
        Map<String, Object> state = new LinkedHashMap<>();
        saveTaskState(getRunnableContext(), theTask, state);

        DBTTaskContext context = getTaskContext();
        if (context != null) {
            DBTaskUtils.saveTaskContext(state, context);
        }
        if (theTask.getType().supportsVariables()) {
            DBTaskUtils.setVariables(state, getTaskVariables());
        }
        theTask.setProperties(state);
        try {
            theTask.getProject().getTaskManager().updateTaskConfiguration(theTask);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Task save error", "Error saving task configuration", e);
        }
    }

    public void createTaskSaveGroup(Composite parent) {
        Group taskGroup = UIUtils.createControlGroup(
            parent, TaskUIMessages.task_config_wizard_group_task_label, 2, GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING, 0);
        createTaskSaveButtons(taskGroup, false, 1);
    }

    public void createTaskSaveButtons(Composite parent, boolean horizontal, int hSpan) {
        if (getContainer().isSelectorMode()) {
            // Do not create save buttons
            UIUtils.createEmptyLabel(parent, hSpan, 1);
        } else {
            Composite panel = new Composite(parent, SWT.NONE);
            if (parent.getLayout() instanceof GridLayout) {
                GridData gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.horizontalSpan = hSpan;
                panel.setLayoutData(gd);
            }
            boolean supportsVariables = getTaskType().supportsVariables();
            panel.setLayout(new GridLayout(horizontal ? (supportsVariables ? 3 : 2) : 1, false));

            if (supportsVariables) {
                UIUtils.createDialogButton(panel, TaskUIMessages.task_config_wizard_button_variables + " ...", new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        configureVariables();
                    }
                });
            }

            saveAsTaskButton = UIUtils.createDialogButton(panel, TaskUIMessages.task_config_wizard_button_save_task, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    saveTask();
                }
            });
            Link tasksLink = UIUtils.createLink(panel, "<a>" + TaskUIMessages.task_config_wizard_link_open_tasks_view + "</a>", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    try {
                        UIUtils.getActiveWorkbenchWindow().getActivePage().showView(DatabaseTasksView.VIEW_ID);
                    } catch (PartInitException e1) {
                        DBWorkbench.getPlatformUI().showError("Show view", "Error opening database tasks view", e1);
                    }
                }
            });
            tasksLink.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        }
    }

    private void configureVariables() {
        Map<String, Object> variables = getTaskVariables();
        EditTaskVariablesDialog dialog = new EditTaskVariablesDialog(getContainer().getShell(), variables);
        if (dialog.open() == IDialogConstants.OK_ID) {
            this.variables = dialog.getVariables();
        }
    }

    protected Map<String, Object> getTaskVariables() {
        if (variables == null) {
            if (currentTask != null) {
                variables = DBTaskUtils.getVariables(currentTask);
            } else {
                variables = new LinkedHashMap<>();
            }
        }
        return variables;
    }

    public DBTTaskContext getTaskContext() {
        if (taskContext == null) {
            if (currentTask != null) {
                taskContext = DBTaskUtils.loadTaskContext(currentTask.getProperties());
            }
        }
        return taskContext;
    }

    protected void saveTaskContext(DBCExecutionContext executionContext) {
        taskContext = DBTaskUtils.extractContext(executionContext);
    }

    public void updateSaveTaskButtons() {
        if (saveAsTaskButton != null) {
            // TODO: we should be able to save/run task immediately if it was saved before.
            // TODO: There is a bug in DT wizard which doesn't let to do it (producers/consumers are initialized only on the last page).
            // TODO: init transfer for all deserialized producers/consumers
            saveAsTaskButton.setEnabled(/*(getTaskWizard() != null && getTaskWizard().isCurrentTaskSaved()) || */canFinish());
        }
    }

    @Override
    public IWizardPage getStartingPage() {
        IWizardPage startingPage = super.getStartingPage();
        if (getContainer().isEditMode()) {
            // Start from second page for task editor
            return getNextPage(startingPage);
        }
        return startingPage;
    }

}