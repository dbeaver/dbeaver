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
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.util.LinkedHashMap;

/**
 * Create task dialog
 */
public class CreateTaskConfigurationDialog extends BaseDialog
{
    private static final String DIALOG_ID = "DBeaver.CreateTaskConfigurationDialog";//$NON-NLS-1$

    private final DBPProject project;

    private Combo taskTypeCombo;
    private Text taskLabelText;
    private Text taskDescriptionText;
    private Tree tastCategoryTree;

    private DBTTaskCategory selectedCategory;
    private DBTTaskType selectedTaskType;
    private DBTTaskType[] taskTypes;


    public CreateTaskConfigurationDialog(Shell parentShell, DBPProject project)
    {
        super(parentShell, "Create new task ", DBIcon.TREE_PACKAGE);
        this.project = project;
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

        UIUtils.createControlLabel(formPanel, "Category");
        tastCategoryTree = new Tree(formPanel, SWT.BORDER | SWT.SINGLE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 100;
        gd.widthHint = 200;
        tastCategoryTree.setLayoutData(gd);
        tastCategoryTree.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                TreeItem[] selection = tastCategoryTree.getSelection();
                if (selection.length == 1) {
                    selectedCategory = (DBTTaskCategory) selection[0].getData();
                    taskTypeCombo.removeAll();
                    taskTypes = selectedCategory.getTaskTypes();
                    for (DBTTaskType type : taskTypes) {
                        taskTypeCombo.add(type.getName());
                    }
                    if (taskTypes.length > 0) {
                        taskTypeCombo.select(0);
                        selectedTaskType = taskTypes[0];
                    } else {
                        selectedTaskType = null;
                    }
                    updateButtons();
                }
            }
        });
        addTaskCategories(null, TaskRegistry.getInstance().getRootCategories());

        taskTypeCombo = UIUtils.createLabelCombo(formPanel, "Type", SWT.BORDER  | SWT.DROP_DOWN | SWT.READ_ONLY);
        taskTypeCombo.addModifyListener(modifyListener);
        taskTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (taskTypeCombo.getSelectionIndex() >= 0) {
                    selectedTaskType = taskTypes[taskTypeCombo.getSelectionIndex()];
                } else {
                    selectedTaskType = null;
                }
                updateButtons();
            }
        });

        taskLabelText = UIUtils.createLabelText(formPanel, "Name", "", SWT.BORDER);
        taskLabelText.addModifyListener(modifyListener);

        taskDescriptionText = UIUtils.createLabelText(formPanel, "Description", "", SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        ((GridData)taskDescriptionText.getLayoutData()).heightHint = taskDescriptionText.getLineHeight() * 5;
        taskDescriptionText.addModifyListener(modifyListener);

        UIUtils.asyncExec(() -> taskLabelText.setFocus());

        return composite;
    }

    private void addTaskCategories(TreeItem parentItem, DBTTaskCategory[] categories) {
        for (DBTTaskCategory cat : categories) {
            TreeItem item = parentItem == null ? new TreeItem(tastCategoryTree, SWT.NONE) : new TreeItem(parentItem, SWT.NONE);
            item.setText(cat.getName());
            item.setImage(DBeaverIcons.getImage(cat.getIcon() == null ? DBIcon.TREE_PACKAGE : cat.getIcon()));
            item.setData(cat);
            addTaskCategories(item, cat.getChildren());
            item.setExpanded(true);
        }
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        updateButtons();
    }

    private void updateButtons() {
        boolean isReady = !taskLabelText.getText().isEmpty() && selectedTaskType != null;
        getButton(IDialogConstants.OK_ID).setEnabled(isReady);
    }

    @Override
    protected void okPressed() {
        DBTTaskManager taskManager = project.getTaskManager();

        try {
            DBTTaskConfigurator configurator = selectedCategory.createConfigurator();
            DBTTask task = taskManager.createTaskConfiguration(selectedTaskType, taskLabelText.getText(), taskDescriptionText.getText(), new LinkedHashMap<>());
            if (!configurator.configureTask(DBWorkbench.getPlatform(), task)) {
                taskManager.deleteTaskConfiguration(task);
            }
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Create task failed", "Error while creating new task", e);
            return;
        }

        super.okPressed();
    }

}
