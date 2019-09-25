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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.task.DBTTaskCategory;
import org.jkiss.dbeaver.model.task.DBTTaskConfigPanel;
import org.jkiss.dbeaver.model.task.DBTTaskConfigurator;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Create task wizard page
 */
class TaskConfigurationCreatePage extends ActiveWizardPage
{
    private Combo taskTypeCombo;
    private Text taskLabelText;
    private Text taskDescriptionText;
    private Tree taskCategoryTree;

    private DBTTaskType[] taskTypes;

    private Composite configPanelPlaceholder;
    private DBTTaskConfigPanel taskConfigPanel;

    private DBTTaskCategory selectedCategory;
    private DBTTaskType selectedTaskType;
    private String taskName;
    private String taskDescription;
    private Map<String, Object> initialProperties = new LinkedHashMap<>();

    TaskConfigurationCreatePage()
    {
        super("Create new task");
        setTitle("New task properties");
        setDescription("Set task name, type and input data");
        setPageComplete(false);
    }

    public DBTTaskCategory getSelectedCategory() {
        return selectedCategory;
    }

    public DBTTaskType getSelectedTaskType() {
        return selectedTaskType;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public Map<String, Object> getInitialProperties() {
        return initialProperties;
    }

    @Override
    public boolean isPageComplete() {
        return selectedTaskType != null && !CommonUtils.isEmpty(taskName);
    }

    @Override
    public boolean canFlipToNextPage() {
        return isPageComplete();
    }

    @Override
    public void createControl(Composite parent) {
        SashForm formSash = new SashForm(parent, SWT.HORIZONTAL);
        formSash.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Composite formPanel = UIUtils.createControlGroup(formSash, "Task info", 2, GridData.FILL_BOTH, 0);
            formPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

            ModifyListener modifyListener = e -> getWizard().getContainer().updateButtons();

            UIUtils.createControlLabel(formPanel, "Category");
            taskCategoryTree = new Tree(formPanel, SWT.BORDER | SWT.SINGLE);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 100;
            gd.widthHint = 200;
            taskCategoryTree.setLayoutData(gd);
            taskCategoryTree.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    TreeItem[] selection = taskCategoryTree.getSelection();
                    if (selection.length == 1) {
                        if (selectedCategory == selection[0].getData()) {
                            return;
                        }
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
                        updateTaskTypeSelection();
                    }
                }
            });
            addTaskCategories(null, TaskRegistry.getInstance().getRootCategories());

            taskTypeCombo = UIUtils.createLabelCombo(formPanel, "Type", SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            taskTypeCombo.addModifyListener(modifyListener);
            taskTypeCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DBTTaskType newTaskType;
                    if (taskTypeCombo.getSelectionIndex() >= 0) {
                        newTaskType= taskTypes[taskTypeCombo.getSelectionIndex()];
                    } else {
                        newTaskType = null;
                    }
                    if (selectedTaskType == newTaskType) {
                        return;
                    }
                    selectedTaskType = newTaskType;
                    updateTaskTypeSelection();
                }
            });

            taskLabelText = UIUtils.createLabelText(formPanel, "Name", "", SWT.BORDER);
            taskLabelText.addModifyListener(e -> {
                taskName = taskLabelText.getText();
                modifyListener.modifyText(e);
            });

            taskDescriptionText = UIUtils.createLabelText(formPanel, "Description", "", SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
            ((GridData) taskDescriptionText.getLayoutData()).heightHint = taskDescriptionText.getLineHeight() * 5;
            taskDescriptionText.addModifyListener(e -> {
                taskDescription = taskDescriptionText.getText();
                modifyListener.modifyText(e);
            });

            UIUtils.asyncExec(() -> taskLabelText.setFocus());
        }
        {
            configPanelPlaceholder = UIUtils.createComposite(formSash, 1);
        }
        formSash.setWeights(new int[] { 500, 500 });

        setControl(formSash);
    }

    private void updateTaskTypeSelection() {
        UIUtils.disposeChildControls(configPanelPlaceholder);
        taskConfigPanel = null;

        if (selectedCategory != null && selectedCategory.supportsConfigurator()) {
            try {
                DBTTaskConfigurator configurator = selectedCategory.createConfigurator();
                DBTTaskConfigPanel configPage = configurator.createInputConfigurator(UIUtils.getDefaultRunnableContext(), selectedTaskType);
                if (configPage != null) {
                    taskConfigPanel = configPage;
                    taskConfigPanel.createControl(configPanelPlaceholder);
                } else {
                    // Something weird was created
                    UIUtils.disposeChildControls(configPanelPlaceholder);
                }
            } catch (Exception e) {
                DBWorkbench.getPlatformUI().showError("Task configurator error", "Error creating task configuration UI", e);
            }
        }
        if (taskConfigPanel == null) {
            Group group = UIUtils.createControlGroup(configPanelPlaceholder, "", 1, GridData.FILL_BOTH, 0);
            group.setLayoutData(new GridData(GridData.FILL_BOTH));
            Label emptyLabel = new Label(group, SWT.NONE);
            emptyLabel.setText("No configuration");
            GridData gd = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.HORIZONTAL_ALIGN_CENTER);
            gd.grabExcessHorizontalSpace = true;
            gd.grabExcessVerticalSpace = true;
            emptyLabel.setLayoutData(gd);
        }
        getShell().layout(true, true);
        getWizard().getContainer().updateButtons();
    }

    private void addTaskCategories(TreeItem parentItem, DBTTaskCategory[] categories) {
        for (DBTTaskCategory cat : categories) {
            TreeItem item = parentItem == null ? new TreeItem(taskCategoryTree, SWT.NONE) : new TreeItem(parentItem, SWT.NONE);
            item.setText(cat.getName());
            item.setImage(DBeaverIcons.getImage(cat.getIcon() == null ? DBIcon.TREE_PACKAGE : cat.getIcon()));
            item.setData(cat);
            addTaskCategories(item, cat.getChildren());
            item.setExpanded(true);
        }
    }

}
