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

package org.jkiss.dbeaver.tasks.ui.wizard;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.internal.UIMessages;

/**
 * Task configuration wizard dialog
 */
public class TaskConfigurationWizardDialog extends ActiveWizardDialog {

    private static final Log log = Log.getLog(TaskConfigurationWizardDialog.class);
    private TaskConfigurationWizard nestedTaskWizard;
    private TaskConfigurationWizardPageTask taskEditPage;

    public TaskConfigurationWizardDialog(IWorkbenchWindow window, TaskConfigurationWizard wizard) {
        this(window, wizard, null);
    }

    public TaskConfigurationWizardDialog(IWorkbenchWindow window, TaskConfigurationWizard wizard, IStructuredSelection selection) {
        super(window, wizard, selection);
        setFinishButtonLabel(UIMessages.button_start);
    }

    public TaskConfigurationWizardDialog(IWorkbenchWindow window) {
        this(window, new TaskConfigurationWizardStub(), null);
    }

    @Override
    protected boolean isModalWizard() {
        return false;
    }

    @Override
    protected TaskConfigurationWizard getWizard() {
        return (TaskConfigurationWizard) super.getWizard();
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
            parent.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

            //createTaskSaveButtons(parent, 1);

            Label spacer = new Label(parent, SWT.NONE);
            spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

            ((GridLayout) parent.getLayout()).numColumns += 1;
        }

        super.createButtonsForButtonBar(parent);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.NEXT_ID &&
            getWizard() instanceof TaskConfigurationWizardStub &&
            ((TaskConfigurationWizardStub)getWizard()).isLastTaskPreconfigPage(getCurrentPage()))
        {
            taskEditPage = getTaskPage();
            try {
                TaskConfigurationWizard nextTaskWizard = taskEditPage.getTaskWizard();
                if (nextTaskWizard != nestedTaskWizard) {
                    // Now we need to create real wizard, initialize it and inject in this dialog
                        nestedTaskWizard = nextTaskWizard;
                        nestedTaskWizard.addPages();
                        setWizard(nestedTaskWizard);
                }
            } catch (Exception e) {
                setErrorMessage("Configuration error: " + e.getMessage());
                log.error("Can't create task " + taskEditPage.getSelectedTaskType().getName() + " configuration wizard", e);
                return;
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
        //updateSaveTaskButtons();
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

}
