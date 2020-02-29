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
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.registry.task.TaskImpl;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Create task dialog
 */
public class EditTaskConfigurationDialog extends BaseDialog
{
    private static final String DIALOG_ID = "DBeaver.EditTaskConfigurationDialog";//$NON-NLS-1$

    private TaskImpl task;
    private final DBPProject project;
    private DBTTaskType taskType;
    private Map<String, Object> state;

    private Combo taskLabelCombo;
    private Text taskDescriptionText;
    private DBTTask[] allTasks;

    public EditTaskConfigurationDialog(Shell parentShell, @NotNull DBTTask task)
    {
        super(parentShell,
            "Edit task [" + task.getName() + "]",
            task.getType().getIcon() == null ? DBIcon.TREE_TASK: task.getType().getIcon());
        this.task = (TaskImpl) task;
        this.project = task.getProject();
        this.taskType = task.getType();
        this.state = task.getProperties();
    }

    public EditTaskConfigurationDialog(Shell parentShell, @NotNull DBPProject project, @NotNull DBTTaskType taskType)
    {
        super(parentShell, "Create task", DBIcon.TREE_TASK);
        this.task = null;
        this.project = project;
        this.taskType = taskType;
        this.state = new LinkedHashMap<>();
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        Composite composite = super.createDialogArea(parent);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite formPanel = UIUtils.createComposite(composite, 2);
        formPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

        ModifyListener modifyListener = e -> {
            updateButtons();
        };

        UIUtils.createLabelText(formPanel, "Type", taskType.getCategory().getName() + " / " + taskType.getName(), SWT.BORDER | SWT.READ_ONLY);

        boolean taskSaved = task != null && !CommonUtils.isEmpty(task.getId());
        taskLabelCombo = UIUtils.createLabelCombo(formPanel, "Name", "", SWT.BORDER | (taskSaved ? SWT.READ_ONLY : SWT.NONE));
        ((GridData)taskLabelCombo.getLayoutData()).widthHint = 300;
        if (task != null) {
            taskLabelCombo.setText(task.getName());
        } else {
            taskLabelCombo.add("");
            DBTTaskManager taskManager = project.getTaskManager();
            allTasks = taskManager.getAllTaskByType(taskType);
            for (DBTTask tc : allTasks) {
                taskLabelCombo.add(tc.getName());
            }

/*
            taskLabelCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int selectionIndex = taskLabelCombo.getSelectionIndex();
                    if (selectionIndex == 0) {
                        task = null;
                        setTitle("Create task " + taskType.getName());
                    } else {
                        task = (TaskImpl) allTasks[selectionIndex - 1];
                        taskDescriptionText.setText(CommonUtils.notEmpty(task.getDescription()));
                        setTitle("Edit task " + task.getName());
                    }
                }
            });
*/
        }

        taskLabelCombo.addModifyListener(modifyListener);
        if (taskSaved) {
            taskLabelCombo.setEnabled(false);
        }

//        if (!CommonUtils.isEmpty(task.getId())) {
//            UIUtils.createLabelText(formPanel, "ID", task.getId(), SWT.BORDER | SWT.READ_ONLY);
//        }

        taskDescriptionText = UIUtils.createLabelText(formPanel, "Description", task == null ? "" : CommonUtils.notEmpty(task.getDescription()), SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        ((GridData)taskDescriptionText.getLayoutData()).heightHint = taskDescriptionText.getLineHeight() * 5;
        taskDescriptionText.addModifyListener(modifyListener);

        UIUtils.asyncExec(() -> taskLabelCombo.setFocus());

        return composite;
    }

    private boolean isTaskEditor() {
        return task != null;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        updateButtons();
    }

    private void updateButtons() {
        boolean isReady = !taskLabelCombo.getText().isEmpty();// && !taskDescriptionText.getText().isEmpty();
        getButton(IDialogConstants.OK_ID).setEnabled(isReady);
    }

    @Override
    protected void okPressed() {
        DBTTaskManager taskManager = project.getTaskManager();
        try {
            if (task == null) {
                task = (TaskImpl) taskManager.createTask(taskType, taskLabelCombo.getText(), taskDescriptionText.getText(), state);
            }
            task.setName(taskLabelCombo.getText());
            task.setDescription(taskDescriptionText.getText());
            task.setUpdateTime(new Date());
            task.setProperties(state);
            taskManager.updateTaskConfiguration(task);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Create task", "Error creating data transfer task", e);
            return;
        }

        super.okPressed();
    }

    public DBTTask getTask() {
        return task;
    }
}
