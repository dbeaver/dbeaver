/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskSettingsInput;
import org.jkiss.dbeaver.registry.task.TaskConstants;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.registry.task.TaskTypeDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigurator;
import org.jkiss.dbeaver.tasks.ui.internal.TaskUIMessages;
import org.jkiss.dbeaver.tasks.ui.registry.TaskUIRegistry;
import org.jkiss.dbeaver.ui.dialogs.IWizardPageNavigable;
import org.jkiss.dbeaver.ui.dialogs.MultiPageWizardDialog;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

/**
 * Task configuration wizard dialog
 */
public class TaskConfigurationWizardDialog extends MultiPageWizardDialog {

    private static final Log log = Log.getLog(TaskConfigurationWizardDialog.class);
    private TaskConfigurationWizard<?> nestedTaskWizard;
    private TaskConfigurationWizardPageTask taskEditPage;
    private boolean editMode;
    private boolean selectorMode;

    public TaskConfigurationWizardDialog(IWorkbenchWindow window, TaskConfigurationWizard<?> wizard) {
        this(window, wizard, null);
    }

    public TaskConfigurationWizardDialog(IWorkbenchWindow window, TaskConfigurationWizard<?> wizard, IStructuredSelection selection) {
        super(window, wizard, selection);
        setFinishButtonLabel(IDialogConstants.PROCEED_LABEL);

        if (selection != null && !selection.isEmpty()) {
            if (wizard.getSettings() instanceof DBTTaskSettingsInput) {
                List<Object> inputObjects = new ArrayList<>();
                for (Object so : selection.toArray()) {
                    if (wizard.getTaskType().isObjectApplicable(so)) {
                        inputObjects.add(so);
                    }
                }
                ((DBTTaskSettingsInput) wizard.getSettings()).loadSettingsFromInput(inputObjects);
            }
        }

        addPageChangedListener(new IPageChangedListener() {
            @Override
            public void pageChanged(PageChangedEvent event) {
                removePageChangedListener(this);
                getWizard().onWizardActivation();
            }
        });
    }

    public TaskConfigurationWizardDialog(IWorkbenchWindow window) {
        this(window, new NewTaskConfigurationWizard(), null);
    }

    @Override
    protected boolean isModalWizard() {
        return RuntimeUtils.isLinux();
    }

    @Override
    public TaskConfigurationWizard<?> getWizard() {
        return (TaskConfigurationWizard) super.getWizard();
    }

    @NotNull
    @Override
    protected EnumSet<PageCompletionMark> getShownCompletionMarks() {
        if (getWizard().isCurrentTaskSaved()) {
            return EnumSet.noneOf(PageCompletionMark.class);
        } else {
            return EnumSet.of(PageCompletionMark.COMPLETE);
        }
    }

    @Override
    protected boolean isDisableControlsOnRun() {
        return true;
    }

    public TaskConfigurationWizard<?> getTaskWizard() {
        return (TaskConfigurationWizard) super.getWizard();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setHelpAvailable(false);

        Control dialogArea = super.createDialogArea(parent);

        getWizard().initializeWizard(parent);

        return dialogArea;
    }

    @Override
    protected void createBottomLeftArea(Composite pane) {
        // Task management controls
        getWizard().createTaskSaveButtons(pane, true, 1);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        {
            parent.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

            //createTaskSaveButtons(parent, 1);

            Label spacer = new Label(parent, SWT.NONE);
            spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

            ((GridLayout) parent.getLayout()).numColumns += 1;
        }

        {
            if (getWizard().isNewTaskEditor() || getNavPagesCount() > 1) {
                createButton(parent, IDialogConstants.BACK_ID, IDialogConstants.BACK_LABEL, false);
                Button nextButton = createButton(parent, IDialogConstants.NEXT_ID, IDialogConstants.NEXT_LABEL, true);
                getShell().setDefaultButton(nextButton);
            }
        }

        super.createButtonsForButtonBar(parent);
    }

    private int getNavPagesCount() {
        int navPagesNum = 0;
        for (IWizardPage page2 : getWizard().getPages()) {
            if (!(page2 instanceof IWizardPageNavigable) ||
                ((IWizardPageNavigable) page2).isPageApplicable() &&
                    ((IWizardPageNavigable) page2).isPageNavigable()) {
                navPagesNum++;
            }
        }
        return navPagesNum;
    }

    @Override
    public void disableButtonsOnProgress() {
        Button button = getButton(IDialogConstants.BACK_ID);
        if (button != null) {
            button.setEnabled(false);
        }
        getWizard().updateSaveTaskButton(false);
        super.disableButtonsOnProgress();
    }

    @Override
    public void enableButtonsAfterProgress() {
        Button button = getButton(IDialogConstants.BACK_ID);
        if (button != null) {
            button.setEnabled(true);
        }
        getWizard().updateSaveTaskButton(true);
        super.enableButtonsAfterProgress();
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.NEXT_ID &&
            getWizard() instanceof NewTaskConfigurationWizard &&
            ((NewTaskConfigurationWizard)getWizard()).isLastTaskPreconfigPage(getCurrentPage()))
        {
            if (!getCurrentPage().isPageComplete()) {
                return;
            }
            taskEditPage = getTaskPage();
            try {
                TaskConfigurationWizard<?> nextTaskWizard = taskEditPage.getTaskWizard();
                if (nextTaskWizard != nestedTaskWizard) {
                    // Now we need to create real wizard, initialize it and inject in this dialog
                        nestedTaskWizard = nextTaskWizard;
                        nestedTaskWizard.addPages();
                        nestedTaskWizard.initializeWizard(this.getShell().getParent());
                        setWizard(nestedTaskWizard);
                }
            } catch (Exception e) {
                setErrorMessage(NLS.bind(TaskUIMessages.task_configuration_wizard_dialog_configuration_error, e.getMessage()));
                log.error("Can't create task " + taskEditPage.getSelectedTaskType().getName() + " configuration wizard", e);
                return;
            }
            // Show first page of new wizard
            for (IWizardPage page : nestedTaskWizard.getPages()) {
                if (page instanceof TaskConfigurationWizardPageSettings) {
                    IWizardPage nextPage = nestedTaskWizard.getNextPage(page);
                    if (nextPage != null) {
                        showPage(nextPage);
                        return;
                    }
                }
            }
            showPage(nestedTaskWizard.getStartingPage());
            return;
        }
        super.buttonPressed(buttonId);
    }

    @Override
    public void updateButtons() {
        super.updateButtons();
        getWizard().updateSaveTaskButtons();
        if (getTaskWizard().canFinish()) {
            Button finishButton = getButton(IDialogConstants.OK_ID);
            if (finishButton != null && !finishButton.isDisposed()) {
                getShell().setDefaultButton(finishButton);
            }
        }
    }

    @Override
    protected Control createContents(Composite parent) {
        return super.createContents(parent);
    }

    TaskConfigurationWizardPageTask getTaskPage() {
        if (taskEditPage != null) {
            return taskEditPage;
        } else {
            IWizardPage[] pages = getWizard().getPages();
            if (pages.length > 0 && pages[0] instanceof TaskConfigurationWizardPageTask) {
                return (TaskConfigurationWizardPageTask)pages[0];
            }
        }
        return null;
    }

    public DBTTask getTask() {
        return getWizard().getCurrentTask();
    }


    public boolean isSelectorMode() {
        return selectorMode;
    }

    public void setSelectorMode(boolean selectorMode) {
        this.selectorMode = selectorMode;
        if (selectorMode) {
            setFinishButtonLabel(TaskUIMessages.task_config_wizard_dialog_button_save);
        }
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    @NotNull
    @Override
    protected IWizardPage getStartingPage() {
        return getWizard().getStartingPage();
    }

    /**
     * Opens new task dialog
     *
     * @param window - workbench window to get parent shell from
     * @param project - project for task execution
     * @param taskTypeId - task id
     * @param selection - database objects to apply the task to
     * @return the return code
     */
    public static int openNewTaskDialog(
        @NotNull IWorkbenchWindow window,
        @NotNull DBPProject project,
        @NotNull String taskTypeId,
        @NotNull IStructuredSelection selection
    ) {
        return openNewTaskDialogImpl(window, project, taskTypeId, selection, false);
    }

    /**
     * Opens new task dialog for the tool
     *
     * @param window - workbench window to get parent shell from
     * @param project - project for task execution
     * @param taskTypeId - task id
     * @param selection - database objects to apply the tool task to
     * @return the return code
     */
    public static int openNewToolTaskDialog(
        @NotNull IWorkbenchWindow window,
        @NotNull DBPProject project,
        @NotNull String taskTypeId,
        @NotNull IStructuredSelection selection
    ) {
        return openNewTaskDialogImpl(window, project, taskTypeId, selection, true);
    }

    private static int openNewTaskDialogImpl(
        @NotNull IWorkbenchWindow window,
        @NotNull DBPProject project,
        @NotNull String taskTypeId,
        @NotNull IStructuredSelection selection,
        boolean isToolTask
    ) {
        TaskTypeDescriptor taskType = TaskRegistry.getInstance().getTaskType(taskTypeId);
        if (taskType == null) {
            DBWorkbench.getPlatformUI().showError("Bad task type", "Task type '" + taskTypeId + "' not found");
            return IDialogConstants.CANCEL_ID;
        }
        try {
            DBTTask task = project.getTaskManager().createTemporaryTask(taskType, taskType.getName());
            task.setProperties(new HashMap<>());
            if (isToolTask) {
                task.getProperties().put(TaskConstants.TOOL_TASK_PROP, true);
            }
            DBTTaskConfigurator configurator = TaskUIRegistry.getInstance().createConfigurator(taskType);
            TaskConfigurationWizard<?> configWizard = configurator.createTaskConfigWizard(task);

            TaskConfigurationWizardDialog dialog = configWizard.createWizardDialog(window, selection);
            return dialog.open();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Task create error", "Error creating task '" + taskTypeId + "'", e);
            return IDialogConstants.CANCEL_ID;
        }
    }

}
