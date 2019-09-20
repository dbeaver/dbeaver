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
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
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
import java.util.Map;

/**
 * Create task dialog
 */
public class EditTaskConfigurationDialog extends BaseDialog
{
    private static final String DIALOG_ID = "DBeaver.EditTaskConfigurationDialog";//$NON-NLS-1$

    private final DBPProject project;
    private final DBTTaskType taskDescriptor;
    private DBTTask configuration;
    private Map<String, Object> state;

    private Combo taskLabelCombo;
    private Text taskDescriptionText;
    private DBTTask[] allTasks;

    public EditTaskConfigurationDialog(Shell parentShell, DBPProject project, DBTTaskType taskDescriptor, Map<String, Object> state)
    {
        super(parentShell, "Create new task " + taskDescriptor.getName(), DBIcon.TREE_PACKAGE);
        this.project = project;
        this.taskDescriptor = taskDescriptor;
        this.state = state;
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

        Composite labelPanel = UIUtils.createComposite(composite, 2);
        labelPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        UIUtils.createLabelText(labelPanel, "Type", taskDescriptor.getName(), SWT.BORDER | SWT.READ_ONLY);

        taskLabelCombo = UIUtils.createLabelCombo(labelPanel, "Task", "", SWT.BORDER);
        ModifyListener modifyListener = e -> {
            updateButtons();
        };
        taskLabelCombo.add("");
        DBTTaskManager taskManager = project.getTaskManager();
        allTasks = taskManager.getTaskConfigurations(taskDescriptor);
        for (DBTTask tc : allTasks) {
            taskLabelCombo.add(tc.getLabel());
        }

        taskLabelCombo.addModifyListener(modifyListener);
        taskLabelCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectionIndex = taskLabelCombo.getSelectionIndex();
                if (selectionIndex == 0) {
                    configuration = null;
                    setTitle("Create task " + taskDescriptor.getName());
                } else {
                    configuration = allTasks[selectionIndex - 1];
                    taskDescriptionText.setText(CommonUtils.notEmpty(configuration.getDescription()));
                    setTitle("Edit task " + configuration.getLabel());
                }
            }
        });

        taskDescriptionText = UIUtils.createLabelText(composite, "Description", "", SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        ((GridData)taskDescriptionText.getLayoutData()).heightHint = taskDescriptionText.getLineHeight() * 5;
        taskDescriptionText.addModifyListener(modifyListener);

        UIUtils.asyncExec(() -> taskLabelCombo.setFocus());

        return composite;
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
        if (configuration == null) {
            try {
                configuration = taskManager.createTaskConfiguration(taskDescriptor, taskLabelCombo.getText(), taskDescriptionText.getText(), state);
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Create task", "Error creating data transfer task", e);
            }
        } else {
            TaskImpl impl = (TaskImpl) configuration;
            impl.setLabel(taskLabelCombo.getText());
            impl.setDescription(taskDescriptionText.getText());
            impl.setUpdateTime(new Date());
            impl.setProperties(state);
            taskManager.updateTaskConfiguration(configuration);
        }

        super.okPressed();
    }

    public DBTTask getTaskConfiguration() {
        return configuration;
    }
}
