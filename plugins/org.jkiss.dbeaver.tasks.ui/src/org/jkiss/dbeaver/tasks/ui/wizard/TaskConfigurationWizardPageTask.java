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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.registry.task.TaskImpl;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Create task wizard page
 */
class TaskConfigurationWizardPageTask extends ActiveWizardPage
{
    private static final Log log = Log.getLog(TaskConfigurationWizardPageTask.class);

    private final DBPProject selectedProject;
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

    private TaskImpl task;

    private Map<DBTTaskType, TaskConfigurationWizard> taskWizards = new HashMap<>();

    TaskConfigurationWizardPageTask(DBTTask task)
    {
        super(task == null ? "Create new task" : "Edit task");
        setTitle(task == null ? "New task properties" : "Edit task properties");
        setDescription("Set task name, type and input data");

        this.task = (TaskImpl) task;
        if (this.task != null) {
            this.taskName = this.task.getName();
            this.taskDescription = this.task.getDescription();
            this.selectedTaskType = this.task.getType();
            this.selectedCategory = selectedTaskType.getCategory();
        }
        this.selectedProject = NavigatorUtils.getSelectedProject();
    }

    @Override
    public TaskConfigurationWizard getWizard() {
        return (TaskConfigurationWizard)super.getWizard();
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
    public boolean canFlipToNextPage() {
        return isPageComplete();
    }

    @Override
    public void createControl(Composite parent) {
        boolean taskSaved = task != null && !CommonUtils.isEmpty(task.getId());

        SashForm formSash = new SashForm(parent, SWT.HORIZONTAL);
        formSash.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Composite formPanel = UIUtils.createControlGroup(formSash, "Task info", 2, GridData.FILL_BOTH, 0);
            formPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

            ModifyListener modifyListener = e -> updatePageCompletion();

            if (task == null) {
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
                            newTaskType = taskTypes[taskTypeCombo.getSelectionIndex()];
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
            } else {
                UIUtils.createLabelText(formPanel, "Category", task.getType().getCategory().getName(), SWT.BORDER | SWT.READ_ONLY);
                UIUtils.createLabelText(formPanel, "Type", task.getType().getName(), SWT.BORDER | SWT.READ_ONLY);
            }

            taskLabelText = UIUtils.createLabelText(formPanel, "Name", task == null ? "" : CommonUtils.notEmpty(task.getName()), SWT.BORDER);
            if (taskSaved) {
                taskLabelText.setEditable(false);
                //taskLabelText.setEnabled(false);
            }
            taskLabelText.addModifyListener(e -> {
                taskName = taskLabelText.getText();
                modifyListener.modifyText(e);
            });

            UIUtils.createControlLabel(formPanel, "Description").setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            taskDescriptionText = new Text(formPanel, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
            taskDescriptionText.setText(task == null ? "" : CommonUtils.notEmpty(task.getDescription()));
            taskDescriptionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            ((GridData) taskDescriptionText.getLayoutData()).heightHint = taskDescriptionText.getLineHeight() * 5;
            taskDescriptionText.addModifyListener(e -> {
                taskDescription = taskDescriptionText.getText();
                modifyListener.modifyText(e);
            });

            if (task != null && !CommonUtils.isEmpty(task.getId())) {
                UIUtils.createLabelText(formPanel, "ID", task.getId(), SWT.BORDER | SWT.READ_ONLY);
            }

            UIUtils.asyncExec(() -> (taskSaved ? taskDescriptionText : taskLabelText).setFocus());
        }
        {
            configPanelPlaceholder = UIUtils.createComposite(formSash, 1);
            if (task != null)
                updateTaskTypeSelection();
        }
        formSash.setWeights(new int[] { 500, 500 });

        setControl(formSash);
    }

    private void updateTaskTypeSelection() {
        UIUtils.disposeChildControls(configPanelPlaceholder);
        taskConfigPanel = null;
        if (task != null) {
            if (task.getType() != selectedTaskType) {
                task.setType(selectedTaskType);
                task.setProperties(new LinkedHashMap<>());
            }
        }

        if (selectedCategory != null && selectedCategory.supportsConfigurator()) {
            try {
                DBTTaskConfigurator configurator = selectedCategory.createConfigurator();
                DBTTaskConfigPanel configPage = configurator.createInputConfigurator(UIUtils.getDefaultRunnableContext(), selectedTaskType);
                if (configPage != null) {
                    taskConfigPanel = configPage;
                    taskConfigPanel.createControl(configPanelPlaceholder, getTaskWizard(), this::updatePageCompletion);
                    if (task != null) {
                        taskConfigPanel.loadSettings();
                        updatePageCompletion();
                    }
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
            item.setImage(DBeaverIcons.getImage(cat.getIcon() == null ? DBIcon.TREE_TASK : cat.getIcon()));
            item.setData(cat);
            addTaskCategories(item, cat.getChildren());
            item.setExpanded(true);
        }
    }

    @Override
    protected boolean determinePageCompletion() {
        return selectedTaskType != null &&
            ((task != null && !CommonUtils.isEmpty(task.getName())) || !CommonUtils.isEmpty(taskName)) &&
            (taskConfigPanel == null || taskConfigPanel.isComplete());
    }

    public TaskConfigurationWizard getTaskWizard() throws DBException {
        if (!(getWizard() instanceof TaskConfigurationWizardStub)) {
            // We already have it
            return getWizard();
        }
        TaskConfigurationWizard realWizard = taskWizards.get(selectedTaskType);
        if (realWizard == null) {
            DBTTaskConfigurator configurator = selectedCategory.createConfigurator();

            if (task == null) {
                task = (TaskImpl) selectedProject.getTaskManager().createTask(selectedTaskType, CommonUtils.notEmpty(taskName), taskDescription, new LinkedHashMap<>());
            }
            realWizard = (TaskConfigurationWizard) configurator.createTaskConfigWizard(task);
            IWorkbenchWindow workbenchWindow = UIUtils.getActiveWorkbenchWindow();
            IWorkbenchPart activePart = workbenchWindow.getActivePage().getActivePart();
            ISelection selection = activePart == null || activePart.getSite() == null || activePart.getSite().getSelectionProvider() == null ?
                null : activePart.getSite().getSelectionProvider().getSelection();
            realWizard.setContainer(getContainer());
            realWizard.init(workbenchWindow.getWorkbench(), selection instanceof IStructuredSelection ? (IStructuredSelection) selection : null);
            taskConfigPanel.saveSettings();

            taskWizards.put(selectedTaskType, realWizard);
        }
        return realWizard;
    }

    public void saveSettings() {
        if (task != null) {
            task.setName(taskLabelText.getText());
            task.setDescription(taskDescriptionText.getText());
            task.setType(selectedTaskType);
            if (taskConfigPanel != null) {
                taskConfigPanel.saveSettings();
            }
        }
    }

}
