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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.internal.UIMessages;

/**
 * Task configuration wizard dialog
 */
public class TaskConfigurationWizardDialog extends ActiveWizardDialog {

    private static final Log log = Log.getLog(TaskConfigurationWizardDialog.class);
    private static final int SAVE_TASK_BTN_ID = 1000;
    private TaskConfigurationWizard nestedTaskWizard;
    private boolean taskCreateWizard;
    private TaskConfigurationWizardPageTask taskEditPage;

    public TaskConfigurationWizardDialog(IWorkbenchWindow window, TaskConfigurationWizard wizard) {
        this(window, wizard, null);
    }

    public TaskConfigurationWizardDialog(IWorkbenchWindow window, TaskConfigurationWizard wizard, IStructuredSelection selection) {
        super(window, wizard, selection);
    }

    TaskConfigurationWizardDialog(IWorkbenchWindow window) {
        this(window, new TaskConfigurationWizardStub(), null);
    }

    @Override
    protected TaskConfigurationWizard getWizard() {
        return (TaskConfigurationWizard) super.getWizard();
    }

    @Override
    public int getShellStyle() {
        int shellStyle = SWT.CLOSE | SWT.MAX | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.RESIZE | getDefaultOrientation();
        if (UIUtils.isInDialog()) {
            shellStyle |= SWT.APPLICATION_MODAL;
        }
        return shellStyle;
    }

    protected TaskConfigurationWizard getTaskWizard() {
        return (TaskConfigurationWizard) super.getWizard();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setHelpAvailable(false);

        return super.createDialogArea(parent);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        {
            /*if (!getWizard().isTaskEditor()) */{
                parent.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

                Button saveAsTaskButton = createButton(parent, SAVE_TASK_BTN_ID, "Save", false);
                //saveAsTaskButton.setImage(DBeaverIcons.getImage(UIIcon.SAVE_AS));

                Label spacer = new Label(parent, SWT.NONE);
                spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

                ((GridLayout) parent.getLayout()).numColumns += 1;
            }
        }

        super.createButtonsForButtonBar(parent);
        Button cancelButton = getButton(IDialogConstants.CANCEL_ID);
        cancelButton.setText(IDialogConstants.CLOSE_LABEL);
        Button finishButton = getButton(IDialogConstants.FINISH_ID);
        finishButton.setText(UIMessages.button_start);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == SAVE_TASK_BTN_ID) {
            getWizard().saveTask();
            return;
        } else if (buttonId == IDialogConstants.NEXT_ID &&
            getWizard() instanceof TaskConfigurationWizardStub &&
            getCurrentPage() instanceof TaskConfigurationWizardPageTask)
        {
            taskCreateWizard = true;
            taskEditPage = (TaskConfigurationWizardPageTask) getCurrentPage();
            if (nestedTaskWizard == null) {
                // Now we need to create real wizard, initialize it and inject in this dialog
                try {
                    nestedTaskWizard = taskEditPage.getTaskWizard();
                    nestedTaskWizard.addPages();
                    setWizard(nestedTaskWizard);

                } catch (Exception e) {
                    setErrorMessage("Configuration error: " + e.getMessage());
                    log.error("Can't create task " + taskEditPage.getSelectedTaskType().getName() + " configuration wizard", e);
                    return;
                }
            }
            // Show first page of new wizard
            showPage(nestedTaskWizard.getStartingPage());
            return;
        }
        super.buttonPressed(buttonId);
    }

    @Override
    public void updateButtons() {
        super.updateButtons();
        Button saveAsButton = getButton(SAVE_TASK_BTN_ID);
        if (saveAsButton != null) {
            saveAsButton.setEnabled(getTaskWizard() != null && getTaskWizard().isTaskEditor() || getWizard().canFinish());
        }
    }

    @Override
    protected Control createContents(Composite parent) {
        return super.createContents(parent);
    }

/*
    private void saveConfigurationAsTask() {
        TaskConfigurationWizard taskWizard = getTaskWizard();
        DBTTaskManager taskManager = taskWizard.getProject().getTaskManager();
        DBTTask currentTask = taskWizard.getCurrentTask();

        Map<String, Object> state = new LinkedHashMap<>();
        taskWizard.saveTaskState(state);

        EditTaskConfigurationDialog dialog;
        if (currentTask != null) {
            currentTask.setProperties(state);
            dialog = new EditTaskConfigurationDialog(getShell(), currentTask);
        } else {
            DBTTaskType taskType = taskManager.getRegistry().getTask(taskWizard.getTaskTypeId());
            if (taskType == null) {
                DBWorkbench.getPlatformUI().showError("Create task", "Task type " + taskWizard.getTaskTypeId() + " not found");
                return;
            }
            dialog = new EditTaskConfigurationDialog(getShell(), taskWizard.getProject(), taskType, state);
        }
        if (dialog.open() == IDialogConstants.OK_ID) {
            DBTTask task = dialog.getTask();
            taskWizard.setCurrentTask(task);
            try {
                taskManager.updateTaskConfiguration(task);
            } catch (Exception e) {
                DBWorkbench.getPlatformUI().showError("Save task", "Error saving task " + task.getName() + "", e);
            }
        }
    }
*/

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

}
